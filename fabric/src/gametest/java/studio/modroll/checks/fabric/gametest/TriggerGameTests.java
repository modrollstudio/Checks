package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.TriggerScenarios;

/** Fabric registration shim: delegates to the shared {@link TriggerScenarios} bodies. */
public class TriggerGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockTriggerTakesEachBranch(GameTestHelper helper) {
        TriggerScenarios.blockTriggerTakesEachBranch(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void entityTriggerFires(GameTestHelper helper) {
        TriggerScenarios.entityTriggerFires(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void itemTriggerFires(GameTestHelper helper) {
        TriggerScenarios.itemTriggerFires(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void cooldownsBlockPlayersAndTargets(GameTestHelper helper) {
        TriggerScenarios.cooldownsBlockPlayersAndTargets(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void disabledOrCanceledTriggersDoNothing(GameTestHelper helper) {
        TriggerScenarios.disabledOrCanceledTriggersDoNothing(helper);
    }
}
