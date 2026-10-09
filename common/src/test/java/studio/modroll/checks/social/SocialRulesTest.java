package studio.modroll.checks.social;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SocialRulesTest {

    private static final double BEHIND_DEGREES = 60;

    @Test
    void priceDiffRoundsToTheNearestItemHalvesAwayFromZero() {
        assertEquals(2, SocialRules.priceDiff(10, 20));
        assertEquals(-2, SocialRules.priceDiff(10, -20));
        assertEquals(1, SocialRules.priceDiff(5, 10));
        assertEquals(-1, SocialRules.priceDiff(5, -10));
        assertEquals(0, SocialRules.priceDiff(1, 10));
        assertEquals(0, SocialRules.priceDiff(24, 0));
    }

    @Test
    void aPositiveModifierLowersPricesAndANegativeOneRaisesThem() {
        assertEquals(-4.0, SocialRules.passivePercent(2, 2));
        assertEquals(2.0, SocialRules.passivePercent(-1, 2));
        assertEquals(0.0, SocialRules.passivePercent(0, 2));
    }

    @Test
    void behindIsWithinTheConeOppositeTheFacing() {
        // Yaw 0 faces +Z, so -Z is straight behind.
        assertTrue(SocialRules.behind(0, 0, -2, BEHIND_DEGREES));
        assertTrue(SocialRules.behind(0, 1, -2, BEHIND_DEGREES));
        assertFalse(SocialRules.behind(0, 0, 2, BEHIND_DEGREES));
        assertFalse(SocialRules.behind(0, 2, 0, BEHIND_DEGREES));
        // Yaw 90 faces -X, so +X is behind.
        assertTrue(SocialRules.behind(90, 2, 0, BEHIND_DEGREES));
        assertFalse(SocialRules.behind(90, -2, 0, BEHIND_DEGREES));
    }

    @Test
    void aPointOnTheTargetIsNotBehindIt() {
        assertFalse(SocialRules.behind(0, 0, 0, BEHIND_DEGREES));
    }
}
