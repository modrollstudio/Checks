package studio.modroll.checks.exploration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import studio.modroll.checks.data.ScoresConfig;

class AilmentsTest {

    private static final ScoresConfig.ReductionSettings SETTINGS = new ScoresConfig.ReductionSettings(true, 0.05, 0.25);
    private static final int TEN_SECONDS = 200;

    @Test
    void eachPositiveMedicinePointWearsAnAilmentOffFivePercentSooner() {
        assertEquals(170, Ailments.duration(TEN_SECONDS, 3, SETTINGS));
    }

    @Test
    void aZeroOrNegativeModifierLeavesTheDurationAlone() {
        assertEquals(TEN_SECONDS, Ailments.duration(TEN_SECONDS, 0, SETTINGS));
        assertEquals(TEN_SECONDS, Ailments.duration(TEN_SECONDS, -1, SETTINGS));
    }

    @Test
    void theShorteningStopsAtTheMaximumAndRoundsDown() {
        assertEquals(150, Ailments.duration(TEN_SECONDS, 9, SETTINGS));
        assertEquals(8, Ailments.duration(9, 1, SETTINGS));
    }
}
