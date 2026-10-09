package studio.modroll.checks.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static studio.modroll.checks.creation.Offers.ATHLETICS;
import static studio.modroll.checks.creation.Offers.PERCEPTION;
import static studio.modroll.checks.creation.Offers.STEALTH;
import static studio.modroll.checks.creation.Offers.TWO_SKILLS;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.body.Size;

class CreationRulesTest {

    private static final List<Integer> ROLLS = List.of(17, 9, 12, 12, 14, 6);

    @Test
    void standardArrayAcceptsAnyAssignment() {
        assertAccepted(CreationMethod.STANDARD_ARRAY, List.of(8, 10, 15, 14, 12, 13), Offers.unrolled());
    }

    @Test
    void standardArrayRejectsAnyOtherValues() {
        assertRejected(
                Rejection.WRONG_ARRAY,
                CreationMethod.STANDARD_ARRAY,
                List.of(15, 15, 13, 12, 10, 8),
                Offers.unrolled());
    }

    @Test
    void pointBuyAcceptsTheFullBudgetAndLess() {
        assertAccepted(CreationMethod.POINT_BUY, List.of(15, 15, 15, 8, 8, 8), Offers.unrolled());
        assertAccepted(CreationMethod.POINT_BUY, List.of(8, 8, 8, 8, 8, 8), Offers.unrolled());
    }

    @Test
    void pointBuyRejectsOverspentPoints() {
        assertRejected(
                Rejection.POINTS_OVERSPENT, CreationMethod.POINT_BUY, List.of(15, 15, 15, 9, 8, 8), Offers.unrolled());
    }

    @Test
    void pointBuyRejectsScoresOutsideTheCostTable() {
        assertRejected(
                Rejection.POINT_BUY_RANGE, CreationMethod.POINT_BUY, List.of(16, 8, 8, 8, 8, 8), Offers.unrolled());
        assertRejected(
                Rejection.POINT_BUY_RANGE, CreationMethod.POINT_BUY, List.of(7, 8, 8, 8, 8, 8), Offers.unrolled());
    }

    @Test
    void rollAcceptsTheRolledTotalsInAnyOrder() {
        assertAccepted(CreationMethod.ROLL, List.of(6, 12, 17, 9, 12, 14), Offers.rolled(CreationMethod.ROLL, ROLLS));
    }

    @Test
    void rollRejectsEditedTotals() {
        assertRejected(
                Rejection.ROLLS_CHANGED,
                CreationMethod.ROLL,
                List.of(18, 9, 12, 12, 14, 6),
                Offers.rolled(CreationMethod.ROLL, ROLLS));
    }

    @Test
    void rollingMethodsNeedRollsFirst() {
        assertRejected(Rejection.NOT_ROLLED, CreationMethod.ROLL, ROLLS, Offers.unrolled());
        assertRejected(Rejection.NOT_ROLLED, CreationMethod.HARDCORE, ROLLS, Offers.unrolled());
    }

    @Test
    void hardcoreAcceptsOnlyTheRolledOrder() {
        CreationOffer offer = Offers.rolled(CreationMethod.HARDCORE, ROLLS);
        assertAccepted(CreationMethod.HARDCORE, ROLLS, offer);
        assertRejected(Rejection.HARDCORE_ORDER, CreationMethod.HARDCORE, List.of(9, 17, 12, 12, 14, 6), offer);
        assertRejected(Rejection.ROLLS_CHANGED, CreationMethod.HARDCORE, List.of(17, 9, 12, 12, 14, 7), offer);
    }

    @Test
    void rollingLocksTheMethod() {
        assertRejected(
                Rejection.METHOD_LOCKED,
                CreationMethod.STANDARD_ARRAY,
                List.of(15, 14, 13, 12, 10, 8),
                Offers.rolled(CreationMethod.ROLL, ROLLS));
        assertRejected(
                Rejection.METHOD_LOCKED, CreationMethod.HARDCORE, ROLLS, Offers.rolled(CreationMethod.ROLL, ROLLS));
    }

    @Test
    void methodsTurnedOffInConfigAreRejected() {
        CreationOffer arrayOnly = Offers.withMethodsAndChoices(List.of(CreationMethod.STANDARD_ARRAY), 2);
        assertRejected(Rejection.METHOD_NOT_ALLOWED, CreationMethod.POINT_BUY, List.of(8, 8, 8, 8, 8, 8), arrayOnly);
    }

    @Test
    void everyAbilityNeedsExactlyOneScore() {
        assertRejected(Rejection.SCORE_COUNT, CreationMethod.POINT_BUY, List.of(8, 8, 8, 8, 8), Offers.unrolled());
    }

