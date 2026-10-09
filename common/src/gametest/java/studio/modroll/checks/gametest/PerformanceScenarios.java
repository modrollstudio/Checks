package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.reputation;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withSocial;
import static studio.modroll.checks.gametest.SocialScenarios.IN_FRONT;
import static studio.modroll.checks.gametest.SocialScenarios.SECOND_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.TARGET_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.act;
import static studio.modroll.checks.gametest.SocialScenarios.golem;
import static studio.modroll.checks.gametest.SocialScenarios.piglin;
import static studio.modroll.checks.gametest.SocialScenarios.replacing;
import static studio.modroll.checks.gametest.SocialScenarios.rolls;
import static studio.modroll.checks.gametest.SocialScenarios.settings;
import static studio.modroll.checks.gametest.SocialScenarios.survivor;
import static studio.modroll.checks.gametest.SocialScenarios.villager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.Instruments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.social.SocialAction;
import studio.modroll.checks.social.SocialEffect;

/**
 * GameTest bodies for Performance, played the way a player plays: hitting a note block or blowing a goat
 * horn, which reach Checks through the loader's game event hook. Players are new survival players with
 * every score 10, so a d20 of 18 makes the DC of 12 and a 5 does not. The audience is the villager and
 * piglin within a few blocks, so no neighbouring test's mobs hear it.
 */
public final class PerformanceScenarios {

    private static final int NAT_ONE = 1;
    private static final int FAIL = 5;
    private static final int GOOD = 18;
    private static final int NAT_TWENTY = 20;
    private static final int CAUGHT = 2;
    private static final BlockPos NOTE_BLOCK = new BlockPos(4, 2, 6);
    private static final double SHORT_RANGE = 6;
    private static final int LONG_COOLDOWN = 1200;

    private static final ScoresConfig.SocialSettings DEFAULTS = ScoresConfig.DEFAULTS.social();
    private static final ScoresConfig.PerformanceSettings PERFORMANCE = DEFAULTS.performance();

    private PerformanceScenarios() {}

