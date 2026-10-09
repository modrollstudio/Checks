package studio.modroll.checks.exploration.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** The climbing speed the server sent this client, applied to the local player's own climbing. */
public final class ClientClimbing {

    private static final float UNCHANGED = 1f;

    private static volatile float speed = UNCHANGED;

    private ClientClimbing() {}

    public static void set(float newSpeed) {
        speed = newSpeed;
    }

    /** Back to vanilla's when leaving a server, so the next one starts without it. */
    public static void reset() {
        speed = UNCHANGED;
    }

    /** Vanilla's upward speed on a ladder, vine or other climbable block, times the sent speed for the local player. */
    public static double climbSpeed(LivingEntity entity, double vanilla) {
        boolean climbing = entity instanceof Player player && player.isLocalPlayer() && entity.onClimbable();
        return climbing ? vanilla * speed : vanilla;
    }
}
