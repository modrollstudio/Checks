package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.loginWatched;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.profiles;
import static studio.modroll.checks.gametest.ScenarioSupport.reputation;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withListeners;
import static studio.modroll.checks.gametest.ScenarioSupport.withProfiles;
import static studio.modroll.checks.gametest.ScenarioSupport.withSocial;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.social.SocialAction;
import studio.modroll.checks.social.SocialEffect;
import studio.modroll.checks.social.SocialMemory;
import studio.modroll.checks.social.SocialMenu;
import studio.modroll.checks.social.SocialOutcome;
import studio.modroll.checks.social.SocialPrices;
import studio.modroll.checks.social.Socials;

/**
 * GameTest bodies for the social menu's actions on villagers, wandering traders and piglins. Players are
 * new survival players with every score 10, so a check adds +0 and the d20 face is the total. Most tests
 * run without cooldowns so one target can be tried again.
 */
public final class SocialScenarios {

    /*
     * Every spot is inside the 8×8×8 test structure, with room for what a mob launches: outside it, an
     * entity may land in a chunk whose entities are not loaded yet, where no search finds it. Targets
     * face south (+z), toward IN_FRONT.
     */
    static final BlockPos TARGET_SPOT = new BlockPos(1, 2, 3);
    static final BlockPos SECOND_SPOT = new BlockPos(2, 2, 3);
    /** Over six blocks from the target, yet inside the structure. */
    static final BlockPos FAR_SPOT = new BlockPos(6, 2, 7);

    private static final BlockPos LLAMA_SPOT = new BlockPos(3, 2, 5);

    static final Vec3 IN_FRONT = new Vec3(1.5, 2, 5.5);
    static final Vec3 BEHIND = new Vec3(1.5, 2, 1.5);
    private static final float FACING_SOUTH = 0;
    private static final int EMERALD_PRICE = 10;
    private static final int WHEAT_PRICE = 20;
    private static final int NAT_ONE = 1;
    private static final int FAIL = 5;
    private static final int LOW = 2;
    private static final int EXACT_PERSUADE_DC = 12;
    private static final int GOOD = 18;
    private static final int NAT_TWENTY = 20;
    private static final int SHORT_TICKS = 2;
    private static final int EXPIRY_WAIT_TICKS = 5;
    private static final int ITEM_SEARCH_RANGE = 4;
    private static final int SPIT_SEARCH_RANGE = 12;
    /** Reaches the neighbour next to the target, not the villager at FAR_SPOT. */
    private static final double SHORT_GOSSIP_RADIUS = 2;

    private static final double REACHING_GOSSIP_RADIUS = 8;
    private static final String WISE_VILLAGER = "{\"matches\": [\"minecraft:villager\"], \"abilities\": {\"wis\": 20}}";
    private static final String WATCHFUL_VILLAGER =
            "{\"matches\": [\"minecraft:villager\"], \"abilities\": {\"wis\": 14}}";
    private static final ScoresConfig.SocialSettings DEFAULTS = ScoresConfig.DEFAULTS.social();
    /** Vanilla's golems attack a player a villager near them rates at this or lower (DefendVillageTargetGoal). */
    private static final int VANILLA_GOLEM_REPUTATION = -100;

    private SocialScenarios() {}

