package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withChecksClients;
import static studio.modroll.checks.gametest.ScenarioSupport.withSender;
import static studio.modroll.checks.gametest.ScenarioSupport.withSocial;
import static studio.modroll.checks.gametest.SocialScenarios.IN_FRONT;
import static studio.modroll.checks.gametest.SocialScenarios.SECOND_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.TARGET_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.calm;
import static studio.modroll.checks.gametest.SocialScenarios.replacing;
import static studio.modroll.checks.gametest.SocialScenarios.settings;
import static studio.modroll.checks.gametest.SocialScenarios.survivor;
import static studio.modroll.checks.gametest.SocialScenarios.watchedSurvivor;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.social.HostilityPayload;
import studio.modroll.checks.social.Insight;

/**
 * GameTest bodies for passive Insight. Players are new survival players with every score 10 and no
 * proficiency, so their passive Insight is 10. Insight's tick runs here, under each test's settings; the
 * server's own runs under the live config, whose DC of 12 a passive 10 never meets.
 */
public final class InsightScenarios {

    private static final int PASSIVE_INSIGHT = 10;
    private static final double RANGE = 6;
    private static final int FUSE_LIT = 1;
    private static final int FUSE_OUT = -1;

    private InsightScenarios() {}

    /**
     * A zombie after the player is marked only while passive Insight meets the DC, and one after nobody never
     * is. The client hears only of a change, and of the end, once the sense is off.
     */
    public static void aMobAfterThePlayerIsMarkedOnlyWhenPassiveInsightMeetsTheDc(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "insight-sense", IN_FRONT);
        Mob hunter = calm(helper, EntityType.ZOMBIE, TARGET_SPOT);
        Mob idle = calm(helper, EntityType.ZOMBIE, SECOND_SPOT);
        hunter.setTarget(player);
        MinecraftServer server = helper.getLevel().getServer();
        List<HostilityPayload> sent = new ArrayList<>();
        try {
            sensing(player, sent, sense(true, PASSIVE_INSIGHT + 1), sense(false, PASSIVE_INSIGHT), () -> {
                Insight.tick(server);
                expect(helper, sent.isEmpty(), "below the DC no mob may be marked");
            });
            sensing(player, sent, sense(true, PASSIVE_INSIGHT), sense(false, PASSIVE_INSIGHT), () -> {
                Insight.tick(server);
                Insight.tick(server);
                expectEquals(helper, 1, sent.size(), "payloads for an unchanged set");
                expect(
                        helper,
                        sent.getFirst().entityIds().equals(List.of(hunter.getId())),
                        "only the zombie after the player may be marked, was "
                                + sent.getFirst().entityIds() + "; the idle one is " + idle.getId());
            });
            sensing(player, sent, sense(false, PASSIVE_INSIGHT), sense(false, PASSIVE_INSIGHT), () -> {
                Insight.tick(server);
                expect(
                        helper,
                        sent.size() == 2 && sent.getLast().entityIds().isEmpty(),
                        "switching the sense off must clear the markers");
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A creeper that lights its fuse within range is marked at once and warns the player with a sound, once
     * per fuse, while passive Insight meets the DC; never with the warning off or below the DC.
     */
    public static void aFusingCreeperWarnsOncePerFuse(GameTestHelper helper) {
        List<Packet<?>> packets = new ArrayList<>();
        ServerPlayer player = watchedSurvivor(helper, "insight-creeper", packets);
        Creeper creeper = calm(helper, EntityType.CREEPER, TARGET_SPOT);
        MinecraftServer server = helper.getLevel().getServer();
        List<HostilityPayload> sent = new ArrayList<>();
        try {
            withChecksClients(client -> client == player, () -> {
                creeper.setSwellDir(FUSE_LIT);
                sensing(player, sent, sense(false, 0), sense(false, 0), () -> Insight.tick(server));
                sensing(player, sent, sense(false, 0), sense(true, PASSIVE_INSIGHT + 1), () -> Insight.tick(server));
                expectEquals(helper, 0, warnings(packets), "warnings while off or below the DC");
                expect(helper, sent.isEmpty(), "nothing may be marked while off or below the DC");
                sensing(player, sent, sense(false, 0), sense(true, PASSIVE_INSIGHT), () -> {
                    Insight.tick(server);
                    expectEquals(helper, 1, warnings(packets), "warnings for a lit fuse");
                    expect(
                            helper,
                            sent.size() == 1 && sent.getFirst().entityIds().equals(List.of(creeper.getId())),
                            "the creeper must be marked at once");
                    Insight.tick(server);
                    expectEquals(helper, 1, warnings(packets), "warnings for the same fuse");
                    creeper.setSwellDir(FUSE_OUT);
                    Insight.tick(server);
                    creeper.setSwellDir(FUSE_LIT);
                    Insight.tick(server);
                    expectEquals(helper, 2, warnings(packets), "warnings once the fuse is lit again");
                });
                return null;
            });
        } finally {
            creeper.discard();
            logout(helper, player);
        }
        helper.succeed();
    }

    private static int warnings(List<Packet<?>> packets) {
        return (int) packets.stream()
                .filter(packet -> packet instanceof ClientboundSoundPacket sound
                        && sound.getSound().value().getLocation().equals(Insight.WARNING))
                .count();
    }

    private static ScoresConfig.InsightSense sense(boolean enabled, int dc) {
        return new ScoresConfig.InsightSense(enabled, dc, RANGE);
    }

    /** Runs {@code body} with these senses, unprofiled, adding what Insight sends the player to {@code sent}. */
    private static void sensing(
            ServerPlayer player,
            List<HostilityPayload> sent,
            ScoresConfig.InsightSense hostility,
            ScoresConfig.InsightSense creepers,
            Runnable body) {
        ScoresConfig.SocialSettings base = settings(action -> true, false, 0);
        withSocial(
                replacing(
                        base,
                        base.intimidateMob(),
                        base.calm(),
                        new ScoresConfig.InsightSettings(hostility, creepers),
                        base.performance()),
                () -> withSender(
                        (to, payload) ->
                                to == player && payload instanceof HostilityPayload hostile && sent.add(hostile),
                        () -> unprofiled(() -> {
                            body.run();
                            return null;
                        })));
    }
}
