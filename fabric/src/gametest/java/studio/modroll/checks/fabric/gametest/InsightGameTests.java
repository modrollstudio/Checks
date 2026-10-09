package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.InsightScenarios;

/** Fabric registration shim: delegates to the shared {@link InsightScenarios} bodies. */
public class InsightGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMobAfterThePlayerIsMarkedOnlyWhenPassiveInsightMeetsTheDc(GameTestHelper helper) {
        InsightScenarios.aMobAfterThePlayerIsMarkedOnlyWhenPassiveInsightMeetsTheDc(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aFusingCreeperWarnsOncePerFuse(GameTestHelper helper) {
        InsightScenarios.aFusingCreeperWarnsOncePerFuse(helper);
    }
}
