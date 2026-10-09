package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.EventScenarios;

/** Fabric registration shim: delegates to the shared {@link EventScenarios} bodies. */
public class EventGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void beforeEventChangesDcBonusAndMode(GameTestHelper helper) {
        EventScenarios.beforeEventChangesDcBonusAndMode(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void advantageAndDisadvantageCancel(GameTestHelper helper) {
        EventScenarios.advantageAndDisadvantageCancel(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void openRollReportsTheModeRolled(GameTestHelper helper) {
        EventScenarios.openRollReportsTheModeRolled(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void cancelSkipsTheRoll(GameTestHelper helper) {
        EventScenarios.cancelSkipsTheRoll(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void afterEventSeesTheResult(GameTestHelper helper) {
        EventScenarios.afterEventSeesTheResult(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void contestFiresOneEventPerSide(GameTestHelper helper) {
        EventScenarios.contestFiresOneEventPerSide(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void disabledEventsCallNoListener(GameTestHelper helper) {
        EventScenarios.disabledEventsCallNoListener(helper);
    }
}
