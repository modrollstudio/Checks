package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.PresetScenarios;

/** NeoForge registration shim: delegates to the shared {@link PresetScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class PresetGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void shippedPresetsLoad(GameTestHelper helper) {
        PresetScenarios.shippedPresetsLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public void packAddsAndReplacesEntries(GameTestHelper helper) {
        PresetScenarios.packAddsAndReplacesEntries(helper);
    }

    @GameTest(template = TEMPLATE)
    public void serverRejectsInvalidPresetSubmissions(GameTestHelper helper) {
        PresetScenarios.serverRejectsInvalidPresetSubmissions(helper);
    }

    @GameTest(template = TEMPLATE)
    public void overlapLetsThePlayerPickAnySkill(GameTestHelper helper) {
        PresetScenarios.overlapLetsThePlayerPickAnySkill(helper);
    }

    @GameTest(template = TEMPLATE)
    public void backgroundBonusesAndClassSavesApply(GameTestHelper helper) {
        PresetScenarios.backgroundBonusesAndClassSavesApply(helper);
    }

    @GameTest(template = TEMPLATE)
    public void startingKitIsGivenOnce(GameTestHelper helper) {
        PresetScenarios.startingKitIsGivenOnce(helper);
    }

    @GameTest(template = TEMPLATE)
    public void presetsPersistInTheWorldSave(GameTestHelper helper) {
        PresetScenarios.presetsPersistInTheWorldSave(helper);
    }

    @GameTest(template = TEMPLATE)
    public void resetClearsPresets(GameTestHelper helper) {
        PresetScenarios.resetClearsPresets(helper);
    }

    @GameTest(template = TEMPLATE)
    public void presetsToggleOffRestoresTheOldBehaviour(GameTestHelper helper) {
        PresetScenarios.presetsToggleOffRestoresTheOldBehaviour(helper);
    }
}
