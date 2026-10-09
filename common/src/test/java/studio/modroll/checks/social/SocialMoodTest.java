package studio.modroll.checks.social;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class SocialMoodTest {

    @Test
    void threatsScareOrAnger() {
        assertEquals(Optional.of(SocialMood.SCARED), SocialMood.of(SocialAction.INTIMIDATE, SocialOutcome.SUCCESS));
        assertEquals(Optional.of(SocialMood.ANGRY), SocialMood.of(SocialAction.INTIMIDATE, SocialOutcome.FAILURE));
    }

    @Test
    void aFailedLieMakesTheTargetSuspiciousAndANaturalOneAngry() {
        assertEquals(Optional.of(SocialMood.SUSPICIOUS), SocialMood.of(SocialAction.DECEIVE, SocialOutcome.FAILURE));
        assertEquals(
                Optional.of(SocialMood.ANGRY), SocialMood.of(SocialAction.DECEIVE, SocialOutcome.CRITICAL_FAILURE));
        assertEquals(Optional.of(SocialMood.HAPPY), SocialMood.of(SocialAction.DECEIVE, SocialOutcome.SUCCESS));
    }

    @Test
    void anUnnoticedPickpocketLeavesNothingToSay() {
        assertEquals(Optional.empty(), SocialMood.of(SocialAction.PICKPOCKET, SocialOutcome.SUCCESS));
        assertEquals(
                Optional.empty(),
                SpeechPools.of(SocialTarget.VILLAGER, SocialAction.PICKPOCKET, SocialOutcome.SUCCESS));
        assertEquals(Optional.of(SocialMood.SUSPICIOUS), SocialMood.of(SocialAction.PICKPOCKET, SocialOutcome.BARELY));
    }

    @Test
    void aCaughtPickpocketHasItsOwnPool() {
        assertEquals(
                Optional.of("checks.speech.villager.caught"),
                SpeechPools.of(SocialTarget.VILLAGER, SocialAction.PICKPOCKET, SocialOutcome.CRITICAL_FAILURE));
        assertEquals(
                Optional.of("checks.speech.villager.failure"),
                SpeechPools.of(SocialTarget.VILLAGER, SocialAction.PERSUADE, SocialOutcome.FAILURE));
    }

    @Test
    void voicesAreNamedAfterTargetAndMood() {
        assertEquals(
                "checks:wandering_trader.suspicious",
                SocialFeel.voiceId(SocialTarget.WANDERING_TRADER, SocialMood.SUSPICIOUS)
                        .toString());
    }

    @Test
    void pleadAndLieHaveTheirOwnSpeechPools() {
        assertEquals(
                Optional.of("checks.speech.villager.lie.critical_failure"),
                SpeechPools.of(SocialTarget.VILLAGER, SocialAction.LIE, SocialOutcome.CRITICAL_FAILURE));
        assertEquals(
                Optional.of("checks.speech.villager.plead.success"),
                SpeechPools.of(SocialTarget.VILLAGER, SocialAction.PLEAD, SocialOutcome.BARELY));
        assertEquals(Optional.of(SocialMood.SUSPICIOUS), SocialMood.of(SocialAction.LIE, SocialOutcome.FAILURE));
        assertEquals(Optional.of(SocialMood.HAPPY), SocialMood.of(SocialAction.PLEAD, SocialOutcome.SUCCESS));
    }

    @Test
    void aPerformancesListenersAreDelightedUnimpressedOrAnnoyed() {
        assertEquals(SocialMood.HAPPY, SocialMood.ofPerformance(SocialOutcome.BARELY));
        assertEquals(SocialMood.SUSPICIOUS, SocialMood.ofPerformance(SocialOutcome.FAILURE));
        assertEquals(SocialMood.ANGRY, SocialMood.ofPerformance(SocialOutcome.CRITICAL_FAILURE));
    }

    @Test
    void aPerformanceHasItsOwnSpeechPoolsWhereBarelyCountsAsASuccess() {
        assertEquals(
                "checks.speech.piglin.performance.success",
                SpeechPools.performance(SocialTarget.PIGLIN, SocialOutcome.BARELY));
        assertEquals(
                "checks.speech.villager.performance.critical_failure",
                SpeechPools.performance(SocialTarget.VILLAGER, SocialOutcome.CRITICAL_FAILURE));
    }
}
