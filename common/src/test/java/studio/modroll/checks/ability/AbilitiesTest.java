package studio.modroll.checks.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;

class AbilitiesTest {

    @Test
    void modifierAtBoundaries() {
        assertEquals(-5, Abilities.modifier(1));
        assertEquals(0, Abilities.modifier(10));
        assertEquals(0, Abilities.modifier(11));
        assertEquals(5, Abilities.modifier(20));
        assertEquals(10, Abilities.modifier(30));
    }

    @Test
    void modifierRoundsTowardNegativeInfinity() {
        assertEquals(-1, Abilities.modifier(8));
        assertEquals(-1, Abilities.modifier(9));
    }

    @Test
    void clampHoldsScoreInRange() {
        assertEquals(1, Abilities.clamp(0));
        assertEquals(1, Abilities.clamp(-4));
        assertEquals(30, Abilities.clamp(99));
        assertEquals(17, Abilities.clamp(17));
    }

    @Test
    void abilitiesHaveStableIds() {
        assertEquals("str", Ability.STRENGTH.id());
        assertEquals("cha", Ability.CHARISMA.id());
        assertEquals(Optional.of(Ability.WISDOM), Ability.byId("wis"));
        assertTrue(Ability.byId("nope").isEmpty());
    }
}
