package studio.modroll.checks.social;

public final class SocialRules {

    private static final double PERCENT = 100;

    private SocialRules() {}

    public static int priceDiff(int baseCount, double percent) {
        double change = baseCount * percent / PERCENT;
        return (int) (Math.signum(change) * Math.round(Math.abs(change)));
    }

    public static double passivePercent(int modifier, double percentPerPoint) {
        return -modifier * percentPerPoint;
    }

    /** {@code yawDegrees} is Minecraft's yaw, 0 facing +Z. */
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
