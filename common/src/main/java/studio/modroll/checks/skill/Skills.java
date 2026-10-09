package studio.modroll.checks.skill;

import studio.modroll.checks.api.Proficiency;

public final class Skills {

    public static final int MIN_BONUS = -30;
    public static final int MAX_BONUS = 30;
    private static final int PASSIVE_BASE = 10;

    private Skills() {}

    public static int clampBonus(int bonus) {
        return Math.clamp(bonus, MIN_BONUS, MAX_BONUS);
    }

    public static int modifier(int abilityModifier, Proficiency proficiency, int proficiencyBonus, int bonus) {
        return abilityModifier + proficiency.bonus(proficiencyBonus) + bonus;
    }

    public static int passive(int skillModifier) {
        return PASSIVE_BASE + skillModifier;
    }
}
