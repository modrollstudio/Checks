package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.ProficiencyScenarios;

/** Fabric registration shim: delegates to the shared {@link ProficiencyScenarios} bodies. */
public class ProficiencyGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void skillModifierForEachProficiencyLevel(GameTestHelper helper) {
        ProficiencyScenarios.skillModifierForEachProficiencyLevel(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void saveAddsProficiencyOnlyWhenProficient(GameTestHelper helper) {
        ProficiencyScenarios.saveAddsProficiencyOnlyWhenProficient(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void proficiencyResolutionOrder(GameTestHelper helper) {
        ProficiencyScenarios.proficiencyResolutionOrder(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void invalidProficiencyCommandsAreRejected(GameTestHelper helper) {
        ProficiencyScenarios.invalidProficiencyCommandsAreRejected(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void proficiencySurvivesRelogAndSave(GameTestHelper helper) {
        ProficiencyScenarios.proficiencySurvivesRelogAndSave(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void proficiencyDisabledRestoresPreviousRules(GameTestHelper helper) {
        ProficiencyScenarios.proficiencyDisabledRestoresPreviousRules(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void getShowsProficiencyMarkersAndSaveModifiers(GameTestHelper helper) {
        ProficiencyScenarios.getShowsProficiencyMarkersAndSaveModifiers(helper);
    }
}
