package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withChecksClients;
import static studio.modroll.checks.gametest.ScenarioSupport.withFeel;
import static studio.modroll.checks.gametest.ScenarioSupport.withSender;
import static studio.modroll.checks.gametest.ScenarioSupport.withSocial;
import static studio.modroll.checks.gametest.SocialScenarios.FAR_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.IN_FRONT;
import static studio.modroll.checks.gametest.SocialScenarios.SECOND_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.TARGET_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.act;
import static studio.modroll.checks.gametest.SocialScenarios.piglin;
import static studio.modroll.checks.gametest.SocialScenarios.settings;
import static studio.modroll.checks.gametest.SocialScenarios.survivor;
import static studio.modroll.checks.gametest.SocialScenarios.villager;
import static studio.modroll.checks.gametest.SocialScenarios.watchedSurvivor;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.check.RollLinePayload;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.social.ItemArcPayload;
import studio.modroll.checks.social.SocialAction;
import studio.modroll.checks.social.SpeechBubblePayload;

/**
 * GameTest bodies for the social feel: speech bubbles and the pickpocketed item, as payloads sent to
 * players with Checks around the target. Players are new survival players with every score 10, facing
 * unprofiled targets whose passive skills are 10; how it all looks is verified by hand.
 */
public final class SocialFeelScenarios {

    private static final int NAT_ONE = 1;
    private static final int FAIL = 5;
    private static final int PASSIVE_DC = 10;
    private static final int PERSUADE_DC = 12;
    private static final int GOOD = 18;
    private static final int NAT_TWENTY = 20;
    private static final double SHORT_RADIUS = 4;
    private static final ResourceLocation ANGRY_VOICE = ResourceLocation.parse("checks:villager.angry");
    private static final ResourceLocation VILLAGER_NO = SoundEvents.VILLAGER_NO.getLocation();
    private static final ScoresConfig.FeelSettings DEFAULTS =
            ScoresConfig.DEFAULTS.social().feel();

    private SocialFeelScenarios() {}

    private record Sent(ServerPlayer to, CustomPacketPayload payload) {}

