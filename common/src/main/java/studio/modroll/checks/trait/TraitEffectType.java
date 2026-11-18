package studio.modroll.checks.trait;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum TraitEffectType {
    VANILLA_SAVE_ADVANTAGE,
    ROLL_BONUS,
    DAMAGE_RESISTANCE,
    EXTRA_HEALTH,
    DARKVISION,
    REROLL,
    LAST_STAND,
    IGNORED_BY,
    SKILL_CHOICES,
    ATTRIBUTE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<TraitEffectType> byId(String id) {
        return Arrays.stream(values()).filter(type -> type.id().equals(id)).findFirst();
    }
}
