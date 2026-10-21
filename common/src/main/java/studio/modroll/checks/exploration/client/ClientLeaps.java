package studio.modroll.checks.exploration.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.exploration.LeapPayload;

/** Adds the push of a made leap to the local player's own speed; it never takes anything from the jump. */
public final class ClientLeaps {

    private ClientLeaps() {}

    public static void push(LeapPayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.setDeltaMovement(pushed(player.getDeltaMovement(), player.onGround(), payload));
        }
    }

    /**
     * The speed with the push added, while still in the air; a push that arrives after landing is dropped,
     * so it never shoves a player along the ground. The rise or fall is always left as it is.
     */
    static Vec3 pushed(Vec3 speed, boolean onGround, LeapPayload payload) {
        return onGround ? speed : speed.add(payload.x(), 0.0, payload.z());
    }
}
