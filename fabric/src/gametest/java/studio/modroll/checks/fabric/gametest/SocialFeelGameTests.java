package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.SocialFeelScenarios;

/** Fabric registration shim: delegates to the shared {@link SocialFeelScenarios} bodies. */
public class SocialFeelGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void speechBubblesFollowTheOutcome(GameTestHelper helper) {
        SocialFeelScenarios.speechBubblesFollowTheOutcome(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void pickpocketedItemsFlyOrAreSnatchedBack(GameTestHelper helper) {
        SocialFeelScenarios.pickpocketedItemsFlyOrAreSnatchedBack(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void feelSwitchesOff(GameTestHelper helper) {
        SocialFeelScenarios.feelSwitchesOff(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void voicesReachChecksClientsAndVanillaSoundsTheRest(GameTestHelper helper) {
        SocialFeelScenarios.voicesReachChecksClientsAndVanillaSoundsTheRest(helper);
    }
}
