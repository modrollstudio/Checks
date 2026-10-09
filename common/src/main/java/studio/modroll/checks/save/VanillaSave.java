package studio.modroll.checks.save;

import java.util.Arrays;
import java.util.Optional;
import studio.modroll.checks.api.Ability;

/** A vanilla hazard that calls for a saving throw, and the ability that saves against it. */
public enum VanillaSave {
    EXPLOSION("explosion", Ability.DEXTERITY),
    POISON("poison", Ability.CONSTITUTION),
    KNOCKBACK("knockback", Ability.STRENGTH),
    FIRE("fire", Ability.DEXTERITY),
    DARKNESS("darkness", Ability.WISDOM);

    private final String id;
    private final Ability ability;

    VanillaSave(String id, Ability ability) {
        this.id = id;
        this.ability = ability;
    }

    /** The key under {@code vanilla_saves} in {@code scores.json} and in the lang file. */
    public String id() {
        return id;
    }

    public Ability ability() {
        return ability;
    }

    public static Optional<VanillaSave> byId(String id) {
        return Arrays.stream(values()).filter(save -> save.id().equals(id)).findFirst();
    }
}
