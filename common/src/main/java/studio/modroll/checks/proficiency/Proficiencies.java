package studio.modroll.checks.proficiency;

import studio.modroll.checks.api.Proficiency;

public final class Proficiencies {

    public static final int MIN_BONUS = 0;
    public static final int MAX_BONUS = 10;

    private Proficiencies() {}

    public static int clampBonus(int bonus) {
        return Math.clamp(bonus, MIN_BONUS, MAX_BONUS);
    }

    public static boolean allowedOnSave(Proficiency proficiency) {
        return proficiency != Proficiency.EXPERTISE;
    }

    public static int saveModifier(int abilityModifier, Proficiency proficiency, int proficiencyBonus) {
        return abilityModifier + proficiency.bonus(proficiencyBonus);
    }
}
