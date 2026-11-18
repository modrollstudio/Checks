package studio.modroll.checks.trait.client;

public final class ClientDarkvision {

    private static volatile float strength;

    private ClientDarkvision() {}

    public static void set(float newStrength) {
        strength = newStrength;
    }

    public static void reset() {
        strength = 0f;
    }

    public static boolean active() {
        return strength > 0f;
    }

    public static float scale(float nightVision) {
        return Math.max(nightVision, strength);
    }
}
