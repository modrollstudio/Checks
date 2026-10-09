package studio.modroll.checks.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static studio.modroll.checks.creation.Offers.ATHLETICS;
import static studio.modroll.checks.creation.Offers.PERCEPTION;
import static studio.modroll.checks.creation.Offers.STEALTH;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.body.Size;
import studio.modroll.checks.body.SizeOption;
import studio.modroll.checks.preset.PresetKind;

class CreationDraftTest {

    private static final List<Integer> ROLLS = List.of(17, 9, 12, 12, 14, 6);

    @Test
    void arrayStartsWithEveryValueInThePoolAndNothingPlaced() {
        CreationDraft draft = new CreationDraft(Offers.unrolled());
        assertEquals(CreationMethod.STANDARD_ARRAY, draft.method());
        assertEquals(List.of(15, 14, 13, 12, 10, 8), draft.pool());
        assertEquals(OptionalInt.empty(), draft.score(Ability.STRENGTH));
        assertEquals(Optional.of(Rejection.SCORE_COUNT), draft.problem());
    }

    @Test
    void aSelectedChipIsPlacedOnTheClickedAbility() {
        CreationDraft draft = new CreationDraft(Offers.unrolled());
        draft.selectChip(0);
        assertTrue(draft.clickAbility(Ability.CONSTITUTION));

        assertEquals(OptionalInt.of(15), draft.score(Ability.CONSTITUTION));
        assertFalse(draft.inPool(0));
        assertEquals(OptionalInt.empty(), draft.selectedChip());
    }

    @Test
    void clickingAPlacedValueReturnsItToThePool() {
        CreationDraft draft = new CreationDraft(Offers.unrolled());
        draft.selectChip(0);
        draft.clickAbility(Ability.STRENGTH);

        assertTrue(draft.clickAbility(Ability.STRENGTH));

        assertEquals(OptionalInt.empty(), draft.score(Ability.STRENGTH));
        assertTrue(draft.inPool(0));
        assertFalse(draft.clickAbility(Ability.STRENGTH));
    }

    @Test
    void placingOverAValueReturnsTheOldOneToThePool() {
        CreationDraft draft = new CreationDraft(Offers.unrolled());
        draft.selectChip(0);
        draft.clickAbility(Ability.STRENGTH);
        draft.selectChip(5);
        draft.clickAbility(Ability.STRENGTH);

        assertEquals(OptionalInt.of(8), draft.score(Ability.STRENGTH));
        assertTrue(draft.inPool(0));
    }

    @Test
    void placedChipsCannotBeSelectedAgain() {
        CreationDraft draft = new CreationDraft(Offers.unrolled());
        draft.selectChip(0);
        draft.clickAbility(Ability.STRENGTH);
        draft.selectChip(0);
        assertEquals(OptionalInt.empty(), draft.selectedChip());
    }

    @Test
    void aFullyPlacedArrayWithSkillsIsConfirmable() {
        CreationDraft draft = new CreationDraft(Offers.unrolled());
        for (Ability ability : Ability.values()) {
            draft.selectChip(Ability.values().length - 1 - ability.ordinal());
            draft.clickAbility(ability);
        }
        draft.toggleSkill(ATHLETICS);
        draft.toggleSkill(STEALTH);

        assertEquals(List.of(8, 10, 12, 13, 14, 15), draft.submission().scores());
        assertEquals(Optional.empty(), draft.problem());
    }

    @Test
    void pointBuyStopsAtTheBudgetAndTheTableEdges() {
        CreationDraft draft = new CreationDraft(Offers.unrolled());
        draft.select(CreationMethod.POINT_BUY);
        assertFalse(draft.canLower(Ability.STRENGTH));
        for (Ability ability : List.of(Ability.STRENGTH, Ability.DEXTERITY, Ability.CONSTITUTION)) {
            for (int i = 0; i < 10; i++) {
                draft.raise(ability);
            }
        }
        assertEquals(List.of(15, 15, 15, 8, 8, 8), draft.submission().scores());
        assertEquals(0, draft.pointsLeft());
        assertFalse(draft.canRaise(Ability.WISDOM));
        assertTrue(draft.canLower(Ability.STRENGTH));
    }

