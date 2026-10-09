package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.DeathStoryScenarios;

/** Fabric registration shim: delegates to the shared {@link DeathStoryScenarios} bodies. */
public class DeathStoryGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void caughtPickpocketKilledByAGolem(GameTestHelper helper) {
        DeathStoryScenarios.caughtPickpocketKilledByAGolem(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void failedThreatKilledByAGolem(GameTestHelper helper) {
        DeathStoryScenarios.failedThreatKilledByAGolem(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void naturalOneThreatKilledByAMob(GameTestHelper helper) {
        DeathStoryScenarios.naturalOneThreatKilledByAMob(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void failedDexSaveKilledByAnExplosion(GameTestHelper helper) {
        DeathStoryScenarios.failedDexSaveKilledByAnExplosion(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void diedWithTheLastStandSpent(GameTestHelper helper) {
        DeathStoryScenarios.diedWithTheLastStandSpent(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void storiesEndWithTheWindow(GameTestHelper helper) {
        DeathStoryScenarios.storiesEndWithTheWindow(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void storiesOnlyTellDeathsTheyExplain(GameTestHelper helper) {
        DeathStoryScenarios.storiesOnlyTellDeathsTheyExplain(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void deathMessagesSwitchOff(GameTestHelper helper) {
        DeathStoryScenarios.deathMessagesSwitchOff(helper);
    }
}
