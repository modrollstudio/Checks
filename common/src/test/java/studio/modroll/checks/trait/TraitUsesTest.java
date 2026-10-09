package studio.modroll.checks.trait;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TraitUsesTest {

    @Test
    void readyWhenNeverUsedOrOnceRecharged() {
        assertTrue(TraitUses.ready(Optional.empty(), 100, 24000));
        assertFalse(TraitUses.ready(Optional.of(100L), 24099, 24000));
        assertTrue(TraitUses.ready(Optional.of(100L), 24100, 24000));
    }
}
