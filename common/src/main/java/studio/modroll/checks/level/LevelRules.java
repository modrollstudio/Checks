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

public final class LevelRules {

    public static final int BASE_ATTACKS = 1;

    private LevelRules() {}

    public static int levelForXp(int xp, LevellingSettings settings) {
        int level = Levels.MIN_LEVEL;
        while (level < Levels.MAX_LEVEL && xp >= threshold(level + 1, settings)) {
            level++;
        }
        return level;
    }

    public static PlayerLevel gainXp(PlayerLevel current, int amount, LevellingSettings settings) {
        int xp = (int) Math.min(Integer.MAX_VALUE, (long) current.xp() + amount);
        return current.withLevelAndXp(Math.max(current.level(), levelForXp(xp, settings)), xp);
    }

    public static PlayerLevel setLevel(PlayerLevel current, int level, LevellingSettings settings) {
        boolean xpFits = levelForXp(current.xp(), settings) == level;
        return current.withLevelAndXp(level, xpFits ? current.xp() : threshold(level, settings));
    }

    public static int threshold(int level, LevellingSettings settings) {
        return settings.xpThresholds().get(level - Levels.MIN_LEVEL);
    }

    public static Optional<Integer> nextThreshold(int level, LevellingSettings settings) {
        return level < Levels.MAX_LEVEL ? Optional.of(threshold(level + 1, settings)) : Optional.empty();
    }

    public static int proficiencyBonus(int level, LevellingSettings settings) {
        return settings.proficiencyBonuses().get(level - Levels.MIN_LEVEL);
    }

    public static int earnedImprovements(int level, Optional<ResourceLocation> classId, LevellingSettings settings) {
        return reached(settings.improvements().levelsFor(classId), level);
    }

    public static int pendingImprovements(
            PlayerLevel current, Optional<ResourceLocation> classId, LevellingSettings settings) {
        return Math.max(
                0,
                earnedImprovements(current.level(), classId, settings)
                        - current.improvements().size());
    }

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

    public static int attacksPerAction(int level, Optional<ResourceLocation> classId, LevellingSettings settings) {
        return BASE_ATTACKS + reached(settings.extraAttack().levelsFor(classId), level);
    }

    public static Optional<Integer> nextExtraAttack(
            int level, Optional<ResourceLocation> classId, LevellingSettings settings) {
        return settings.extraAttack().levelsFor(classId).stream()
                .filter(at -> at > level)
                .min(Integer::compare);
    }

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
            int attacks = attacksPerAction(at, classId, settings);
            double health = at == Levels.MIN_LEVEL
                    ? levelOneHealth
                    : healthAt.applyAsDouble(at) - healthAt.applyAsDouble(at - 1);
            gains.add(new LevelGain(
                    at,
                    health,
                    rose ? Optional.of(bonus) : Optional.empty(),
                    earnedImprovements(at, classId, settings) - earnedImprovements(at - 1, classId, settings),
                    earnedExpertise(at, classId, settings) - earnedExpertise(at - 1, classId, settings),
                    attacks > attacksPerAction(at - 1, classId, settings) ? Optional.of(attacks) : Optional.empty()));
        }
        return gains;
    }

    private static int reached(List<Integer> levels, int level) {
        return (int) levels.stream().filter(at -> at <= level).count();
    }

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
