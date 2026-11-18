package studio.modroll.checks.api;

import java.util.Optional;

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

    public String id() {
        return id;
    }

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
