package studio.modroll.checks.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BonusSplitsTest {

    private static final List<List<Integer>> DEFAULTS = List.of(List.of(2, 1), List.of(1, 1, 1));

    @Test
    void partialSplitsThatCanStillGrowAreReachable() {
        assertTrue(BonusSplits.reachable(List.of(), DEFAULTS));
        assertTrue(BonusSplits.reachable(List.of(1), DEFAULTS));
        assertTrue(BonusSplits.reachable(List.of(2), DEFAULTS));
        assertTrue(BonusSplits.reachable(List.of(1, 1), DEFAULTS));
        assertTrue(BonusSplits.reachable(List.of(1, 2), DEFAULTS));
    }

    @Test
    void splitsNoOptionCanHoldAreNot() {
        assertFalse(BonusSplits.reachable(List.of(3), DEFAULTS));
        assertFalse(BonusSplits.reachable(List.of(2, 2), DEFAULTS));
        assertFalse(BonusSplits.reachable(List.of(2, 1, 1), DEFAULTS));
        assertFalse(BonusSplits.reachable(List.of(1, 1, 1, 1), DEFAULTS));
        assertFalse(BonusSplits.reachable(List.of(1), List.of()));
    }

    @Test
    void remainingListsWhatEachMatchingOptionStillNeeds() {
        assertEquals(DEFAULTS, BonusSplits.remaining(List.of(), DEFAULTS));
        assertEquals(List.of(List.of(2), List.of(1, 1)), BonusSplits.remaining(List.of(1), DEFAULTS));
        assertEquals(List.of(List.of(1)), BonusSplits.remaining(List.of(2), DEFAULTS));
        assertEquals(List.of(List.of()), BonusSplits.remaining(List.of(1, 2), DEFAULTS));
    }
}
