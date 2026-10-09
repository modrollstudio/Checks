package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withSender;
import static studio.modroll.checks.gametest.ScenarioSupport.withSocial;
import static studio.modroll.checks.gametest.SocialScenarios.FAR_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.IN_FRONT;
import static studio.modroll.checks.gametest.SocialScenarios.SECOND_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.TARGET_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.act;
import static studio.modroll.checks.gametest.SocialScenarios.expectFleesFrom;
import static studio.modroll.checks.gametest.SocialScenarios.settings;
import static studio.modroll.checks.gametest.SocialScenarios.survivor;
import static studio.modroll.checks.gametest.SocialScenarios.villager;
import static studio.modroll.checks.gametest.SocialScenarios.walking;
import static studio.modroll.checks.gametest.SocialScenarios.watchedSurvivor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import studio.modroll.checks.check.RollLinePayload;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.social.SocialAction;
import studio.modroll.checks.social.SocialEffect;
import studio.modroll.checks.social.Socials;
import studio.modroll.checks.social.SpeechBubblePayload;

/**
 * GameTest bodies for witnesses: villagers in gossip range who see a caught pickpocket or a failed threat
 * on another villager. Gossip reaches {@link #WITNESS_RADIUS} blocks here, so nothing in a neighbouring
 * test becomes a witness. Players are new survival players with every score 10 against unprofiled
 * villagers, so a d20 of 2 fails every check.
 */
public final class WitnessScenarios {

    private static final int FAIL = 2;
    /** Covers the whole test structure, and no further. */
    private static final double WITNESS_RADIUS = 6;
    /** Reaches the neighbour next to the target, not the villager at FAR_SPOT. */
    private static final double SHORT_RADIUS = 2;
    /** Inside the structure and in range, walled in with stone so it sees neither the player nor the target. */
    private static final BlockPos HIDDEN_SPOT = new BlockPos(5, 2, 6);

    private static final int TARGET_REFUSE_TICKS = 20;
    private static final int WITNESS_REFUSE_TICKS = 10;
    private static final int BETWEEN_THE_REFUSALS = 15;
    private static final double SAME_SPOT = 0.01;
    private static final ScoresConfig.SocialSettings DEFAULTS = ScoresConfig.DEFAULTS.social();

    private WitnessScenarios() {}

