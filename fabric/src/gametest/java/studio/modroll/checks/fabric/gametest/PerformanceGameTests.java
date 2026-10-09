package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.PerformanceScenarios;

/** Fabric registration shim: delegates to the shared {@link PerformanceScenarios} bodies. The on-guard one runs in a batch of
 * its own: its alarm reaches past its structure.
 */
public class PerformanceGameTests implements FabricGameTest {

    private static final String WIDE = "checks:wide";

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aNoteBlockPerformanceFollowsTheDie(GameTestHelper helper) {
        PerformanceScenarios.aNoteBlockPerformanceFollowsTheDie(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aGoatHornPerformsAndCoolsDown(GameTestHelper helper) {
        PerformanceScenarios.aGoatHornPerformsAndCoolsDown(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void redstoneNotesRollNothing(GameTestHelper helper) {
        PerformanceScenarios.redstoneNotesRollNothing(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void noAudienceOrSwitchedOffRollsNothing(GameTestHelper helper) {
        PerformanceScenarios.noAudienceOrSwitchedOffRollsNothing(helper);
    }

    @GameTest(template = WIDE, batch = "performance_on_guard")
    public void villagersOnGuardIgnoreAPerformance(GameTestHelper helper) {
        PerformanceScenarios.villagersOnGuardIgnoreAPerformance(helper);
    }
}
