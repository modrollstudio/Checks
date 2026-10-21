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
 * Athletics: a sprinting jump by a player in survival or adventure toward a drop of at least the configured
 * depth, a gap or a pit edge, rolls Athletics through the API, and a made check adds a push along the way the
 * player faces. The check only ever adds to a leap: the client makes the jump, and the server never sets the
 * player's speed, so the jump itself always happens as in vanilla, whatever the roll. The push goes to a
 * client with Checks, which adds it to its own speed if still in the air; a client without Checks leaps as in
 * vanilla. Every other jump, and any jump in water, while gliding, flying or riding, is a plain jump and rolls
 * nothing. Each leap rolls once, however soon after the last: only a jump from the ground rolls, so a second
 * call for the same jump rolls nothing and the next roll waits for the player to land. There is no cooldown.
 */
public final class Leaps {

    private static final ResourceLocation ATHLETICS = Checks.id("athletics");
    private static final double LOOK_AHEAD = 1.0;

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
        return ExplorationChecks.made(player, ATHLETICS, settings.dc(), "leap").orElse(false);
    }

    /**
     * The client moves the player, so it is sent the push to add to the speed it already has. A ping late,
     * the server's idea of that speed is stale; the push never replaces it.
     */
    private static void boost(ServerPlayer player, double boost) {
        Vec3 push = facing(player).scale(boost);
        ClientPayloads.send(player, new LeapPayload(push.x, push.z));
    }

    private static Vec3 facing(ServerPlayer player) {
        return Vec3.directionFromRotation(0, player.getYRot());
    }
}
