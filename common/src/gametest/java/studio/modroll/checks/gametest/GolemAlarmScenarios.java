package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withSocial;
import static studio.modroll.checks.gametest.SocialScenarios.IN_FRONT;
import static studio.modroll.checks.gametest.SocialScenarios.SECOND_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.TARGET_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.act;
import static studio.modroll.checks.gametest.SocialScenarios.golem;
import static studio.modroll.checks.gametest.SocialScenarios.settings;
import static studio.modroll.checks.gametest.SocialScenarios.survivor;
import static studio.modroll.checks.gametest.SocialScenarios.villager;

import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.social.GolemAlarms;
import studio.modroll.checks.social.SocialAction;

/**
 * GameTest bodies for golems calling for backup, in the 32 blocks wide {@code wide} structure. Each runs
 * in a batch of its own: an alarm reaches far past its structure, and must not turn a neighbouring test's
 * golems. A caught pickpocket turns the golem next to the villager; the others stand further along.
 * Players are new survival players with every score 10, so a d20 of 2 is caught.
 */
public final class GolemAlarmScenarios {

    private static final int CAUGHT = 2;
    /** Makes Calm's DC 13 with a Persuasion modifier of +0. */
    private static final int CALMED = 18;
    /** 22 blocks from the golem next to the villager, and out of its golem-calling range. */
    private static final BlockPos FAR_GOLEM = new BlockPos(24, 2, 3);
    /** Within 16 blocks of both the near and the far golem. */
    private static final BlockPos MIDDLE_GOLEM = new BlockPos(12, 2, 3);

    private static final BlockPos BUILT_GOLEM = new BlockPos(26, 2, 5);
    private static final double SHORT_RADIUS = 16;
    private static final int SHORT_ALARM_TICKS = 5;
    private static final int PAST_THE_ALARM = 10;
    private static final int SPREAD_TICKS = 5;
    private static final ScoresConfig.GolemAlarmSettings DEFAULTS =
            ScoresConfig.DEFAULTS.social().golemAlarm();

    private GolemAlarmScenarios() {}