    /**
     * A neighbour who sees a caught pickpocket refuses the player with a refusal line, runs, shows angry
     * particles and says a caught line; a villager walled in out of sight does none of it.
     */
    public static void caughtPickpocketHasWitnesses(GameTestHelper helper) {
        List<Packet<?>> packets = new ArrayList<>();
        ServerPlayer player = watchedSurvivor(helper, "wit-pick", packets);
        Villager victim = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager witness = villager(helper, VillagerProfession.FARMER, SECOND_SPOT);
        Villager hidden = walledIn(helper);
        List<CustomPacketPayload> payloads = new ArrayList<>();
        try {
            witnessing(WITNESS_RADIUS, true, DEFAULTS.pickpocket().failure().refuseTicks(), () -> {
                withSender(sentTo(player, payloads), () -> {
                    act(player, victim, SocialAction.PICKPOCKET, FAIL);
                    return null;
                });
                expect(helper, Socials.refuses(player, witness), "the witness must refuse to trade");
                expectFleesFrom(helper, witness, player, "the witness");
                expectParticles(helper, packets, witness, ParticleTypes.ANGRY_VILLAGER, "angry");
                expectBubble(helper, payloads, witness, "checks.speech.villager.caught.");

                packets.clear();
                boolean refused = Socials.onUseEntity(player, InteractionHand.MAIN_HAND, witness)
                        .consumesAction();
                expect(helper, refused, "the witness's refusal must consume the interaction");
                expectRefusalLine(helper, packets);

                expect(helper, !Socials.refuses(player, hidden), "a villager out of sight still trades");
                expect(helper, !walking(hidden), "a villager out of sight stays put");
                expect(helper, bubblesOver(payloads, hidden) == 0, "a villager out of sight says nothing");
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A neighbour who sees a failed threat refuses the player, runs, sweats and says a failure line. */
    public static void failedThreatHasWitnesses(GameTestHelper helper) {
        List<Packet<?>> packets = new ArrayList<>();
        ServerPlayer player = watchedSurvivor(helper, "wit-threat", packets);
        Villager victim = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager witness = villager(helper, VillagerProfession.FARMER, SECOND_SPOT);
        List<CustomPacketPayload> payloads = new ArrayList<>();
        try {
            witnessing(WITNESS_RADIUS, true, DEFAULTS.intimidate().failure().refuseTicks(), () -> {
                withSender(sentTo(player, payloads), () -> {
                    act(player, victim, SocialAction.INTIMIDATE, FAIL);
                    return null;
                });
                expect(helper, Socials.refuses(player, witness), "the witness must refuse to trade");
                expectFleesFrom(helper, witness, player, "the witness");
                expectParticles(helper, packets, witness, ParticleTypes.SPLASH, "scared");
                expectBubble(helper, payloads, witness, "checks.speech.villager.failure.");
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** With gossip reaching only next door, a villager in plain sight further off is no witness. */
    public static void witnessesMustBeInGossipRange(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "wit-range", IN_FRONT);
        Villager victim = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager near = villager(helper, VillagerProfession.FARMER, SECOND_SPOT);
        Villager far = villager(helper, VillagerProfession.FARMER, FAR_SPOT);
        try {
            witnessing(SHORT_RADIUS, true, DEFAULTS.pickpocket().failure().refuseTicks(), () -> {
                act(player, victim, SocialAction.PICKPOCKET, FAIL);
                expect(helper, far.hasLineOfSight(player), "the far villager must see the player");
                expect(helper, Socials.refuses(player, near), "the near villager must be a witness");
                expect(helper, !Socials.refuses(player, far), "a villager out of range still trades");
                expect(helper, !walking(far), "a villager out of range stays put");
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A witness refuses for half the target's refusal by default, so it trades again first. */
    public static void witnessRefusalIsHalfTheTargets(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "wit-half", IN_FRONT);
        Villager victim = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager witness = villager(helper, VillagerProfession.FARMER, SECOND_SPOT);
        ScoresConfig.SocialSettings shortRefusal = witnessSettings(WITNESS_RADIUS, true, TARGET_REFUSE_TICKS);
        expect(
                helper,
                Math.round(TARGET_REFUSE_TICKS * shortRefusal.witnesses().refuseFraction()) == WITNESS_REFUSE_TICKS,
                "a witness must refuse for half the target's refusal by default");
        withSocial(
                shortRefusal,
                () -> unprofiled(() -> {
                    act(player, victim, SocialAction.PICKPOCKET, FAIL);
                    return null;
                }));
        helper.runAfterDelay(BETWEEN_THE_REFUSALS, () -> {
            try {
                withSocial(shortRefusal, () -> {
                    expect(helper, Socials.refuses(player, victim), "the target still refuses");
                    expect(helper, !Socials.refuses(player, witness), "the witness trades again");
                    return null;
                });
            } finally {
                logout(helper, player);
            }
            helper.succeed();
        });
    }

    /**
     * With witnesses off nobody but the target reacts, and refusals witnesses gave while on are lifted;
     * the target's own refusal stays.
     */
    public static void witnessesSwitchOff(GameTestHelper helper) {
        ServerPlayer seen = survivor(helper, "wit-off-seen", IN_FRONT);
        ServerPlayer unseen = survivor(helper, "wit-off-unseen", IN_FRONT);
        Villager victim = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager witness = villager(helper, VillagerProfession.FARMER, SECOND_SPOT);
        int refuseTicks = DEFAULTS.pickpocket().failure().refuseTicks();
        try {
            witnessing(WITNESS_RADIUS, true, refuseTicks, () -> {
                act(seen, victim, SocialAction.PICKPOCKET, FAIL);
                expect(helper, Socials.refuses(seen, witness), "the witness must refuse while on");
            });
            witnessing(WITNESS_RADIUS, false, refuseTicks, () -> {
                expect(helper, !Socials.refuses(seen, witness), "switching off lifts its refusal");
                expect(helper, Socials.refuses(seen, victim), "the target's own refusal stays");
                act(unseen, victim, SocialAction.PICKPOCKET, FAIL);
                expect(helper, !Socials.refuses(unseen, witness), "no new refusal while off");
                expect(
                        helper,
                        witness.getBrain()
                                        .getMemory(MemoryModuleType.HURT_BY_ENTITY)
                                        .orElse(null)
                                != unseen,
                        "no witness runs while off");
            });
        } finally {
            logout(helper, seen);
            logout(helper, unseen);
        }
        helper.succeed();
    }

    /** A villager at HIDDEN_SPOT in a stone box: walls, floor and roof. */
    private static Villager walledIn(GameTestHelper helper) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    boolean inside = x == 0 && z == 0 && (y == 0 || y == 1);
                    if (!inside) {
                        helper.setBlock(HIDDEN_SPOT.offset(x, y, z), Blocks.STONE);
                    }
                }
            }
        }
        return villager(helper, VillagerProfession.FARMER, HIDDEN_SPOT);
    }

    private static void witnessing(double radius, boolean on, int targetRefuseTicks, Runnable body) {
        withSocial(
                witnessSettings(radius, on, targetRefuseTicks),
                () -> unprofiled(() -> {
                    body.run();
                    return null;
                }));
    }

    /** Test settings with gossip and witnesses reaching {@code radius}, and the pickpocket's refusal set. */
    private static ScoresConfig.SocialSettings witnessSettings(double radius, boolean on, int targetRefuseTicks) {
        ScoresConfig.SocialSettings base = settings(action -> true, false, 0);
        ScoresConfig.PickpocketSettings pickpocket = base.pickpocket();
        SocialEffect failure = pickpocket.failure();
        return new ScoresConfig.SocialSettings(
                true,
                base.nearbyRadius(),
                radius,
                base.persuade(),
                base.deceive(),
                base.intimidate(),
                new ScoresConfig.PickpocketSettings(
                        true,
                        pickpocket.cooldownTicks(),
                        pickpocket.behindDegrees(),
                        new SocialEffect(
                                failure.pricePercent(),
                                failure.priceTicks(),
                                failure.gossip(),
                                targetRefuseTicks,
                                failure.angersGolems())),
                base.passivePrices(),
                base.reactions(),
                new ScoresConfig.WitnessSettings(
                        on,
                        DEFAULTS.witnesses().refuseFraction(),
                        DEFAULTS.witnesses().fleeDistance()),
                base.golemAlarm(),
                DEFAULTS.onGuard(),
                DEFAULTS.wary(),
                base.feel(),
                base.intimidateMob(),
                base.calm(),
                base.insight(),
                base.performance());
    }

    /**
     * Records only what reaches {@code player}: a speech bubble goes to every player in earshot, and
     * players from GameTests running alongside can be close enough to get a copy.
     */
    private static BiPredicate<ServerPlayer, CustomPacketPayload> sentTo(
            ServerPlayer player, List<CustomPacketPayload> payloads) {
        return (to, payload) -> {
            if (payload instanceof RollLinePayload) {
                return false;
            }
            if (to == player) {
                payloads.add(payload);
            }
            return true;
        };
    }

    private static void expectParticles(
            GameTestHelper helper, List<Packet<?>> packets, Villager over, ParticleOptions particle, String mood) {
        boolean shown = packets.stream()
                .filter(ClientboundLevelParticlesPacket.class::isInstance)
                .map(ClientboundLevelParticlesPacket.class::cast)
                .anyMatch(sent -> sent.getParticle().getType() == particle.getType()
                        && Math.abs(sent.getX() - over.getX()) < SAME_SPOT
                        && Math.abs(sent.getZ() - over.getZ()) < SAME_SPOT);
        expect(helper, shown, "the witness must show " + mood + " particles");
    }

    private static void expectBubble(
            GameTestHelper helper, List<CustomPacketPayload> payloads, Villager over, String pool) {
        List<String> keys = payloads.stream()
                .filter(SpeechBubblePayload.class::isInstance)
                .map(SpeechBubblePayload.class::cast)
                .filter(bubble -> bubble.entityId() == over.getId())
                .map(bubble -> bubble.line().getContents() instanceof TranslatableContents line ? line.getKey() : "")
                .toList();
        expect(
                helper,
                keys.size() == 1 && keys.getFirst().startsWith(pool),
                "the witness must say one line from " + pool + "*, said " + keys);
    }

    private static long bubblesOver(List<CustomPacketPayload> payloads, Villager over) {
        return payloads.stream()
                .filter(payload -> payload instanceof SpeechBubblePayload bubble && bubble.entityId() == over.getId())
                .count();
    }

    private static void expectRefusalLine(GameTestHelper helper, List<Packet<?>> packets) {
        boolean refused = packets.stream()
                .filter(ClientboundSystemChatPacket.class::isInstance)
                .map(ClientboundSystemChatPacket.class::cast)
                .anyMatch(sent -> sent.overlay()
                        && sent.content().getContents() instanceof TranslatableContents line
                        && line.getKey().startsWith("checks.social.refuses."));
        expect(helper, refused, "the witness must say a refusal line");
    }
}
