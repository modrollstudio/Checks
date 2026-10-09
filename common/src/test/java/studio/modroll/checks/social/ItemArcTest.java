package studio.modroll.checks.social;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ItemArcTest {

    private static final Vec3 HAND = new Vec3(0, 1, 0);
    private static final Vec3 THIEF = new Vec3(4, 1, 0);
    private static final double GROUND = 0;

    @Test
    void aStolenItemArcsFromTheHandToTheThief() {
        assertEquals(HAND, ItemArc.position(HAND, THIEF, GROUND, false, 0));
        assertEquals(THIEF, ItemArc.position(HAND, THIEF, GROUND, false, ItemArc.STOLEN_TICKS));
        Vec3 halfway = ItemArc.position(HAND, THIEF, GROUND, false, ItemArc.STOLEN_TICKS / 2.0);
        assertTrue(halfway.y > HAND.y, "the item must rise in flight, was at " + halfway);
    }

    @Test
    void aCaughtItemLandsThenIsSnatchedBackToTheHand() {
        Vec3 landed = ItemArc.position(HAND, THIEF, GROUND, true, ItemArc.SNATCH_TICK - 1);
        assertEquals(GROUND, landed.y);
        assertEquals(HAND, ItemArc.position(HAND, THIEF, GROUND, true, ItemArc.CAUGHT_TICKS));
    }
}