    @Test
    void skillPicksMustBeTheRequiredCountOfDistinctOfferedSkills() {
        List<Integer> array = List.of(15, 14, 13, 12, 10, 8);
        assertEquals(
                Optional.of(Rejection.SKILL_COUNT),
                validate(CreationMethod.STANDARD_ARRAY, array, List.of(ATHLETICS, STEALTH, PERCEPTION)));
        assertEquals(
                Optional.of(Rejection.SKILL_COUNT), validate(CreationMethod.STANDARD_ARRAY, array, List.of(STEALTH)));
        assertEquals(
                Optional.of(Rejection.DUPLICATE_SKILL),
                validate(CreationMethod.STANDARD_ARRAY, array, List.of(STEALTH, STEALTH)));
        assertEquals(
                Optional.of(Rejection.UNKNOWN_SKILL),
                validate(
                        CreationMethod.STANDARD_ARRAY,
                        array,
                        List.of(STEALTH, ResourceLocation.parse("checks:sailing"))));
    }

    @Test
    void requiredSkillsNeverExceedTheOfferedSkills() {
        CreationOffer greedy = Offers.withMethodsAndChoices(Offers.unrolled().methods(), 5);
        assertEquals(3, SkillPlan.of(greedy, PresetChoices.CUSTOM).required());
    }

    private static Optional<Rejection> validate(
            CreationMethod method, List<Integer> scores, List<ResourceLocation> skills) {
        return CreationRules.validate(Offers.custom(method, scores, skills), Offers.unrolled());
    }

    private static void assertAccepted(CreationMethod method, List<Integer> scores, CreationOffer offer) {
        assertEquals(Optional.empty(), CreationRules.validate(Offers.custom(method, scores, TWO_SKILLS), offer));
    }

    private static void assertRejected(
            Rejection expected, CreationMethod method, List<Integer> scores, CreationOffer offer) {
        assertEquals(Optional.of(expected), CreationRules.validate(Offers.custom(method, scores, TWO_SKILLS), offer));
    }

    @Test
    void presetsMustBeOffered() {
        ResourceLocation unknown = ResourceLocation.parse("test:merfolk");
        assertEquals(
                Optional.of(Rejection.UNKNOWN_SPECIES),
                validatePresets(
                        choices(Optional.of(unknown), Optional.empty(), Optional.empty()), List.of(), NO_BONUSES));
        assertEquals(
                Optional.of(Rejection.UNKNOWN_BACKGROUND),
                validatePresets(
                        choices(Optional.empty(), Optional.of(unknown), Optional.empty()), List.of(), NO_BONUSES));
        assertEquals(
                Optional.of(Rejection.UNKNOWN_CLASS),
                validatePresets(
                        choices(Optional.empty(), Optional.empty(), Optional.of(unknown)), List.of(), NO_BONUSES));
    }

