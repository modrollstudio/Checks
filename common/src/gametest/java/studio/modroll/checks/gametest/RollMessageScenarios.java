package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.collecting;
import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withRollMessages;
import static studio.modroll.checks.gametest.ScenarioSupport.withSaves;
import static studio.modroll.checks.gametest.ScenarioSupport.withSender;
import static studio.modroll.checks.gametest.ScenarioSupport.withSocial;
import static studio.modroll.checks.gametest.SocialScenarios.IN_FRONT;
import static studio.modroll.checks.gametest.SocialScenarios.TARGET_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.act;
import static studio.modroll.checks.gametest.SocialScenarios.settings;
import static studio.modroll.checks.gametest.SocialScenarios.villager;
import static studio.modroll.checks.gametest.SocialScenarios.watchedSurvivor;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.GameType;
import studio.modroll.checks.check.RollLinePayload;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.social.SocialAction;

/**
 * GameTest bodies for roll lines above the hotbar: a client with Checks gets the roll and its detail with
 * their duration, anyone else only the roll as vanilla's action bar message. A poison save (a d20 of 19
 * makes it) and a made persuasion are the rolls.
 */
public final class RollMessageScenarios {

    private static final int SAVE = 19;
    private static final int POISON_TICKS = 200;
    private static final int SHORT_DURATION = 40;
    private static final int PERSUADE_SUCCESS = 18;
    private static final String PERSUADE_SUCCESS_POOL = "checks.social.persuade.success.";

    private RollMessageScenarios() {}

    /** A client with Checks gets the save's line with the configured duration, and it survives the wire. */
    public static void checksClientsGetTheLineWithItsDuration(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "roll-line");
        try {
            List<CustomPacketPayload> sent = new ArrayList<>();
            withRollMessages(
                    new ScoresConfig.RollMessageSettings(true, SHORT_DURATION),
                    () -> withSender(collecting(RollLinePayload.class, sent), () -> poisonSave(player)));
            expectEquals(helper, 1, sent.size(), "roll lines sent");
            expect(helper, sent.getFirst() instanceof RollLinePayload, "the line must be a roll line payload");
            RollLinePayload line = (RollLinePayload) sent.getFirst();
            expectEquals(helper, SHORT_DURATION, line.durationTicks(), "duration");
            expect(helper, line.roll().getString().contains("DC 12"), "the line must be the save, was " + line);
            expect(helper, line.detail().isEmpty(), "a save has no detail line, had " + line.detail());
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(), helper.getLevel().registryAccess());
            RollLinePayload.STREAM_CODEC.encode(buffer, line);
            expect(helper, RollLinePayload.STREAM_CODEC.decode(buffer).equals(line), "the line must survive the wire");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** With roll messages off nothing is offered to the client: it gets vanilla's action bar message. */
    public static void rollMessagesSwitchOff(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "roll-line-off");
        try {
            List<CustomPacketPayload> offered = new ArrayList<>();
            withRollMessages(
                    new ScoresConfig.RollMessageSettings(false, SHORT_DURATION),
                    () -> withSender(collecting(RollLinePayload.class, offered), () -> poisonSave(player)));
            expectEquals(helper, 0, offered.size(), "roll lines offered with roll messages off");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A social action's roll and its flavor line go to a client with Checks as two lines, and both survive
     * the wire.
     */
    public static void socialRollsSendTheFlavorOnALineOfItsOwn(GameTestHelper helper) {
        ServerPlayer player = SocialScenarios.survivor(helper, "roll-line-social", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        try {
            List<CustomPacketPayload> sent = new ArrayList<>();
            withSender(collecting(RollLinePayload.class, sent), () -> persuade(player, villager));
            expectEquals(helper, 1, sent.size(), "roll lines sent");
            RollLinePayload line = (RollLinePayload) sent.getFirst();
            expectRoll(helper, line.roll(), "the first line is the roll");
            expect(helper, !hasFlavor(line.roll()), "the roll line must not carry the flavor too");
            expect(
                    helper,
                    line.detail()
                            .map(detail -> key(detail).startsWith(PERSUADE_SUCCESS_POOL))
                            .orElse(false),
                    "the second line must be the persuasion flavor, was " + line.detail());
            expectEquals(helper, 2, line.lines().size(), "lines");
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(), helper.getLevel().registryAccess());
            RollLinePayload.STREAM_CODEC.encode(buffer, line);
            expect(
                    helper,
                    RollLinePayload.STREAM_CODEC.decode(buffer).equals(line),
                    "both lines must survive the wire");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A client without Checks gets only the roll, as vanilla's action bar message, so it fits on one line. */
    public static void clientsWithoutChecksGetOnlyTheRoll(GameTestHelper helper) {
        List<Packet<?>> packets = new ArrayList<>();
        ServerPlayer player = watchedSurvivor(helper, "roll-line-vanilla", packets);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        try {
            packets.clear();
            withSender((to, payload) -> false, () -> persuade(player, villager));
            List<Component> actionBar = packets.stream()
                    .filter(packet -> packet instanceof ClientboundSystemChatPacket chat && chat.overlay())
                    .map(packet -> ((ClientboundSystemChatPacket) packet).content())
                    .toList();
            expectEquals(helper, 1, actionBar.size(), "action bar messages");
            expectRoll(helper, actionBar.getFirst(), "the action bar message is the roll");
            expect(
                    helper,
                    !hasFlavor(actionBar.getFirst()),
                    "the roll must not carry the flavor line, was "
                            + actionBar.getFirst().getString());
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    private static Void persuade(ServerPlayer player, Villager villager) {
        withSocial(
                settings(action -> true, false, 0),
                () -> unprofiled(() -> {
                    act(player, villager, SocialAction.PERSUADE, PERSUADE_SUCCESS);
                    return null;
                }));
        return null;
    }

    private static void expectRoll(GameTestHelper helper, Component line, String what) {
        expect(helper, key(line).equals("checks.social.roll"), what + ", was " + key(line));
    }

    private static boolean hasFlavor(Component roll) {
        return roll.getContents() instanceof TranslatableContents contents
                && Arrays.stream(contents.getArgs())
                        .anyMatch(arg ->
                                arg instanceof Component part && key(part).startsWith(PERSUADE_SUCCESS_POOL));
    }

    private static String key(Component component) {
        return component.getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
    }

    private static Void poisonSave(ServerPlayer player) {
        ScoresConfig.SaveSettings defaults = ScoresConfig.DEFAULTS.saves();
        withSaves(
                new ScoresConfig.SaveSettings(
                        defaults.profiledMobs(), 0, defaults.knockbackMinStrength(), defaults.saves()),
                () -> withD20s(() -> player.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS)), SAVE));
        return null;
    }

    private static ServerPlayer survivor(GameTestHelper helper, String name) {
        ServerPlayer player = login(helper, newProfile(name));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }
}
