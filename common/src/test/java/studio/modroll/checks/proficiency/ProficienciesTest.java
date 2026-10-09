package studio.modroll.checks.proficiency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Proficiency;

class ProficienciesTest {

    @Test
    void saveModifierAddsTheBonusOnlyWhenProficient() {
        assertEquals(-1, Proficiencies.saveModifier(-1, Proficiency.NONE, 2));
        assertEquals(1, Proficiencies.saveModifier(-1, Proficiency.PROFICIENT, 2));
        assertEquals(7, Proficiencies.saveModifier(3, Proficiency.PROFICIENT, 4));
    }

    @Test
    void expertiseIsNotAllowedOnSaves() {
        assertTrue(Proficiencies.allowedOnSave(Proficiency.NONE));
        assertTrue(Proficiencies.allowedOnSave(Proficiency.PROFICIENT));
        assertFalse(Proficiencies.allowedOnSave(Proficiency.EXPERTISE));
    }

    @Test
    void clampBonusHoldsBonusInRange() {
        assertEquals(Proficiencies.MAX_BONUS, Proficiencies.clampBonus(99));
        assertEquals(Proficiencies.MIN_BONUS, Proficiencies.clampBonus(-1));
        assertEquals(3, Proficiencies.clampBonus(3));
    }

    @Test
    void levelsMultiplyTheBonusAndParseByLowercaseId() {
        assertEquals(0, Proficiency.NONE.bonus(3));
        assertEquals(3, Proficiency.PROFICIENT.bonus(3));
        assertEquals(6, Proficiency.EXPERTISE.bonus(3));
        assertEquals(Optional.of(Proficiency.EXPERTISE), Proficiency.byId("expertise"));
        assertEquals(Optional.empty(), Proficiency.byId("EXPERTISE"));
        assertEquals(Optional.empty(), Proficiency.byId("master"));
    }
}
