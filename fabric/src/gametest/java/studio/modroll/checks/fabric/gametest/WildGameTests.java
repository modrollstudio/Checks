package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.WildScenarios;

/** Fabric registration shim: delegates to the shared {@link WildScenarios} bodies. */
public class WildGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMadeAnimalHandlingCheckTamesOnThatAttempt(GameTestHelper helper) {
        WildScenarios.aMadeAnimalHandlingCheckTamesOnThatAttempt(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aHorseDecidesOnAnimalHandlingOrItsTemper(GameTestHelper helper) {
        WildScenarios.aHorseDecidesOnAnimalHandlingOrItsTemper(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tamingRollsNothingWhileOffOrInCreative(GameTestHelper helper) {
        WildScenarios.tamingRollsNothingWhileOffOrInCreative(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void survivalSlowsHunger(GameTestHelper helper) {
        WildScenarios.survivalSlowsHunger(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void medicineShortensAilments(GameTestHelper helper) {
        WildScenarios.medicineShortensAilments(helper);
    }
}
