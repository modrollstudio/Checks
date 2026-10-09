package studio.modroll.checks.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.data.ScoresConfig.LevellingSettings;

class LevelRulesTest {

    private static final LevellingSettings SETTINGS = ScoresConfig.DEFAULTS.levelling();
    private static final List<Integer> PLUS_TWO_STR = List.of(2, 0, 0, 0, 0, 0);

    @Test
    void levelForXpFollowsTheThresholds() {
        assertEquals(1, LevelRules.levelForXp(0, SETTINGS));
        assertEquals(1, LevelRules.levelForXp(29, SETTINGS));
        assertEquals(2, LevelRules.levelForXp(30, SETTINGS));
        assertEquals(5, LevelRules.levelForXp(650, SETTINGS));
        assertEquals(19, LevelRules.levelForXp(35499, SETTINGS));
        assertEquals(20, LevelRules.levelForXp(35500, SETTINGS));
        assertEquals(20, LevelRules.levelForXp(Integer.MAX_VALUE, SETTINGS));
    }

    @Test
    void gainingXpRaisesTheLevelPastEveryThresholdItMeets() {
        PlayerLevel level = LevelRules.gainXp(PlayerLevel.START, 300, SETTINGS);

        assertEquals(4, level.level());
        assertEquals(300, level.xp());
    }

    @Test
    void xpSaturatesInsteadOfOverflowing() {
        PlayerLevel near = PlayerLevel.START.withLevelAndXp(20, Integer.MAX_VALUE - 1);

        assertEquals(Integer.MAX_VALUE, LevelRules.gainXp(near, 10, SETTINGS).xp());
    }

    @Test
    void xpNeverLowersASetLevel() {
        PlayerLevel milestone = LevelRules.setLevel(PlayerLevel.START, 10, SETTINGS);

        assertEquals(10, LevelRules.gainXp(milestone, 1, SETTINGS).level());
    }

    @Test
    void settingALevelMovesXpToItsThresholdUnlessItAlreadyFits() {
        PlayerLevel set = LevelRules.setLevel(PlayerLevel.START.withLevelAndXp(5, 700), 3, SETTINGS);
        assertEquals(3, set.level());
        assertEquals(90, set.xp());

        PlayerLevel kept = LevelRules.setLevel(PlayerLevel.START.withLevelAndXp(5, 700), 5, SETTINGS);
        assertEquals(700, kept.xp());
    }

    @Test
    void thresholdsForTheCurrentAndNextLevel() {
        assertEquals(650, LevelRules.threshold(5, SETTINGS));
        assertEquals(Optional.of(1400), LevelRules.nextThreshold(5, SETTINGS));
        assertEquals(Optional.empty(), LevelRules.nextThreshold(20, SETTINGS));
    }

    @Test
    void proficiencyBonusPerLevel() {
        for (int level = 1; level <= 20; level++) {
            int expected = level <= 4 ? 2 : level <= 8 ? 3 : level <= 12 ? 4 : level <= 16 ? 5 : 6;
            assertEquals(expected, LevelRules.proficiencyBonus(level, SETTINGS), "level " + level);
        }
    }

    @Test
    void improvementsAreEarnedAtTheirLevelsAndStackUntilChosen() {
        assertEquals(0, pendingAt(3));
        assertEquals(1, pendingAt(4));
        assertEquals(2, pendingAt(8));
        assertEquals(5, pendingAt(20));
        PlayerLevel chosen = PlayerLevel.START.withLevelAndXp(8, 0).withImprovement(Map.of(Ability.STRENGTH, 2));
        assertEquals(1, LevelRules.pendingImprovements(chosen, SETTINGS));
    }

    @Test
    void loweringAndRaisingALevelNeverGrantsAnImprovementTwice() {
        PlayerLevel chosen = PlayerLevel.START.withLevelAndXp(4, 0).withImprovement(Map.of(Ability.STRENGTH, 2));
        PlayerLevel lowered = LevelRules.setLevel(chosen, 2, SETTINGS);

        assertEquals(0, LevelRules.pendingImprovements(lowered, SETTINGS));
        assertEquals(0, LevelRules.pendingImprovements(LevelRules.setLevel(lowered, 4, SETTINGS), SETTINGS));
    }

    @Test
    void plusTwoToOneOrPlusOneToTwoIsAccepted() {
        assertEquals(Optional.empty(), validate(PLUS_TWO_STR, scores(10), 1));
        assertEquals(Optional.empty(), validate(List.of(0, 1, 0, 0, 0, 1), scores(10), 1));
    }

    @Test
    void otherSplitsAreRejected() {
        assertEquals(Optional.of(ImprovementRejection.WRONG_SPLIT), validate(List.of(1, 0, 0, 0, 0, 0), scores(10), 1));
        assertEquals(Optional.of(ImprovementRejection.WRONG_SPLIT), validate(List.of(3, 0, 0, 0, 0, 0), scores(10), 1));
        assertEquals(Optional.of(ImprovementRejection.WRONG_SPLIT), validate(List.of(2, 1, 0, 0, 0, 0), scores(10), 1));
        assertEquals(Optional.of(ImprovementRejection.WRONG_SPLIT), validate(List.of(0, 0, 0, 0, 0, 0), scores(10), 1));
        assertEquals(
                Optional.of(ImprovementRejection.WRONG_SPLIT), validate(List.of(3, -1, 0, 0, 0, 0), scores(10), 1));
        assertEquals(Optional.of(ImprovementRejection.WRONG_SPLIT), validate(List.of(2, 0, 0), scores(10), 1));
    }

    @Test
    void anImprovementCannotRaiseAScoreAboveTheMax() {
        assertEquals(Optional.of(ImprovementRejection.OVER_MAX), validate(PLUS_TWO_STR, scores(19), 1));
        assertEquals(Optional.empty(), validate(List.of(1, 1, 0, 0, 0, 0), scores(19), 1));
    }

    @Test
    void anImprovementNeedsOnePending() {
        assertEquals(Optional.of(ImprovementRejection.NONE_PENDING), validate(PLUS_TWO_STR, scores(10), 0));
    }

    @Test
    void improvementKeepsOnlyTheRaisedAbilities() {
        assertEquals(
                Map.of(Ability.DEXTERITY, 1, Ability.CHARISMA, 1), LevelRules.improvement(List.of(0, 1, 0, 0, 0, 1)));
    }

    @Test
    void chosenImprovementsAddUpPerAbility() {
        PlayerLevel level = PlayerLevel.START
                .withImprovement(Map.of(Ability.STRENGTH, 2))
                .withImprovement(Map.of(Ability.STRENGTH, 1, Ability.DEXTERITY, 1));

        assertEquals(3, level.improvementBonus(Ability.STRENGTH));
        assertEquals(1, level.improvementBonus(Ability.DEXTERITY));
        assertEquals(0, level.improvementBonus(Ability.WISDOM));
    }

    private static int pendingAt(int level) {
        return LevelRules.pendingImprovements(PlayerLevel.START.withLevelAndXp(level, 0), SETTINGS);
    }

    private static Optional<ImprovementRejection> validate(
            List<Integer> increases, Map<Ability, Integer> scores, int pending) {
        return LevelRules.validateImprovement(increases, scores, pending, SETTINGS.improvements());
    }

    /** STR at {@code strength}, every other ability at 10. */
    private static Map<Ability, Integer> scores(int strength) {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            scores.put(ability, 10);
        }
        scores.put(Ability.STRENGTH, strength);
        return scores;
    }
}
