package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.loginWatched;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.withConfig;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * GameTest bodies for {@code /checks roll} run by players without op: a player rolls for their own character,
 * the result reaches every player within {@code player_rolls.radius}, and rolling for anyone else stays op only.
 */
public final class PlayerRollScenarios {

    private static final int PLAYER_PERMISSION = 0;
    private static final double NEAR = 4.0;
    private static final double FAR = 40.0;

    private PlayerRollScenarios() {}

    /** The roller and a player nearby see the result, with the roller's name; a player far off does not. */
    public static void aPlayerRollsForThemselvesAndPlayersNearbySeeIt(GameTestHelper helper) {
        List<Packet<?>> rollerSent = new ArrayList<>();
        List<Packet<?>> nearSent = new ArrayList<>();
        List<Packet<?>> farSent = new ArrayList<>();
        ServerPlayer roller = placed(helper, loginWatched(helper, newProfile("PrRoller"), rollerSent), 0);
        ServerPlayer near = placed(helper, loginWatched(helper, newProfile("PrNear"), nearSent), NEAR);
        ServerPlayer far = placed(helper, loginWatched(helper, newProfile("PrFar"), farSent), FAR);
        try {
            rollerSent.clear();
            nearSent.clear();
            farSent.clear();
            withD20s(() -> runAs(helper, roller, "checks roll dex 5"), 13);

            expectRollLine(helper, rollerSent, "the roller");
            expectRollLine(helper, nearSent, "a player " + NEAR + " blocks away");
            expect(helper, rollLines(farSent).isEmpty(), "a player " + FAR + " blocks away must not see the roll");
        } finally {
            logout(helper, roller);
            logout(helper, near);
            logout(helper, far);
        }
        helper.succeed();
    }

    /** The short form rolls for whoever runs it, with or without a DC; naming yourself still works too. */
    public static void theShortFormRollsForThePlayerWhoRunsIt(GameTestHelper helper) {
        ServerPlayer roller = placed(helper, loginWatched(helper, newProfile("PrShort"), new ArrayList<>()), 0);
        try {
            int open = withD20s(() -> runAs(helper, roller, "checks roll stealth"), 9);
            int named = withD20s(() -> runAs(helper, roller, "checks roll PrShort stealth"), 9);
            int againstDc = withD20s(() -> runAs(helper, roller, "checks roll stealth 15"), 9);
            expect(
                    helper,
                    open == named && againstDc == open,
                    "the short form, the named form and the short form with a DC must all roll the runner's own"
                            + " stealth: " + open + ", " + named + ", " + againstDc);
        } finally {
            logout(helper, roller);
        }
        helper.succeed();
    }

    /** Without op a player may not roll for another player. */
    public static void aPlayerCannotRollForAnotherPlayer(GameTestHelper helper) {
        ServerPlayer roller = placed(helper, loginWatched(helper, newProfile("PrOther"), new ArrayList<>()), 0);
        ServerPlayer other = placed(helper, loginWatched(helper, newProfile("PrTarget"), new ArrayList<>()), NEAR);
        try {
            expectRefused(helper, roller, "checks roll PrTarget dex", "rolling for another player without op");
        } finally {
            logout(helper, roller);
            logout(helper, other);
        }
        helper.succeed();
    }

    /** With {@code player_rolls.enabled: false} the command is op only again, even for your own character. */
    public static void playerRollsOffMakeTheCommandOpOnly(GameTestHelper helper) {
        ServerPlayer roller = placed(helper, loginWatched(helper, newProfile("PrOff"), new ArrayList<>()), 0);
        try {
            withPlayerRolls(new ScoresConfig.PlayerRollSettings(false, 16.0), () -> {
                expectRefused(helper, roller, "checks roll dex", "a player's own roll with player rolls off");
                expectRefused(helper, roller, "checks roll PrOff dex", "a player's named roll with player rolls off");
                return null;
            });
        } finally {
            logout(helper, roller);
        }
        helper.succeed();
    }

    private static ServerPlayer placed(GameTestHelper helper, ServerPlayer player, double east) {
        Vec3 pos = helper.absoluteVec(new Vec3(1.5 + east, 1, 1.5));
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);
        return player;
    }

    private static int runAs(GameTestHelper helper, ServerPlayer player, String command) {
        try {
            return execute(helper, player, command);
        } catch (CommandSyntaxException e) {
            helper.fail("command \"" + command + "\" failed: " + e.getMessage());
            return 0;
        }
    }

    private static void expectRefused(GameTestHelper helper, ServerPlayer player, String command, String what) {
        try {
            execute(helper, player, command);
        } catch (CommandSyntaxException e) {
            return;
        }
        helper.fail(what + " must be refused");
    }

    private static int execute(GameTestHelper helper, ServerPlayer player, String command)
            throws CommandSyntaxException {
        return helper.getLevel()
                .getServer()
                .getCommands()
                .getDispatcher()
                .execute(command, player.createCommandSourceStack().withPermission(PLAYER_PERMISSION));
    }

    private static void expectRollLine(GameTestHelper helper, List<Packet<?>> sent, String who) {
        List<String> lines = rollLines(sent);
        expect(
                helper,
                lines.size() == 1 && lines.getFirst().startsWith("PrRoller rolls dex vs DC 5"),
                who + " must see one roll line naming the roller, saw " + lines);
    }

    private static List<String> rollLines(List<Packet<?>> sent) {
        return sent.stream()
                .filter(packet -> packet instanceof ClientboundSystemChatPacket chat && !chat.overlay())
                .map(packet -> ((ClientboundSystemChatPacket) packet).content().getString())
                .filter(line -> line.startsWith("PrRoller rolls"))
                .toList();
    }

    private static <T> T withPlayerRolls(ScoresConfig.PlayerRollSettings patched, Supplier<T> body) {
        ScoresConfig live = ScoresRuntime.config();
        return withConfig(
                new ScoresConfig(
                        live.playerDefaults(),
                        live.derivation(),
                        live.profilesEnabled(),
                        live.skillsEnabled(),
                        live.proficiency(),
                        live.critfall(),
                        live.statScreenEnabled(),
                        live.creation(),
                        live.levelling(),
                        live.extensions(),
                        live.body(),
                        live.saves(),
                        live.traits(),
                        live.social(),
                        live.rollMessages(),
                        live.deathMessages(),
                        live.exploration(),
                        live.rest(),
                        patched),
                body);
    }
}
