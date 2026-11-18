package studio.modroll.checks.social;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

public final class GolemAlarms {

    private static final Map<UUID, Alarm> WANTED = new HashMap<>();

    private GolemAlarms() {}

    private static final class Alarm {
        private long until;
        private ResourceKey<Level> level;
        private Vec3 at;
        private final Set<UUID> golems = new HashSet<>();
        private final Set<UUID> calmed = new HashSet<>();
        private final Set<SocialAction> tried = EnumSet.noneOf(SocialAction.class);
    }

    static void callGolems(ServerPlayer player, LivingEntity around, double radius) {
        List<IronGolem> golems = turnOnAround(player, around, radius);
        if (!golems.isEmpty()) {
            raise(player, around, golems);
        }
    }

    static void restart(ServerPlayer player, LivingEntity around, double radius) {
        raise(player, around, turnOnAround(player, around, radius));
    }

    public static boolean onGuard(ServerPlayer player, Entity entity) {
        ScoresConfig.SocialSettings settings = ScoresRuntime.config().social();
        Alarm alarm = WANTED.get(player.getUUID());
        return settings.onGuard().enabled()
                && settings.golemAlarm().enabled()
                && alarm != null
                && Socials.now(player.server) < alarm.until
                && entity instanceof Villager
                && entity.level().dimension() == alarm.level
                && entity.position().closerThan(alarm.at, settings.golemAlarm().radius());
    }

    static boolean tried(ServerPlayer player, SocialAction action) {
        Alarm alarm = WANTED.get(player.getUUID());
        return alarm != null && alarm.tried.contains(action);
    }

    static void exempt(ServerPlayer player, IronGolem golem) {
        Alarm alarm = WANTED.get(player.getUUID());
        if (alarm != null) {
            alarm.golems.remove(golem.getUUID());
            alarm.calmed.add(golem.getUUID());
        }
    }

    static void markTried(ServerPlayer player, SocialAction action) {
        Alarm alarm = WANTED.get(player.getUUID());
        if (alarm != null) {
            alarm.tried.add(action);
        }
    }

    static void standDown(ServerPlayer player) {
        Alarm alarm = WANTED.remove(player.getUUID());
        ServerLevel level = alarm == null ? null : player.server.getLevel(alarm.level);
        if (level == null) {
            return;
        }
        for (UUID id : alarm.golems) {
            if (level.getEntity(id) instanceof IronGolem golem && after(golem, player)) {
                golem.stopBeingAngry();
            }
        }
        double across = settings().radius() * 2;
        for (Villager villager :
                level.getEntitiesOfClass(Villager.class, AABB.ofSize(alarm.at, across, across, across))) {
            SocialReactions.calmDown(player, villager);
        }
    }

    public static void tick(MinecraftServer server) {
        if (WANTED.isEmpty()) {
            return;
        }
        ScoresConfig.GolemAlarmSettings settings = settings();
        long now = Socials.now(server);
        WANTED.entrySet().removeIf(wanted -> {
            ServerPlayer player = server.getPlayerList().getPlayer(wanted.getKey());
            return !settings.enabled() || now >= wanted.getValue().until || (player != null && !player.isAlive());
        });
        WANTED.forEach((wanted, alarm) -> {
            ServerPlayer player = server.getPlayerList().getPlayer(wanted);
            if (player != null) {
                spread(player, alarm, settings.radius());
            }
        });
    }

    public static void clear() {
        WANTED.clear();
    }

    private static List<IronGolem> turnOnAround(ServerPlayer player, LivingEntity around, double radius) {
        List<IronGolem> golems = mayAttack(player, around, radius);
        golems.forEach(golem -> turnOn(golem, player));
        return golems;
    }

    private static void raise(ServerPlayer player, Entity at, List<IronGolem> turned) {
        ScoresConfig.GolemAlarmSettings settings = settings();
        if (!settings.enabled() || settings.durationTicks() <= 0) {
            return;
        }
        Alarm alarm = WANTED.computeIfAbsent(player.getUUID(), wanted -> new Alarm());
        alarm.until = Socials.now(player.server) + settings.durationTicks();
        alarm.level = at.level().dimension();
        alarm.at = at.position();
        turned.forEach(golem -> alarm.golems.add(golem.getUUID()));
        spread(player, alarm, settings.radius());
    }

    private static void spread(ServerPlayer player, Alarm alarm, double radius) {
        List<? extends IronGolem> callers =
                player.serverLevel().getEntities(EntityType.IRON_GOLEM, golem -> golem.getTarget() == player);
        for (IronGolem caller : callers) {
            for (IronGolem golem : mayAttack(player, caller, radius)) {
                if (golem.getTarget() != player && !alarm.calmed.contains(golem.getUUID())) {
                    turnOn(golem, player);
                    alarm.golems.add(golem.getUUID());
                }
            }
        }
    }

    private static List<IronGolem> mayAttack(ServerPlayer player, Entity around, double radius) {
        return player.level()
                .getEntitiesOfClass(
                        IronGolem.class,
                        around.getBoundingBox().inflate(radius),
                        golem -> golem.canAttackType(EntityType.PLAYER) && golem.canAttack(player));
    }

    private static void turnOn(IronGolem golem, ServerPlayer player) {
        golem.setTarget(player);
        golem.setPersistentAngerTarget(player.getUUID());
        golem.startPersistentAngerTimer();
    }

    private static boolean after(IronGolem golem, ServerPlayer player) {
        return golem.getTarget() == player || player.getUUID().equals(golem.getPersistentAngerTarget());
    }

    private static ScoresConfig.GolemAlarmSettings settings() {
        return ScoresRuntime.config().social().golemAlarm();
    }
}
