package studio.modroll.checks.social;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.Checks;
import studio.modroll.checks.ClientPayloads;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.text.FallbackText;

/**
 * Cosmetics that make social actions feel alive: mood voices, speech bubbles and the pickpocket animation.
 * None of them changes an outcome, and any line picked is picked with Minecraft's own randomness. Voices
 * are sound events no registry knows ({@code checks:<target>.<mood>}), so a resource pack can swap them
 * while clients without Checks can still join. Only clients with Checks hear a voice, in place of the
 * reaction's vanilla sound, which everyone else still hears; speech bubbles and the flying item reach
 * players with Checks around the target.
 */
public final class SocialFeel {

    private static final int STARE_TICKS = 20;
    private static final int GLANCE_TICKS = 12;
    private static final double GLANCE_DISTANCE = 4;
    private static final float QUARTER_TURN = 90;
    private static final float POP_VOLUME = 0.2f;
    private static final float POP_PITCH = 1.6f;
    private static final float SNATCH_VOLUME = 0.6f;
    private static final float SNATCH_PITCH = 0.7f;
    private static final float VOICE_VOLUME = 1f;
    private static final float VOICE_PITCH = 1f;

    private static final List<Pending> PENDING = new ArrayList<>();

    /** Until a loader sets one, every client counts as one without Checks and hears vanilla's sounds. */
    private static Predicate<ServerPlayer> checksClient = player -> false;

    private SocialFeel() {}

    private record Pending(long dueTick, Runnable action) {}

    /** Each loader sets how to tell whether a player's client has Checks. */
    public static void setChecksClient(Predicate<ServerPlayer> hasChecks) {
        checksClient = hasChecks;
    }

    public static Predicate<ServerPlayer> checksClient() {
        return checksClient;
    }

    public static ResourceLocation voiceId(SocialTarget target, SocialMood mood) {
        return Checks.id(target.id() + "." + mood.id());
    }

    /** The target voices its mood and says a line about the outcome, each when switched on. */
    static void react(
            ServerPlayer player, LivingEntity target, SocialTarget kind, SocialAction action, SocialOutcome outcome) {
        voiceAndSay(player, target, kind, SocialMood.of(action, outcome), SpeechPools.of(kind, action, outcome));
    }

    /** The speaker voices {@code mood} and says a line from {@code pool}, each when switched on; mobs keep quiet. */
    static void voiceAndSay(
            ServerPlayer player,
            LivingEntity speaker,
            SocialTarget kind,
            Optional<SocialMood> mood,
            Optional<String> pool) {
        ScoresConfig.FeelSettings feel = settings();
        if (kind.mob()) {
            return;
        }
        if (feel.voices()) {
            mood.ifPresent(voiced -> voice(speaker, kind, voiced));
        }
        if (feel.speechBubbles().enabled()) {
            pool.ifPresent(line -> say(player, speaker, line, feel));
        }
    }

    /** A villager that saw the player's crime says a line from the target's pool for that failure. */
    static void witnessed(ServerPlayer player, Villager witness, SocialAction action) {
        ScoresConfig.FeelSettings feel = settings();
        if (feel.speechBubbles().enabled()) {
            SpeechPools.of(SocialTarget.VILLAGER, action, SocialOutcome.FAILURE)
                    .ifPresent(pool -> say(player, witness, pool, feel));
        }
    }

    /**
     * The thief's arm swings and the item flies to them with a pop; a target that barely missed it stares
     * at the thief, then looks around.
     */
    static void stolen(ServerPlayer thief, LivingEntity target, ItemStack item, boolean barely) {
        ScoresConfig.FeelSettings feel = settings();
        if (!feel.pickpocketAnimation()) {
            return;
        }
        thief.swing(InteractionHand.MAIN_HAND, true);
        showNearby(target, new ItemArcPayload(target.getId(), thief.getId(), item.copy(), false), feel.radius());
        later(thief.server, ItemArc.STOLEN_TICKS, () -> pop(thief));
        if (barely && target instanceof Villager villager) {
            lookAround(villager, thief);
        }
    }

    /** The thief's arm swings, the item drops, and the target snatches it back. */
    static void caught(ServerPlayer thief, LivingEntity target, ItemStack item) {
        ScoresConfig.FeelSettings feel = settings();
        if (!feel.pickpocketAnimation()) {
            return;
        }
        thief.swing(InteractionHand.MAIN_HAND, true);
        showNearby(target, new ItemArcPayload(target.getId(), thief.getId(), item.copy(), true), feel.radius());
        later(thief.server, ItemArc.SNATCH_TICK, () -> snatch(target));
    }

