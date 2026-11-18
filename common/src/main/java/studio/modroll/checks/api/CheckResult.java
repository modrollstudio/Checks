package studio.modroll.checks.api;

import java.util.OptionalInt;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.critfall.api.dice.RollMode;

public record CheckResult(RollDetail roll, int modifier, int bonus, int total, OptionalInt dc, boolean success) {

    private static final int NATURAL_1 = 1;
    private static final int NATURAL_20 = 20;

    public int natural() {
        return roll.kept();
    }

    public RollMode mode() {
        return roll.mode();
    }

    public boolean isNatural1() {
        return natural() == NATURAL_1;
    }

    public boolean isNatural20() {
        return natural() == NATURAL_20;
    }
}
