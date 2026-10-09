package studio.modroll.checks.social;

/** The arithmetic behind social actions. Pure, so it is unit tested directly. */
public final class SocialRules {

    private static final double PERCENT = 100;

    private SocialRules() {}

    /**
     * What a {@code percent} price change adds to a trade costing {@code baseCount} items, rounded to the
     * nearest item, halves away from zero; negative for a discount.
     */
    public static int priceDiff(int baseCount, double percent) {
        double change = baseCount * percent / PERCENT;
        return (int) (Math.signum(change) * Math.round(Math.abs(change)));
    }

    /** A passive price change: a positive modifier lowers prices by {@code percentPerPoint} per point. */
    public static double passivePercent(int modifier, double percentPerPoint) {
        return -modifier * percentPerPoint;
    }

    /**
     * Whether a point {@code dx, dz} away from a target facing {@code yawDegrees} (Minecraft's yaw, 0 facing
     * +Z) lies within {@code behindDegrees} of straight behind it. A point on top of the target is not.
     */
    public static boolean behind(float yawDegrees, double dx, double dz, double behindDegrees) {
        double distance = Math.hypot(dx, dz);
        if (distance == 0) {
            return false;
        }
        double yaw = Math.toRadians(yawDegrees);
        double backX = Math.sin(yaw);
        double backZ = -Math.cos(yaw);
        double cosine = (dx * backX + dz * backZ) / distance;
        return cosine >= Math.cos(Math.toRadians(behindDegrees));
    }
}
