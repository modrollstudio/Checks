package studio.modroll.checks.check;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class RollLinePayloadTest {

    private static final Component ROLL = Component.literal("Persuasion vs DC 12: d20 18 (+0) = 18, success");
    private static final Component FLAVOR = Component.literal("A fair price, and then some.");

    @Test
    void theDetailGoesOnALineBelowTheRoll() {
        assertEquals(List.of(ROLL, FLAVOR), new RollLinePayload(ROLL, Optional.of(FLAVOR), 160).lines());
    }

    @Test
    void aRollWithoutDetailIsOneLine() {
        assertEquals(List.of(ROLL), new RollLinePayload(ROLL, Optional.empty(), 160).lines());
    }
}
