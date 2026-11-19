package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.RestScenarios;

public class RestGameTests implements FabricGameTest {

    // A batch of its own: its rest lasts a few ticks, and other rest tests tick every rest on the server.
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rest_short")
    public void aShortRestSpendsHitDiceUntilFullHealth(GameTestHelper helper) {
        RestScenarios.aShortRestSpendsHitDiceUntilFullHealth(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rest_no_dice")
    public void aShortRestWithoutHitDiceHealsNothing(GameTestHelper helper) {
        RestScenarios.aShortRestWithoutHitDiceHealsNothing(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rest_min_heal")
    public void aHitDieHealsAtLeastOne(GameTestHelper helper) {
        RestScenarios.aHitDieHealsAtLeastOne(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rest_hit_dice_off")
    public void aShortRestWithHitDiceOffHealsNothing(GameTestHelper helper) {
        RestScenarios.aShortRestWithHitDiceOffHealsNothing(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "rest_breaks")
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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aRestListenerThatThrowsIsSkipped(GameTestHelper helper) {
        RestScenarios.aRestListenerThatThrowsIsSkipped(helper);
    }
}
