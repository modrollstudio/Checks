package studio.modroll.checks.trait.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ClientDarkvisionTest {

    @AfterEach
    void reset() {
        ClientDarkvision.reset();
    }

    @Test
    void darkvisionIsAFloorUnderNightVision() {
        ClientDarkvision.set(0.4f);
        assertTrue(ClientDarkvision.active());
        assertEquals(0.4f, ClientDarkvision.scale(0f));
        assertEquals(1f, ClientDarkvision.scale(1f));
    }

    @Test
    void zeroStrengthLeavesTheLightmapAlone() {
        ClientDarkvision.set(0f);
        assertFalse(ClientDarkvision.active());
        assertEquals(0f, ClientDarkvision.scale(0f));
    }
}
