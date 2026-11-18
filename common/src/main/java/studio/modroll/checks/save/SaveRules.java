package studio.modroll.checks.save;

public final class SaveRules {

    private SaveRules() {}

    public static int scaledTicks(int ticks, double multiplier) {
        return (int) Math.floor(ticks * multiplier);
    }
}
