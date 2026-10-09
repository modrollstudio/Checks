package studio.modroll.checks.exploration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class MobHintsTest {

    private static final int MAX_LENGTH = 70;

    @Test
    void aHintSaysTheHeartsTheHitInHeartsAndTheWeakness() {
        assertEquals(
                "About 15 hearts, hits for 1.5. Weak to Smite.",
                hint(15, OptionalDouble.of(1.5), Optional.of(MobHints.Weakness.SMITE), false));
    }

    @Test
    void aMobThatDoesNotAttackGetsNoHitAndOneHeartIsSingular() {
        assertEquals("About 1 heart.", hint(1, OptionalDouble.empty(), Optional.empty(), false));
        assertEquals("About 0.5 hearts.", hint(0.5, OptionalDouble.empty(), Optional.empty(), false));
    }

    @Test
    void fireImmunityComesLast() {
        assertEquals("About 10 hearts, hits for 3. Fireproof.", hint(10, OptionalDouble.of(3), Optional.empty(), true));
        assertEquals(
                "About 1.5 hearts. Weak to Impaling. Fireproof.",
                hint(1.5, OptionalDouble.empty(), Optional.of(MobHints.Weakness.IMPALING), true));
    }

    @Test
    void evenTheLongestHintsFitOnOneLine() {
        for (String longest : List.of(
                hint(999.5, OptionalDouble.of(99.5), Optional.of(MobHints.Weakness.BANE_OF_ARTHROPODS), false),
                hint(999.5, OptionalDouble.of(99.5), Optional.of(MobHints.Weakness.SMITE), true),
                hint(99, OptionalDouble.of(9.5), Optional.of(MobHints.Weakness.BANE_OF_ARTHROPODS), true))) {
            assertTrue(longest.length() < MAX_LENGTH, longest + " is " + longest.length() + " characters");
        }
    }

    @Test
    void healthAndDamageRoundToTheNearestHalfHeart() {
        assertEquals(10, MobHints.hearts(20));
        assertEquals(1.5, MobHints.hearts(3));
        assertEquals(1.5, MobHints.hearts(2.6));
        assertEquals(7.5, MobHints.hearts(15));
    }

    @Test
    void aMobAlwaysHasAtLeastHalfAHeart() {
        assertEquals(0.5, MobHints.hearts(0.2));
    }

    private static String hint(
            double hearts, OptionalDouble hits, Optional<MobHints.Weakness> weakness, boolean fireproof) {
        return MobHints.generated(hearts, hits, weakness, fireproof).getString();
    }
}
