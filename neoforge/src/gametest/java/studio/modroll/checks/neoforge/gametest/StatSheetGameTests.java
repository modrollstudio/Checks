package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.StatSheetScenarios;

/** NeoForge registration shim: delegates to the shared {@link StatSheetScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class StatSheetGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void sheetMatchesSetScoresAndProficiency(GameTestHelper helper) {
        StatSheetScenarios.sheetMatchesSetScoresAndProficiency(helper);
    }

    @GameTest(template = TEMPLATE)
    public void sheetFollowsToggles(GameTestHelper helper) {
        StatSheetScenarios.sheetFollowsToggles(helper);
    }
}
