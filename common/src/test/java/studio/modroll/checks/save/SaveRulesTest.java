package studio.modroll.checks.save;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SaveRulesTest {

    @Test
    void scaledTicksRoundDown() {
        assertEquals(130, SaveRules.scaledTicks(260, 0.5));
        assertEquals(2, SaveRules.scaledTicks(5, 0.5));
        assertEquals(0, SaveRules.scaledTicks(160, 0.0));
    }
}
