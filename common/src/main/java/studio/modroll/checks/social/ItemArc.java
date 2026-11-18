package studio.modroll.checks.social;

import net.minecraft.world.phys.Vec3;

public final class ItemArc {

    public static final int STOLEN_TICKS = 10;
    public static final int CAUGHT_TICKS = 24;
    public static final int SNATCH_TICK = 14;

    private static final int DROP_TICKS = 8;
    private static final double STOLEN_HEIGHT = 0.8;
    private static final double DROP_HEIGHT = 0.4;
    private static final double SNATCH_HEIGHT = 0.3;
    private static final double DROP_DISTANCE = 0.5;
    private static final double PARABOLA = 4;

    private ItemArc() {}

    public static int ticks(boolean caught) {
        return caught ? CAUGHT_TICKS : STOLEN_TICKS;
    }

    public static Vec3 position(Vec3 from, Vec3 to, double groundY, boolean caught, double tick) {
        if (!caught) {
            return hop(from, to, STOLEN_HEIGHT, tick / STOLEN_TICKS);
        }
        Vec3 dropped = from.lerp(to, DROP_DISTANCE);
        Vec3 ground = new Vec3(dropped.x, groundY, dropped.z);
        if (tick < DROP_TICKS) {
            return hop(from, ground, DROP_HEIGHT, tick / DROP_TICKS);
        }
        if (tick < SNATCH_TICK) {
            return ground;
        }
        return hop(ground, from, SNATCH_HEIGHT, (tick - SNATCH_TICK) / (CAUGHT_TICKS - SNATCH_TICK));
    }

    private static Vec3 hop(Vec3 start, Vec3 end, double height, double progress) {
        double t = Math.clamp(progress, 0, 1);
        return start.lerp(end, t).add(0, height * PARABOLA * t * (1 - t), 0);
    }
}