    /** A golem 22 blocks away joins the chase; one a player built stays out of it. */
    public static void guardsCallForBackup(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "alarm-backup", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        IronGolem near = golem(helper, SECOND_SPOT, false);
        IronGolem far = golem(helper, FAR_GOLEM, false);
        IronGolem built = golem(helper, BUILT_GOLEM, true);
        try {
            alarmed(DEFAULTS, () -> act(player, villager, SocialAction.PICKPOCKET, CAUGHT));
            expect(helper, near.getTarget() == player, "the golem by the villager must turn on the thief");
            expect(helper, far.getTarget() == player, "a golem 22 blocks away must join the chase");
            expect(helper, built.getTarget() == null, "a golem a player built must stay out of it");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A golem beyond the radius of every golem after the player is not called; one that comes into range
     * later joins on the next tick, and calls it on the tick after. The ticks run here, under the short
     * radius; the server's own run under the live config.
     */
    public static void backupReachesItsRadiusAndSpreads(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "alarm-radius", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        IronGolem near = golem(helper, SECOND_SPOT, false);
        IronGolem far = golem(helper, FAR_GOLEM, false);
        MinecraftServer server = helper.getLevel().getServer();
        ScoresConfig.GolemAlarmSettings shortRange =
                new ScoresConfig.GolemAlarmSettings(true, SHORT_RADIUS, DEFAULTS.durationTicks());
        try {
            alarmed(shortRange, () -> {
                act(player, villager, SocialAction.PICKPOCKET, CAUGHT);
                expect(helper, near.getTarget() == player, "the golem by the villager must turn on the thief");
                GolemAlarms.tick(server);
                expect(helper, far.getTarget() == null, "a golem beyond the radius must not be called");
                IronGolem middle = golem(helper, MIDDLE_GOLEM, false);
                GolemAlarms.tick(server);
                expect(helper, middle.getTarget() == player, "a golem that comes into range must join");
                expect(helper, far.getTarget() == null, "the far golem is called only by the one that joined");
                GolemAlarms.tick(server);
                expect(helper, far.getTarget() == player, "the golem that joined must call the ones around it");
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Past its time, the alarm calls no golem that comes into range. */
    public static void alarmExpires(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "alarm-expiry", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        IronGolem near = golem(helper, SECOND_SPOT, false);
        ScoresConfig.GolemAlarmSettings brief =
                new ScoresConfig.GolemAlarmSettings(true, DEFAULTS.radius(), SHORT_ALARM_TICKS);
        AtomicReference<IronGolem> late = new AtomicReference<>();
        alarmed(brief, () -> act(player, villager, SocialAction.PICKPOCKET, CAUGHT));
        expect(helper, near.getTarget() == player, "the golem by the villager must turn on the thief");
        helper.runAfterDelay(PAST_THE_ALARM, () -> late.set(golem(helper, MIDDLE_GOLEM, false)));
        helper.runAfterDelay(PAST_THE_ALARM + SPREAD_TICKS, () -> {
            try {
                expect(helper, late.get().getTarget() == null, "an expired alarm must call no golem");
            } finally {
                logout(helper, player);
            }
            helper.succeed();
        });
    }

    /** Once the player dies, the alarm calls no golem that comes into range. */
    public static void alarmEndsWhenThePlayerDies(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "alarm-death", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        IronGolem near = golem(helper, SECOND_SPOT, false);
        alarmed(DEFAULTS, () -> act(player, villager, SocialAction.PICKPOCKET, CAUGHT));
        expect(helper, near.getTarget() == player, "the golem by the villager must turn on the thief");
        player.kill();
        expect(helper, !player.isAlive(), "the player must die");
        IronGolem late = golem(helper, MIDDLE_GOLEM, false);
        helper.runAfterDelay(SPREAD_TICKS, () -> {
            try {
                expect(helper, late.getTarget() == null, "the alarm must end with the player's death");
            } finally {
                logout(helper, player);
            }
            helper.succeed();
        });
    }

    /** With the alarm off, only the golems around the villager turn, as before. */
    public static void golemAlarmSwitchesOff(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "alarm-off", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        IronGolem near = golem(helper, SECOND_SPOT, false);
        IronGolem far = golem(helper, FAR_GOLEM, false);
        ScoresConfig.GolemAlarmSettings off =
                new ScoresConfig.GolemAlarmSettings(false, DEFAULTS.radius(), DEFAULTS.durationTicks());
        alarmed(off, () -> act(player, villager, SocialAction.PICKPOCKET, CAUGHT));
        expect(helper, near.getTarget() == player, "the golem by the villager must still turn on the thief");
        helper.runAfterDelay(SPREAD_TICKS, () -> {
            try {
                expect(helper, far.getTarget() == null, "no golem may be called with the alarm off");
            } finally {
                logout(helper, player);
            }
            helper.succeed();
        });
    }

    /** On Peaceful no golem turns on a player, so none calls for backup. */
    public static void noBackupOnPeaceful(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "alarm-peaceful", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        IronGolem near = golem(helper, SECOND_SPOT, false);
        IronGolem far = golem(helper, FAR_GOLEM, false);
        MinecraftServer server = helper.getLevel().getServer();
        Difficulty before = server.getWorldData().getDifficulty();
        try {
            server.setDifficulty(Difficulty.PEACEFUL, true);
            alarmed(DEFAULTS, () -> act(player, villager, SocialAction.PICKPOCKET, CAUGHT));
            expect(helper, near.getTarget() == null, "no golem turns on a player on Peaceful");
            expect(helper, far.getTarget() == null, "no golem is called on Peaceful");
        } finally {
            server.setDifficulty(before, true);
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A golem the player calms sits out the rest of the alarm: the golem still after them, well within the
     * alarm radius of it, can't call it back in.
     */
    public static void aCalmedGolemSitsOutTheAlarm(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "alarm-calm", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        IronGolem near = golem(helper, SECOND_SPOT, false);
        IronGolem far = golem(helper, FAR_GOLEM, false);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            alarmed(DEFAULTS, () -> {
                act(player, villager, SocialAction.PICKPOCKET, CAUGHT);
                expect(helper, far.getTarget() == player, "a golem 22 blocks away must join the chase");
                act(player, near, SocialAction.CALM, CALMED);
                expect(
                        helper,
                        near.getTarget() == null && near.getPersistentAngerTarget() == null,
                        "the calmed golem must stop going after the player");
                GolemAlarms.tick(server);
                expect(helper, near.getTarget() == null, "no golem may call the calmed one back in");
                expect(helper, far.getTarget() == player, "the alarm must go on without it");
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Test settings with this golem alarm on top. */
    private static void alarmed(ScoresConfig.GolemAlarmSettings alarm, Runnable body) {
        ScoresConfig.SocialSettings base = settings(action -> true, false, 0);
        ScoresConfig.SocialSettings withAlarm = new ScoresConfig.SocialSettings(
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
                alarm,
                base.onGuard(),
                base.wary(),
                base.feel(),
                base.intimidateMob(),
                base.calm(),
                base.insight(),
                base.performance());
        withSocial(
                withAlarm,
                () -> unprofiled(() -> {
                    body.run();
                    return null;
                }));
    }
}
