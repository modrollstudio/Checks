package studio.modroll.checks.social;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

final class SocialReactions {

    private static final int HEAD_SHAKE_TICKS = 40;

    private static final int ANGRY_PARTICLES = 5;
    private static final int HAPPY_PARTICLES = 8;
    private static final int SWEAT_PARTICLES = 12;
    private static final double PARTICLE_SPEED = 0.02;
    private static final int SPIT_POWER = 1;
    private static final int CLOSE_ENOUGH = 0;

    private SocialReactions() {}

    static void play(ServerPlayer player, LivingEntity target, Set<SocialReaction> reactions) {
        ScoresConfig.SocialSettings settings = ScoresRuntime.config().social();
        if (!settings.reactions().enabled()) {
            return;
        }
        for (SocialReaction reaction : reactions) {
            play(player, target, reaction, settings);
        }
    }

    private static void play(
            ServerPlayer player, LivingEntity target, SocialReaction reaction, ScoresConfig.SocialSettings settings) {
        ScoresConfig.ReactionSettings reactions = settings.reactions();
        Consumer<SoundEvent> sound = vanilla -> SocialFeel.reactionSound(target, vanilla);
        switch (reaction) {
            case SHAKE_HEAD -> shakeHead(target, sound);
            case ANGRY -> {
                showAngry(target);
                if (target instanceof Piglin) {
                    sound.accept(SoundEvents.PIGLIN_ANGRY);
                }
            }
            case PLEASED -> {
                particles(target, ParticleTypes.HAPPY_VILLAGER, HAPPY_PARTICLES);
                pleasedSound(target).ifPresent(sound);
            }
            case FLEE -> flee(player, target, reactions.fleeDistance(), reactions);
            case COWER -> {
                showScared(target);
                if (target instanceof Piglin) {
                    sound.accept(SoundEvents.PIGLIN_RETREAT);
                }
            }
            case LLAMAS_SPIT -> {
                if (settings.deceive().wanderingTrader().llamasSpit()) {
                    llamasSpit(player, target, settings.nearbyRadius());
                }
            }
        }
    }

    static void show(LivingEntity listener, SocialReaction reaction) {
        if (!ScoresRuntime.config().social().reactions().enabled()) {
            return;
        }
        if (reaction == SocialReaction.PLEASED) {
            particles(listener, ParticleTypes.HAPPY_VILLAGER, HAPPY_PARTICLES);
        } else if (reaction == SocialReaction.ANGRY) {
            showAngry(listener);
        }
    }

    static void shakeHead(LivingEntity target) {
        shakeHead(target, target::playSound);
    }

    private static void shakeHead(LivingEntity target, Consumer<SoundEvent> sayNo) {
        if (!(target instanceof AbstractVillager trader)) {
            return;
        }
        trader.setUnhappyCounter(HEAD_SHAKE_TICKS);
        sayNo.accept(trader instanceof WanderingTrader ? SoundEvents.WANDERING_TRADER_NO : SoundEvents.VILLAGER_NO);
    }

    private static Optional<SoundEvent> pleasedSound(LivingEntity target) {
        return switch (target) {
            case Piglin piglin -> Optional.of(SoundEvents.PIGLIN_ADMIRING_ITEM);
            case WanderingTrader trader -> Optional.of(SoundEvents.WANDERING_TRADER_YES);
            case Villager villager -> Optional.of(SoundEvents.VILLAGER_YES);
            default -> Optional.empty();
        };
    }

    static void showAngry(LivingEntity target) {
        particles(target, ParticleTypes.ANGRY_VILLAGER, ANGRY_PARTICLES);
    }

    static void showScared(LivingEntity target) {
        particles(target, ParticleTypes.SPLASH, SWEAT_PARTICLES);
    }

    /** Idle behaviors soon replace a bare walk target, so the villager panics as it does when hurt by the player. */
    static void flee(
            ServerPlayer player, LivingEntity target, double distance, ScoresConfig.ReactionSettings reactions) {
        if (!(target instanceof Villager villager)) {
            return;
        }
        Brain<Villager> brain = villager.getBrain();
        brain.eraseMemory(MemoryModuleType.PATH);
        brain.setMemoryWithExpiry(MemoryModuleType.HURT_BY_ENTITY, player, reactions.fleeTicks());
        brain.setActiveActivityIfPossible(Activity.PANIC);
        brain.setMemory(
                MemoryModuleType.WALK_TARGET,
                new WalkTarget(awayFrom(player, villager, distance), (float) reactions.fleeSpeed(), CLOSE_ENOUGH));
    }

    static void calmDown(ServerPlayer player, Villager villager) {
        Brain<Villager> brain = villager.getBrain();
        if (brain.getMemory(MemoryModuleType.HURT_BY_ENTITY).orElse(null) == player) {
            brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }

    static Vec3 awayFrom(ServerPlayer player, LivingEntity villager, double distance) {
        Vec3 away = villager.position().subtract(player.position()).multiply(1, 0, 1);
        Vec3 direction = away.lengthSqr() == 0 ? villager.getLookAngle().multiply(1, 0, 1) : away;
        return villager.position().add(direction.normalize().scale(distance));
    }

    private static void llamasSpit(ServerPlayer player, LivingEntity trader, double radius) {
        for (Llama llama : player.level()
                .getEntitiesOfClass(
                        Llama.class,
                        trader.getBoundingBox().inflate(radius),
                        llama -> llama.getLeashHolder() == trader)) {
            llama.performRangedAttack(player, SPIT_POWER);
        }
    }

    private static void particles(LivingEntity target, ParticleOptions particle, int count) {
        ((ServerLevel) target.level())
                .sendParticles(
                        particle,
                        target.getX(),
                        target.getY() + target.getBbHeight(),
                        target.getZ(),
                        count,
                        target.getBbWidth() / 2,
                        target.getBbHeight() / 4,
                        target.getBbWidth() / 2,
                        PARTICLE_SPEED);
    }
}
