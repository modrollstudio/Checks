package studio.modroll.checks.rest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.body.BodyRules;
import studio.modroll.checks.body.BodyValues;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.level.LevelStore;
import studio.modroll.checks.level.Levelling;
import studio.modroll.checks.level.PlayerLevel;
import studio.modroll.checks.score.ScoreService;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.text.FallbackText;
import studio.modroll.checks.trait.TraitUses;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.dice.DiceExpression;

/**
 * Hit Dice and rests. A player has one Hit Die per level, each the class's hit die. Using a lit campfire with an
 * empty hand starts a short rest; it breaks when the player takes damage, attacks, moves away, the fire goes out, a
 * monster comes near, or another mod says the player can't rest (meant for Initiative's fights). When it is done the
 * player spends Hit Dice one at a time until they are at full health or out of dice. Sleeping through the night is a
 * long rest: it heals fully, gives back the spent dice and readies once-a-day traits. Hit Dice roll through Critfall.
 */
public final class Rests {

    /** A short rest at {@code campfire}, started on server tick {@code start}. */
    private record Session(ResourceKey<Level> dimension, BlockPos campfire, long start) {}

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final List<Predicate<ServerPlayer>> BLOCKERS = new CopyOnWriteArrayList<>();
    private static final String KEY = "checks.rest.";

    private Rests() {}

    /** While {@code blocker} answers true for a player, they can't start a rest and a rest under way breaks. */
    public static void addBlocker(Predicate<ServerPlayer> blocker) {
        BLOCKERS.add(blocker);
    }

    /** One Hit Die per character level, less those spent; 0 while Hit Dice are off. */
    public static int hitDiceLeft(ServerPlayer player) {
        if (!settings().hitDice().enabled()) {
            return 0;
        }
        int spent = LevelStore.get(player.server).level(player.getUUID()).hitDiceSpent();
        return Math.max(0, Levelling.activeLevel(player).level() - spent);
    }

