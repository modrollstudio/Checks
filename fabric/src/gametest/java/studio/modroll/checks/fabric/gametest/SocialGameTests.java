package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.SocialScenarios;

/** Fabric registration shim: delegates to the shared {@link SocialScenarios} bodies. */
public class SocialGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void persuadeOutcomesFollowTheDie(GameTestHelper helper) {
        SocialScenarios.persuadeOutcomesFollowTheDie(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void pricesApplyWhileTradingAndAreTakenBack(GameTestHelper helper) {
        SocialScenarios.pricesApplyWhileTradingAndAreTakenBack(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void priceChangeExpires(GameTestHelper helper) {
        SocialScenarios.priceChangeExpires(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void deceiveRollsAgainstPassiveInsight(GameTestHelper helper) {
        SocialScenarios.deceiveRollsAgainstPassiveInsight(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void pickpocketUsesPassivePerceptionAndAdvantageFromBehind(GameTestHelper helper) {
        SocialScenarios.pickpocketUsesPassivePerceptionAndAdvantageFromBehind(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void cooldownBlocksARepeat(GameTestHelper helper) {
        SocialScenarios.cooldownBlocksARepeat(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void caughtPickpocketAngersGolemsAndIsRefused(GameTestHelper helper) {
        SocialScenarios.caughtPickpocketAngersGolemsAndIsRefused(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void intimidateVillagerOutcomes(GameTestHelper helper) {
        SocialScenarios.intimidateVillagerOutcomes(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void deceivingPiglins(GameTestHelper helper) {
        SocialScenarios.deceivingPiglins(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void intimidatingPiglins(GameTestHelper helper) {
        SocialScenarios.intimidatingPiglins(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void passivePricesShiftByProfession(GameTestHelper helper) {
        SocialScenarios.passivePricesShiftByProfession(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void eachActionSwitchesOff(GameTestHelper helper) {
        SocialScenarios.eachActionSwitchesOff(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void menuGreysOutWhatDoesNotFit(GameTestHelper helper) {
        SocialScenarios.menuGreysOutWhatDoesNotFit(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void playersAndOtherMobsAreNeverTargets(GameTestHelper helper) {
        SocialScenarios.playersAndOtherMobsAreNeverTargets(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void negativeGossipReachesTheVillage(GameTestHelper helper) {
        SocialScenarios.negativeGossipReachesTheVillage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void caughtPickpocketMakesTheVictimRun(GameTestHelper helper) {
        SocialScenarios.caughtPickpocketMakesTheVictimRun(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void intimidateAndPersuadeReactions(GameTestHelper helper) {
        SocialScenarios.intimidateAndPersuadeReactions(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void lyingToAWanderingTraderGetsYouSpatAt(GameTestHelper helper) {
        SocialScenarios.lyingToAWanderingTraderGetsYouSpatAt(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void reactionsSwitchOff(GameTestHelper helper) {
        SocialScenarios.reactionsSwitchOff(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void wanderingTraderOutcomesStayWithTheTrader(GameTestHelper helper) {
        SocialScenarios.wanderingTraderOutcomesStayWithTheTrader(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aSingleCrimeStaysAboveTheGolemThreshold(GameTestHelper helper) {
        SocialScenarios.aSingleCrimeStaysAboveTheGolemThreshold(helper);
    }
}