    @Test
    void aChosenSizeMustBeOneTheSpeciesOffers() {
        assertEquals(Optional.empty(), validatePresets(sized(Offers.HUMAN, Size.SMALL), TWO_SKILLS, NO_BONUSES));
        assertEquals(Optional.empty(), validatePresets(sized(Offers.HUMAN, Size.MEDIUM), TWO_SKILLS, NO_BONUSES));
        assertEquals(
                Optional.of(Rejection.SIZE_NOT_OFFERED),
                validatePresets(sized(Offers.ELF, Size.SMALL), TWO_SKILLS, NO_BONUSES));
        assertEquals(
                Optional.of(Rejection.SIZE_NOT_OFFERED),
                validatePresets(sized(Offers.HOMUNCULUS, Size.MEDIUM), TWO_SKILLS, NO_BONUSES));
        assertEquals(
                Optional.of(Rejection.SIZE_NOT_OFFERED),
                validatePresets(
                        new PresetChoices(
                                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Size.SMALL)),
                        TWO_SKILLS,
                        NO_BONUSES));
    }

    @Test
    void noChosenSizeMeansTheDefault() {
        assertEquals(
                Optional.empty(),
                validatePresets(
                        choices(Optional.of(Offers.HUMAN), Optional.empty(), Optional.empty()),
                        TWO_SKILLS,
                        NO_BONUSES));
    }

    private static PresetChoices sized(ResourceLocation species, Size size) {
        return new PresetChoices(Optional.of(species), Optional.empty(), Optional.empty(), Optional.of(size));
    }

    @Test
    void presetsAreRefusedWhileTheOfferHasNone() {
        assertEquals(
                Optional.of(Rejection.UNKNOWN_SPECIES),
                CreationRules.validate(
                        new CreationSubmission(
                                CreationMethod.STANDARD_ARRAY,
                                ARRAY,
                                NO_BONUSES,
                                TWO_SKILLS,
                                choices(Optional.of(Offers.ELF), Optional.empty(), Optional.empty()),
                                List.of()),
                        Offers.unrolled()));
    }

    @Test
    void aSoldierFighterWithEitherBonusSplitIsAccepted() {
        assertEquals(Optional.empty(), soldierFighter(List.of(2, 0, 1, 0, 0, 0), FIGHTER_PICKS));
        assertEquals(Optional.empty(), soldierFighter(List.of(1, 1, 1, 0, 0, 0), FIGHTER_PICKS));
    }

    @Test
    void bonusesOnlyGoToTheBackgroundsAbilities() {
        assertEquals(Optional.of(Rejection.BONUS_NOT_LISTED), soldierFighter(List.of(2, 0, 0, 0, 0, 1), FIGHTER_PICKS));
        assertEquals(
                Optional.of(Rejection.BONUS_NOT_LISTED),
                CreationRules.validate(
                        new CreationSubmission(
                                CreationMethod.STANDARD_ARRAY,
                                ARRAY,
                                List.of(2, 1, 0, 0, 0, 0),
                                TWO_SKILLS,
                                PresetChoices.CUSTOM,
                                List.of()),
                        Offers.withPresets()),
                "a Custom background takes no bonuses");
    }

    @Test
    void bonusesMustMatchAnAllowedSplit() {
        assertEquals(Optional.of(Rejection.BONUS_PATTERN), soldierFighter(List.of(2, 2, 0, 0, 0, 0), FIGHTER_PICKS));
        assertEquals(Optional.of(Rejection.BONUS_PATTERN), soldierFighter(List.of(3, 0, 0, 0, 0, 0), FIGHTER_PICKS));
        assertEquals(Optional.of(Rejection.BONUS_PATTERN), soldierFighter(NO_BONUSES, FIGHTER_PICKS));
        assertEquals(Optional.of(Rejection.BONUS_PATTERN), soldierFighter(List.of(2, 1), FIGHTER_PICKS));
    }

    @Test
    void bonusesCannotRaiseAScorePastTheCap() {
        CreationOffer rolled = withPresetsRolled(List.of(19, 9, 12, 12, 14, 6));
        CreationSubmission submission = new CreationSubmission(
                CreationMethod.ROLL,
                List.of(19, 9, 12, 12, 14, 6),
                List.of(2, 0, 1, 0, 0, 0),
                FIGHTER_PICKS,
                choices(Optional.empty(), Optional.of(Offers.SOLDIER), Optional.of(Offers.FIGHTER)),
                List.of());
        assertEquals(Optional.of(Rejection.BONUS_OVER_MAX), CreationRules.validate(submission, rolled));
    }

    @Test
    void skillsOutsideTheClassListAreRejectedWithoutAnOverlap() {
        // Soldier skills are not on the Wizard list, so every Wizard pick must come from it.
        CreationSubmission submission = new CreationSubmission(
                CreationMethod.STANDARD_ARRAY,
                ARRAY,
                List.of(2, 0, 1, 0, 0, 0),
                List.of(STEALTH, Offers.ARCANA),
                choices(Optional.empty(), Optional.of(Offers.SOLDIER), Optional.of(Offers.WIZARD)),
                List.of());
        assertEquals(
                Optional.of(Rejection.SKILL_NOT_ON_CLASS_LIST),
                CreationRules.validate(submission, Offers.withPresets()));
    }

    @Test
    void classSkillPicksMustBeTheRequiredCount() {
        assertEquals(Optional.of(Rejection.SKILL_COUNT), soldierFighter(List.of(2, 0, 1, 0, 0, 0), List.of(STEALTH)));
        assertEquals(
                Optional.of(Rejection.SKILL_COUNT),
                soldierFighter(List.of(2, 0, 1, 0, 0, 0), List.of(STEALTH, PERCEPTION, Offers.ARCANA)));
    }

    private static final List<Integer> ARRAY = List.of(15, 14, 13, 12, 10, 8);
    private static final List<Integer> NO_BONUSES = Offers.NO_BONUSES;
    private static final List<ResourceLocation> FIGHTER_PICKS = List.of(STEALTH, PERCEPTION);

    private static Optional<Rejection> soldierFighter(List<Integer> bonuses, List<ResourceLocation> skills) {
        return CreationRules.validate(
                new CreationSubmission(
                        CreationMethod.STANDARD_ARRAY,
                        ARRAY,
                        bonuses,
                        skills,
                        choices(Optional.empty(), Optional.of(Offers.SOLDIER), Optional.of(Offers.FIGHTER)),
                        List.of()),
                Offers.withPresets());
    }

    private static Optional<Rejection> validatePresets(
            PresetChoices choices, List<ResourceLocation> skills, List<Integer> bonuses) {
        return CreationRules.validate(
                new CreationSubmission(CreationMethod.STANDARD_ARRAY, ARRAY, bonuses, skills, choices, List.of()),
                Offers.withPresets());
    }

    private static PresetChoices choices(
            Optional<ResourceLocation> species,
            Optional<ResourceLocation> background,
            Optional<ResourceLocation> classPreset) {
        return new PresetChoices(species, background, classPreset, Optional.empty());
    }

    private static CreationOffer withPresetsRolled(List<Integer> totals) {
        CreationOffer presets = Offers.withPresets();
        CreationOffer rolled = Offers.rolled(CreationMethod.ROLL, totals);
        return new CreationOffer(
                presets.methods(),
                presets.standardArray(),
                presets.pointBuy(),
                presets.skillChoices(),
                presets.skills(),
                rolled.rolls(),
                presets.presets(),
                Map.of());
    }
}