    /** Whether a short rest is under way. */
    public static boolean resting(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    /** Starts a short rest on an empty handed use of a lit campfire. */
    public static InteractionResult onUseBlock(ServerPlayer player, InteractionHand hand, BlockPos pos) {
        if (!shortRestsOn()
                || hand != InteractionHand.MAIN_HAND
                || player.isSpectator()
                || !player.getMainHandItem().isEmpty()
                || !CampfireBlock.isLitCampfire(player.level().getBlockState(pos))) {
            return InteractionResult.PASS;
        }
        if (resting(player)) {
            tell(player, FallbackText.of(KEY + "short.already"));
            return InteractionResult.SUCCESS;
        }
        Session session = new Session(player.level().dimension(), pos.immutable(), player.server.getTickCount());
        Optional<Component> problem = problem(player, session);
        if (problem.isPresent()) {
            tell(player, FallbackText.of(KEY + "short.cannot", problem.get()).withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        SESSIONS.put(player.getUUID(), session);
        int seconds = settings().shortRest().ticks() / SharedConstants.TICKS_PER_SECOND;
        tell(player, FallbackText.of(KEY + "short.started", seconds));
        return InteractionResult.SUCCESS;
    }

    /** Called once per server tick: ends rests that broke, and finishes those that have lasted. */
    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        for (Map.Entry<UUID, Session> entry : SESSIONS.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Session session = entry.getValue();
            Optional<Component> problem = player == null ? Optional.of(Component.empty()) : problem(player, session);
            if (problem.isPresent() || !shortRestsOn()) {
                SESSIONS.remove(entry.getKey());
                if (player != null && problem.isPresent()) {
                    broken(player, problem.get());
                }
            } else if (now - session.start() >= settings().shortRest().ticks()) {
                SESSIONS.remove(entry.getKey());
                finish(player);
            }
        }
    }

    /** Taking damage breaks the victim's rest; dealing it breaks the attacker's. */
    public static void onDamaged(LivingEntity victim, DamageSource source) {
        if (victim instanceof ServerPlayer player) {
            interrupt(player, FallbackText.of(KEY + "reason.hurt"));
        }
        if (source.getEntity() instanceof ServerPlayer attacker) {
            interrupt(attacker, FallbackText.of(KEY + "reason.attacked"));
        }
    }

    /** A melee swing at an entity breaks the attacker's rest, hit or miss. */
    public static void onAttack(ServerPlayer player) {
        interrupt(player, FallbackText.of(KEY + "reason.attacked"));
    }

    /**
     * Ends a short rest by spending Hit Dice one at a time, each rolled through Critfall and healing the roll plus the
     * CON modifier, until the player is at full health or out of dice; then says what it did in one line.
     */
    private static void finish(ServerPlayer player) {
        float before = player.getHealth();
        int spent = 0;
        while (hitDiceLeft(player) > 0 && player.getHealth() < player.getMaxHealth()) {
            int rolled = RollService.roll(DiceExpression.parse("1d" + BodyValues.hitDie(player)))
                    .total();
            int con = ScoreService.modifier(player, Ability.CONSTITUTION);
            ScoresConfig.HitDiceSettings hitDice = settings().hitDice();
            player.heal(BodyRules.hitDieHealing(rolled, con, hitDice.minHeal(), hitDice.healMultiplier()));
            LevelStore store = LevelStore.get(player.server);
            PlayerLevel level = store.level(player.getUUID());
            store.set(player.getUUID(), level.withHitDiceSpent(level.hitDiceSpent() + 1));
            spent++;
        }
        int healed = Math.round(player.getHealth() - before);
        if (spent > 0) {
            tell(player, FallbackText.of(KEY + (spent == 1 ? "short.spent_one" : "short.spent"), spent, healed));
        } else if (player.getHealth() >= player.getMaxHealth()) {
            tell(player, FallbackText.of(KEY + "short.full_health"));
        } else {
            tell(player, FallbackText.of(KEY + "short.no_dice"));
        }
    }

    /** Called just before a night is skipped: every player asleep in {@code level} takes a long rest. */
    public static void onNightSlept(ServerLevel level) {
        level.players().stream().filter(LivingEntity::isSleeping).forEach(Rests::longRest);
    }

    /**
     * Gives back {@code regain_fraction} of the level's Hit Dice, at least {@code min_regain}; heals fully and readies
     * once-a-day traits when those are on.
     */
    public static void longRest(ServerPlayer player) {
        ScoresConfig.LongRestSettings settings = settings().longRest();
        if (!settings.enabled()) {
            return;
        }
        SESSIONS.remove(player.getUUID());
        if (settings.healToFull()) {
            player.setHealth(player.getMaxHealth());
        }
        if (settings.rechargesTraits() && ScoresRuntime.config().traits().enabled()) {
            TraitUses.get(player.server).recharge(player.getUUID());
        }
        if (!settings().hitDice().enabled()) {
            return;
        }
        LevelStore store = LevelStore.get(player.server);
        PlayerLevel level = store.level(player.getUUID());
        int pool = Levelling.activeLevel(player).level();
        int regained = BodyRules.hitDiceRegained(pool, settings.regainFraction(), settings.minRegain());
        store.set(player.getUUID(), level.withHitDiceSpent(level.hitDiceSpent() - regained));
        tell(player, FallbackText.of(KEY + "long.done", hitDiceLeft(player), pool));
    }

    public static void clear() {
        SESSIONS.clear();
    }

    /** Why the player can't rest at the session's campfire right now; empty when they can. */
    private static Optional<Component> problem(ServerPlayer player, Session session) {
        ScoresConfig.ShortRestSettings settings = settings().shortRest();
        Vec3 fire = Vec3.atCenterOf(session.campfire());
        if (!player.isAlive()
                || player.level().dimension() != session.dimension()
                || player.position().distanceTo(fire) > settings.campfireRadius()) {
            return Optional.of(FallbackText.of(KEY + "reason.left"));
        }
        if (!CampfireBlock.isLitCampfire(player.level().getBlockState(session.campfire()))) {
            return Optional.of(FallbackText.of(KEY + "reason.fire_out"));
        }
        if (blocked(player)) {
            return Optional.of(FallbackText.of(KEY + "reason.blocked"));
        }
        if (monstersNear(player, fire, settings)) {
            return Optional.of(FallbackText.of(KEY + "reason.monsters"));
        }
        return Optional.empty();
    }

    /** As vanilla's bed: a monster that would stop the player sleeping. */
    private static boolean monstersNear(ServerPlayer player, Vec3 fire, ScoresConfig.ShortRestSettings settings) {
        AABB area = new AABB(fire, fire)
                .inflate(settings.monsterRange(), settings.monsterHeight(), settings.monsterRange());
        return !player.level()
                .getEntitiesOfClass(Monster.class, area, monster -> monster.isPreventingPlayerRest(player))
                .isEmpty();
    }

    private static boolean blocked(ServerPlayer player) {
        return BLOCKERS.stream().anyMatch(blocker -> blocker.test(player));
    }

    private static void interrupt(ServerPlayer player, Component reason) {
        if (SESSIONS.remove(player.getUUID()) != null) {
            broken(player, reason);
        }
    }

    private static void broken(ServerPlayer player, Component reason) {
        tell(player, FallbackText.of(KEY + "short.broken", reason).withStyle(ChatFormatting.RED));
    }

    private static void tell(ServerPlayer player, Component message) {
        player.sendSystemMessage(message);
    }

    private static boolean shortRestsOn() {
        return settings().shortRest().enabled() && settings().hitDice().enabled();
    }

    private static ScoresConfig.RestSettings settings() {
        return ScoresRuntime.config().rest();
    }
}
