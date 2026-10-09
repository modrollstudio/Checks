package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.ProficiencyScenarios;

/** NeoForge registration shim: delegates to the shared {@link ProficiencyScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class ProficiencyGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void skillModifierForEachProficiencyLevel(GameTestHelper helper) {
        ProficiencyScenarios.skillModifierForEachProficiencyLevel(helper);
    }

    @GameTest(template = TEMPLATE)
    public void saveAddsProficiencyOnlyWhenProficient(GameTestHelper helper) {
        ProficiencyScenarios.saveAddsProficiencyOnlyWhenProficient(helper);
    }

    @GameTest(template = TEMPLATE)
    public void proficiencyResolutionOrder(GameTestHelper helper) {
        ProficiencyScenarios.proficiencyResolutionOrder(helper);
    }

    @GameTest(template = TEMPLATE)
    public void invalidProficiencyCommandsAreRejected(GameTestHelper helper) {
        ProficiencyScenarios.invalidProficiencyCommandsAreRejected(helper);
    }

    @GameTest(template = TEMPLATE)
    public void proficiencySurvivesRelogAndSave(GameTestHelper helper) {
        ProficiencyScenarios.proficiencySurvivesRelogAndSave(helper);
    }

    @GameTest(template = TEMPLATE)
    public void proficiencyDisabledRestoresPreviousRules(GameTestHelper helper) {
        ProficiencyScenarios.proficiencyDisabledRestoresPreviousRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public void getShowsProficiencyMarkersAndSaveModifiers(GameTestHelper helper) {
        ProficiencyScenarios.getShowsProficiencyMarkersAndSaveModifiers(helper);
    }
}
