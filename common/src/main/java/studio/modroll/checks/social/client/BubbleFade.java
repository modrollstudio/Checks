package studio.modroll.checks.social.client;

/** How opaque a speech bubble is: it fades in over its first {@link #IN_TICKS} ticks and out over its last {@link #OUT_TICKS}. */
final class BubbleFade {

    static final float IN_TICKS = 4;
    static final float OUT_TICKS = 6;

    private BubbleFade() {}

    /** From 0 (hidden) to 1 (opaque), {@code age} ticks after it appeared with {@code remaining} ticks left. */
    static float at(float age, float remaining) {
        return Math.clamp(Math.min(age / IN_TICKS, remaining / OUT_TICKS), 0f, 1f);
    }
}
