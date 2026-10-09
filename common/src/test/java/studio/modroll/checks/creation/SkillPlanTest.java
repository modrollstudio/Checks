package studio.modroll.checks.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static studio.modroll.checks.creation.Offers.ARCANA;
import static studio.modroll.checks.creation.Offers.ATHLETICS;
import static studio.modroll.checks.creation.Offers.ELF;
import static studio.modroll.checks.creation.Offers.FIGHTER;
import static studio.modroll.checks.creation.Offers.HISTORY;
import static studio.modroll.checks.creation.Offers.HUMAN;
import static studio.modroll.checks.creation.Offers.INTIMIDATION;
import static studio.modroll.checks.creation.Offers.PERCEPTION;
import static studio.modroll.checks.creation.Offers.SAGE;
import static studio.modroll.checks.creation.Offers.SOLDIER;
import static studio.modroll.checks.creation.Offers.STEALTH;
import static studio.modroll.checks.creation.Offers.WIZARD;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class SkillPlanTest {

    @Test
    void customBackgroundAndClassGiveOnlyTheFreePicks() {
        SkillPlan plan = plan(Optional.empty(), Optional.empty());
        assertEquals(Set.of(), plan.fixed());
        assertEquals(2, plan.required());
        assertEquals(Optional.empty(), plan.validate(List.of(STEALTH, ARCANA)));
    }

    @Test
    void backgroundSkillsAreFixedAndCannotBePickedAgain() {
        SkillPlan plan = plan(Optional.of(SAGE), Optional.empty());
        assertEquals(Set.of(ARCANA, HISTORY), plan.fixed());
        assertEquals(0, plan.required());
        assertEquals(Optional.of(Rejection.SKILL_ALREADY_PROFICIENT), plan.validate(List.of(ARCANA)));
    }

    @Test
    void classPicksComeFromTheClassList() {
        // Sage + Fighter overlap only on History, so one pick may leave the list.
        SkillPlan plan = plan(Optional.of(SAGE), Optional.of(FIGHTER));
        assertEquals(2, plan.required());
        assertEquals(1, plan.offListAllowance());
        assertEquals(Optional.empty(), plan.validate(List.of(ATHLETICS, PERCEPTION)));
        assertEquals(Optional.empty(), plan.validate(List.of(ATHLETICS, STEALTH)));
        assertEquals(
                Optional.of(Rejection.SKILL_NOT_ON_CLASS_LIST),
                plan(Optional.of(SOLDIER), Optional.of(WIZARD)).validate(List.of(STEALTH, ARCANA)),
                "Soldier and Wizard do not overlap, so no pick may leave the list");
    }

    @Test
    void eachOverlappingSkillFreesOnePickForAnySkill() {
        // Soldier gives Athletics and Intimidation, both on the Fighter list: both picks may be anything.
        SkillPlan plan = plan(Optional.of(SOLDIER), Optional.of(FIGHTER));
        assertEquals(Set.of(ATHLETICS, INTIMIDATION), plan.overlap());
        assertEquals(2, plan.overlapPicks());
        assertEquals(2, plan.offListAllowance());
        assertEquals(Optional.empty(), plan.validate(List.of(STEALTH, ARCANA)));
        assertEquals(Optional.of(Rejection.SKILL_ALREADY_PROFICIENT), plan.validate(List.of(ATHLETICS, STEALTH)));
    }

    @Test
    void canPickStopsAtTheCountAndTheOffListAllowance() {
        SkillPlan plan = plan(Optional.of(SAGE), Optional.of(FIGHTER));
        assertTrue(plan.canPick(STEALTH, List.of()));
        assertFalse(plan.canPick(ARCANA, List.of(STEALTH)), "arcana is off the list and the one off-list pick is used");
        assertTrue(plan.canPick(PERCEPTION, List.of(STEALTH)));
        assertFalse(plan.canPick(INTIMIDATION, List.of(STEALTH, PERCEPTION)), "both picks are used");
        assertFalse(plan.canPick(HISTORY, List.of()), "history comes from the background");
    }

    @Test
    void wrongCountsAreRejected() {
        SkillPlan plan = plan(Optional.of(SAGE), Optional.of(FIGHTER));
        assertEquals(Optional.of(Rejection.SKILL_COUNT), plan.validate(List.of(ATHLETICS)));
        assertEquals(Optional.of(Rejection.SKILL_COUNT), plan.validate(List.of(ATHLETICS, PERCEPTION, INTIMIDATION)));
    }

    @Test
    void customBackgroundWithAClassAddsFreePicksToTheClassPicks() {
        SkillPlan plan = plan(Optional.empty(), Optional.of(FIGHTER));
        assertEquals(2, plan.classPicks());
        assertEquals(2, plan.freePicks());
        assertEquals(4, plan.required());
        assertEquals(2, plan.offListAllowance());
        assertEquals(Optional.empty(), plan.validate(List.of(ATHLETICS, PERCEPTION, STEALTH, ARCANA)));
    }

    @Test
    void speciesPicksComeFromTheSpeciesListApartFromTheOtherPicks() {
        SkillPlan plan = SkillPlan.of(
                Offers.withSpeciesSkillChoices(),
                new PresetChoices(Optional.of(ELF), Optional.of(SOLDIER), Optional.of(FIGHTER), Optional.empty()));
        assertEquals(Set.of(PERCEPTION, STEALTH), plan.speciesOptions());
        assertEquals(1, plan.speciesRequired());
        List<ResourceLocation> picks = List.of(PERCEPTION, ARCANA);
        assertEquals(Optional.empty(), plan.validateSpecies(List.of(STEALTH), picks));
        assertEquals(Optional.of(Rejection.DUPLICATE_SKILL), plan.validateSpecies(List.of(PERCEPTION), picks));
        assertEquals(Optional.of(Rejection.SKILL_NOT_FROM_SPECIES), plan.validateSpecies(List.of(HISTORY), picks));
        assertEquals(Optional.of(Rejection.SPECIES_SKILL_COUNT), plan.validateSpecies(List.of(), picks));
        assertFalse(plan.canPickForSpecies(PERCEPTION, List.of(), picks));
        assertTrue(plan.canPickForSpecies(STEALTH, List.of(), picks));
    }

    @Test
    void anOpenSpeciesChoiceTakesAnySkillButTheBackgrounds() {
        SkillPlan plan = SkillPlan.of(
                Offers.withSpeciesSkillChoices(),
                new PresetChoices(Optional.of(HUMAN), Optional.of(SOLDIER), Optional.empty(), Optional.empty()));
        assertEquals(2, plan.speciesRequired());
        assertFalse(plan.speciesOptions().contains(ATHLETICS));
        assertEquals(Optional.empty(), plan.validateSpecies(List.of(STEALTH, ARCANA), List.of()));
        assertEquals(
                Optional.of(Rejection.SKILL_ALREADY_PROFICIENT),
                plan.validateSpecies(List.of(ATHLETICS, ARCANA), List.of()));
    }

    @Test
    void aSpeciesWithoutSkillChoicesPicksNothing() {
        SkillPlan plan = SkillPlan.of(
                Offers.withPresets(),
                new PresetChoices(Optional.of(ELF), Optional.empty(), Optional.empty(), Optional.empty()));
        assertEquals(0, plan.speciesRequired());
        assertEquals(Optional.empty(), plan.validateSpecies(List.of(), List.of()));
    }

    @Test
    void pooledChoicesAddUpAndAnOpenOneOpensThemAll() {
        assertEquals(
                new SkillChoice(2, Set.of(PERCEPTION, STEALTH)),
                SkillChoice.pooled(
                        List.of(new SkillChoice(1, Set.of(PERCEPTION)), new SkillChoice(1, Set.of(STEALTH)))));
        assertEquals(
                new SkillChoice(2, Set.of()),
                SkillChoice.pooled(List.of(new SkillChoice(1, Set.of(PERCEPTION)), new SkillChoice(1, Set.of()))));
        assertEquals(SkillChoice.NONE, SkillChoice.pooled(List.of()));
    }

    private static SkillPlan plan(Optional<ResourceLocation> background, Optional<ResourceLocation> classPreset) {
        return SkillPlan.of(
                Offers.withPresets(), new PresetChoices(Optional.empty(), background, classPreset, Optional.empty()));
    }
}
