package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.RestScenarios;

/** Fabric registration shim: delegates to the shared {@link RestScenarios} bodies. */
public class RestGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aShortRestSpendsHitDiceUntilFullHealth(GameTestHelper helper) {
        RestScenarios.aShortRestSpendsHitDiceUntilFullHealth(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aShortRestWithoutHitDiceHealsNothing(GameTestHelper helper) {
        RestScenarios.aShortRestWithoutHitDiceHealsNothing(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aHitDieHealsAtLeastOne(GameTestHelper helper) {
        RestScenarios.aHitDieHealsAtLeastOne(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aShortRestBreaks(GameTestHelper helper) {
        RestScenarios.aShortRestBreaks(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aLongRestHealsAndRegainsHitDiceAndTraits(GameTestHelper helper) {
        RestScenarios.aLongRestHealsAndRegainsHitDiceAndTraits(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void restsSwitchOff(GameTestHelper helper) {
        RestScenarios.restsSwitchOff(helper);
    }
}
