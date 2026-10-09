package studio.modroll.checks.check;

import java.util.Locale;
import java.util.Optional;
import studio.modroll.critfall.api.dice.RollMode;

/** Roll mode ids for datapack JSON: {@code normal}, {@code advantage} and {@code disadvantage}. */
public final class RollModes {

    private RollModes() {}

    public static Optional<RollMode> byId(String id) {
        for (RollMode mode : RollMode.values()) {
            if (id(mode).equals(id)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }

    public static String id(RollMode mode) {
        return mode.name().toLowerCase(Locale.ROOT);
    }
}
