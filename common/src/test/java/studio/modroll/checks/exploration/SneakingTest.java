package studio.modroll.checks.exploration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SneakingTest {

    private static final double PER_POINT = 0.05;
    private static final double MIN = 0.25;
    private static final double EPSILON = 1e-9;

    @Test
    void aMobWhosePerceptionMatchesOrBeatsStealthNoticesAsUsual() {
        assertEquals(1.0, Sneaking.visibility(0, PER_POINT, MIN), EPSILON);
        assertEquals(1.0, Sneaking.visibility(-4, PER_POINT, MIN), EPSILON);
    }

    @Test
    void eachPointStealthWinsByShrinksTheDistance() {
        assertEquals(0.8, Sneaking.visibility(4, PER_POINT, MIN), EPSILON);
    }

    @Test
    void theDistanceNeverShrinksBelowTheMinimum() {
        assertEquals(MIN, Sneaking.visibility(30, PER_POINT, MIN), EPSILON);
    }
}
