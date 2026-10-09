package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.TriggerScenarios;

/** NeoForge registration shim: delegates to the shared {@link TriggerScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class TriggerGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void blockTriggerTakesEachBranch(GameTestHelper helper) {
        TriggerScenarios.blockTriggerTakesEachBranch(helper);
    }

    @GameTest(template = TEMPLATE)
    public void entityTriggerFires(GameTestHelper helper) {
        TriggerScenarios.entityTriggerFires(helper);
    }

    @GameTest(template = TEMPLATE)
    public void itemTriggerFires(GameTestHelper helper) {
        TriggerScenarios.itemTriggerFires(helper);
    }

    @GameTest(template = TEMPLATE)
    public void cooldownsBlockPlayersAndTargets(GameTestHelper helper) {
        TriggerScenarios.cooldownsBlockPlayersAndTargets(helper);
    }

    @GameTest(template = TEMPLATE)
    public void disabledOrCanceledTriggersDoNothing(GameTestHelper helper) {
        TriggerScenarios.disabledOrCanceledTriggersDoNothing(helper);
    }
}
