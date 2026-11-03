package studio.modroll.checks.body;

import java.util.Optional;
import studio.modroll.checks.data.ScoresConfig;

/** The arithmetic behind health, armor class and the attribute extras. Pure, so it is unit tested directly. */
public final class BodyRules {

    /** Keeps a very low modifier from stopping a bow or crossbow altogether. */
    static final double MIN_SPEED = 0.1;

    private BodyRules() {}

    /**
     * What DEX adds to AC by the body armor's category: all of it without body armor or in light armor, up
     * to {@code mediumMaxDex} in medium, none in heavy.
     */
    public static int armorClassBonus(int dexModifier, ArmorCategory category, int mediumMaxDex) {
        return switch (category) {
            case NONE, LIGHT -> dexModifier;
            case MEDIUM -> Math.min(dexModifier, mediumMaxDex);
            case HEAVY -> 0;
        };
    }

    /**
     * What keeps body armor from lowering AC: when its DEX cap leaves {@code armorClass + dexBonus} below
     * {@code armorClassWithout + dexModifier}, the AC without it and with all of DEX, the difference; zero otherwise.
     */
    public static int bodyArmorFloor(int armorClass, int dexBonus, int armorClassWithout, int dexModifier) {
        return Math.max(0, armorClassWithout + dexModifier - (armorClass + dexBonus));
    }

    public static double conHealth(int conModifier, ScoresConfig.HealthSettings settings) {
        return conModifier * settings.perConPoint();
    }

    /**
     * What each level after the first adds: the hit die's fixed value ({@code sides / 2 + 1}), plus the CON modifier
     * when {@code con_per_level} is on, at least {@code min_per_level}.
     */
    public static int perLevel(int hitDie, int conModifier, ScoresConfig.HealthSettings settings) {
        int con = settings.conPerLevel() ? conModifier : 0;
        return Math.max(settings.minPerLevel(), hitDie / 2 + 1 + con);
    }

    /** Levels 2 to {@code level} each add {@link #perLevel} times the multiplier; rounded down to whole hit points. */
    public static double levelHealth(int level, int hitDie, int conModifier, ScoresConfig.HealthSettings settings) {
        int levelsAfterFirst = Math.max(0, level - 1);
        return Math.floor(levelsAfterFirst * perLevel(hitDie, conModifier, settings) * settings.perLevelMultiplier());
    }

    /** {@code perLevel} hit points for each character level, rounded down to whole hit points. */
    public static double levelHealth(int level, double perLevel) {
        return Math.floor(level * perLevel);
    }

    /** {@code bonus} rounded down to an even number of hit points, so the last heart is never half empty. */
    public static double wholeHearts(double bonus) {
        return 2 * Math.floor(bonus / 2);
    }

    /** What spending a Hit Die heals: the roll plus the CON modifier, at least {@code min}, times the multiplier. */
    public static float hitDieHealing(int rolled, int conModifier, int min, double multiplier) {
        return (float) (Math.max(min, rolled + conModifier) * multiplier);
    }

    /** The Hit Dice a long rest gives back: {@code fraction} of the level, rounded down, at least {@code min}. */
    public static int hitDiceRegained(int level, double fraction, int min) {
        return Math.max(min, (int) Math.floor(level * fraction));
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
