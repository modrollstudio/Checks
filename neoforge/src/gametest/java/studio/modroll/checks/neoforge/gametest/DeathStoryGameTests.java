package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.DeathStoryScenarios;

/** NeoForge registration shim: delegates to the shared {@link DeathStoryScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class DeathStoryGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void caughtPickpocketKilledByAGolem(GameTestHelper helper) {
        DeathStoryScenarios.caughtPickpocketKilledByAGolem(helper);
    }

    @GameTest(template = TEMPLATE)
    public void failedThreatKilledByAGolem(GameTestHelper helper) {
        DeathStoryScenarios.failedThreatKilledByAGolem(helper);
    }

    @GameTest(template = TEMPLATE)
    public void naturalOneThreatKilledByAMob(GameTestHelper helper) {
        DeathStoryScenarios.naturalOneThreatKilledByAMob(helper);
    }

    @GameTest(template = TEMPLATE)
    public void failedDexSaveKilledByAnExplosion(GameTestHelper helper) {
        DeathStoryScenarios.failedDexSaveKilledByAnExplosion(helper);
    }

    @GameTest(template = TEMPLATE)
    public void diedWithTheLastStandSpent(GameTestHelper helper) {
        DeathStoryScenarios.diedWithTheLastStandSpent(helper);
    }

    @GameTest(template = TEMPLATE)
    public void storiesEndWithTheWindow(GameTestHelper helper) {
        DeathStoryScenarios.storiesEndWithTheWindow(helper);
    }

    @GameTest(template = TEMPLATE)
    public void storiesOnlyTellDeathsTheyExplain(GameTestHelper helper) {
        DeathStoryScenarios.storiesOnlyTellDeathsTheyExplain(helper);
    }

    @GameTest(template = TEMPLATE)
    public void deathMessagesSwitchOff(GameTestHelper helper) {
        DeathStoryScenarios.deathMessagesSwitchOff(helper);
    }
}
