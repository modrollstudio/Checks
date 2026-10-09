package studio.modroll.checks.save;

/** The arithmetic behind the vanilla saves. Pure, so it is unit tested directly. */
public final class SaveRules {

    private SaveRules() {}

    /** Only knockback stronger than the configured minimum calls for a save, so plain hits never do. */
    public static boolean bigKnockback(double strength, double minStrength) {
        return strength > minStrength;
    }

    /** A duration in ticks scaled by a save's multiplier, rounded down. */
    public static int scaledTicks(int ticks, double multiplier) {
        return (int) Math.floor(ticks * multiplier);
    }
}
