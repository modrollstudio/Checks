package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.SaveScenarios;

/** Fabric registration shim: delegates to the shared {@link SaveScenarios} bodies. */
public class SaveGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void explosionCallsForDexAndStrSaves(GameTestHelper helper) {
        SaveScenarios.explosionCallsForDexAndStrSaves(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void poisonSaveHalvesTheDuration(GameTestHelper helper) {
        SaveScenarios.poisonSaveHalvesTheDuration(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void commandEffectsNeverCallForASave(GameTestHelper helper) {
        SaveScenarios.commandEffectsNeverCallForASave(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void darknessSaveHalvesTheDuration(GameTestHelper helper) {
        SaveScenarios.darknessSaveHalvesTheDuration(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void knockbackSaveOnlyForBigPushes(GameTestHelper helper) {
        SaveScenarios.knockbackSaveOnlyForBigPushes(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void fireSaveKeepsThePlayerFromCatchingFire(GameTestHelper helper) {
        SaveScenarios.fireSaveKeepsThePlayerFromCatchingFire(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void bonusSourceAdvantageApplies(GameTestHelper helper) {
        SaveScenarios.bonusSourceAdvantageApplies(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void cooldownReusesTheLastResult(GameTestHelper helper) {
        SaveScenarios.cooldownReusesTheLastResult(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void eachSaveSwitchesOff(GameTestHelper helper) {
        SaveScenarios.eachSaveSwitchesOff(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void mobsSaveOnlyWhenProfiledAndSwitchedOn(GameTestHelper helper) {
        SaveScenarios.mobsSaveOnlyWhenProfiledAndSwitchedOn(helper);
    }
}
