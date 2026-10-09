package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.EventScenarios;

/** NeoForge registration shim: delegates to the shared {@link EventScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class EventGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void beforeEventChangesDcBonusAndMode(GameTestHelper helper) {
        EventScenarios.beforeEventChangesDcBonusAndMode(helper);
    }

    @GameTest(template = TEMPLATE)
    public void advantageAndDisadvantageCancel(GameTestHelper helper) {
        EventScenarios.advantageAndDisadvantageCancel(helper);
    }

    @GameTest(template = TEMPLATE)
    public void openRollReportsTheModeRolled(GameTestHelper helper) {
        EventScenarios.openRollReportsTheModeRolled(helper);
    }

    @GameTest(template = TEMPLATE)
    public void cancelSkipsTheRoll(GameTestHelper helper) {
        EventScenarios.cancelSkipsTheRoll(helper);
    }

    @GameTest(template = TEMPLATE)
    public void afterEventSeesTheResult(GameTestHelper helper) {
        EventScenarios.afterEventSeesTheResult(helper);
    }

    @GameTest(template = TEMPLATE)
    public void contestFiresOneEventPerSide(GameTestHelper helper) {
        EventScenarios.contestFiresOneEventPerSide(helper);
    }

    @GameTest(template = TEMPLATE)
    public void disabledEventsCallNoListener(GameTestHelper helper) {
        EventScenarios.disabledEventsCallNoListener(helper);
    }
}
