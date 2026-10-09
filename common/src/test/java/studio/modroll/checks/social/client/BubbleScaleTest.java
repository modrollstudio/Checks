package studio.modroll.checks.social.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BubbleScaleTest {

    private static final float EPSILON = 1e-6f;

    /** On screen a bubble's size goes as its scale over its distance. */
    private static float screenSize(double distance) {
        return (float) (BubbleScale.at(distance) / distance);
    }

    @Test
    void closerThanFullSizeABubbleLooksAsBigAsFromThere() {
        float fromFullSize = screenSize(BubbleScale.FULL_SIZE_DISTANCE);
        assertEquals(fromFullSize, screenSize(2.5), EPSILON);
        assertEquals(fromFullSize, screenSize(1), EPSILON);
    }

    @Test
    void fartherAwayABubbleKeepsItsFullScale() {
        assertEquals(BubbleScale.TEXT_SCALE, BubbleScale.at(10), EPSILON);
        assertEquals(BubbleScale.TEXT_SCALE, BubbleScale.at(BubbleScale.FULL_SIZE_DISTANCE), EPSILON);
    }
}
