package studio.modroll.checks.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.data.ScoresConfig.LevellingSettings;

class LevelRulesTest {

    private static final LevellingSettings SETTINGS = ScoresConfig.DEFAULTS.levelling();
    private static final Optional<ResourceLocation> NO_CLASS = Optional.empty();
    private static final Optional<ResourceLocation> FIGHTER = Optional.of(Checks.id("fighter"));
    private static final Optional<ResourceLocation> ROGUE = Optional.of(Checks.id("rogue"));
    private static final Optional<ResourceLocation> BARD = Optional.of(Checks.id("bard"));
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
        assertEquals(1, LevelRules.pendingImprovements(chosen, NO_CLASS, SETTINGS));
    }

    @Test
    void loweringAndRaisingALevelNeverGrantsAnImprovementTwice() {
        PlayerLevel chosen = PlayerLevel.START.withLevelAndXp(4, 0).withImprovement(Map.of(Ability.STRENGTH, 2));
        PlayerLevel lowered = LevelRules.setLevel(chosen, 2, SETTINGS);

        assertEquals(0, LevelRules.pendingImprovements(lowered, NO_CLASS, SETTINGS));
        assertEquals(0, LevelRules.pendingImprovements(LevelRules.setLevel(lowered, 4, SETTINGS), NO_CLASS, SETTINGS));
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

        assertEquals(3, level.improvementBonus(Ability.STRENGTH, 2));
        assertEquals(1, level.improvementBonus(Ability.DEXTERITY, 2));
        assertEquals(0, level.improvementBonus(Ability.WISDOM, 2));
    }

    @Test
    void onlyEarnedImprovementsApply() {
        PlayerLevel level = PlayerLevel.START
                .withImprovement(Map.of(Ability.STRENGTH, 2))
                .withImprovement(Map.of(Ability.STRENGTH, 1, Ability.DEXTERITY, 1));

        assertEquals(2, level.improvementBonus(Ability.STRENGTH, 1));
        assertEquals(0, level.improvementBonus(Ability.DEXTERITY, 1));
        assertEquals(0, level.improvementBonus(Ability.STRENGTH, 0));
    }

    @Test
    void fightersAndRoguesEarnTheirOwnImprovements() {
        assertEquals(1, LevelRules.earnedImprovements(5, FIGHTER, SETTINGS));
        assertEquals(2, LevelRules.earnedImprovements(6, FIGHTER, SETTINGS));
        assertEquals(7, LevelRules.earnedImprovements(20, FIGHTER, SETTINGS));
        assertEquals(2, LevelRules.earnedImprovements(9, ROGUE, SETTINGS));
        assertEquals(3, LevelRules.earnedImprovements(10, ROGUE, SETTINGS));
        assertEquals(6, LevelRules.earnedImprovements(20, ROGUE, SETTINGS));
        assertEquals(5, LevelRules.earnedImprovements(20, NO_CLASS, SETTINGS));
        assertEquals(5, LevelRules.earnedImprovements(20, Optional.of(Checks.id("wizard")), SETTINGS));
    }

    @Test
    void expertiseComesInPairsAtTheClassLevels() {
        assertEquals(2, LevelRules.earnedExpertise(1, ROGUE, SETTINGS));
        assertEquals(2, LevelRules.earnedExpertise(5, ROGUE, SETTINGS));
        assertEquals(4, LevelRules.earnedExpertise(6, ROGUE, SETTINGS));
        assertEquals(0, LevelRules.earnedExpertise(2, BARD, SETTINGS));
        assertEquals(2, LevelRules.earnedExpertise(3, BARD, SETTINGS));
        assertEquals(4, LevelRules.earnedExpertise(10, BARD, SETTINGS));
        assertEquals(0, LevelRules.earnedExpertise(20, FIGHTER, SETTINGS));
        PlayerLevel chose = PlayerLevel.START.withLevelAndXp(6, 0).withExpertise(List.of(Checks.id("stealth")));
        assertEquals(3, LevelRules.pendingExpertise(chose, ROGUE, SETTINGS));
    }

    @Test
    void expertiseNeedsDifferentProficientSkillsAndOnePending() {
        ResourceLocation stealth = Checks.id("stealth");
        ResourceLocation arcana = Checks.id("arcana");
        Predicate<ResourceLocation> proficient = stealth::equals;
        assertEquals(Optional.empty(), LevelRules.validateExpertise(List.of(stealth), proficient, 2));
        assertEquals(
                Optional.of(ImprovementRejection.NO_EXPERTISE_PENDING),
                LevelRules.validateExpertise(List.of(stealth), proficient, 0));
        assertEquals(
                Optional.of(ImprovementRejection.WRONG_EXPERTISE),
                LevelRules.validateExpertise(List.of(stealth, stealth), skill -> true, 2));
        assertEquals(
                Optional.of(ImprovementRejection.WRONG_EXPERTISE),
                LevelRules.validateExpertise(List.of(stealth, arcana), skill -> true, 1));
        assertEquals(
                Optional.of(ImprovementRejection.WRONG_EXPERTISE),
                LevelRules.validateExpertise(List.of(), proficient, 2));
        assertEquals(
                Optional.of(ImprovementRejection.NOT_PROFICIENT),
                LevelRules.validateExpertise(List.of(arcana), proficient, 2));
    }

    @Test
    void earnedExpertiseKeepsTheFirstChosen() {
        PlayerLevel level = PlayerLevel.START.withExpertise(List.of(Checks.id("stealth"), Checks.id("arcana")));
        assertEquals(Set.of(Checks.id("stealth")), level.expertise(1));
        assertEquals(Set.of(), level.expertise(0));
        assertEquals(Set.of(Checks.id("stealth"), Checks.id("arcana")), level.expertise(5));
    }

    @Test
    void gainsListWhatEachLevelGave() {
        List<LevelGain> gains = LevelRules.gains(6, ROGUE, SETTINGS, level -> level * 1.5, 24);

        assertEquals(6, gains.size());
        assertEquals(new LevelGain(1, 24, Optional.of(2), 0, 2), gains.get(0));
        assertEquals(new LevelGain(2, 1.5, Optional.empty(), 0, 0), gains.get(1));
        assertEquals(new LevelGain(4, 1.5, Optional.empty(), 1, 0), gains.get(3));
        assertEquals(new LevelGain(5, 1.5, Optional.of(3), 0, 0), gains.get(4));
        assertEquals(new LevelGain(6, 1.5, Optional.empty(), 0, 2), gains.get(5));
    }

    @Test
    void theHitDicePoolIsTheLevelLessThoseSpent() {
        PlayerLevel level = PlayerLevel.START.withLevelAndXp(5, 0).withHitDiceSpent(3);
        assertEquals(2, level.hitDiceLeft());
        assertEquals(5, level.withHitDiceSpent(9).hitDiceSpent());
        assertEquals(0, level.withHitDiceSpent(-2).hitDiceSpent());
        assertEquals(0, level.withLevelAndXp(2, 0).hitDiceLeft());
    }

    private static int pendingAt(int level) {
        return LevelRules.pendingImprovements(PlayerLevel.START.withLevelAndXp(level, 0), NO_CLASS, SETTINGS);
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
