package studio.modroll.checks.social.client;

/** How large a speech bubble is drawn, so it never looks bigger than from {@link #FULL_SIZE_DISTANCE} blocks. */
final class BubbleScale {

    static final float TEXT_SCALE = 0.025f;
    static final double FULL_SIZE_DISTANCE = 4;

    private BubbleScale() {}

    /** The scale {@code distance} blocks from the camera: full size from there on, smaller nearer. */
    static float at(double distance) {
        return (float) (TEXT_SCALE * Math.min(1, distance / FULL_SIZE_DISTANCE));
    }
}
