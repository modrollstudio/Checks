package studio.modroll.checks.social.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BubbleFadeTest {

    private static final float EPSILON = 1e-6f;
    private static final float LONG = 100;

    @Test
    void aBubbleFadesInOverItsFirstTicks() {
        assertEquals(0, BubbleFade.at(0, LONG), EPSILON);
        assertEquals(0.5f, BubbleFade.at(BubbleFade.IN_TICKS / 2, LONG), EPSILON);
        assertEquals(1, BubbleFade.at(BubbleFade.IN_TICKS, LONG), EPSILON);
    }

    @Test
    void aBubbleFadesOutOverItsLastTicks() {
        assertEquals(1, BubbleFade.at(LONG, BubbleFade.OUT_TICKS), EPSILON);
        assertEquals(0.5f, BubbleFade.at(LONG, BubbleFade.OUT_TICKS / 2), EPSILON);
        assertEquals(0, BubbleFade.at(LONG, 0), EPSILON);
    }

    @Test
    void aBriefBubbleStaysPartlyFadedAndNeverGoesBelowHidden() {
        assertEquals(0.5f, BubbleFade.at(BubbleFade.IN_TICKS / 2, BubbleFade.OUT_TICKS / 2), EPSILON);
        assertEquals(0, BubbleFade.at(LONG, -1), EPSILON);
    }
}
