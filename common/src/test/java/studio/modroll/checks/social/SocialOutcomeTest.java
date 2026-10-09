package studio.modroll.checks.social;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SocialOutcomeTest {

    private static final int DC = 12;
    private static final int MARGIN = 2;

    @Test
    void naturalOneAndTwentyDecideOutright() {
        assertEquals(SocialOutcome.CRITICAL_FAILURE, SocialOutcome.of(1, 30, DC, MARGIN));
        assertEquals(SocialOutcome.CRITICAL_SUCCESS, SocialOutcome.of(20, 5, DC, MARGIN));
    }

    @Test
    void belowTheDcFails() {
        assertEquals(SocialOutcome.FAILURE, SocialOutcome.of(10, 11, DC, MARGIN));
    }

    @Test
    void makingTheDcByAtMostTheMarginBarelySucceeds() {
        assertEquals(SocialOutcome.BARELY, SocialOutcome.of(12, 12, DC, MARGIN));
        assertEquals(SocialOutcome.BARELY, SocialOutcome.of(14, 14, DC, MARGIN));
        assertEquals(SocialOutcome.SUCCESS, SocialOutcome.of(15, 15, DC, MARGIN));
    }

    @Test
    void aZeroMarginMakesOnlyAnExactHitBarely() {
        assertEquals(SocialOutcome.BARELY, SocialOutcome.of(12, 12, DC, 0));
        assertEquals(SocialOutcome.SUCCESS, SocialOutcome.of(13, 13, DC, 0));
    }

    @Test
    void barelySuccessAndCriticalSuccessSucceed() {
        assertTrue(SocialOutcome.BARELY.succeeded());
        assertTrue(SocialOutcome.SUCCESS.succeeded());
        assertTrue(SocialOutcome.CRITICAL_SUCCESS.succeeded());
        assertFalse(SocialOutcome.FAILURE.succeeded());
        assertFalse(SocialOutcome.CRITICAL_FAILURE.succeeded());
    }

    @Test
    void pleadAndLieCountABarelyAsASuccessInTheirPools() {
        assertEquals("success", SocialOutcome.BARELY.poolId(SocialAction.PLEAD));
        assertEquals("success", SocialOutcome.BARELY.poolId(SocialAction.LIE));
        assertEquals("barely", SocialOutcome.BARELY.poolId(SocialAction.PERSUADE));
        assertEquals("critical_failure", SocialOutcome.CRITICAL_FAILURE.poolId(SocialAction.LIE));
    }
}
