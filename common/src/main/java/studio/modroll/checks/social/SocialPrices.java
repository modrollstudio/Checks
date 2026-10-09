package studio.modroll.checks.social;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * Social price changes, applied the way vanilla applies reputation: as each trade's special price while a
 * player trades, taken back when they stop. Called by each loader's mixin as a trader's trading player
 * changes; villagers add their own reputation prices first.
 */
public final class SocialPrices {

    /** What Checks added to each offer, per trader, until its trading player changes. */
    private static final Map<AbstractVillager, Map<MerchantOffer, Integer>> APPLIED = new WeakHashMap<>();

    private SocialPrices() {}

    /** Before the trading player changes: takes back what was added for the last one. */
    public static void beforeTradingPlayer(AbstractVillager trader) {
        Map<MerchantOffer, Integer> applied = APPLIED.remove(trader);
        if (applied != null) {
            applied.forEach((offer, diff) -> offer.addToSpecialPriceDiff(-diff));
        }
    }

    /** After it changed: adds the new trading player's price change to every offer; {@code null} when trading stopped. */
    public static void afterTradingPlayer(AbstractVillager trader, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || trader.level().isClientSide()) {
            return;
        }
        double percent = percent(trader, serverPlayer);
        if (percent == 0) {
            return;
        }
        Map<MerchantOffer, Integer> applied = new IdentityHashMap<>();
        for (MerchantOffer offer : trader.getOffers()) {
            int diff = SocialRules.priceDiff(offer.getBaseCostA().getCount(), percent);
            if (diff != 0) {
                offer.addToSpecialPriceDiff(diff);
                applied.put(offer, diff);
            }
        }
        APPLIED.put(trader, applied);
    }

    /** The player's passive price shift at a villager plus every price change a switched-on action left. */
    public static double percent(AbstractVillager trader, ServerPlayer player) {
        ScoresConfig.SocialSettings settings = ScoresRuntime.config().social();
        if (!settings.enabled()) {
            return 0;
        }
        double fromActions = SocialMemory.get(player.server)
                .priceChanges(player.getUUID(), trader.getUUID(), Socials.now(player.server))
                .entrySet()
                .stream()
                .filter(change -> settings.active(change.getKey()))
                .mapToDouble(Map.Entry::getValue)
                .sum();
        return fromActions + passivePercent(trader, player, settings.passivePrices());
    }

    /** CHA at every villager, plus the profession's skill; wandering traders have no profession and shift nothing. */
    private static double passivePercent(
            AbstractVillager trader, ServerPlayer player, ScoresConfig.PassivePriceSettings settings) {
        if (!settings.enabled() || !(trader instanceof Villager villager)) {
            return 0;
        }
        double charisma = SocialRules.passivePercent(
                ChecksApi.abilityModifier(player, Ability.CHARISMA), settings.charismaPercentPerPoint());
        return charisma + professionPercent(villager, player, settings);
    }

    private static double professionPercent(
            Villager villager, ServerPlayer player, ScoresConfig.PassivePriceSettings settings) {
        ResourceLocation profession = BuiltInRegistries.VILLAGER_PROFESSION.getKey(
                villager.getVillagerData().getProfession());
        ScoresConfig.ProfessionPrice price = settings.professions().get(profession);
        if (price == null) {
            return 0;
        }
        return ChecksApi.skill(price.skill())
                .map(skill ->
                        SocialRules.passivePercent(ChecksApi.skillModifier(player, skill), price.percentPerPoint()))
                .orElse(0.0);
    }

    public static void clear() {
        APPLIED.clear();
    }
}