    /** Runs whatever is due; called once per server tick. */
    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        List<Pending> due =
                PENDING.stream().filter(pending -> pending.dueTick() <= now).toList();
        PENDING.removeAll(due);
        due.forEach(pending -> pending.action().run());
    }

    public static void clear() {
        PENDING.clear();
    }

    /**
     * A reaction's vanilla sound, from the target. While voices are on, only players without Checks hear
     * it; those with Checks hear the target's voice instead.
     */
    static void reactionSound(LivingEntity target, SoundEvent sound) {
        if (!settings().voices()) {
            target.playSound(sound);
            return;
        }
        playFor(target, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), checksClient.negate());
    }

    private static void voice(LivingEntity target, SocialTarget kind, SocialMood mood) {
        Holder<SoundEvent> sound = Holder.direct(SoundEvent.createVariableRangeEvent(voiceId(kind, mood)));
        later(target.getServer(), mood.delayTicks(), () -> {
            if (target.isAlive()) {
                playFor(target, sound, checksClient);
            }
        });
    }

    /** As {@link LivingEntity#playSound(SoundEvent)}, but only to the players in earshot that {@code hears}. */
    private static void playFor(LivingEntity target, Holder<SoundEvent> sound, Predicate<ServerPlayer> hears) {
        if (target.isSilent()) {
            return;
        }
        ServerLevel level = (ServerLevel) target.level();
        double range = sound.value().getRange(VOICE_VOLUME);
        ClientboundSoundPacket packet = new ClientboundSoundPacket(
                sound,
                target.getSoundSource(),
                target.getX(),
                target.getY(),
                target.getZ(),
                VOICE_VOLUME,
                VOICE_PITCH,
                level.getRandom().nextLong());
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(target) < range * range && hears.test(player)) {
                player.connection.send(packet);
            }
        }
    }

    private static void say(ServerPlayer player, LivingEntity target, String pool, ScoresConfig.FeelSettings feel) {
        SpeechBubblePayload bubble = new SpeechBubblePayload(
                target.getId(),
                FallbackText.oneOf(pool, target.getRandom(), player.getDisplayName()),
                feel.speechBubbles().durationTicks());
        showNearby(target, bubble, feel.radius());
    }

    private static void showNearby(LivingEntity target, CustomPacketPayload payload, double radius) {
        for (ServerPlayer viewer : ((ServerLevel) target.level()).players()) {
            if (viewer.distanceToSqr(target) <= radius * radius) {
                ClientPayloads.send(viewer, payload);
            }
        }
    }

    private static void pop(ServerPlayer thief) {
        thief.level()
                .playSound(
                        null,
                        thief.getX(),
                        thief.getY(),
                        thief.getZ(),
                        SoundEvents.ITEM_PICKUP,
                        SoundSource.PLAYERS,
                        POP_VOLUME,
                        POP_PITCH);
    }

    private static void snatch(LivingEntity target) {
        if (target.isAlive()) {
            target.swing(InteractionHand.MAIN_HAND);
            target.playSound(SoundEvents.ITEM_PICKUP, SNATCH_VOLUME, SNATCH_PITCH);
        }
    }

    /** Through the villager's brain, which turns its head to whatever it is told to look at. */
    private static void lookAround(Villager villager, ServerPlayer thief) {
        villager.getBrain()
                .setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET, new EntityTracker(thief, true), STARE_TICKS);
        later(thief.server, STARE_TICKS, () -> glance(villager, -QUARTER_TURN));
        later(thief.server, STARE_TICKS + GLANCE_TICKS, () -> glance(villager, QUARTER_TURN));
    }

    private static void glance(Villager villager, float turn) {
        Vec3 direction = Vec3.directionFromRotation(0, villager.getYRot() + turn);
        Vec3 spot = villager.getEyePosition().add(direction.scale(GLANCE_DISTANCE));
        villager.getBrain().setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(spot), GLANCE_TICKS);
    }

    private static void later(MinecraftServer server, int ticks, Runnable action) {
        if (ticks <= 0) {
            action.run();
            return;
        }
        PENDING.add(new Pending(server.getTickCount() + ticks, action));
    }

    private static ScoresConfig.FeelSettings settings() {
        return ScoresRuntime.config().social().feel();
    }
}
