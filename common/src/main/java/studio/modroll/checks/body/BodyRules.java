package studio.modroll.checks.body;

import java.util.Map;
import java.util.Optional;
import studio.modroll.checks.data.ScoresConfig;

/** The arithmetic behind health, armor class and the attribute extras. Pure, so it is unit tested directly. */
public final class BodyRules {

    /** Keeps a very low modifier from stopping a bow or crossbow altogether. */
    static final double MIN_SPEED = 0.1;

    private BodyRules() {}

    /** What DEX adds to AC: all of it unarmored or in light armor, up to {@code mediumMaxDex} in medium, none in heavy. */
    public static int armorClassBonus(int dexModifier, ArmorCategory category, int mediumMaxDex) {
        return switch (category) {
            case NONE, LIGHT -> dexModifier;
            case MEDIUM -> Math.min(dexModifier, mediumMaxDex);
            case HEAVY -> 0;
        };
    }

    public static double conHealth(int conModifier, ScoresConfig.HealthSettings settings) {
        return conModifier * settings.perConPoint();
    }

    /**
     * Each level gained, up to {@code level}, with a recorded hit die adds that die's fixed 5e value
     * ({@code sides / 2 + 1}) times the multiplier; rounded down to whole hit points.
     */
    public static double levelHealth(int level, Map<Integer, Integer> hitDice, ScoresConfig.HealthSettings settings) {
        double total = hitDice.entrySet().stream()
                .filter(entry -> entry.getKey() <= level)
                .mapToDouble(entry -> (entry.getValue() / 2 + 1) * settings.perLevelMultiplier())
                .sum();
        return Math.floor(total);
    }

    /** {@code perLevel} hit points for each character level, rounded down to whole hit points. */
    public static double levelHealth(int level, double perLevel) {
        return Math.floor(level * perLevel);
    }

    public static double extraAmount(int modifier, ScoresConfig.ExtraSetting setting) {
        return modifier * setting.perPoint();
    }

    /** A bow's draw time counted faster (or slower), so full power comes sooner. */
    public static int bowCharge(int charge, double amount) {
        return (int) Math.round(charge * speed(amount));
    }

    /** A crossbow's load time shortened (or lengthened); never below one tick. */
    public static int crossbowChargeDuration(int duration, double amount) {
        return Math.max(1, (int) Math.round(duration / speed(amount)));
    }

    public static float exhaustion(float exhaustion, double amount) {
        return (float) (exhaustion * Math.max(0.0, 1.0 - amount));
    }

    /**
     * The health to give back after a login, once the modifiers are reapplied. Vanilla loaded the saved
     * health capped at {@code loadedMax}; it is restored only when the loaded health is exactly the stored
     * health so capped, so a stored value older than the player's own save never heals them.
     */
    public static Optional<Float> restoredHealth(float stored, float loaded, float loadedMax, float newMax) {
        if (stored <= loaded || loaded != Math.min(stored, loadedMax)) {
            return Optional.empty();
        }
        return Optional.of(Math.min(stored, newMax));
    }

    private static double speed(double amount) {
        return Math.max(MIN_SPEED, 1.0 + amount);
    }
}