    /**
     * Each outcome's line comes from the target kind's pool for it, a caught pickpocket's from the caught
     * pool; the bubble survives the wire, and a player beyond the radius gets none until the radius
     * reaches them.
     */
    public static void speechBubblesFollowTheOutcome(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "feel-bubble", IN_FRONT);
        ServerPlayer far = survivor(helper, "feel-bubble-far", Vec3.atBottomCenterOf(FAR_SPOT));
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Piglin piglin = piglin(helper, SECOND_SPOT);
        ScoresConfig.FeelSettings shortReach =
                new ScoresConfig.FeelSettings(SHORT_RADIUS, true, DEFAULTS.speechBubbles(), true);
        try {
            withSocial(
                    settings(action -> true, false, 0),
                    () -> withFeel(
                            shortReach,
                            () -> unprofiled(() -> {
                                expectLine(
                                        helper,
                                        player,
                                        villager,
                                        SocialAction.PERSUADE,
                                        NAT_TWENTY,
                                        "villager.critical_success");
                                expectLine(helper, player, villager, SocialAction.PERSUADE, GOOD, "villager.success");
                                expectLine(
                                        helper,
                                        player,
                                        villager,
                                        SocialAction.PERSUADE,
                                        PERSUADE_DC,
                                        "villager.barely");
                                expectLine(helper, player, villager, SocialAction.PERSUADE, FAIL, "villager.failure");
                                expectLine(
                                        helper,
                                        player,
                                        villager,
                                        SocialAction.PERSUADE,
                                        NAT_ONE,
                                        "villager.critical_failure");
                                expectLine(helper, player, villager, SocialAction.PICKPOCKET, FAIL, "villager.caught");
                                expectLine(helper, player, piglin, SocialAction.DECEIVE, FAIL, "piglin.failure");

                                List<Sent> sent = sentBy(() -> act(player, villager, SocialAction.PERSUADE, GOOD));
                                expect(
                                        helper,
                                        sent.stream().noneMatch(s -> s.to() == far),
                                        "a player beyond the radius sees nothing");
                                SpeechBubblePayload bubble = only(helper, sent, player, SpeechBubblePayload.class);
                                expectEquals(
                                        helper,
                                        DEFAULTS.speechBubbles().durationTicks(),
                                        bubble.durationTicks(),
                                        "bubble ticks");
                                RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                                        Unpooled.buffer(), helper.getLevel().registryAccess());
                                SpeechBubblePayload.STREAM_CODEC.encode(buffer, bubble);
                                expect(
                                        helper,
                                        SpeechBubblePayload.STREAM_CODEC
                                                .decode(buffer)
                                                .equals(bubble),
                                        "the bubble must survive the wire");
                                return null;
                            })));
            withSocial(
                    settings(action -> true, false, 0),
                    () -> withFeel(
                            DEFAULTS,
                            () -> unprofiled(() -> {
                                List<Sent> sent = sentBy(() -> act(player, villager, SocialAction.PERSUADE, GOOD));
                                only(helper, sent, far, SpeechBubblePayload.class);
                                return null;
                            })));
        } finally {
            logout(helper, player);
            logout(helper, far);
        }
        helper.succeed();
    }

    /**
     * A lifted item flies to the thief, who swings an arm, and the villager says nothing; one that barely
     * goes unnoticed has the villager stare at the thief; a caught one is snatched back.
     */
    public static void pickpocketedItemsFlyOrAreSnatchedBack(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "feel-pickpocket", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        try {
            withSocial(
                    settings(action -> true, false, 0),
                    () -> unprofiled(() -> {
                        List<Sent> lifted = sentBy(() -> act(player, villager, SocialAction.PICKPOCKET, GOOD));
                        ItemArcPayload flight = only(helper, lifted, player, ItemArcPayload.class);
                        expect(helper, !flight.caught(), "a lifted item is not caught");
                        expect(
                                helper,
                                flight.fromId() == villager.getId() && flight.toId() == player.getId(),
                                "from the villager to the thief");
                        // Any die but the d20 rolls its maximum, so the last offer's result is taken.
                        expect(
                                helper,
                                flight.item().is(Items.EMERALD),
                                "the lifted emerald must fly, was " + flight.item());
                        expect(helper, player.swinging, "the thief's arm must swing");
                        expect(
                                helper,
                                of(lifted, player, SpeechBubblePayload.class).isEmpty(),
                                "an unnoticed theft is silent");

                        act(player, villager, SocialAction.PICKPOCKET, PASSIVE_DC);
                        expect(
                                helper,
                                villager.getBrain()
                                        .getMemory(MemoryModuleType.LOOK_TARGET)
                                        .isPresent(),
                                "a villager that barely missed it must look at the thief");

                        List<Sent> caught = sentBy(() -> act(player, villager, SocialAction.PICKPOCKET, FAIL));
                        ItemArcPayload snatched = only(helper, caught, player, ItemArcPayload.class);
                        expect(helper, snatched.caught(), "a caught pickpocket's item is snatched back");
                        expect(
                                helper,
                                snatched.item().is(Items.BREAD)
                                        || snatched.item().is(Items.EMERALD),
                                "the item must be one of the villager's trades, was " + snatched.item());
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** With the animation and bubbles off, nothing is sent and no arm swings. */
    public static void feelSwitchesOff(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "feel-off", IN_FRONT);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        ScoresConfig.FeelSettings off = new ScoresConfig.FeelSettings(
                DEFAULTS.radius(),
                false,
                new ScoresConfig.SpeechBubbleSettings(
                        false, DEFAULTS.speechBubbles().durationTicks()),
                false);
        try {
            withSocial(
                    settings(action -> true, false, 0),
                    () -> withFeel(
                            off,
                            () -> unprofiled(() -> {
                                List<Sent> sent = sentBy(() -> {
                                    act(player, villager, SocialAction.PICKPOCKET, GOOD);
                                    act(player, villager, SocialAction.PICKPOCKET, FAIL);
                                    act(player, villager, SocialAction.PERSUADE, NAT_TWENTY);
                                });
                                expect(
                                        helper,
                                        sent.stream().noneMatch(s -> s.to() == player),
                                        "nothing must be sent, was " + sent);
                                expect(helper, !player.swinging, "no arm must swing");
                                return null;
                            })));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A failed persuasion angers the villager: a player with Checks hears its angry voice and not
     * vanilla's no, a player without Checks hears vanilla's no and no voice.
     */
    public static void voicesReachChecksClientsAndVanillaSoundsTheRest(GameTestHelper helper) {
        List<Packet<?>> toChecks = new ArrayList<>();
        List<Packet<?>> toVanilla = new ArrayList<>();
        ServerPlayer withChecks = watchedSurvivor(helper, "feel-voice-checks", toChecks);
        ServerPlayer withoutChecks = watchedSurvivor(helper, "feel-voice-vanilla", toVanilla);
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        try {
            withSocial(
                    settings(action -> true, false, 0),
                    () -> withChecksClients(
                            player -> player == withChecks,
                            () -> unprofiled(() -> {
                                toChecks.clear();
                                toVanilla.clear();
                                act(withChecks, villager, SocialAction.PERSUADE, FAIL);
                                return null;
                            })));
            expectSounds(helper, List.of(ANGRY_VOICE), toChecks, "a Checks client");
            expectSounds(helper, List.of(VILLAGER_NO), toVanilla, "a vanilla client");
        } finally {
            logout(helper, withChecks);
            logout(helper, withoutChecks);
        }
        helper.succeed();
    }

    private static void expectSounds(
            GameTestHelper helper, List<ResourceLocation> expected, List<Packet<?>> sent, String who) {
        List<ResourceLocation> heard = sounds(sent);
        expect(helper, heard.equals(expected), who + " must hear " + expected + ", heard " + heard);
    }

    /** The lists are emptied right before the action, which runs at once, so no other test's sounds get in. */
    private static List<ResourceLocation> sounds(List<Packet<?>> sent) {
        return sent.stream()
                .filter(ClientboundSoundPacket.class::isInstance)
                .map(packet ->
                        ((ClientboundSoundPacket) packet).getSound().value().getLocation())
                .toList();
    }

    private static void expectLine(
            GameTestHelper helper,
            ServerPlayer player,
            LivingEntity target,
            SocialAction action,
            int face,
            String pool) {
        SpeechBubblePayload bubble =
                only(helper, sentBy(() -> act(player, target, action, face)), player, SpeechBubblePayload.class);
        expect(helper, bubble.entityId() == target.getId(), "the bubble must float above the target");
        String key = bubble.line().getContents() instanceof TranslatableContents line ? line.getKey() : "";
        expect(
                helper,
                key.startsWith("checks.speech." + pool + "."),
                action.id() + " on a d20 of " + face + " must say a " + pool + " line, said " + key);
    }

    private static List<Sent> sentBy(Runnable body) {
        List<Sent> sent = new ArrayList<>();
        withSender((to, payload) -> !(payload instanceof RollLinePayload) && sent.add(new Sent(to, payload)), () -> {
            body.run();
            return null;
        });
        return sent;
    }

    private static <P extends CustomPacketPayload> List<P> of(List<Sent> sent, ServerPlayer to, Class<P> type) {
        return sent.stream()
                .filter(s -> s.to() == to && type.isInstance(s.payload()))
                .map(s -> type.cast(s.payload()))
                .toList();
    }

    private static <P extends CustomPacketPayload> P only(
            GameTestHelper helper, List<Sent> sent, ServerPlayer to, Class<P> type) {
        List<P> payloads = of(sent, to, type);
        expectEquals(helper, 1, payloads.size(), type.getSimpleName() + "s sent");
        return payloads.getFirst();
    }
}
