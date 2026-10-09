package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.MobSocialScenarios;

/** Fabric registration shim: delegates to the shared {@link MobSocialScenarios} bodies. */
public class MobSocialGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void intimidateSendsAHostileMobRunning(GameTestHelper helper) {
        MobSocialScenarios.intimidateSendsAHostileMobRunning(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aFailedThreatDoesNothingAndANaturalOneEnrages(GameTestHelper helper) {
        MobSocialScenarios.aFailedThreatDoesNothingAndANaturalOneEnrages(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void strongBossAndExcludedMobsAreFearless(GameTestHelper helper) {
        MobSocialScenarios.strongBossAndExcludedMobsAreFearless(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void calmClearsAngerForEachListedMob(GameTestHelper helper) {
        MobSocialScenarios.calmClearsAngerForEachListedMob(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void calmFailsLeavesAngerAndCoolsDown(GameTestHelper helper) {
        MobSocialScenarios.calmFailsLeavesAngerAndCoolsDown(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void mobActionsSwitchOff(GameTestHelper helper) {
        MobSocialScenarios.mobActionsSwitchOff(helper);
    }
}
