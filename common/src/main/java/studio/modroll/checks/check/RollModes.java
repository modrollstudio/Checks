package studio.modroll.checks.check;

import java.util.Arrays;
import java.util.Locale;
import studio.modroll.checks.data.LenientJson;
import studio.modroll.critfall.api.dice.RollMode;

public final class RollModes {

    private RollModes() {}

    public static RollMode parse(LenientJson json) {
        return json.optionalString("mode")
                .map(text -> Arrays.stream(RollMode.values())
                        .filter(mode -> id(mode).equals(text))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "'mode' must be normal, advantage or disadvantage, was '" + text + "'")))
                .orElse(RollMode.NORMAL);
    }

    public static String id(RollMode mode) {
        return mode.name().toLowerCase(Locale.ROOT);
    }
}
