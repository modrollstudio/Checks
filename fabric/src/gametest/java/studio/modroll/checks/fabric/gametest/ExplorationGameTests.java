package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.ExplorationScenarios;

/** Fabric registration shim: delegates to the shared {@link ExplorationScenarios} bodies. */
public class ExplorationGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMadeAthleticsCheckCarriesALeapFurther(GameTestHelper helper) {
        ExplorationScenarios.aMadeAthleticsCheckCarriesALeapFurther(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aLeapRollsOnlyWhenItCouldMatter(GameTestHelper helper) {
        ExplorationScenarios.aLeapRollsOnlyWhenItCouldMatter(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aLeapWithinTheCooldownReusesTheLastResult(GameTestHelper helper) {
        ExplorationScenarios.aLeapWithinTheCooldownReusesTheLastResult(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMadeAcrobaticsCheckSoftensAFall(GameTestHelper helper) {
        ExplorationScenarios.aMadeAcrobaticsCheckSoftensAFall(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void landingsSwitchOff(GameTestHelper helper) {
        ExplorationScenarios.landingsSwitchOff(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void sneakingPastADullMobGetsCloser(GameTestHelper helper) {
        ExplorationScenarios.sneakingPastADullMobGetsCloser(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void passivePerceptionSpotsArmedTripwires(GameTestHelper helper) {
        ExplorationScenarios.passivePerceptionSpotsArmedTripwires(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMadeSleightOfHandCheckDisarmsATripwire(GameTestHelper helper) {
        ExplorationScenarios.aMadeSleightOfHandCheckDisarmsATripwire(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aFailedSleightOfHandCheckSpringsTheTrap(GameTestHelper helper) {
        ExplorationScenarios.aFailedSleightOfHandCheckSpringsTheTrap(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void disarmingRollsOnlyWhenItCouldMatter(GameTestHelper helper) {
        ExplorationScenarios.disarmingRollsOnlyWhenItCouldMatter(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMadeAthleticsCheckTearsThroughACobweb(GameTestHelper helper) {
        ExplorationScenarios.aMadeAthleticsCheckTearsThroughACobweb(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aCobwebRollsOnlyWhenCaughtAndSwitchedOn(GameTestHelper helper) {
        ExplorationScenarios.aCobwebRollsOnlyWhenCaughtAndSwitchedOn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void athleticsSpeedsUpClimbing(GameTestHelper helper) {
        ExplorationScenarios.athleticsSpeedsUpClimbing(helper);
    }
}
