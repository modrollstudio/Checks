package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.PerformanceScenarios;

/** NeoForge registration shim: delegates to the shared {@link PerformanceScenarios} bodies. The on-guard one runs in a batch
 * of its own: its alarm reaches past its structure.
 */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class PerformanceGameTests {

    private static final String TEMPLATE = "empty";
    private static final String WIDE = "wide";

    @GameTest(template = TEMPLATE)
    public void aNoteBlockPerformanceFollowsTheDie(GameTestHelper helper) {
        PerformanceScenarios.aNoteBlockPerformanceFollowsTheDie(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aGoatHornPerformsAndCoolsDown(GameTestHelper helper) {
        PerformanceScenarios.aGoatHornPerformsAndCoolsDown(helper);
    }

    @GameTest(template = TEMPLATE)
    public void redstoneNotesRollNothing(GameTestHelper helper) {
        PerformanceScenarios.redstoneNotesRollNothing(helper);
    }

    @GameTest(template = TEMPLATE)
    public void noAudienceOrSwitchedOffRollsNothing(GameTestHelper helper) {
        PerformanceScenarios.noAudienceOrSwitchedOffRollsNothing(helper);
    }

    @GameTest(template = WIDE, batch = "performance_on_guard")
    public void villagersOnGuardIgnoreAPerformance(GameTestHelper helper) {
        PerformanceScenarios.villagersOnGuardIgnoreAPerformance(helper);
    }
}
