package studio.modroll.checks.trait.client;

/**
 * The darkvision strength the server sent this client; presentation only. The lightmap treats it as a
 * floor under night vision's brightening, so darkness looks dim, not lit, and torches still matter.
 */
public final class ClientDarkvision {

    private static volatile float strength;

    private ClientDarkvision() {}

    public static void set(float newStrength) {
        strength = newStrength;
    }

    /** Back to none when leaving a server, so the next one starts without it. */
    public static void reset() {
        strength = 0f;
    }

    public static boolean active() {
        return strength > 0f;
    }

    /** Night vision's own scale, or the darkvision strength when that is higher. */
    public static float scale(float nightVision) {
        return Math.max(nightVision, strength);
    }
}
