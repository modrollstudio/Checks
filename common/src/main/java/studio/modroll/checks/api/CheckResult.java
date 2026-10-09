package studio.modroll.checks.api;

import java.util.OptionalInt;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.critfall.api.dice.RollMode;

/**
 * One rolled check, save or contest side.
 *
 * @param roll the d20: its mode, the kept face and the dropped one under advantage or disadvantage
 * @param modifier the stat's own modifier
 * @param bonus what bonus sources and listeners added
 * @param total the kept face + modifier + bonus
 * @param dc the DC rolled against; empty for a contest side or an open roll
 * @param success the DC was met, or this side won the contest; always false for an open roll
 */
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
