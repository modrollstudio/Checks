package studio.modroll.checks.trigger;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.CheckRoll;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.check.RollMessages;
import studio.modroll.checks.check.RollText;
import studio.modroll.checks.data.MatchEntry;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.critfall.api.combat.SaveResult;

/**
 * Fires the loaded check triggers from the loaders' interaction hooks. Block and entity triggers answer
 * the main hand only, so one click rolls once. A trigger that fires consumes the interaction; one on
 * cooldown, or with a canceled roll, leaves it to vanilla.
 */
public final class CheckTriggers {

    private static final TriggerCooldowns COOLDOWNS = new TriggerCooldowns();
    private static volatile List<CheckTrigger> triggers = List.of();

    private CheckTriggers() {}

    public static void set(Map<ResourceLocation, CheckTrigger> loaded) {
        triggers = loaded.values().stream()
                .sorted(Comparator.comparing(CheckTrigger::id))
                .toList();
    }

    /** In id order. */
    public static List<CheckTrigger> all() {
        return triggers;
    }

    /** Also forgets every cooldown. */
    public static void clear() {
        triggers = List.of();
        COOLDOWNS.clear();
    }

    public static InteractionResult onUseBlock(ServerPlayer player, InteractionHand hand, BlockPos pos) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        BlockState state = player.level().getBlockState(pos);
        String target = player.level().dimension().location() + "@" + pos.toShortString();
        return fire(
                player,
                CheckTrigger.Interaction.USE_BLOCK,
                match -> match.matches(
                        BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                        tag -> state.is(TagKey.create(Registries.BLOCK, tag))),
                Vec3.atCenterOf(pos),
                Optional.of(target));
    }

    public static InteractionResult onUseEntity(ServerPlayer player, InteractionHand hand, Entity entity) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        return fire(
                player,
                CheckTrigger.Interaction.USE_ENTITY,
                match -> match.matches(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), tag -> entity.getType()
                        .is(TagKey.create(Registries.ENTITY_TYPE, tag))),
                entity.position(),
                Optional.of(entity.getStringUUID()));
    }

    public static InteractionResult onUseItem(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return InteractionResult.PASS;
        }
        return fire(
                player,
                CheckTrigger.Interaction.USE_ITEM,
                match -> match.matches(
                        BuiltInRegistries.ITEM.getKey(stack.getItem()),
                        tag -> stack.is(TagKey.create(Registries.ITEM, tag))),
                player.position(),
                Optional.empty());
    }

    /** Every matching trigger fires, in id order. */
    private static InteractionResult fire(
            ServerPlayer player,
            CheckTrigger.Interaction on,
            Predicate<MatchEntry> targets,
            Vec3 at,
            Optional<String> target) {
        if (!ScoresRuntime.config().extensions().triggers() || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        boolean fired = false;
        for (CheckTrigger trigger : triggers) {
            if (trigger.on() == on && targets.test(trigger.target())) {
                fired |= fire(trigger, player, at, target);
            }
        }
        return fired ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private static boolean fire(CheckTrigger trigger, ServerPlayer player, Vec3 at, Optional<String> target) {
        long now = player.server.getTickCount();
        if (!COOLDOWNS.ready(trigger.id(), player.getUUID(), target, now)) {
            return false;
        }
        CheckRoll roll = ChecksApi.check(player, trigger.stat(), trigger.dc(), trigger.mode());
        if (roll.canceled()) {
            return false;
        }
        SaveResult result = roll.result();
        COOLDOWNS.start(trigger, player.getUUID(), target, now);
        announce(player, trigger, result);
        trigger.action(trigger.outcome(result.natural(), result.saved())).ifPresent(action -> run(action, player, at));
        return true;
    }

    private static void announce(ServerPlayer player, CheckTrigger trigger, SaveResult result) {
        RollMessages.showResult(player, RollText.check(trigger.stat(), result));
    }

    /** As vanilla runs an advancement reward: the function as the player, at the target, at function level. */
    private static void run(CheckTrigger.Action action, ServerPlayer player, Vec3 at) {
        MinecraftServer server = player.server;
        CommandSourceStack source = player.createCommandSourceStack()
                .withPosition(at)
                .withSuppressedOutput()
                .withPermission(server.getFunctionCompilationLevel());
        action.function().ifPresent(id -> server.getFunctions()
                .get(id)
                .ifPresentOrElse(
                        function -> server.getFunctions().execute(function, source),
                        () -> Checks.LOG.warn("Check trigger function {} does not exist", id)));
        action.lootTable().ifPresent(id -> giveLoot(player, id, at));
    }

    private static void giveLoot(ServerPlayer player, ResourceLocation id, Vec3 at) {
        LootTable table =
                player.server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
        LootParams params = new LootParams.Builder(player.serverLevel())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.ORIGIN, at)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.ADVANCEMENT_REWARD);
        for (ItemStack stack : table.getRandomItems(params)) {
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
        }
        player.containerMenu.broadcastChanges();
    }
}