    @Test
    void hardcoreScoresArePlacedInOrderAndCannotMove() {
        CreationDraft draft = new CreationDraft(Offers.rolled(CreationMethod.HARDCORE, ROLLS));
        assertEquals(CreationMethod.HARDCORE, draft.method());
        assertFalse(draft.usesPool());
        assertFalse(draft.clickAbility(Ability.STRENGTH));
        assertEquals(ROLLS, draft.submission().scores());
        assertEquals(OptionalInt.of(1), draft.chip(Ability.DEXTERITY));
    }

    @Test
    void rollingFillsThePoolAndLocksTheOtherMethods() {
        CreationDraft draft = new CreationDraft(Offers.unrolled());
        draft.select(CreationMethod.ROLL);
        assertTrue(draft.awaitingRoll());
        assertEquals(Optional.of(Rejection.NOT_ROLLED), draft.problem());

        draft.update(Offers.rolled(CreationMethod.ROLL, ROLLS));
        draft.select(CreationMethod.STANDARD_ARRAY);

        assertEquals(CreationMethod.ROLL, draft.method());
        assertEquals(ROLLS, draft.pool());
        assertTrue(draft.usesPool());
    }

    @Test
    void skillPicksStopAtTheRequiredCount() {
        CreationDraft draft = new CreationDraft(Offers.unrolled());
        draft.select(CreationMethod.POINT_BUY);
        draft.toggleSkill(ATHLETICS);
        draft.toggleSkill(STEALTH);
        draft.toggleSkill(PERCEPTION);
        assertFalse(draft.picked(PERCEPTION));
        assertEquals(Optional.empty(), draft.problem());

        draft.toggleSkill(STEALTH);
        assertEquals(Optional.of(Rejection.SKILL_COUNT), draft.problem());
    }

    @Test
    void withoutPresetsTheStepsAreScoresSkillsAndConfirm() {
        CreationDraft draft = new CreationDraft(Offers.unrolled());
        assertEquals(List.of(CreationStep.SCORES, CreationStep.SKILLS, CreationStep.CONFIRM), draft.steps());
        assertEquals(CreationStep.SCORES, draft.step());
    }

    @Test
    void presetStepsWaitForAChoiceAndCustomCounts() {
        CreationDraft draft = new CreationDraft(Offers.withPresets());
        assertEquals(CreationStep.SPECIES, draft.step());
        assertTrue(draft.stepProblem().isPresent());
        draft.next();
        assertEquals(CreationStep.SPECIES, draft.step());

        draft.choose(PresetKind.SPECIES, Optional.empty());
        draft.next();
        assertEquals(CreationStep.BACKGROUND, draft.step());
        draft.back();
        assertEquals(CreationStep.SPECIES, draft.step());
        assertFalse(draft.choices().species().isPresent());
    }

    @Test
    void aSpeciesStartsAtItsDefaultSizeAndOnlyOfferedSizesCanBeChosen() {
        CreationDraft draft = new CreationDraft(Offers.withPresets());
        draft.choose(PresetKind.SPECIES, Optional.of(Offers.HUMAN));
        assertEquals(Optional.of(new SizeOption(Size.MEDIUM, 1.0)), draft.size());
        assertEquals(Optional.empty(), draft.choices().size());

        draft.chooseSize(Size.SMALL);
        assertEquals(Optional.of(new SizeOption(Size.SMALL, 0.6)), draft.size());
        assertEquals(Optional.of(Size.SMALL), draft.choices().size());

        draft.choose(PresetKind.SPECIES, Optional.of(Offers.ELF));
        assertEquals(Optional.of(new SizeOption(Size.MEDIUM, 0.95)), draft.size());
        draft.chooseSize(Size.SMALL);
        assertEquals(Optional.empty(), draft.choices().size());
    }

