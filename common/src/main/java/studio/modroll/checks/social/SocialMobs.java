package studio.modroll.checks.social;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * A frightened mob runs through a goal of its own ahead of every other, so it neither attacks nor closes in. Frights
 * live in memory only, as goals do.
 */
public final class SocialMobs {

    /** The loaders' shared convention tag for bosses; both fill it with the ender dragon and the wither. */
    private static final TagKey<EntityType<?>> BOSSES =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));

    /** Ahead of every vanilla goal, so a frightened mob runs whatever else it wanted to do. */
    private static final int FLEE_PRIORITY = -1;

    private static final List<Fright> FRIGHTS = new ArrayList<>();

    /** Each loader sets how to reach a mob's goals, which vanilla keeps protected. */
    private static Function<Mob, GoalSelector> goals = mob -> {
        throw new IllegalStateException("no loader gave Checks access to mob goals");
    };

    private SocialMobs() {}

    private record Fright(PathfinderMob mob, Goal goal, long until) {}

    public static void setGoals(Function<Mob, GoalSelector> access) {
        goals = access;
    }

    static boolean calmable(Entity entity) {
        return entity instanceof Wolf
                || entity instanceof Bee
                || entity instanceof EnderMan
                || entity instanceof ZombifiedPiglin
                || entity instanceof IronGolem
                || (entity instanceof Mob
                        && entity.getType().is(tag(settings().calm().tag())));
    }

    static boolean fearless(LivingEntity mob) {
        ScoresConfig.IntimidateMobSettings settings = settings().intimidateMob();
        return mob.getMaxHealth() > settings.maxHealth()
                || mob.getType().is(BOSSES)
                || mob.getType().is(tag(settings.excludeTag()));
    }

    static boolean angryAt(LivingEntity mob, ServerPlayer player) {
        return (mob instanceof Mob attacker && attacker.getTarget() == player)
                || (mob instanceof NeutralMob neutral && neutral.isAngryAt(player));
    }

    static void frighten(ServerPlayer player, PathfinderMob mob, ScoresConfig.IntimidateMobSettings settings) {
        stopFleeing(mob);
        mob.setTarget(null);
        UUID id = player.getUUID();
        Goal flee = new AvoidEntityGoal<>(
                mob,
                Player.class,
                (float) settings.fleeDistance(),
                settings.fleeSpeed(),
                settings.fleeSpeed(),
                entity -> entity.getUUID().equals(id));
        goals.apply(mob).addGoal(FLEE_PRIORITY, flee);
        FRIGHTS.add(new Fright(mob, flee, Socials.now(player.server) + settings.fleeTicks()));
    }

    static void rush(ServerPlayer player, Mob mob, ScoresConfig.IntimidateMobSettings settings) {
        mob.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED, settings.speedBoostTicks(), settings.speedBoostAmplifier()));
        mob.setTarget(player);
    }

    static void calm(ServerPlayer player, Mob mob) {
        if (mob instanceof NeutralMob neutral && player.getUUID().equals(neutral.getPersistentAngerTarget())) {
            neutral.stopBeingAngry();
        }
        if (mob.getTarget() == player) {
            mob.setTarget(null);
        }
        if (mob instanceof IronGolem golem) {
            GolemAlarms.exempt(player, golem);
        }
    }

    public static boolean fleeing(Mob mob) {
        return FRIGHTS.stream().anyMatch(fright -> fright.mob() == mob);
    }

    public static void tick(MinecraftServer server) {
        if (FRIGHTS.isEmpty()) {
            return;
        }
        boolean on = settings().active(SocialAction.INTIMIDATE_MOB);
        long now = Socials.now(server);
        List<Fright> over = FRIGHTS.stream()
                .filter(fright -> !on || now >= fright.until() || fright.mob().isRemoved())
                .toList();
        over.forEach(SocialMobs::end);
    }

    public static void clear() {
        FRIGHTS.clear();
    }

    private static void stopFleeing(PathfinderMob mob) {
        FRIGHTS.stream().filter(fright -> fright.mob() == mob).toList().forEach(SocialMobs::end);
    }

    private static void end(Fright fright) {
        goals.apply(fright.mob()).removeGoal(fright.goal());
        FRIGHTS.remove(fright);
    }

    private static TagKey<EntityType<?>> tag(ResourceLocation id) {
        return TagKey.create(Registries.ENTITY_TYPE, id);
    }

    private static ScoresConfig.SocialSettings settings() {
        return ScoresRuntime.config().social();
    }
}
