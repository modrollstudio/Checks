package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.RestScenarios;

/** NeoForge registration shim: delegates to the shared {@link RestScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class RestGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void aShortRestSpendsHitDiceUntilFullHealth(GameTestHelper helper) {
        RestScenarios.aShortRestSpendsHitDiceUntilFullHealth(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aShortRestWithoutHitDiceHealsNothing(GameTestHelper helper) {
        RestScenarios.aShortRestWithoutHitDiceHealsNothing(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aHitDieHealsAtLeastOne(GameTestHelper helper) {
        RestScenarios.aHitDieHealsAtLeastOne(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aShortRestBreaks(GameTestHelper helper) {
        RestScenarios.aShortRestBreaks(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aLongRestHealsAndRegainsHitDiceAndTraits(GameTestHelper helper) {
        RestScenarios.aLongRestHealsAndRegainsHitDiceAndTraits(helper);
    }

    @GameTest(template = TEMPLATE)
    public void restsSwitchOff(GameTestHelper helper) {
        RestScenarios.restsSwitchOff(helper);
    }
}
