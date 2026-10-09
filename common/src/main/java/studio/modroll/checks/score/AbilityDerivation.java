package studio.modroll.checks.score;

import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.data.ScoresConfig;

/** Minecraft has no mental attributes, so INT, WIS and CHA take the configured flat default. */
public final class AbilityDerivation {

    private AbilityDerivation() {}

    public static int derive(Ability ability, ScoresConfig.Derivation c, AttributeValues attrs) {
        double raw =
                switch (ability) {
                    case STRENGTH -> c.strengthBase() + c.strengthPerAttackDamage() * attrs.attackDamage();
                    case DEXTERITY -> c.dexterityBase() + c.dexterityPerSpeed() * attrs.movementSpeed();
                    case CONSTITUTION -> c.constitutionBase() + c.constitutionPerHealth() * attrs.maxHealth();
                    case INTELLIGENCE, WISDOM, CHARISMA -> c.defaultMentalScore();
                };
        return Abilities.clamp((int) Math.round(raw));
    }
}
