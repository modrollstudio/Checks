package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.SocialScenarios;

/** NeoForge registration shim: delegates to the shared {@link SocialScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class SocialGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void persuadeOutcomesFollowTheDie(GameTestHelper helper) {
        SocialScenarios.persuadeOutcomesFollowTheDie(helper);
    }

    @GameTest(template = TEMPLATE)
    public void pricesApplyWhileTradingAndAreTakenBack(GameTestHelper helper) {
        SocialScenarios.pricesApplyWhileTradingAndAreTakenBack(helper);
    }

    @GameTest(template = TEMPLATE)
    public void priceChangeExpires(GameTestHelper helper) {
        SocialScenarios.priceChangeExpires(helper);
    }

    @GameTest(template = TEMPLATE)
    public void deceiveRollsAgainstPassiveInsight(GameTestHelper helper) {
        SocialScenarios.deceiveRollsAgainstPassiveInsight(helper);
    }

    @GameTest(template = TEMPLATE)
    public void pickpocketUsesPassivePerceptionAndAdvantageFromBehind(GameTestHelper helper) {
        SocialScenarios.pickpocketUsesPassivePerceptionAndAdvantageFromBehind(helper);
    }

    @GameTest(template = TEMPLATE)
    public void cooldownBlocksARepeat(GameTestHelper helper) {
        SocialScenarios.cooldownBlocksARepeat(helper);
    }

    @GameTest(template = TEMPLATE)
    public void caughtPickpocketAngersGolemsAndIsRefused(GameTestHelper helper) {
        SocialScenarios.caughtPickpocketAngersGolemsAndIsRefused(helper);
    }

    @GameTest(template = TEMPLATE)
    public void intimidateVillagerOutcomes(GameTestHelper helper) {
        SocialScenarios.intimidateVillagerOutcomes(helper);
    }

    @GameTest(template = TEMPLATE)
    public void deceivingPiglins(GameTestHelper helper) {
        SocialScenarios.deceivingPiglins(helper);
    }

    @GameTest(template = TEMPLATE)
    public void intimidatingPiglins(GameTestHelper helper) {
        SocialScenarios.intimidatingPiglins(helper);
    }

    @GameTest(template = TEMPLATE)
    public void passivePricesShiftByProfession(GameTestHelper helper) {
        SocialScenarios.passivePricesShiftByProfession(helper);
    }

    @GameTest(template = TEMPLATE)
    public void eachActionSwitchesOff(GameTestHelper helper) {
        SocialScenarios.eachActionSwitchesOff(helper);
    }

    @GameTest(template = TEMPLATE)
    public void menuGreysOutWhatDoesNotFit(GameTestHelper helper) {
        SocialScenarios.menuGreysOutWhatDoesNotFit(helper);
    }

    @GameTest(template = TEMPLATE)
    public void playersAndOtherMobsAreNeverTargets(GameTestHelper helper) {
        SocialScenarios.playersAndOtherMobsAreNeverTargets(helper);
    }

    @GameTest(template = TEMPLATE)
    public void negativeGossipReachesTheVillage(GameTestHelper helper) {
        SocialScenarios.negativeGossipReachesTheVillage(helper);
    }

    @GameTest(template = TEMPLATE)
    public void caughtPickpocketMakesTheVictimRun(GameTestHelper helper) {
        SocialScenarios.caughtPickpocketMakesTheVictimRun(helper);
    }

    @GameTest(template = TEMPLATE)
    public void intimidateAndPersuadeReactions(GameTestHelper helper) {
        SocialScenarios.intimidateAndPersuadeReactions(helper);
    }

    @GameTest(template = TEMPLATE)
    public void lyingToAWanderingTraderGetsYouSpatAt(GameTestHelper helper) {
        SocialScenarios.lyingToAWanderingTraderGetsYouSpatAt(helper);
    }

    @GameTest(template = TEMPLATE)
    public void reactionsSwitchOff(GameTestHelper helper) {
        SocialScenarios.reactionsSwitchOff(helper);
    }

    @GameTest(template = TEMPLATE)
    public void wanderingTraderOutcomesStayWithTheTrader(GameTestHelper helper) {
        SocialScenarios.wanderingTraderOutcomesStayWithTheTrader(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aSingleCrimeStaysAboveTheGolemThreshold(GameTestHelper helper) {
        SocialScenarios.aSingleCrimeStaysAboveTheGolemThreshold(helper);
    }
}
