package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.StatSheetScenarios;

/** Fabric registration shim: delegates to the shared {@link StatSheetScenarios} bodies. */
public class StatSheetGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void sheetMatchesSetScoresAndProficiency(GameTestHelper helper) {
        StatSheetScenarios.sheetMatchesSetScoresAndProficiency(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void sheetFollowsToggles(GameTestHelper helper) {
        StatSheetScenarios.sheetFollowsToggles(helper);
    }
}
