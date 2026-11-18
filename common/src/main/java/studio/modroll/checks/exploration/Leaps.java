package studio.modroll.checks.exploration;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.Checks;
import studio.modroll.checks.ClientPayloads;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * The client makes the jump and the server never sets the player's speed, so the jump always happens as in vanilla and
 * a made check only adds a push. Only a jump from the ground rolls, so each leap rolls once.
 */
public final class Leaps {

    private static final ResourceLocation ATHLETICS = Checks.id("athletics");
    private static final double LOOK_AHEAD = 1.0;

    private Leaps() {}

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

    private static boolean leaping(ServerPlayer player, int minDrop) {
        return player.onGround()
                && player.isSprinting()
                && !player.isInWater()
                && !player.isFallFlying()
                && !player.getAbilities().flying
                && !player.isPassenger()
                && ExplorationChecks.rollsFor(player)
                && dropAhead(player, minDrop);
    }

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
        return ExplorationChecks.made(player, ATHLETICS, settings.dc(), "leap").orElse(false);
    }

    /** A ping late, the server's idea of the speed is stale, so the client adds the push to its own. */
    private static void boost(ServerPlayer player, double boost) {
        Vec3 push = facing(player).scale(boost);
        ClientPayloads.send(player, new LeapPayload(push.x, push.z));
    }

    private static Vec3 facing(ServerPlayer player) {
        return Vec3.directionFromRotation(0, player.getYRot());
    }
}
