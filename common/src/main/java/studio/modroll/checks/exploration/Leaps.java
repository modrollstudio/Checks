package studio.modroll.checks.exploration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.Checks;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * Athletics: a sprinting jump by a player in survival or adventure toward a drop of at least the configured
 * depth, a gap or a pit edge, rolls Athletics through the API, and a made check carries the leap further.
 * Every other jump, and any jump in water, while gliding, flying or riding, is a plain jump and rolls
 * nothing. Within the cooldown another leap reuses the last result without rolling. The cooldown lives in
 * memory only, so a restart ends it.
 */
public final class Leaps {

    private static final ResourceLocation ATHLETICS = Checks.id("athletics");
    private static final double LOOK_AHEAD = 1.0;
    /** What vanilla adds along the player's facing to a sprinting jump; the client has added it already. */
    private static final double SPRINT_JUMP_IMPULSE = 0.2;

    private static final Map<UUID, Last> LAST = new HashMap<>();

    private record Last(long readyAt, boolean made) {}

    private Leaps() {}

    /** Called whenever a living entity jumps; only a server player's sprinting leap toward a drop counts. */
    public static void onJump(LivingEntity entity) {
        ScoresConfig.LeapSettings settings =
                ScoresRuntime.config().exploration().leap();
        if (settings.enabled()
                && entity instanceof ServerPlayer player
                && leaping(player, settings.minDrop())
                && made(player, settings)) {
            boost(player, settings.boost());
        }
    }

    public static void clear() {
        LAST.clear();
    }

    private static boolean leaping(ServerPlayer player, int minDrop) {
        return player.isSprinting()
                && !player.isInWater()
                && !player.isFallFlying()
                && !player.getAbilities().flying
                && !player.isPassenger()
                && ExplorationChecks.rollsFor(player)
                && dropAhead(player, minDrop);
    }

    /** One block ahead, where a plain jump would land, nothing to stand on for {@code depth} blocks down. */
    private static boolean dropAhead(ServerPlayer player, int depth) {
        BlockPos ahead =
                BlockPos.containing(player.position().add(facing(player).scale(LOOK_AHEAD)));
        for (int down = 1; down <= depth; down++) {
            BlockPos below = ahead.below(down);
            if (!player.level()
                    .getBlockState(below)
                    .getCollisionShape(player.level(), below)
                    .isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean made(ServerPlayer player, ScoresConfig.LeapSettings settings) {
        long now = player.server.getTickCount();
        Last last = LAST.get(player.getUUID());
        if (last != null && now < last.readyAt()) {
            return last.made();
        }
        return ExplorationChecks.made(player, ATHLETICS, settings.dc(), "leap")
                .map(result -> {
                    LAST.values().removeIf(old -> old.readyAt() <= now);
                    LAST.put(player.getUUID(), new Last(now + settings.cooldownTicks(), result));
                    return result;
                })
                .orElse(false);
    }

    /**
     * The client moves the player, so it is sent the velocity it gave the jump plus the boost: the ground
     * speed it last reported, the sprint-jump impulse and the boost along the facing, and the jump's rise.
     */
    private static void boost(ServerPlayer player, double boost) {
        Vec3 ground = player.getKnownMovement();
        Vec3 push = facing(player).scale(SPRINT_JUMP_IMPULSE + boost);
        player.setDeltaMovement(ground.x + push.x, player.getDeltaMovement().y, ground.z + push.z);
        player.hurtMarked = true;
    }

    private static Vec3 facing(ServerPlayer player) {
        return Vec3.directionFromRotation(0, player.getYRot());
    }
}
