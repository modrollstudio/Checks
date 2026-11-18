package studio.modroll.checks.save;

/** The arithmetic behind the vanilla saves. Pure, so it is unit tested directly. */
public final class SaveRules {

    private SaveRules() {}

    public static int scaledTicks(int ticks, double multiplier) {
        return (int) Math.floor(ticks * multiplier);
    }
}
