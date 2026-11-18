package studio.modroll.checks.body;

import java.util.Optional;
import studio.modroll.checks.data.ScoresConfig;

public final class BodyRules {

    static final double MIN_SPEED = 0.1;

    private BodyRules() {}

    public static int armorClassBonus(int dexModifier, ArmorCategory category, int mediumMaxDex) {
        return switch (category) {
            case NONE, LIGHT -> dexModifier;
            case MEDIUM -> Math.min(dexModifier, mediumMaxDex);
            case HEAVY -> 0;
        };
    }

    public static int bodyArmorFloor(int armorClass, int dexBonus, int armorClassWithout, int dexModifier) {
        return Math.max(0, armorClassWithout + dexModifier - (armorClass + dexBonus));
    }

    public static double conHealth(int conModifier, ScoresConfig.HealthSettings settings) {
        return conModifier * settings.perConPoint();
    }

    public static int perLevel(int hitDie, int conModifier, ScoresConfig.HealthSettings settings) {
        int con = settings.conPerLevel() ? conModifier : 0;
        return Math.max(settings.minPerLevel(), hitDie / 2 + 1 + con);
    }

    public static double levelHealth(int level, int hitDie, int conModifier, ScoresConfig.HealthSettings settings) {
        int levelsAfterFirst = Math.max(0, level - 1);
        return Math.floor(levelsAfterFirst * perLevel(hitDie, conModifier, settings) * settings.perLevelMultiplier());
    }

    public static double levelHealth(int level, double perLevel) {
        return Math.floor(level * perLevel);
    }

    public static double wholeHearts(double bonus) {
        return 2 * Math.floor(bonus / 2);
    }

    public static float hitDieHealing(int rolled, int conModifier, int min, double multiplier) {
        return (float) (Math.max(min, rolled + conModifier) * multiplier);
    }

    public static int hitDiceRegained(int level, double fraction, int min) {
        return Math.max(min, (int) Math.floor(level * fraction));
    }

    public static double extraAmount(int modifier, ScoresConfig.ExtraSetting setting) {
        return modifier * setting.perPoint();
    }

    public static int bowCharge(int charge, double amount) {
        return (int) Math.round(charge * speed(amount));
    }

    public static int crossbowChargeDuration(int duration, double amount) {
        return Math.max(1, (int) Math.round(duration / speed(amount)));
    }

    public static float exhaustion(float exhaustion, double amount) {
        return (float) (exhaustion * Math.max(0.0, 1.0 - amount));
    }

    /**
     * Vanilla caps loaded health at {@code loadedMax}; it is restored only when the loaded health is exactly the stored
     * one so capped, so a stored value older than the player's save never heals them.
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
