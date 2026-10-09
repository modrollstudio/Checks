package studio.modroll.checks.api;

import java.util.Optional;

/**
 * How much of an entity's proficiency bonus a skill or saving throw adds: none (×0), proficient (×1)
 * or expertise (×2). Saving throws are never expertise.
 */
public enum Proficiency {
    NONE("none", 0),
    PROFICIENT("proficient", 1),
    EXPERTISE("expertise", 2);

    private final String id;
    private final int multiplier;

    Proficiency(String id, int multiplier) {
        this.id = id;
        this.multiplier = multiplier;
    }

    /** The lowercase id used in datapack JSON, commands, and NBT. */
    public String id() {
        return id;
    }

    /** What this level adds on top of the modifier for the given proficiency bonus. */
    public int bonus(int proficiencyBonus) {
        return proficiencyBonus * multiplier;
    }

    public static Optional<Proficiency> byId(String id) {
        for (Proficiency proficiency : values()) {
            if (proficiency.id.equals(id)) {
                return Optional.of(proficiency);
            }
        }
        return Optional.empty();
    }
}