    @Test
    void customSpeciesHasNoSize() {
        CreationDraft draft = new CreationDraft(Offers.withPresets());
        draft.choose(PresetKind.SPECIES, Optional.empty());
        draft.chooseSize(Size.SMALL);
        assertEquals(Optional.empty(), draft.size());
        assertEquals(Optional.empty(), draft.choices().size());
    }

    @Test
    void presetsTheOfferDoesNotListCannotBeChosen() {
        CreationDraft draft = new CreationDraft(Offers.withPresets());
        draft.choose(PresetKind.CLASS, Optional.of(ResourceLocation.parse("test:merfolk")));
        assertFalse(draft.isChosen(PresetKind.CLASS));
    }

    @Test
    void useSuggestedPlacesTheHighestValuesInTheClassOrder() {
        CreationDraft draft = new CreationDraft(Offers.withPresets());
        assertFalse(draft.canUseSuggested());
        draft.choose(PresetKind.CLASS, Optional.of(Offers.WIZARD));

        draft.useSuggested();

        // Wizard: INT, CON, DEX, WIS, CHA, STR.
        assertEquals(List.of(8, 13, 14, 15, 12, 10), draft.submission().scores());
    }

    @Test
    void bonusesGoOnlyOnTheBackgroundsAbilitiesUpToTheLargestSplit() {
        CreationDraft draft = new CreationDraft(Offers.withPresets());
        draft.choose(PresetKind.BACKGROUND, Optional.of(Offers.SOLDIER));
        assertFalse(draft.canRaiseBonus(Ability.WISDOM));
        draft.raiseBonus(Ability.STRENGTH);
        draft.raiseBonus(Ability.STRENGTH);
        draft.raiseBonus(Ability.STRENGTH);
        draft.raiseBonus(Ability.CONSTITUTION);
        draft.choose(PresetKind.CLASS, Optional.of(Offers.FIGHTER));
        draft.useSuggested();

        assertEquals(List.of(2, 0, 1, 0, 0, 0), draft.submission().bonuses());
        assertEquals(OptionalInt.of(17), draft.finalScore(Ability.STRENGTH));
        assertEquals(OptionalInt.of(15), draft.finalScore(Ability.CONSTITUTION));
        assertEquals(OptionalInt.of(13), draft.finalScore(Ability.DEXTERITY));
    }

    @Test
    void anotherBackgroundClearsItsBonusesAndThePicks() {
        CreationDraft draft = new CreationDraft(Offers.withPresets());
        draft.choose(PresetKind.BACKGROUND, Optional.of(Offers.SOLDIER));
        draft.choose(PresetKind.CLASS, Optional.of(Offers.FIGHTER));
        draft.raiseBonus(Ability.STRENGTH);
        draft.toggleSkill(STEALTH);

        draft.choose(PresetKind.BACKGROUND, Optional.of(Offers.SAGE));

        assertEquals(0, draft.bonus(Ability.STRENGTH));
        assertEquals(0, draft.pickedCount());
    }

    @Test
    void speciesPicksAreKeptApartAndAnotherSpeciesClearsThem() {
        CreationDraft draft = new CreationDraft(Offers.withSpeciesSkillChoices());
        draft.choose(PresetKind.SPECIES, Optional.of(Offers.ELF));
        draft.choose(PresetKind.BACKGROUND, Optional.of(Offers.SOLDIER));
        draft.choose(PresetKind.CLASS, Optional.of(Offers.FIGHTER));
        draft.toggleSpeciesSkill(Offers.PERCEPTION);
        draft.toggleSpeciesSkill(STEALTH);

        assertEquals(List.of(Offers.PERCEPTION), draft.speciesPicks());
        assertFalse(draft.canPick(Offers.PERCEPTION));
        draft.toggleSkill(Offers.PERCEPTION);
        assertFalse(draft.picked(Offers.PERCEPTION));
        assertEquals(3, draft.requiredCount());
        assertEquals(List.of(Offers.PERCEPTION), draft.submission().speciesSkills());

        draft.choose(PresetKind.SPECIES, Optional.of(Offers.HUMAN));
        assertEquals(List.of(), draft.speciesPicks());
    }

