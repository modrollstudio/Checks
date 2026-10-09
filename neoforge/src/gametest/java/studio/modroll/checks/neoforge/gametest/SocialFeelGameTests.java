package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.SocialFeelScenarios;

/** NeoForge registration shim: delegates to the shared {@link SocialFeelScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class SocialFeelGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void speechBubblesFollowTheOutcome(GameTestHelper helper) {
        SocialFeelScenarios.speechBubblesFollowTheOutcome(helper);
    }

    @GameTest(template = TEMPLATE)
    public void pickpocketedItemsFlyOrAreSnatchedBack(GameTestHelper helper) {
        SocialFeelScenarios.pickpocketedItemsFlyOrAreSnatchedBack(helper);
    }

    @GameTest(template = TEMPLATE)
    public void feelSwitchesOff(GameTestHelper helper) {
        SocialFeelScenarios.feelSwitchesOff(helper);
    }

    @GameTest(template = TEMPLATE)
    public void voicesReachChecksClientsAndVanillaSoundsTheRest(GameTestHelper helper) {
        SocialFeelScenarios.voicesReachChecksClientsAndVanillaSoundsTheRest(helper);
    }
}
