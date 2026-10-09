package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.InsightScenarios;

/** NeoForge registration shim: delegates to the shared {@link InsightScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class InsightGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void aMobAfterThePlayerIsMarkedOnlyWhenPassiveInsightMeetsTheDc(GameTestHelper helper) {
        InsightScenarios.aMobAfterThePlayerIsMarkedOnlyWhenPassiveInsightMeetsTheDc(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aFusingCreeperWarnsOncePerFuse(GameTestHelper helper) {
        InsightScenarios.aFusingCreeperWarnsOncePerFuse(helper);
    }
}
