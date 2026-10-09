package studio.modroll.checks.social;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.dice.DiceExpression;

/** What social outcomes do to the world. */
final class SocialEffects {

    private SocialEffects() {}

    /**
     * Price change and refusal on a trader. Only a villager also spreads gossip and calls iron golems: what
     * happens with a wandering trader stays between him and the player.
     */
    static void apply(ServerPlayer player, LivingEntity target, SocialAction action, SocialEffect effect) {
        SocialMemory memory = SocialMemory.get(player.server);
        long now = Socials.now(player.server);
        if (target instanceof AbstractVillager && effect.changesPrices()) {
            memory.changePrices(
                    action, player.getUUID(), target.getUUID(), effect.pricePercent(), now, effect.priceTicks());
        }
        if (target instanceof AbstractVillager) {
            memory.refuse(action, player.getUUID(), target.getUUID(), now, effect.refuseTicks());
        }
        if (!(target instanceof Villager villager)) {
            return;
        }
        effect.gossip().ifPresent(gossip -> {
            for (Villager listener : listeners(villager, gossip)) {
                listener.getGossips().add(player.getUUID(), gossip.type(), gossip.amount());
            }
        });
        if (effect.angersGolems()) {
            angerGolems(player, villager);
        }
    }

    /** The villager, or with {@code nearby} every villager within the gossip radius of it. */
    private static List<Villager> listeners(Villager villager, SocialEffect.Gossip gossip) {
        if (!gossip.nearby()) {
            return List.of(villager);
        }
        double radius = ScoresRuntime.config().social().gossipRadius();
        return villager.level()
                .getEntitiesOfClass(Villager.class, villager.getBoundingBox().inflate(radius));
    }

    /** Golems that may attack players, so never one a player built; any that turn raise the alarm. */
    private static void angerGolems(ServerPlayer player, LivingEntity around) {
        GolemAlarms.callGolems(player, around, radius());
    }

    /** The piglin, and when {@code nearby} every piglin around it, turn on the player for {@code ticks}. */
    static void angerPiglins(ServerPlayer player, Piglin piglin, boolean nearby, int ticks) {
        List<Piglin> piglins = nearby
                ? player.level()
                        .getEntitiesOfClass(
                                Piglin.class, piglin.getBoundingBox().inflate(radius()))
                : List.of(piglin);
        for (Piglin angered : piglins) {
            if (Sensor.isEntityAttackableIgnoringLineOfSight(angered, player)) {
                angered.getBrain().eraseMemory(MemoryModuleType.ADMIRING_ITEM);
                angered.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, player.getUUID(), ticks);
            }
        }
    }

    /** One of the trader's trade results, chosen with a die rolled through Critfall, into the player's inventory. */
    static ItemStack takeTradeItem(ServerPlayer player, AbstractVillager trader) {
        MerchantOffers offers = trader.getOffers();
        int roll = RollService.roll(DiceExpression.parse("1d" + offers.size())).total();
        ItemStack item = offers.get(roll - 1).getResult().copyWithCount(1);
        give(player, item.copy());
        return item;
    }

    /** One roll of the piglin bartering loot table, thrown to the player as a barter is. */
    static List<ItemStack> barter(ServerPlayer player, Piglin piglin) {
        LootParams params = new LootParams.Builder(player.serverLevel())
                .withParameter(LootContextParams.THIS_ENTITY, piglin)
                .create(LootContextParamSets.PIGLIN_BARTER);
        List<ItemStack> items = player.server
                .reloadableRegistries()
                .getLootTable(BuiltInLootTables.PIGLIN_BARTERING)
                .getRandomItems(params);
        for (ItemStack item : items) {
            BehaviorUtils.throwItem(piglin, item.copy(), player.position());
        }
        return items;
    }

    private static void give(ServerPlayer player, ItemStack item) {
        if (!player.addItem(item)) {
            player.drop(item, false);
        }
        player.containerMenu.broadcastChanges();
    }

    private static double radius() {
        return ScoresRuntime.config().social().nearbyRadius();
    }
}
