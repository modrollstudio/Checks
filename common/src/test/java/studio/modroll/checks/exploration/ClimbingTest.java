package studio.modroll.checks.exploration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClimbingTest {

    private static final double PER_POINT = 0.1;
    private static final double MAX_BONUS = 0.5;
    private static final float EPSILON = 1e-6f;

    @Test
    void eachPositiveAthleticsPointClimbsTenPercentFaster() {
        assertEquals(1.3f, Climbing.speed(3, PER_POINT, MAX_BONUS), EPSILON);
    }

    @Test
    void aZeroOrNegativeModifierClimbsAtVanillasSpeed() {
        assertEquals(1f, Climbing.speed(0, PER_POINT, MAX_BONUS), EPSILON);
        assertEquals(1f, Climbing.speed(-2, PER_POINT, MAX_BONUS), EPSILON);
    }

    @Test
    void theBonusStopsAtTheMaximum() {
        assertEquals(1.5f, Climbing.speed(9, PER_POINT, MAX_BONUS), EPSILON);
    }
}
