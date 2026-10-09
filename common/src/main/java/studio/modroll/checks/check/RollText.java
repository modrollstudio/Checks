package studio.modroll.checks.check;

import net.minecraft.network.chat.Component;
import studio.modroll.checks.api.Stat;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.text.FallbackText;
import studio.modroll.critfall.api.combat.SaveResult;
import studio.modroll.critfall.api.dice.RollDetail;

/**
 * Roll lines: a check against a DC, and its d20 part, the face or under advantage or disadvantage the mode
 * and both faces.
 */
public final class RollText {

    private RollText() {}

    /** {@code Athletics vs DC 12: d20 15 (+2) = 17, success}. */
    public static Component check(Stat stat, SaveResult result) {
        return FallbackText.of(
                result.saved() ? "checks.trigger.success" : "checks.trigger.failure",
                SheetText.statName(stat),
                result.dc(),
                d20(result.roll()),
                SheetText.signed(result.saveBonus()),
                result.saveTotal());
    }

    public static Component d20(RollDetail roll) {
        return switch (roll.mode()) {
            case NORMAL -> FallbackText.of("checks.roll.d20", roll.kept());
            case ADVANTAGE -> twoD20s("checks.roll.d20.advantage", roll);
            case DISADVANTAGE -> twoD20s("checks.roll.d20.disadvantage", roll);
        };
    }

    private static Component twoD20s(String key, RollDetail roll) {
        return FallbackText.of(key, roll.kept(), roll.dropped().getAsInt());
    }
}