    @Test
    void aSoldierFighterDraftIsConfirmable() {
        CreationDraft draft = new CreationDraft(Offers.withPresets());
        draft.choose(PresetKind.SPECIES, Optional.of(Offers.ELF));
        draft.choose(PresetKind.BACKGROUND, Optional.of(Offers.SOLDIER));
        draft.choose(PresetKind.CLASS, Optional.of(Offers.FIGHTER));
        draft.useSuggested();
        draft.raiseBonus(Ability.STRENGTH);
        draft.raiseBonus(Ability.DEXTERITY);
        draft.raiseBonus(Ability.CONSTITUTION);
        draft.toggleSkill(STEALTH);
        draft.toggleSkill(Offers.ARCANA);

        assertEquals(Optional.empty(), draft.problem());
        for (int i = 0; i < draft.steps().size(); i++) {
            draft.next();
        }
        assertTrue(draft.onLastStep());
    }

    @Test
    void everyPlusIsDisabledOnceASplitIsComplete() {
        CreationDraft draft = soldierDraft();
        draft.raiseBonus(Ability.STRENGTH);
        draft.raiseBonus(Ability.STRENGTH);
        assertEquals(List.of(List.of(1)), draft.bonusesLeft());
        assertEquals(Optional.of(CreationDraft.BonusLimit.NO_SPLIT), draft.raiseLimit(Ability.STRENGTH));
        assertTrue(draft.canRaiseBonus(Ability.DEXTERITY));

        draft.raiseBonus(Ability.DEXTERITY);

        assertEquals(List.of(List.of()), draft.bonusesLeft());
        for (Ability ability : List.of(Ability.STRENGTH, Ability.DEXTERITY, Ability.CONSTITUTION)) {
            assertFalse(draft.canRaiseBonus(ability), ability.id());
            assertEquals(Optional.of(CreationDraft.BonusLimit.ALL_USED), draft.raiseLimit(ability));
        }
    }

    @Test
    void threeOnesAlsoCompleteTheSplit() {
        CreationDraft draft = soldierDraft();
        draft.raiseBonus(Ability.STRENGTH);
        draft.raiseBonus(Ability.DEXTERITY);
        assertEquals(List.of(List.of(1)), draft.bonusesLeft());
        assertTrue(draft.canRaiseBonus(Ability.STRENGTH), "STR +2 with DEX +1 is still a +2/+1 split");

        draft.raiseBonus(Ability.CONSTITUTION);

        assertEquals(List.of(List.of()), draft.bonusesLeft());
        assertFalse(draft.canRaiseBonus(Ability.CONSTITUTION));
    }

    @Test
    void aBonusCannotPushAScorePastTheCap() {
        CreationDraft draft = new CreationDraft(Offers.withPresets());
        draft.choose(PresetKind.BACKGROUND, Optional.of(Offers.SOLDIER));
        draft.select(CreationMethod.POINT_BUY);
        for (int i = 0; i < 10; i++) {
            draft.raise(Ability.STRENGTH);
        }
        assertEquals(Optional.empty(), draft.raiseLimit(Ability.STRENGTH), "15 + 1 stays under 20");
        CreationOffer capped = new CreationOffer(
                draft.offer().methods(),
                draft.offer().standardArray(),
                draft.offer().pointBuy(),
                draft.offer().skillChoices(),
                draft.offer().skills(),
                draft.offer().rolls(),
                new PresetOffer(
                        true, List.of(), Offers.PRESETS.backgrounds(), List.of(), Offers.PRESETS.bonusOptions(), 15),
                Map.of());
        draft.update(capped);

        assertEquals(Optional.of(CreationDraft.BonusLimit.OVER_MAX), draft.raiseLimit(Ability.STRENGTH));
    }

    private static CreationDraft soldierDraft() {
        CreationDraft draft = new CreationDraft(Offers.withPresets());
        draft.choose(PresetKind.BACKGROUND, Optional.of(Offers.SOLDIER));
        return draft;
    }
}
