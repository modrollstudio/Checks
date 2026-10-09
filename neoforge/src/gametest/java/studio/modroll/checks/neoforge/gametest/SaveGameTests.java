package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.SaveScenarios;

/** NeoForge registration shim: delegates to the shared {@link SaveScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class SaveGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void explosionCallsForDexAndStrSaves(GameTestHelper helper) {
        SaveScenarios.explosionCallsForDexAndStrSaves(helper);
    }

    @GameTest(template = TEMPLATE)
    public void poisonSaveHalvesTheDuration(GameTestHelper helper) {
        SaveScenarios.poisonSaveHalvesTheDuration(helper);
    }

    @GameTest(template = TEMPLATE)
    public void commandEffectsNeverCallForASave(GameTestHelper helper) {
        SaveScenarios.commandEffectsNeverCallForASave(helper);
    }

    @GameTest(template = TEMPLATE)
    public void darknessSaveHalvesTheDuration(GameTestHelper helper) {
        SaveScenarios.darknessSaveHalvesTheDuration(helper);
    }

    @GameTest(template = TEMPLATE)
    public void knockbackSaveOnlyForBigPushes(GameTestHelper helper) {
        SaveScenarios.knockbackSaveOnlyForBigPushes(helper);
    }

    @GameTest(template = TEMPLATE)
    public void fireSaveKeepsThePlayerFromCatchingFire(GameTestHelper helper) {
        SaveScenarios.fireSaveKeepsThePlayerFromCatchingFire(helper);
    }

    @GameTest(template = TEMPLATE)
    public void bonusSourceAdvantageApplies(GameTestHelper helper) {
        SaveScenarios.bonusSourceAdvantageApplies(helper);
    }

    @GameTest(template = TEMPLATE)
    public void cooldownReusesTheLastResult(GameTestHelper helper) {
        SaveScenarios.cooldownReusesTheLastResult(helper);
    }

    @GameTest(template = TEMPLATE)
    public void eachSaveSwitchesOff(GameTestHelper helper) {
        SaveScenarios.eachSaveSwitchesOff(helper);
    }

    @GameTest(template = TEMPLATE)
    public void mobsSaveOnlyWhenProfiledAndSwitchedOn(GameTestHelper helper) {
        SaveScenarios.mobsSaveOnlyWhenProfiledAndSwitchedOn(helper);
    }
}
