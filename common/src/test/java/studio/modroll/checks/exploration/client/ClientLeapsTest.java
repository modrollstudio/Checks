package studio.modroll.checks.exploration.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.exploration.LeapPayload;

class ClientLeapsTest {

    private static final LeapPayload PUSH = new LeapPayload(0.0, 0.25);

    @Test
    void aPushAddsToTheLeapInTheAirAndKeepsItsRiseOrFall() {
        assertEquals(new Vec3(0.125, 0.42, 0.75), ClientLeaps.pushed(new Vec3(0.125, 0.42, 0.5), false, PUSH));
        assertEquals(new Vec3(0.0, -0.3, 0.5), ClientLeaps.pushed(new Vec3(0.0, -0.3, 0.25), false, PUSH));
    }

    @Test
    void aPushThatArrivesAfterLandingIsDropped() {
        Vec3 walking = new Vec3(0.0, 0.0, 0.2);
        assertEquals(walking, ClientLeaps.pushed(walking, true, PUSH));
    }
}
