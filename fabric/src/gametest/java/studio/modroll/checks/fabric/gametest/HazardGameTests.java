package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.HazardScenarios;

/** Fabric registration shim: delegates to the shared {@link HazardScenarios} bodies. */
public class HazardGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dexSaveUsesModifierAndProficiency(GameTestHelper helper) {
        HazardScenarios.dexSaveUsesModifierAndProficiency(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void mappedKeyUsesItsAbilityAndUnknownKeyIsUntouched(GameTestHelper helper) {
        HazardScenarios.mappedKeyUsesItsAbilityAndUnknownKeyIsUntouched(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dwarfHasAdvantageOnPoisonHazards(GameTestHelper helper) {
        HazardScenarios.dwarfHasAdvantageOnPoisonHazards(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void gnomeHasAdvantageOnMentalHazardSaves(GameTestHelper helper) {
        HazardScenarios.gnomeHasAdvantageOnMentalHazardSaves(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void disabledIntegrationGrantsNoAdvantage(GameTestHelper helper) {
        HazardScenarios.disabledIntegrationGrantsNoAdvantage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void hazardDamageCallsForNoSecondRoll(GameTestHelper helper) {
        HazardScenarios.hazardDamageCallsForNoSecondRoll(helper);
    }
}
