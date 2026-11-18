package studio.modroll.checks.score;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.data.ScoresConfig;

class AbilityDerivationTest {

    private static final ScoresConfig.Derivation DEFAULTS = ScoresConfig.DEFAULTS.derivation();

    @Test
    void strengthFollowsAttackDamage() {
        assertEquals(11, AbilityDerivation.derive(Ability.STRENGTH, DEFAULTS, new AttributeValues(3, 0, 0)));
    }

    @Test
    void dexterityFollowsMovementSpeed() {
        assertEquals(12, AbilityDerivation.derive(Ability.DEXTERITY, DEFAULTS, new AttributeValues(0, 0.25, 0)));
    }

    @Test
    void constitutionFollowsMaxHealth() {
        assertEquals(12, AbilityDerivation.derive(Ability.CONSTITUTION, DEFAULTS, new AttributeValues(0, 0, 20)));
    }

    @Test
    void mentalAbilitiesUseTheFlatDefault() {
        AttributeValues attrs = new AttributeValues(50, 50, 50);
        assertEquals(10, AbilityDerivation.derive(Ability.INTELLIGENCE, DEFAULTS, attrs));
        assertEquals(10, AbilityDerivation.derive(Ability.WISDOM, DEFAULTS, attrs));
        assertEquals(10, AbilityDerivation.derive(Ability.CHARISMA, DEFAULTS, attrs));
    }

    @Test
    void resultIsClampedToRange() {
        assertEquals(30, AbilityDerivation.derive(Ability.CONSTITUTION, DEFAULTS, new AttributeValues(0, 0, 500)));
        assertEquals(
                1,
                AbilityDerivation.derive(
                        Ability.STRENGTH,
                        new ScoresConfig.Derivation(true, -100, 1, 6, 24, 8, 0.2, 10),
                        new AttributeValues(0, 0, 0)));
    }

    @Test
    void configConstantsDriveTheResult() {
        ScoresConfig.Derivation steep = new ScoresConfig.Derivation(true, 8, 3.0, 6, 24, 8, 0.2, 10);
        assertEquals(17, AbilityDerivation.derive(Ability.STRENGTH, steep, new AttributeValues(3, 0, 0)));
    }
}
