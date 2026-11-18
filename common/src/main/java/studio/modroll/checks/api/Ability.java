package studio.modroll.checks.api;

import java.util.Optional;

public enum Ability implements Stat {
    STRENGTH("str"),
    DEXTERITY("dex"),
    CONSTITUTION("con"),
    INTELLIGENCE("int"),
    WISDOM("wis"),
    CHARISMA("cha");

    private final String id;

    Ability(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<Ability> byId(String id) {
        for (Ability ability : values()) {
            if (ability.id.equals(id)) {
                return Optional.of(ability);
            }
        }
        return Optional.empty();
    }
}