    /**
     * A note block played well charms the piglin and tells the villager good things about the player; a
     * natural 20 tells it more, a failure nothing, and a natural 1 bad things.
     */
    public static void aNoteBlockPerformanceFollowsTheDie(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "perf-note", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        piglin(helper, SECOND_SPOT);
        helper.setBlock(NOTE_BLOCK, Blocks.NOTE_BLOCK);
        try {
            performing(performance(true, 0), () -> {
                expect(helper, !PiglinAi.isWearingGold(player), "no charm before playing");
                expectEquals(helper, 1, rolls(() -> hitNoteBlock(helper, player, GOOD)), "rolls for a note");
                expectGossip(helper, villager, player, PERFORMANCE.success(), 1);
                expect(helper, PiglinAi.isWearingGold(player), "a made Performance must charm piglins");
                hitNoteBlock(helper, player, NAT_TWENTY);
                expectEquals(
                        helper,
                        amount(PERFORMANCE.success()) + amount(PERFORMANCE.criticalSuccess()),
                        reputation(villager, player, GossipType.MINOR_POSITIVE),
                        "praise after a natural 20");
                hitNoteBlock(helper, player, FAIL);
                expectEquals(helper, 0, reputation(villager, player, GossipType.MINOR_NEGATIVE), "after a failure");
                hitNoteBlock(helper, player, NAT_ONE);
                expectGossip(helper, villager, player, PERFORMANCE.criticalFailure(), 1);
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A goat horn rolls as a note block does; then the cooldown holds off the next one. */
    public static void aGoatHornPerformsAndCoolsDown(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "perf-horn", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        try {
            performing(performance(true, LONG_COOLDOWN), () -> {
                expectEquals(helper, 1, rolls(() -> blowHorn(helper, player, GOOD)), "rolls for a horn");
                expectGossip(helper, villager, player, PERFORMANCE.success(), 1);
                expectEquals(helper, 0, rolls(() -> blowHorn(helper, player, GOOD)), "rolls on cooldown");
                expectGossip(helper, villager, player, PERFORMANCE.success(), 1);
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A note block played by redstone has no player behind it: nothing is rolled. */
    public static void redstoneNotesRollNothing(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "perf-redstone", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        helper.setBlock(NOTE_BLOCK, Blocks.NOTE_BLOCK);
        try {
            performing(performance(true, 0), () -> {
                expectEquals(
                        helper,
                        0,
                        rolls(() -> helper.setBlock(NOTE_BLOCK.east(), Blocks.REDSTONE_BLOCK)),
                        "rolls for a redstone note");
                expect(
                        helper,
                        helper.getBlockState(NOTE_BLOCK).getValue(NoteBlock.POWERED),
                        "the note block must have been powered");
                expectEquals(helper, 0, reputation(villager, player, GossipType.MINOR_POSITIVE), "praise");
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * With no one to hear it, or Performance off, nothing is rolled; switching it off also lifts the piglin
     * charm it gave.
     */
    public static void noAudienceOrSwitchedOffRollsNothing(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "perf-off", IN_FRONT);
        helper.setBlock(NOTE_BLOCK, Blocks.NOTE_BLOCK);
        try {
            performing(
                    performance(true, 0),
                    () -> expectEquals(
                            helper, 0, rolls(() -> hitNoteBlock(helper, player, GOOD)), "rolls with nobody around"));
            piglin(helper, SECOND_SPOT);
            performing(performance(true, 0), () -> hitNoteBlock(helper, player, GOOD));
            performing(performance(false, 0), () -> {
                expectEquals(helper, 0, rolls(() -> hitNoteBlock(helper, player, GOOD)), "rolls with it off");
                expect(helper, !PiglinAi.isWearingGold(player), "the charm must lift with Performance off");
            });
            performing(
                    performance(true, 0),
                    () -> expect(
                            helper, PiglinAi.isWearingGold(player), "the charm must come back with Performance on"));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * Villagers on guard during a golem alarm are no audience: with only them around nothing is rolled, and a
     * piglin's hearing it tells them nothing. In the {@code wide} structure, in a batch of its own.
     */
    public static void villagersOnGuardIgnoreAPerformance(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "perf-guard", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        golem(helper, SECOND_SPOT, false);
        helper.setBlock(NOTE_BLOCK, Blocks.NOTE_BLOCK);
        try {
            guardedPerforming(() -> {
                act(player, villager, SocialAction.PICKPOCKET, CAUGHT);
                expectEquals(helper, 0, rolls(() -> hitNoteBlock(helper, player, GOOD)), "rolls for guarded villagers");
                piglin(helper, NOTE_BLOCK.west());
                expectEquals(helper, 1, rolls(() -> hitNoteBlock(helper, player, GOOD)), "rolls for a piglin");
                expectEquals(helper, 0, reputation(villager, player, GossipType.MINOR_POSITIVE), "praise on guard");
                expect(helper, PiglinAi.isWearingGold(player), "the piglin must still be charmed");
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    private static void hitNoteBlock(GameTestHelper helper, ServerPlayer player, int face) {
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(NOTE_BLOCK);
        withD20s(
                () -> {
                    level.getBlockState(at).attack(level, at, player);
                    return null;
                },
                face);
    }

    private static void blowHorn(GameTestHelper helper, ServerPlayer player, int face) {
        Holder<Instrument> ponder = helper.getLevel()
                .registryAccess()
                .registryOrThrow(Registries.INSTRUMENT)
                .getHolderOrThrow(Instruments.PONDER_GOAT_HORN);
        ItemStack horn = InstrumentItem.create(Items.GOAT_HORN, ponder);
        player.setItemInHand(InteractionHand.MAIN_HAND, horn);
        withD20s(
                () -> {
                    horn.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                    player.stopUsingItem();
                    return null;
                },
                face);
    }

    private static void expectGossip(
            GameTestHelper helper,
            Villager villager,
            ServerPlayer player,
            ScoresConfig.PerformanceEffect effect,
            int times) {
        SocialEffect.Gossip gossip = effect.gossip().orElseThrow();
        expectEquals(
                helper,
                gossip.amount() * gossip.type().weight * times,
                reputation(villager, player, gossip.type()),
                gossip.type().getSerializedName() + " reputation");
    }

    private static int amount(ScoresConfig.PerformanceEffect effect) {
        return effect.gossip().map(SocialEffect.Gossip::amount).orElse(0);
    }

    private static ScoresConfig.PerformanceSettings performance(boolean enabled, int cooldown) {
        return new ScoresConfig.PerformanceSettings(
                enabled,
                PERFORMANCE.dc(),
                cooldown,
                SHORT_RANGE,
                PERFORMANCE.success(),
                PERFORMANCE.criticalSuccess(),
                PERFORMANCE.criticalFailure());
    }

    private static void performing(ScoresConfig.PerformanceSettings performance, Runnable body) {
        perform(settings(action -> true, false, 0), performance, body);
    }

    /** Performance with no cooldown, with the golem alarm and on guard on and no witnesses. */
    private static void guardedPerforming(Runnable body) {
        ScoresConfig.SocialSettings base = settings(action -> true, false, 0);
        ScoresConfig.SocialSettings alarmed = new ScoresConfig.SocialSettings(
                true,
                base.nearbyRadius(),
                base.gossipRadius(),
                base.persuade(),
                base.deceive(),
                base.intimidate(),
                base.pickpocket(),
                base.passivePrices(),
                base.reactions(),
                base.witnesses(),
                DEFAULTS.golemAlarm(),
                base.onGuard(),
                base.wary(),
                base.feel(),
                base.intimidateMob(),
                base.calm(),
                base.insight(),
                base.performance());
        perform(alarmed, performance(true, 0), body);
    }

    private static void perform(
            ScoresConfig.SocialSettings base, ScoresConfig.PerformanceSettings performance, Runnable body) {
        withSocial(
                replacing(base, base.intimidateMob(), base.calm(), base.insight(), performance),
                () -> unprofiled(() -> {
                    body.run();
                    return null;
                }));
    }
}
