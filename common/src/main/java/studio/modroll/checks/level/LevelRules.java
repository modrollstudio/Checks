package studio.modroll.checks.level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntToDoubleFunction;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.data.ScoresConfig.ImprovementSettings;
import studio.modroll.checks.data.ScoresConfig.LevellingSettings;

/**
 * Levelling arithmetic, free of Minecraft so it is unit-testable. XP only ever raises the level; a set
 * level moves the XP to that level's threshold unless it already lies within the level. Improvements
 * and Expertise earned are derived from the level and class, so lowering and raising a level never grants
 * one twice.
 */
public final class LevelRules {

    private LevelRules() {}

    /** The highest level whose threshold {@code xp} meets. */
    public static int levelForXp(int xp, LevellingSettings settings) {
        int level = Levels.MIN_LEVEL;
        while (level < Levels.MAX_LEVEL && xp >= threshold(level + 1, settings)) {
            level++;
        }
        return level;
    }

    /** Adds XP, saturating at {@link Integer#MAX_VALUE}, and raises the level to what the new total reaches. */
    public static PlayerLevel gainXp(PlayerLevel current, int amount, LevellingSettings settings) {
        int xp = (int) Math.min(Integer.MAX_VALUE, (long) current.xp() + amount);
        return current.withLevelAndXp(Math.max(current.level(), levelForXp(xp, settings)), xp);
    }

    public static PlayerLevel setLevel(PlayerLevel current, int level, LevellingSettings settings) {
        boolean xpFits = levelForXp(current.xp(), settings) == level;
        return current.withLevelAndXp(level, xpFits ? current.xp() : threshold(level, settings));
    }

    /** The total XP {@code level} needs. */
    public static int threshold(int level, LevellingSettings settings) {
        return settings.xpThresholds().get(level - Levels.MIN_LEVEL);
    }

    /** The total XP the next level needs; empty at the top level. */
    public static Optional<Integer> nextThreshold(int level, LevellingSettings settings) {
        return level < Levels.MAX_LEVEL ? Optional.of(threshold(level + 1, settings)) : Optional.empty();
    }

    public static int proficiencyBonus(int level, LevellingSettings settings) {
        return settings.proficiencyBonuses().get(level - Levels.MIN_LEVEL);
    }

    /** The improvements the shared levels and the class's own earn up to {@code level}. */
    public static int earnedImprovements(int level, Optional<ResourceLocation> classId, LevellingSettings settings) {
        return reached(settings.improvements().levelsFor(classId), level);
    }

    /** Improvements earned up to the player's level that they have not chosen yet. */
    public static int pendingImprovements(
            PlayerLevel current, Optional<ResourceLocation> classId, LevellingSettings settings) {
        return Math.max(
                0,
                earnedImprovements(current.level(), classId, settings)
                        - current.improvements().size());
    }

    /** The skills the class's Expertise levels let the character pick, up to {@code level}. */
    public static int earnedExpertise(int level, Optional<ResourceLocation> classId, LevellingSettings settings) {
        return reached(settings.expertise().levelsFor(classId), level)
                * settings.expertise().skillsPerLevel();
    }

    public static int pendingExpertise(
            PlayerLevel current, Optional<ResourceLocation> classId, LevellingSettings settings) {
        return Math.max(
                0,
                earnedExpertise(current.level(), classId, settings)
                        - current.expertise().size());
    }

    /**
     * Checks a choice of {@code skills} for Expertise: one to {@code pending} different skills, each one the
     * character is proficient in and has no Expertise in yet.
     */
    public static Optional<ImprovementRejection> validateExpertise(
            List<ResourceLocation> skills, Predicate<ResourceLocation> proficient, int pending) {
        if (pending <= 0) {
            return Optional.of(ImprovementRejection.NO_EXPERTISE_PENDING);
        }
        if (skills.isEmpty() || skills.size() > pending || Set.copyOf(skills).size() != skills.size()) {
            return Optional.of(ImprovementRejection.WRONG_EXPERTISE);
        }
        if (!skills.stream().allMatch(proficient)) {
            return Optional.of(ImprovementRejection.NOT_PROFICIENT);
        }
        return Optional.empty();
    }

    /**
     * What each level up to {@code level} gave: the max health it added, {@code healthAt} being what the levels
     * add at each level (all of level 1's max health for level 1), the proficiency bonus where it rose, and the
     * improvements and Expertise it earned.
     */
    public static List<LevelGain> gains(
            int level,
            Optional<ResourceLocation> classId,
            LevellingSettings settings,
            IntToDoubleFunction healthAt,
            double levelOneHealth) {
        List<LevelGain> gains = new ArrayList<>();
        for (int at = Levels.MIN_LEVEL; at <= level; at++) {
            int bonus = proficiencyBonus(at, settings);
            boolean rose = at == Levels.MIN_LEVEL || bonus != proficiencyBonus(at - 1, settings);
            double health = at == Levels.MIN_LEVEL
                    ? levelOneHealth
                    : healthAt.applyAsDouble(at) - healthAt.applyAsDouble(at - 1);
            gains.add(new LevelGain(
                    at,
                    health,
                    rose ? Optional.of(bonus) : Optional.empty(),
                    earnedImprovements(at, classId, settings) - earnedImprovements(at - 1, classId, settings),
                    earnedExpertise(at, classId, settings) - earnedExpertise(at - 1, classId, settings)));
        }
        return gains;
    }

    private static int reached(List<Integer> levels, int level) {
        return (int) levels.stream().filter(at -> at <= level).count();
    }

    /**
     * Checks one improvement, {@code increases} in ability order, against the player's current
     * {@code scores}: the non-zero increases must match an allowed split, and no score may pass the max.
     */
    public static Optional<ImprovementRejection> validateImprovement(
            List<Integer> increases, Map<Ability, Integer> scores, int pending, ImprovementSettings settings) {
        if (pending <= 0) {
            return Optional.of(ImprovementRejection.NONE_PENDING);
        }
        if (increases.size() != Ability.values().length || increases.stream().anyMatch(increase -> increase < 0)) {
            return Optional.of(ImprovementRejection.WRONG_SPLIT);
        }
        List<Integer> given =
                descending(increases.stream().filter(increase -> increase > 0).toList());
        if (settings.options().stream().map(LevelRules::descending).noneMatch(given::equals)) {
            return Optional.of(ImprovementRejection.WRONG_SPLIT);
        }
        for (Ability ability : Ability.values()) {
            if (scores.get(ability) + increases.get(ability.ordinal()) > settings.maxScore()) {
                return Optional.of(ImprovementRejection.OVER_MAX);
            }
        }
        return Optional.empty();
    }

    /** The non-zero increases of a valid improvement, keyed by ability. */
    public static Map<Ability, Integer> improvement(List<Integer> increases) {
        Map<Ability, Integer> improvement = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            int increase = increases.get(ability.ordinal());
            if (increase > 0) {
                improvement.put(ability, increase);
            }
        }
        return improvement;
    }

    private static List<Integer> descending(List<Integer> values) {
        return values.stream().sorted(Comparator.reverseOrder()).toList();
    }
}
