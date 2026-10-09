package studio.modroll.checks.trait;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** The fixed set of things a trait can do; each has its own switch under {@code traits} in {@code scores.json}. */
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

    /** The {@code type} in trait JSON and the key under {@code traits} in {@code scores.json}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<TraitEffectType> byId(String id) {
        return Arrays.stream(values()).filter(type -> type.id().equals(id)).findFirst();
    }
}
