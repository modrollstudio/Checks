package studio.modroll.checks.social;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static studio.modroll.checks.social.SocialReaction.ANGRY;
import static studio.modroll.checks.social.SocialReaction.COWER;
import static studio.modroll.checks.social.SocialReaction.FLEE;
import static studio.modroll.checks.social.SocialReaction.LLAMAS_SPIT;
import static studio.modroll.checks.social.SocialReaction.PLEASED;
import static studio.modroll.checks.social.SocialReaction.SHAKE_HEAD;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SocialReactionTest {

    @Test
    void aCaughtPickpocketSendsTheVictimRunning() {
        assertEquals(
                Set.of(SHAKE_HEAD, ANGRY, FLEE),
                reactions(SocialAction.PICKPOCKET, SocialTarget.VILLAGER, SocialOutcome.FAILURE));
        assertEquals(Set.of(), reactions(SocialAction.PICKPOCKET, SocialTarget.VILLAGER, SocialOutcome.SUCCESS));
    }

    @Test
    void intimidationScaresOrAngers() {
        assertEquals(
                Set.of(FLEE, COWER), reactions(SocialAction.INTIMIDATE, SocialTarget.VILLAGER, SocialOutcome.SUCCESS));
        assertEquals(
                Set.of(SHAKE_HEAD, ANGRY),
                reactions(SocialAction.INTIMIDATE, SocialTarget.VILLAGER, SocialOutcome.FAILURE));
    }

    @Test
    void persuasionPleasesOrGetsANo() {
        for (SocialOutcome made : Set.of(SocialOutcome.BARELY, SocialOutcome.SUCCESS, SocialOutcome.CRITICAL_SUCCESS)) {
            assertEquals(Set.of(PLEASED), reactions(SocialAction.PERSUADE, SocialTarget.VILLAGER, made));
        }
        assertEquals(
                Set.of(SHAKE_HEAD),
                reactions(SocialAction.PERSUADE, SocialTarget.WANDERING_TRADER, SocialOutcome.FAILURE));
        assertEquals(
                Set.of(SHAKE_HEAD, ANGRY),
                reactions(SocialAction.PERSUADE, SocialTarget.VILLAGER, SocialOutcome.CRITICAL_FAILURE));
    }

    @Test
    void onlyAWanderingTradersLlamasSpitAtALiar() {
        assertEquals(
                Set.of(SHAKE_HEAD, LLAMAS_SPIT),
                reactions(SocialAction.DECEIVE, SocialTarget.WANDERING_TRADER, SocialOutcome.FAILURE));
        assertEquals(Set.of(SHAKE_HEAD), reactions(SocialAction.DECEIVE, SocialTarget.VILLAGER, SocialOutcome.FAILURE));
        assertEquals(
                Set.of(PLEASED), reactions(SocialAction.DECEIVE, SocialTarget.WANDERING_TRADER, SocialOutcome.SUCCESS));
    }

    @Test
    void piglinsAdmireBackOffOrSnarl() {
        assertEquals(Set.of(PLEASED), reactions(SocialAction.DECEIVE, SocialTarget.PIGLIN, SocialOutcome.SUCCESS));
        assertEquals(Set.of(ANGRY), reactions(SocialAction.DECEIVE, SocialTarget.PIGLIN, SocialOutcome.FAILURE));
        assertEquals(Set.of(COWER), reactions(SocialAction.INTIMIDATE, SocialTarget.PIGLIN, SocialOutcome.SUCCESS));
        assertEquals(
                Set.of(ANGRY), reactions(SocialAction.INTIMIDATE, SocialTarget.PIGLIN, SocialOutcome.CRITICAL_FAILURE));
    }

    @Test
    void aScaredMobSweatsAndARushingOneBristles() {
        assertEquals(
                Set.of(COWER), reactions(SocialAction.INTIMIDATE_MOB, SocialTarget.HOSTILE, SocialOutcome.SUCCESS));
        assertEquals(Set.of(), reactions(SocialAction.INTIMIDATE_MOB, SocialTarget.HOSTILE, SocialOutcome.FAILURE));
        assertEquals(
                Set.of(ANGRY),
                reactions(SocialAction.INTIMIDATE_MOB, SocialTarget.HOSTILE, SocialOutcome.CRITICAL_FAILURE));
    }

    @Test
    void aCalmedMobSettlesAndAnUnmovedOneBristles() {
        assertEquals(Set.of(PLEASED), reactions(SocialAction.CALM, SocialTarget.NEUTRAL, SocialOutcome.BARELY));
        assertEquals(Set.of(ANGRY), reactions(SocialAction.CALM, SocialTarget.NEUTRAL, SocialOutcome.FAILURE));
    }

    @Test
    void aPerformancePleasesBoresOrAngers() {
        assertEquals(Set.of(PLEASED), SocialReaction.ofPerformance(SocialOutcome.BARELY));
        assertEquals(Set.of(PLEASED), SocialReaction.ofPerformance(SocialOutcome.CRITICAL_SUCCESS));
        assertEquals(Set.of(), SocialReaction.ofPerformance(SocialOutcome.FAILURE));
        assertEquals(Set.of(ANGRY), SocialReaction.ofPerformance(SocialOutcome.CRITICAL_FAILURE));
    }

    private static Set<SocialReaction> reactions(SocialAction action, SocialTarget target, SocialOutcome outcome) {
        return SocialReaction.of(action, target, outcome);
    }
}
