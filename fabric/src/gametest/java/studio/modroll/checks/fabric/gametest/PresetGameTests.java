package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.PresetScenarios;

/** Fabric registration shim: delegates to the shared {@link PresetScenarios} bodies. */
public class PresetGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void shippedPresetsLoad(GameTestHelper helper) {
        PresetScenarios.shippedPresetsLoad(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void packAddsAndReplacesEntries(GameTestHelper helper) {
        PresetScenarios.packAddsAndReplacesEntries(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void serverRejectsInvalidPresetSubmissions(GameTestHelper helper) {
        PresetScenarios.serverRejectsInvalidPresetSubmissions(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void overlapLetsThePlayerPickAnySkill(GameTestHelper helper) {
        PresetScenarios.overlapLetsThePlayerPickAnySkill(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void backgroundBonusesAndClassSavesApply(GameTestHelper helper) {
        PresetScenarios.backgroundBonusesAndClassSavesApply(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void startingKitIsGivenOnce(GameTestHelper helper) {
        PresetScenarios.startingKitIsGivenOnce(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void presetsPersistInTheWorldSave(GameTestHelper helper) {
        PresetScenarios.presetsPersistInTheWorldSave(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void resetClearsPresets(GameTestHelper helper) {
        PresetScenarios.resetClearsPresets(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void presetsToggleOffRestoresTheOldBehaviour(GameTestHelper helper) {
        PresetScenarios.presetsToggleOffRestoresTheOldBehaviour(helper);
    }
}
