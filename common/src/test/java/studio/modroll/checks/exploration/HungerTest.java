package studio.modroll.checks.exploration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import studio.modroll.checks.data.ScoresConfig;

class HungerTest {

    private static final ScoresConfig.ReductionSettings SETTINGS = new ScoresConfig.ReductionSettings(true, 0.05, 0.25);
    private static final float EXHAUSTION = 4f;
    private static final float EPSILON = 1e-6f;

    @Test
    void eachPositiveSurvivalPointDrainsFivePercentSlower() {
        assertEquals(3.4f, Hunger.exhaustion(EXHAUSTION, 3, SETTINGS), EPSILON);
    }

    @Test
    void aZeroOrNegativeModifierDrainsAsInVanilla() {
        assertEquals(EXHAUSTION, Hunger.exhaustion(EXHAUSTION, 0, SETTINGS), EPSILON);
        assertEquals(EXHAUSTION, Hunger.exhaustion(EXHAUSTION, -2, SETTINGS), EPSILON);
    }

    @Test
    void theSlowdownStopsAtTheMaximum() {
        assertEquals(3f, Hunger.exhaustion(EXHAUSTION, 10, SETTINGS), EPSILON);
    }
}
