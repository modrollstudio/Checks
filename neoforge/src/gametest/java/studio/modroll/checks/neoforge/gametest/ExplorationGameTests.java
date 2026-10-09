package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.ExplorationScenarios;

/** NeoForge registration shim: delegates to the shared {@link ExplorationScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class ExplorationGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void aMadeAthleticsCheckCarriesALeapFurther(GameTestHelper helper) {
        ExplorationScenarios.aMadeAthleticsCheckCarriesALeapFurther(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aLeapRollsOnlyWhenItCouldMatter(GameTestHelper helper) {
        ExplorationScenarios.aLeapRollsOnlyWhenItCouldMatter(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aLeapWithinTheCooldownReusesTheLastResult(GameTestHelper helper) {
        ExplorationScenarios.aLeapWithinTheCooldownReusesTheLastResult(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aMadeAcrobaticsCheckSoftensAFall(GameTestHelper helper) {
        ExplorationScenarios.aMadeAcrobaticsCheckSoftensAFall(helper);
    }

    @GameTest(template = TEMPLATE)
    public void landingsSwitchOff(GameTestHelper helper) {
        ExplorationScenarios.landingsSwitchOff(helper);
    }

    @GameTest(template = TEMPLATE)
    public void sneakingPastADullMobGetsCloser(GameTestHelper helper) {
        ExplorationScenarios.sneakingPastADullMobGetsCloser(helper);
    }

    @GameTest(template = TEMPLATE)
    public void passivePerceptionSpotsArmedTripwires(GameTestHelper helper) {
        ExplorationScenarios.passivePerceptionSpotsArmedTripwires(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aMadeSleightOfHandCheckDisarmsATripwire(GameTestHelper helper) {
        ExplorationScenarios.aMadeSleightOfHandCheckDisarmsATripwire(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aFailedSleightOfHandCheckSpringsTheTrap(GameTestHelper helper) {
        ExplorationScenarios.aFailedSleightOfHandCheckSpringsTheTrap(helper);
    }

    @GameTest(template = TEMPLATE)
    public void disarmingRollsOnlyWhenItCouldMatter(GameTestHelper helper) {
        ExplorationScenarios.disarmingRollsOnlyWhenItCouldMatter(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aMadeAthleticsCheckTearsThroughACobweb(GameTestHelper helper) {
        ExplorationScenarios.aMadeAthleticsCheckTearsThroughACobweb(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aCobwebRollsOnlyWhenCaughtAndSwitchedOn(GameTestHelper helper) {
        ExplorationScenarios.aCobwebRollsOnlyWhenCaughtAndSwitchedOn(helper);
    }

    @GameTest(template = TEMPLATE)
    public void athleticsSpeedsUpClimbing(GameTestHelper helper) {
        ExplorationScenarios.athleticsSpeedsUpClimbing(helper);
    }
}
