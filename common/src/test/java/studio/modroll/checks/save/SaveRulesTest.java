package studio.modroll.checks.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SaveRulesTest {

    @Test
    void onlyKnockbackAboveTheMinimumCallsForASave() {
        assertFalse(SaveRules.bigKnockback(0.4, 0.7));
        assertFalse(SaveRules.bigKnockback(0.7, 0.7));
        assertTrue(SaveRules.bigKnockback(0.75, 0.7));
    }

    @Test
    void scaledTicksRoundDown() {
        assertEquals(130, SaveRules.scaledTicks(260, 0.5));
        assertEquals(2, SaveRules.scaledTicks(5, 0.5));
        assertEquals(0, SaveRules.scaledTicks(160, 0.0));
    }
}
