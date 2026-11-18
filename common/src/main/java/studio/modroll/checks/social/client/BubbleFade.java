package studio.modroll.checks.social.client;

final class BubbleFade {

    static final float IN_TICKS = 4;
    static final float OUT_TICKS = 6;

    private BubbleFade() {}

    static float at(float age, float remaining) {
        return Math.clamp(Math.min(age / IN_TICKS, remaining / OUT_TICKS), 0f, 1f);
    }
}
