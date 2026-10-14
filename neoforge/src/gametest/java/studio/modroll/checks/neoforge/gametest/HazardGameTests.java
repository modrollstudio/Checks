package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.HazardScenarios;

/** NeoForge registration shim: delegates to the shared {@link HazardScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class HazardGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void dexSaveUsesModifierAndProficiency(GameTestHelper helper) {
        HazardScenarios.dexSaveUsesModifierAndProficiency(helper);
    }

    @GameTest(template = TEMPLATE)
    public void mappedKeyUsesItsAbilityAndUnknownKeyIsUntouched(GameTestHelper helper) {
        HazardScenarios.mappedKeyUsesItsAbilityAndUnknownKeyIsUntouched(helper);
    }

    @GameTest(template = TEMPLATE)
    public void dwarfHasAdvantageOnPoisonHazards(GameTestHelper helper) {
        HazardScenarios.dwarfHasAdvantageOnPoisonHazards(helper);
    }

    @GameTest(template = TEMPLATE)
    public void gnomeHasAdvantageOnMentalHazardSaves(GameTestHelper helper) {
        HazardScenarios.gnomeHasAdvantageOnMentalHazardSaves(helper);
    }

    @GameTest(template = TEMPLATE)
    public void disabledIntegrationGrantsNoAdvantage(GameTestHelper helper) {
        HazardScenarios.disabledIntegrationGrantsNoAdvantage(helper);
    }

    @GameTest(template = TEMPLATE)
    public void hazardDamageCallsForNoSecondRoll(GameTestHelper helper) {
        HazardScenarios.hazardDamageCallsForNoSecondRoll(helper);
    }

    @GameTest(template = TEMPLATE)
    public void halflingRerollsNaturalOneOnHazardSave(GameTestHelper helper) {
        HazardScenarios.halflingRerollsNaturalOneOnHazardSave(helper);
    }

    @GameTest(template = TEMPLATE)
    public void traitAndSourceBonusesAddToHazardSave(GameTestHelper helper) {
        HazardScenarios.traitAndSourceBonusesAddToHazardSave(helper);
    }

    @GameTest(template = TEMPLATE)
    public void sourceModesApplyToHazardSave(GameTestHelper helper) {
        HazardScenarios.sourceModesApplyToHazardSave(helper);
    }
}