    /** Persuade: natural 1, failure, barely, success and natural 20 each set their price change and gossip. */
    public static void persuadeOutcomesFollowTheDie(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-persuade", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        ScoresConfig.Deal deal = DEFAULTS.persuade().deal();
        try {
            withSocial(
                    settings(action -> true, true, 0),
                    () -> unprofiled(() -> {
                        act(player, villager, SocialAction.PERSUADE, NAT_ONE);
                        expectPrice(
                                helper, player, villager, SocialAction.PERSUADE, deal, SocialOutcome.CRITICAL_FAILURE);
                        expectEquals(
                                helper,
                                -25,
                                reputation(villager, player, GossipType.MINOR_NEGATIVE),
                                "humiliation gossip");
                        act(player, villager, SocialAction.PERSUADE, FAIL);
                        expectPrice(helper, player, villager, SocialAction.PERSUADE, deal, SocialOutcome.FAILURE);
                        act(player, villager, SocialAction.PERSUADE, EXACT_PERSUADE_DC);
                        expectPrice(helper, player, villager, SocialAction.PERSUADE, deal, SocialOutcome.BARELY);
                        act(player, villager, SocialAction.PERSUADE, GOOD);
                        expectPrice(helper, player, villager, SocialAction.PERSUADE, deal, SocialOutcome.SUCCESS);
                        act(player, villager, SocialAction.PERSUADE, NAT_TWENTY);
                        expectPrice(
                                helper, player, villager, SocialAction.PERSUADE, deal, SocialOutcome.CRITICAL_SUCCESS);
                        expectEquals(
                                helper, 10, reputation(villager, player, GossipType.MINOR_POSITIVE), "praise gossip");
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A made Persuasion check lowers the prices shown when trading starts; they return when trading stops. */
    public static void pricesApplyWhileTradingAndAreTakenBack(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-trade", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        WanderingTrader trader = trader(helper, SECOND_SPOT);
        try {
            withSocial(
                    settings(action -> true, false, 0),
                    () -> unprofiled(() -> {
                        for (AbstractVillager target : List.of(villager, trader)) {
                            act(player, target, SocialAction.PERSUADE, GOOD);
                            player.interactOn(target, InteractionHand.MAIN_HAND);
                            expectEquals(helper, 8, cost(target, 0), "emerald price while trading");
                            expectEquals(helper, 16, cost(target, 1), "wheat price while trading");
                            player.closeContainer();
                            expect(helper, !target.isTrading(), "closing the screen must stop trading");
                            expectEquals(helper, EMERALD_PRICE, cost(target, 0), "emerald price after trading");
                            expectEquals(helper, WHEAT_PRICE, cost(target, 1), "wheat price after trading");
                        }
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A price change lasts its ticks, then trading shows the usual prices again. */
    public static void priceChangeExpires(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-expire", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        withSocial(
                shortSuccess(),
                () -> unprofiled(() -> {
                    act(player, villager, SocialAction.PERSUADE, GOOD);
                    expect(helper, SocialPrices.percent(villager, player) < 0, "the discount must apply at first");
                    return null;
                }));
        helper.runAfterDelay(EXPIRY_WAIT_TICKS, () -> {
            try {
                withSocial(settings(action -> true, false, 0), () -> {
                    expect(helper, SocialPrices.percent(villager, player) == 0, "the discount must expire");
                    return null;
                });
            } finally {
                logout(helper, player);
            }
            helper.succeed();
        });
    }

    /** Deceive's DC is the target's passive Insight, 15 for WIS 20; making it by 1 barely succeeds. */
    public static void deceiveRollsAgainstPassiveInsight(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-deceive", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        try {
            withProfiles(
                    profiles(WISE_VILLAGER),
                    () -> withSocial(settings(action -> true, false, 0), () -> {
                        List<Integer> dcs = dcsOf(() -> act(player, villager, SocialAction.DECEIVE, 16));
                        expect(
                                helper,
                                dcs.equals(List.of(15)),
                                "deceive must roll against DC 15, rolled against " + dcs);
                        expectPrice(
                                helper,
                                player,
                                villager,
                                SocialAction.DECEIVE,
                                DEFAULTS.deceive().deal(),
                                SocialOutcome.BARELY);
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * Pickpocketing rolls against passive Perception (12 for WIS 14), with advantage only from behind; a
     * success takes one of the villager's trade results.
     */
    public static void pickpocketUsesPassivePerceptionAndAdvantageFromBehind(GameTestHelper helper) {
        ServerPlayer front = survivor(helper, "soc-pick-front", IN_FRONT);
        ServerPlayer behind = survivor(helper, "soc-pick-back", BEHIND);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        try {
            withProfiles(
                    profiles(WATCHFUL_VILLAGER),
                    () -> withSocial(settings(action -> true, false, 0), () -> {
                        expect(
                                helper,
                                !option(front, villager, SocialAction.PICKPOCKET)
                                        .advantage(),
                                "advantage in front");
                        expect(
                                helper,
                                option(behind, villager, SocialAction.PICKPOCKET)
                                        .advantage(),
                                "advantage behind");
                        List<Integer> dcs = dcsOf(() -> act(behind, villager, SocialAction.PICKPOCKET, 3, 15));
                        expect(
                                helper,
                                dcs.equals(List.of(12)),
                                "pickpocket must roll against DC 12, rolled against " + dcs);
                        // Any die but the d20 rolls its maximum, so the last offer's result is taken.
                        expectEquals(helper, 1, behind.getInventory().countItem(Items.EMERALD), "emeralds lifted");
                        return null;
                    }));
        } finally {
            logout(helper, front);
            logout(helper, behind);
        }
        helper.succeed();
    }

    /** After an action its option shows the cooldown, and trying again rolls nothing. */
    public static void cooldownBlocksARepeat(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-cooldown", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        try {
            withSocial(
                    DEFAULTS,
                    () -> unprofiled(() -> {
                        act(player, villager, SocialAction.PERSUADE, GOOD);
                        SocialMenu.Option option = option(player, villager, SocialAction.PERSUADE);
                        expect(
                                helper,
                                option.availability() == SocialMenu.Availability.COOLDOWN,
                                "persuade must cool down");
                        expect(
                                helper,
                                option.cooldownTicks() > 0
                                        && option.cooldownTicks()
                                                <= DEFAULTS.persuade().cooldownTicks(),
                                "cooldown left must be within the configured cooldown, was " + option.cooldownTicks());
                        expectEquals(
                                helper, 0, rolls(() -> act(player, villager, SocialAction.PERSUADE, GOOD)), "rolls");
                        expect(
                                helper,
                                option(player, villager, SocialAction.DECEIVE).available(),
                                "another action must not share the cooldown");
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A caught pickpocket: strong negative gossip, the villager refuses to trade, and a village golem turns
     * on the player; one the player built does not.
     */
    public static void caughtPickpocketAngersGolemsAndIsRefused(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-caught", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        IronGolem villageGolem = golem(helper, false);
        IronGolem playerGolem = golem(helper, true);
        try {
            withSocial(
                    settings(action -> true, false, 0),
                    () -> unprofiled(() -> {
                        act(player, villager, SocialAction.PICKPOCKET, LOW);
                        expectEquals(
                                helper, -95, reputation(villager, player, GossipType.MAJOR_NEGATIVE), "caught gossip");
                        expect(helper, villageGolem.getTarget() == player, "the village golem must target the player");
                        expect(
                                helper,
                                player.getUUID().equals(villageGolem.getPersistentAngerTarget()),
                                "the village golem must stay angry at the player");
                        expect(helper, playerGolem.getTarget() == null, "a player-built golem must stay calm");
                        expect(helper, Socials.refuses(player, villager), "the villager must refuse to trade");
                        InteractionResult used = Socials.onUseEntity(player, InteractionHand.MAIN_HAND, villager);
                        expect(helper, used.consumesAction(), "the refusal must consume the interaction");
                        expect(helper, player.getInventory().isEmpty(), "a caught pickpocket takes nothing");
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * One caught pickpocket or failed threat leaves the player's reputation with the target and every villager
     * who heard the gossip at -95: above the -100 at which vanilla's golems attack on their own, and still
     * low enough to make them wary.
     */
    public static void aSingleCrimeStaysAboveTheGolemThreshold(GameTestHelper helper) {
        ServerPlayer thief = survivor(helper, "soc-one-theft", IN_FRONT);
        ServerPlayer bully = survivor(helper, "soc-one-threat", IN_FRONT);
        Villager target = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager neighbour = villager(helper, VillagerProfession.FARMER, SECOND_SPOT);
        int wary = DEFAULTS.wary().maxReputation();
        try {
            withSocial(
                    settings(action -> true, false, 0),
                    () -> unprofiled(() -> {
                        act(thief, target, SocialAction.PICKPOCKET, LOW);
                        act(bully, target, SocialAction.INTIMIDATE, LOW);
                        for (ServerPlayer player : List.of(thief, bully)) {
                            for (Villager villager : List.of(target, neighbour)) {
                                int reputation = villager.getPlayerReputation(player);
                                expect(
                                        helper,
                                        reputation > VANILLA_GOLEM_REPUTATION && reputation <= wary,
                                        player.getGameProfile().getName() + " must be rated just above "
                                                + VANILLA_GOLEM_REPUTATION + ", was " + reputation);
                            }
                        }
                        return null;
                    }));
        } finally {
            logout(helper, thief);
            logout(helper, bully);
        }
        helper.succeed();
    }

    /**
     * Intimidating a villager: success hands over a trade result and raises prices with fear gossip;
     * failure brings refusal, strong gossip and golems.
     */
    public static void intimidateVillagerOutcomes(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-intimidate", IN_FRONT);
        Villager scared = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager brave = villager(helper, VillagerProfession.FARMER, SECOND_SPOT);
        IronGolem golem = golem(helper, false);
        ScoresConfig.IntimidateSettings intimidate = DEFAULTS.intimidate();
        try {
            withSocial(
                    settings(action -> true, false, 0),
                    () -> unprofiled(() -> {
                        act(player, scared, SocialAction.INTIMIDATE, GOOD);
                        expectEquals(helper, 1, player.getInventory().countItem(Items.EMERALD), "item handed over");
                        expect(
                                helper,
                                priceChange(player, scared, SocialAction.INTIMIDATE)
                                        .equals(Optional.of(intimidate.success().pricePercent())),
                                "fear must raise prices");
                        expectEquals(helper, -20, reputation(scared, player, GossipType.MINOR_NEGATIVE), "fear gossip");
                        expect(helper, golem.getTarget() == null, "a successful threat calls no golem");

                        act(player, brave, SocialAction.INTIMIDATE, LOW);
                        expect(helper, Socials.refuses(player, brave), "the brave villager must refuse to trade");
                        expect(helper, !Socials.refuses(player, scared), "the scared villager still trades");
                        expectEquals(
                                helper, -95, reputation(brave, player, GossipType.MAJOR_NEGATIVE), "outrage gossip");
                        expect(helper, golem.getTarget() == player, "the golem must target the player");
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * Deceiving a piglin: success makes piglins see gold on the player, until deception is switched off;
     * failure angers that piglin.
     */
    public static void deceivingPiglins(GameTestHelper helper) {
        ServerPlayer liar = survivor(helper, "soc-piglin-ok", IN_FRONT);
        ServerPlayer caught = survivor(helper, "soc-piglin-bad", IN_FRONT);
        Piglin piglin = piglin(helper, TARGET_SPOT);
        try {
            withSocial(
                    settings(action -> true, false, 0),
                    () -> unprofiled(() -> {
                        expect(helper, !PiglinAi.isWearingGold(liar), "no gold before deceiving");
                        act(liar, piglin, SocialAction.DECEIVE, GOOD);
                        expect(helper, PiglinAi.isWearingGold(liar), "a made Deception check must pass for gold");
                        act(caught, piglin, SocialAction.DECEIVE, LOW);
                        expect(helper, angryAt(piglin, caught), "a failed lie must anger the piglin");
                        expect(helper, !PiglinAi.isWearingGold(caught), "a failed lie gives no disguise");
                        return null;
                    }));
            withSocial(settings(action -> action != SocialAction.DECEIVE, false, 0), () -> {
                expect(helper, !PiglinAi.isWearingGold(liar), "the disguise must end with deception off");
                return null;
            });
        } finally {
            logout(helper, liar);
            logout(helper, caught);
        }
        helper.succeed();
    }

    /** Intimidating a piglin: success throws a barter item; failure angers every piglin nearby. */
    public static void intimidatingPiglins(GameTestHelper helper) {
        ServerPlayer bully = survivor(helper, "soc-pig-bully", IN_FRONT);
        ServerPlayer weakling = survivor(helper, "soc-pig-weak", IN_FRONT);
        Piglin piglin = piglin(helper, TARGET_SPOT);
        Piglin neighbour = piglin(helper, SECOND_SPOT);
        try {
            withSocial(
                    settings(action -> true, false, 0),
                    () -> unprofiled(() -> {
                        int before = itemsAround(helper).size();
                        act(bully, piglin, SocialAction.INTIMIDATE, GOOD);
                        expect(helper, itemsAround(helper).size() > before, "the piglin must throw a barter item");
                        expect(helper, !angryAt(piglin, bully), "a successful threat angers no piglin");
                        act(weakling, piglin, SocialAction.INTIMIDATE, LOW);
                        expect(helper, angryAt(piglin, weakling), "the piglin must turn on the player");
                        expect(helper, angryAt(neighbour, weakling), "nearby piglins must turn on the player");
                        return null;
                    }));
        } finally {
            logout(helper, bully);
            logout(helper, weakling);
        }
        helper.succeed();
    }

    /**
     * CHA 14 (+2) lowers every villager's prices by 4%; Religion +3 lowers a cleric's by 6% more, History
     * +1 a librarian's and a cartographer's by 2% more; wandering traders have no profession. CHA 6 raises
     * prices instead.
     */
    public static void passivePricesShiftByProfession(GameTestHelper helper) {
        ServerPlayer charming = survivor(helper, "soc-passive", IN_FRONT);
        ServerPlayer rude = survivor(helper, "soc-rude", IN_FRONT);
        Villager farmer = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager cleric = villager(helper, VillagerProfession.CLERIC, TARGET_SPOT);
        Villager librarian = villager(helper, VillagerProfession.LIBRARIAN, TARGET_SPOT);
        Villager cartographer = villager(helper, VillagerProfession.CARTOGRAPHER, TARGET_SPOT);
        WanderingTrader trader = trader(helper, SECOND_SPOT);
        try {
            for (String value : List.of("cha 14", "religion 3", "history 1")) {
                run(helper, "checks set " + charming.getScoreboardName() + " " + value);
            }
            run(helper, "checks set " + rude.getScoreboardName() + " cha 6");
            withSocial(
                    settings(action -> true, true, 0),
                    () -> unprofiled(() -> {
                        expectPercent(helper, -4, farmer, charming, "farmer");
                        expectPercent(helper, -10, cleric, charming, "cleric");
                        expectPercent(helper, -6, librarian, charming, "librarian");
                        expectPercent(helper, -6, cartographer, charming, "cartographer");
                        expectPercent(helper, 0, trader, charming, "wandering trader");
                        expectPercent(helper, 4, farmer, rude, "farmer for CHA 6");
                        charming.interactOn(cleric, InteractionHand.MAIN_HAND);
                        expectEquals(helper, 9, cost(cleric, 0), "cleric's emerald price while trading");
                        charming.closeContainer();
                        return null;
                    }));
            withSocial(settings(action -> true, false, 0), () -> {
                expectPercent(helper, 0, cleric, charming, "cleric with passive prices off");
                return null;
            });
        } finally {
            logout(helper, charming);
            logout(helper, rude);
        }
        helper.succeed();
    }

    /**
     * Each action switched off alone disappears from the menu, rolls nothing and stops its price changes and
     * refusals counting; the master switch closes the menu and stops every effect.
     */
    public static void eachActionSwitchesOff(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-off", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        try {
            unprofiled(() -> {
                withSocial(settings(action -> true, false, 0), () -> {
                    act(player, villager, SocialAction.PERSUADE, GOOD);
                    act(player, villager, SocialAction.PICKPOCKET, LOW);
                    return null;
                });
                for (SocialAction off : SocialAction.values()) {
                    withSocial(settings(action -> action != off, false, 0), () -> {
                        SocialMenu menu = Socials.menu(player, villager.getId()).orElseThrow();
                        expect(
                                helper,
                                menu.options().stream().noneMatch(option -> option.action() == off),
                                off.id() + " must leave the menu");
                        expectEquals(helper, 0, rolls(() -> act(player, villager, off, GOOD)), off.id() + " rolls");
                        return null;
                    });
                }
                withSocial(settings(action -> action != SocialAction.PERSUADE, false, 0), () -> {
                    expect(helper, SocialPrices.percent(villager, player) == 0, "persuade's discount with it off");
                    return null;
                });
                withSocial(settings(action -> action != SocialAction.PICKPOCKET, false, 0), () -> {
                    expect(helper, !Socials.refuses(player, villager), "pickpocket's refusal with it off");
                    return null;
                });
                withSocial(master(false), () -> {
                    expect(helper, Socials.menu(player, villager.getId()).isEmpty(), "no menu with social off");
                    expect(helper, SocialPrices.percent(villager, player) == 0, "no price change with social off");
                    expect(helper, !Socials.refuses(player, villager), "no refusal with social off");
                    return null;
                });
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * The menu greys out what does not fit the target: a piglin can't be persuaded or pickpocketed, and a
     * villager without trades has nothing to haggle over.
     */
    public static void menuGreysOutWhatDoesNotFit(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-menu", IN_FRONT);
        Piglin piglin = piglin(helper, TARGET_SPOT);
        Villager unemployed = villager(helper, VillagerProfession.NONE, SECOND_SPOT);
        setOffers(unemployed, new MerchantOffers());
        try {
            withSocial(DEFAULTS, () -> {
                expectAvailability(helper, player, piglin, SocialAction.PERSUADE, SocialMenu.Availability.WRONG_TARGET);
                expectAvailability(helper, player, piglin, SocialAction.DECEIVE, SocialMenu.Availability.AVAILABLE);
                expectAvailability(helper, player, piglin, SocialAction.INTIMIDATE, SocialMenu.Availability.AVAILABLE);
                expectAvailability(
                        helper, player, piglin, SocialAction.PICKPOCKET, SocialMenu.Availability.WRONG_TARGET);
                expectAvailability(
                        helper, player, unemployed, SocialAction.PERSUADE, SocialMenu.Availability.NO_TRADES);
                expectEquals(helper, 0, rolls(() -> act(player, piglin, SocialAction.PICKPOCKET, GOOD)), "rolls");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Another player, or a cow, gets no menu, and nothing can be tried on them. */
    public static void playersAndOtherMobsAreNeverTargets(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-player", IN_FRONT);
        ServerPlayer other = survivor(helper, "soc-other", Vec3.atBottomCenterOf(TARGET_SPOT));
        Mob cow = calm(helper, EntityType.COW, SECOND_SPOT);
        try {
            withSocial(DEFAULTS, () -> {
                expect(helper, Socials.menu(player, other.getId()).isEmpty(), "no menu for a player");
                expect(helper, Socials.menu(player, cow.getId()).isEmpty(), "no menu for a cow");
                for (SocialAction action : SocialAction.values()) {
                    expectEquals(
                            helper, 0, rolls(() -> act(player, other, action, GOOD)), action.id() + " on a player");
                }
                return null;
            });
        } finally {
            logout(helper, player);
            logout(helper, other);
        }
        helper.succeed();
    }

    /**
     * Negative gossip marked {@code nearby} reaches every villager within the gossip radius at once (3 blocks
     * here): the neighbour hears about the theft, the villager six blocks away does not. Praise stays with
     * the target.
     */
    public static void negativeGossipReachesTheVillage(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-gossip", IN_FRONT);
        Villager victim = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager neighbour = villager(helper, VillagerProfession.FARMER, SECOND_SPOT);
        Villager farAway = villager(helper, VillagerProfession.FARMER, FAR_SPOT);
        try {
            withSocial(
                    village(SHORT_GOSSIP_RADIUS, true),
                    () -> unprofiled(() -> {
                        act(player, victim, SocialAction.PICKPOCKET, LOW);
                        expectEquals(helper, -95, reputation(victim, player, GossipType.MAJOR_NEGATIVE), "victim");
                        expectEquals(
                                helper, -95, reputation(neighbour, player, GossipType.MAJOR_NEGATIVE), "neighbour");
                        expectEquals(helper, 0, reputation(farAway, player, GossipType.MAJOR_NEGATIVE), "far villager");
                        act(player, victim, SocialAction.PERSUADE, NAT_TWENTY);
                        expectEquals(
                                helper, 10, reputation(victim, player, GossipType.MINOR_POSITIVE), "praised villager");
                        expectEquals(
                                helper,
                                0,
                                reputation(neighbour, player, GossipType.MINOR_POSITIVE),
                                "neighbour praise");
                        return null;
                    }));
            withSocial(
                    village(REACHING_GOSSIP_RADIUS, true),
                    () -> unprofiled(() -> {
                        act(player, victim, SocialAction.PICKPOCKET, LOW);
                        expect(
                                helper,
                                reputation(farAway, player, GossipType.MAJOR_NEGATIVE) < 0,
                                "the far villager must hear it once gossip reaches it");
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A caught pickpocket: the villager shakes its head and runs from the player; see WitnessScenarios for the rest. */
    public static void caughtPickpocketMakesTheVictimRun(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-react-pick", IN_FRONT);
        Villager victim = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        try {
            withSocial(
                    village(DEFAULTS.gossipRadius(), true),
                    () -> unprofiled(() -> {
                        act(player, victim, SocialAction.PICKPOCKET, LOW);
                        expect(helper, victim.getUnhappyCounter() > 0, "the victim must shake its head");
                        expectFleesFrom(helper, victim, player, "the victim");
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * Intimidate: a scared villager runs; a brave one shakes its head and stands its ground. Persuade: a
     * pleased villager doesn't shake its head, an unconvinced one does.
     */
    public static void intimidateAndPersuadeReactions(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-react", IN_FRONT);
        Villager scared = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager brave = villager(helper, VillagerProfession.FARMER, SECOND_SPOT);
        try {
            withSocial(
                    village(0, true),
                    () -> unprofiled(() -> {
                        act(player, scared, SocialAction.INTIMIDATE, GOOD);
                        expectFleesFrom(helper, scared, player, "the scared villager");
                        expect(helper, scared.getUnhappyCounter() == 0, "the scared villager must not shake its head");
                        act(player, brave, SocialAction.INTIMIDATE, LOW);
                        expect(helper, brave.getUnhappyCounter() > 0, "the brave villager must shake its head");
                        expect(helper, !walking(brave), "the brave villager must stand its ground");

                        brave.setUnhappyCounter(0);
                        act(player, brave, SocialAction.PERSUADE, GOOD);
                        expect(helper, brave.getUnhappyCounter() == 0, "a pleased villager must not shake its head");
                        act(player, brave, SocialAction.PERSUADE, FAIL);
                        expect(helper, brave.getUnhappyCounter() > 0, "an unconvinced villager must shake its head");
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A wandering trader who catches the lie refuses the player and his llama spits; a villager caught lying
     * still trades. With reactions off the llama keeps quiet, but the trader still refuses.
     */
    public static void lyingToAWanderingTraderGetsYouSpatAt(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-llama", IN_FRONT);
        ServerPlayer quiet = survivor(helper, "soc-llama-off", IN_FRONT);
        WanderingTrader trader = trader(helper, TARGET_SPOT);
        Villager villager = villager(helper, VillagerProfession.FARMER, SECOND_SPOT);
        Llama llama = calm(helper, EntityType.TRADER_LLAMA, LLAMA_SPOT);
        llama.setLeashedTo(trader, true);
        try {
            withSocial(
                    village(0, true),
                    () -> unprofiled(() -> {
                        act(player, trader, SocialAction.DECEIVE, LOW);
                        expect(helper, Socials.refuses(player, trader), "the trader must refuse the liar");
                        expectEquals(helper, 1, spitsAt(helper, llama, player), "spits launched at the liar");
                        act(player, villager, SocialAction.DECEIVE, LOW);
                        expect(helper, !Socials.refuses(player, villager), "a villager caught lying still trades");
                        return null;
                    }));
            withSocial(
                    village(0, false),
                    () -> unprofiled(() -> {
                        act(quiet, trader, SocialAction.DECEIVE, LOW);
                        expect(helper, Socials.refuses(quiet, trader), "the trader refuses with reactions off too");
                        expectEquals(helper, 1, spitsAt(helper, llama, player), "spits launched with reactions off");
                        return null;
                    }));
        } finally {
            logout(helper, player);
            logout(helper, quiet);
        }
        helper.succeed();
    }

    /** With reactions off nothing shakes its head or runs, but the outcome itself still happens. */
    public static void reactionsSwitchOff(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-react-off", IN_FRONT);
        Villager victim = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager bystander = villager(helper, VillagerProfession.FARMER, SECOND_SPOT);
        try {
            withSocial(
                    village(DEFAULTS.gossipRadius(), false),
                    () -> unprofiled(() -> {
                        act(player, victim, SocialAction.PICKPOCKET, LOW);
                        expect(helper, victim.getUnhappyCounter() == 0, "no head shake with reactions off");
                        expect(helper, !walking(victim) && !walking(bystander), "nobody runs with reactions off");
                        expect(helper, Socials.refuses(player, victim), "the caught pickpocket is still refused");
                        expectEquals(helper, -95, reputation(bystander, player, GossipType.MAJOR_NEGATIVE), "gossip");
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * What happens with a wandering trader stays with him: a natural 1 lie tells no villager around him, and
     * calls no golem even when the outcome is set to.
     */
    public static void wanderingTraderOutcomesStayWithTheTrader(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "soc-trader-only", IN_FRONT);
        WanderingTrader trader = trader(helper, TARGET_SPOT);
        Villager neighbour = villager(helper, VillagerProfession.FARMER, SECOND_SPOT);
        IronGolem golem = golem(helper, false);
        try {
            withSocial(
                    golemsOnCriticalLies(),
                    () -> unprofiled(() -> {
                        act(player, trader, SocialAction.DECEIVE, NAT_ONE);
                        expectEquals(helper, 0, reputation(neighbour, player, GossipType.MINOR_NEGATIVE), "neighbour");
                        expect(helper, golem.getTarget() == null, "no golem comes for a lie to a wandering trader");
                        expectPrice(
                                helper,
                                player,
                                trader,
                                SocialAction.DECEIVE,
                                DEFAULTS.deceive().deal(),
                                SocialOutcome.CRITICAL_FAILURE);
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Running and panicking, so its idle behaviors cannot walk it back to the player. */
    static void expectFleesFrom(GameTestHelper helper, Villager villager, ServerPlayer player, String who) {
        expect(helper, villager.getBrain().isActive(Activity.PANIC), who + " must panic");
        expect(
                helper,
                villager.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY).orElse(null) == player,
                who + " must keep away from the player");
        Optional<Vec3> target = villager.getBrain()
                .getMemory(MemoryModuleType.WALK_TARGET)
                .map(walk -> walk.getTarget().currentPosition());
        expect(helper, target.isPresent(), who + " must have somewhere to run");
        expect(
                helper,
                target.get().distanceTo(player.position()) > villager.position().distanceTo(player.position()),
                who + " must run away from the player");
    }

    static boolean walking(Villager villager) {
        return villager.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
    }

    /** Spits the llama launched toward the target; where they land is up to vanilla. */
    private static int spitsAt(GameTestHelper helper, Llama llama, LivingEntity target) {
        AABB area = new AABB(helper.absolutePos(TARGET_SPOT)).inflate(SPIT_SEARCH_RANGE);
        return helper.getLevel()
                .getEntitiesOfClass(
                        LlamaSpit.class,
                        area,
                        spit -> spit.getOwner() == llama
                                && spit.getDeltaMovement().dot(target.position().subtract(spit.position())) > 0)
                .size();
    }

    static void act(ServerPlayer player, Entity target, SocialAction action, int... faces) {
        withD20s(
                () -> {
                    Socials.act(player, target.getId(), action);
                    return null;
                },
                faces);
    }

    static SocialMenu.Option option(ServerPlayer player, Entity target, SocialAction action) {
        return Socials.menu(player, target.getId()).orElseThrow().options().stream()
                .filter(option -> option.action() == action)
                .findFirst()
                .orElseThrow();
    }

    static void expectAvailability(
            GameTestHelper helper,
            ServerPlayer player,
            Entity target,
            SocialAction action,
            SocialMenu.Availability expected) {
        SocialMenu.Availability actual = option(player, target, action).availability();
        expect(helper, actual == expected, action.id() + " must be " + expected + ", was " + actual);
    }

    private static void expectPrice(
            GameTestHelper helper,
            ServerPlayer player,
            AbstractVillager trader,
            SocialAction action,
            ScoresConfig.Deal deal,
            SocialOutcome outcome) {
        double expected = deal.effect(outcome).pricePercent();
        Optional<Double> actual = priceChange(player, trader, action);
        expect(
                helper,
                actual.equals(Optional.of(expected)),
                outcome.id() + " must change prices by " + expected + "%, was " + actual);
    }

    private static void expectPercent(
            GameTestHelper helper, double expected, AbstractVillager trader, ServerPlayer player, String what) {
        double actual = SocialPrices.percent(trader, player);
        expect(helper, actual == expected, what + " price change must be " + expected + "%, was " + actual);
    }

    private static Optional<Double> priceChange(ServerPlayer player, AbstractVillager trader, SocialAction action) {
        Map<SocialAction, Double> changes = SocialMemory.get(player.server)
                .priceChanges(player.getUUID(), trader.getUUID(), Socials.now(player.server));
        return Optional.ofNullable(changes.get(action));
    }

    private static int cost(AbstractVillager trader, int offer) {
        return trader.getOffers().get(offer).getCostA().getCount();
    }

    static boolean angryAt(Piglin piglin, ServerPlayer player) {
        return piglin.getBrain()
                .getMemory(MemoryModuleType.ANGRY_AT)
                .map(player.getUUID()::equals)
                .orElse(false);
    }

    private static List<ItemEntity> itemsAround(GameTestHelper helper) {
        AABB area = new AABB(helper.absolutePos(TARGET_SPOT)).inflate(ITEM_SEARCH_RANGE);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area);
    }

    /** How many rolls {@code body} made. */
    static int rolls(Runnable body) {
        return dcsOf(body).size();
    }

    /** The DC of each roll {@code body} made, in order. */
    private static List<Integer> dcsOf(Runnable body) {
        List<Integer> dcs = new ArrayList<>();
        withListeners(before -> {}, after -> dcs.add(after.result().dc().orElse(-1)), () -> {
            body.run();
            return null;
        });
        return dcs;
    }

    static ServerPlayer survivor(GameTestHelper helper, String name, Vec3 at) {
        return placed(helper, login(helper, newProfile(name)), at);
    }

    /** A survivor in front of the target spot, with every packet the server sends them added to {@code sent}. */
    static ServerPlayer watchedSurvivor(GameTestHelper helper, String name, List<Packet<?>> sent) {
        return placed(helper, loginWatched(helper, newProfile(name), sent), IN_FRONT);
    }

    private static ServerPlayer placed(GameTestHelper helper, ServerPlayer player, Vec3 at) {
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(at));
        return player;
    }

    /** A villager facing south with two trades: 10 emeralds for bread, 20 wheat for an emerald. */
    static Villager villager(GameTestHelper helper, VillagerProfession profession, BlockPos at) {
        Villager villager = calm(helper, EntityType.VILLAGER, at);
        villager.setVillagerData(new VillagerData(VillagerType.PLAINS, profession, 1));
        setOffers(villager, offers());
        return villager;
    }

    private static WanderingTrader trader(GameTestHelper helper, BlockPos at) {
        WanderingTrader trader = calm(helper, EntityType.WANDERING_TRADER, at);
        setOffers(trader, offers());
        return trader;
    }

    /** Replaces the trades in place; {@code overrideOffers} only works on a client-side merchant. */
    private static void setOffers(AbstractVillager trader, MerchantOffers offers) {
        trader.getOffers().clear();
        trader.getOffers().addAll(offers);
    }

    private static MerchantOffers offers() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, EMERALD_PRICE), new ItemStack(Items.BREAD), 12, 1, 0.05f));
        offers.add(
                new MerchantOffer(new ItemCost(Items.WHEAT, WHEAT_PRICE), new ItemStack(Items.EMERALD), 12, 1, 0.05f));
        return offers;
    }

    static Piglin piglin(GameTestHelper helper, BlockPos at) {
        Piglin piglin = calm(helper, EntityType.PIGLIN, at);
        piglin.setImmuneToZombification(true);
        return piglin;
    }

    static IronGolem golem(GameTestHelper helper, boolean playerCreated) {
        return golem(helper, SECOND_SPOT, playerCreated);
    }

    static IronGolem golem(GameTestHelper helper, BlockPos at, boolean playerCreated) {
        IronGolem golem = calm(helper, EntityType.IRON_GOLEM, at);
        golem.setPlayerCreated(playerCreated);
        return golem;
    }

    static <T extends Mob> T calm(GameTestHelper helper, EntityType<T> type, BlockPos at) {
        T mob = helper.spawn(type, at);
        mob.setNoAi(true);
        mob.setYRot(FACING_SOUTH);
        mob.setYHeadRot(FACING_SOUTH);
        mob.yBodyRot = FACING_SOUTH;
        return mob;
    }

    /**
     * Defaults with these actions on, passive prices on or off, and every cooldown set to {@code cooldown}.
     * Witnesses and the golem alarm are off: they reach past the test structure into the tests next to it.
     * Wariness is off too, so trying a target twice never rolls a second d20.
     */
    static ScoresConfig.SocialSettings settings(Predicate<SocialAction> on, boolean passive, int cooldown) {
        ScoresConfig.PassivePriceSettings prices = DEFAULTS.passivePrices();
        return new ScoresConfig.SocialSettings(
                true,
                DEFAULTS.nearbyRadius(),
                DEFAULTS.gossipRadius(),
                new ScoresConfig.PersuadeSettings(
                        on.test(SocialAction.PERSUADE),
                        DEFAULTS.persuade().dc(),
                        cooldown,
                        DEFAULTS.persuade().deal()),
                new ScoresConfig.DeceiveSettings(
                        on.test(SocialAction.DECEIVE),
                        cooldown,
                        DEFAULTS.deceive().deal(),
                        DEFAULTS.deceive().piglin(),
                        DEFAULTS.deceive().wanderingTrader()),
                new ScoresConfig.IntimidateSettings(
                        on.test(SocialAction.INTIMIDATE),
                        DEFAULTS.intimidate().dc(),
                        cooldown,
                        DEFAULTS.intimidate().success(),
                        DEFAULTS.intimidate().failure(),
                        DEFAULTS.intimidate().piglinAngerTicks()),
                new ScoresConfig.PickpocketSettings(
                        on.test(SocialAction.PICKPOCKET),
                        cooldown,
                        DEFAULTS.pickpocket().behindDegrees(),
                        DEFAULTS.pickpocket().failure()),
                new ScoresConfig.PassivePriceSettings(passive, prices.charismaPercentPerPoint(), prices.professions()),
                DEFAULTS.reactions(),
                new ScoresConfig.WitnessSettings(
                        false,
                        DEFAULTS.witnesses().refuseFraction(),
                        DEFAULTS.witnesses().fleeDistance()),
                new ScoresConfig.GolemAlarmSettings(
                        false,
                        DEFAULTS.golemAlarm().radius(),
                        DEFAULTS.golemAlarm().durationTicks()),
                DEFAULTS.onGuard(),
                new ScoresConfig.WarySettings(false, DEFAULTS.wary().maxReputation()),
                DEFAULTS.feel(),
                DEFAULTS.intimidateMob(),
                DEFAULTS.calm(),
                DEFAULTS.insight(),
                DEFAULTS.performance());
    }

    /** {@code base} with these settings for mobs, Insight and Performance. */
    static ScoresConfig.SocialSettings replacing(
            ScoresConfig.SocialSettings base,
            ScoresConfig.IntimidateMobSettings intimidateMob,
            ScoresConfig.CalmSettings calm,
            ScoresConfig.InsightSettings insight,
            ScoresConfig.PerformanceSettings performance) {
        return new ScoresConfig.SocialSettings(
                base.enabled(),
                base.nearbyRadius(),
                base.gossipRadius(),
                base.persuade(),
                base.deceive(),
                base.intimidate(),
                base.pickpocket(),
                base.passivePrices(),
                base.reactions(),
                base.witnesses(),
                base.golemAlarm(),
                base.onGuard(),
                base.wary(),
                base.feel(),
                intimidateMob,
                calm,
                insight,
                performance);
    }

    /** All actions on with no cooldowns, as the master switch says. */
    private static ScoresConfig.SocialSettings master(boolean enabled) {
        ScoresConfig.SocialSettings all = settings(action -> true, true, 0);
        return new ScoresConfig.SocialSettings(
                enabled,
                all.nearbyRadius(),
                all.gossipRadius(),
                all.persuade(),
                all.deceive(),
                all.intimidate(),
                all.pickpocket(),
                all.passivePrices(),
                all.reactions(),
                all.witnesses(),
                all.golemAlarm(),
                all.onGuard(),
                all.wary(),
                all.feel(),
                all.intimidateMob(),
                all.calm(),
                all.insight(),
                all.performance());
    }

    /** All actions on with no cooldowns, gossip reaching {@code gossipRadius} and reactions on or off. */
    private static ScoresConfig.SocialSettings village(double gossipRadius, boolean reactions) {
        ScoresConfig.SocialSettings all = settings(action -> true, false, 0);
        ScoresConfig.ReactionSettings defaults = all.reactions();
        return new ScoresConfig.SocialSettings(
                true,
                all.nearbyRadius(),
                gossipRadius,
                all.persuade(),
                all.deceive(),
                all.intimidate(),
                all.pickpocket(),
                all.passivePrices(),
                new ScoresConfig.ReactionSettings(
                        reactions, defaults.fleeDistance(), defaults.fleeTicks(), defaults.fleeSpeed()),
                all.witnesses(),
                all.golemAlarm(),
                all.onGuard(),
                all.wary(),
                all.feel(),
                all.intimidateMob(),
                all.calm(),
                all.insight(),
                all.performance());
    }

    /** No cooldowns, and a natural 1 lie also angering golems. */
    private static ScoresConfig.SocialSettings golemsOnCriticalLies() {
        ScoresConfig.SocialSettings base = settings(action -> true, false, 0);
        ScoresConfig.DeceiveSettings deceive = base.deceive();
        SocialEffect critical = deceive.deal().effect(SocialOutcome.CRITICAL_FAILURE);
        Map<SocialOutcome, SocialEffect> outcomes = new EnumMap<>(deceive.deal().outcomes());
        outcomes.put(
                SocialOutcome.CRITICAL_FAILURE,
                new SocialEffect(
                        critical.pricePercent(),
                        critical.priceTicks(),
                        critical.gossip(),
                        critical.refuseTicks(),
                        true));
        return new ScoresConfig.SocialSettings(
                true,
                base.nearbyRadius(),
                base.gossipRadius(),
                base.persuade(),
                new ScoresConfig.DeceiveSettings(
                        true,
                        0,
                        new ScoresConfig.Deal(deceive.deal().barelyMargin(), outcomes),
                        deceive.piglin(),
                        deceive.wanderingTrader()),
                base.intimidate(),
                base.pickpocket(),
                base.passivePrices(),
                base.reactions(),
                base.witnesses(),
                base.golemAlarm(),
                base.onGuard(),
                base.wary(),
                base.feel(),
                base.intimidateMob(),
                base.calm(),
                base.insight(),
                base.performance());
    }

    /** No cooldowns, no passive prices, and a made Persuasion check's discount lasting two ticks. */
    private static ScoresConfig.SocialSettings shortSuccess() {
        ScoresConfig.SocialSettings base = settings(action -> true, false, 0);
        ScoresConfig.Deal deal = base.persuade().deal();
        SocialEffect success = deal.effect(SocialOutcome.SUCCESS);
        Map<SocialOutcome, SocialEffect> outcomes = new EnumMap<>(deal.outcomes());
        outcomes.put(
                SocialOutcome.SUCCESS,
                new SocialEffect(
                        success.pricePercent(),
                        SHORT_TICKS,
                        success.gossip(),
                        success.refuseTicks(),
                        success.angersGolems()));
        return new ScoresConfig.SocialSettings(
                true,
                base.nearbyRadius(),
                base.gossipRadius(),
                new ScoresConfig.PersuadeSettings(
                        true, base.persuade().dc(), 0, new ScoresConfig.Deal(deal.barelyMargin(), outcomes)),
                base.deceive(),
                base.intimidate(),
                base.pickpocket(),
                base.passivePrices(),
                base.reactions(),
                base.witnesses(),
                base.golemAlarm(),
                base.onGuard(),
                base.wary(),
                base.feel(),
                base.intimidateMob(),
                base.calm(),
                base.insight(),
                base.performance());
    }
}
