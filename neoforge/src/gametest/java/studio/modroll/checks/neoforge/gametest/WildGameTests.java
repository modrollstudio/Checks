package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.WildScenarios;

/** NeoForge registration shim: delegates to the shared {@link WildScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class WildGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void aMadeAnimalHandlingCheckTamesOnThatAttempt(GameTestHelper helper) {
        WildScenarios.aMadeAnimalHandlingCheckTamesOnThatAttempt(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aHorseDecidesOnAnimalHandlingOrItsTemper(GameTestHelper helper) {
        WildScenarios.aHorseDecidesOnAnimalHandlingOrItsTemper(helper);
    }

    @GameTest(template = TEMPLATE)
    public void tamingRollsNothingWhileOffOrInCreative(GameTestHelper helper) {
        WildScenarios.tamingRollsNothingWhileOffOrInCreative(helper);
    }

    @GameTest(template = TEMPLATE)
    public void survivalSlowsHunger(GameTestHelper helper) {
        WildScenarios.survivalSlowsHunger(helper);
    }

    @GameTest(template = TEMPLATE)
    public void medicineShortensAilments(GameTestHelper helper) {
        WildScenarios.medicineShortensAilments(helper);
    }
}
