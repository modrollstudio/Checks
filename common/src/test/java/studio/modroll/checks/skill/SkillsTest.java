package studio.modroll.checks.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Proficiency;

class SkillsTest {

    @Test
    void modifierWithoutProficiencyIsAbilityModifierPlusBonus() {
        assertEquals(5, Skills.modifier(Abilities.modifier(16), Proficiency.NONE, 2, 2));
        assertEquals(-1, Skills.modifier(Abilities.modifier(8), Proficiency.NONE, 2, 0));
        assertEquals(-3, Skills.modifier(Abilities.modifier(10), Proficiency.NONE, 2, -3));
    }

    @Test
    void proficientAddsTheProficiencyBonusOnce() {
        assertEquals(7, Skills.modifier(Abilities.modifier(16), Proficiency.PROFICIENT, 2, 2));
        assertEquals(2, Skills.modifier(Abilities.modifier(8), Proficiency.PROFICIENT, 3, 0));
    }

    @Test
    void expertiseAddsTheProficiencyBonusTwice() {
        assertEquals(9, Skills.modifier(Abilities.modifier(16), Proficiency.EXPERTISE, 2, 2));
        assertEquals(5, Skills.modifier(Abilities.modifier(8), Proficiency.EXPERTISE, 3, 0));
    }

    @Test
    void zeroProficiencyBonusAddsNothingAtAnyLevel() {
        for (Proficiency level : Proficiency.values()) {
            assertEquals(3, Skills.modifier(Abilities.modifier(16), level, 0, 0));
        }
    }

    @Test
    void passiveIsTenPlusModifier() {
        assertEquals(10, Skills.passive(0));
        assertEquals(15, Skills.passive(5));
        assertEquals(8, Skills.passive(-2));
        assertEquals(19, Skills.passive(Skills.modifier(Abilities.modifier(16), Proficiency.EXPERTISE, 2, 2)));
    }

    @Test
    void clampBonusHoldsBonusInRange() {
        assertEquals(Skills.MAX_BONUS, Skills.clampBonus(999));
        assertEquals(Skills.MIN_BONUS, Skills.clampBonus(-999));
        assertEquals(4, Skills.clampBonus(4));
    }
}
