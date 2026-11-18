package studio.modroll.checks.social.client;

final class BubbleScale {

    static final float TEXT_SCALE = 0.025f;
    static final double FULL_SIZE_DISTANCE = 4;

    private BubbleScale() {}

    static float at(double distance) {
        return (float) (TEXT_SCALE * Math.min(1, distance / FULL_SIZE_DISTANCE));
    }
}
