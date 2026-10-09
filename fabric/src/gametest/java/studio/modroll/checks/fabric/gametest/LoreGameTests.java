package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.LoreScenarios;

/** Fabric registration shim: delegates to the shared {@link LoreScenarios} bodies. */
public class LoreGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMadeInvestigationCheckFindsOneMoreItem(GameTestHelper helper) {
        LoreScenarios.aMadeInvestigationCheckFindsOneMoreItem(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aFailedInvestigationCheckFindsNothingElse(GameTestHelper helper) {
        LoreScenarios.aFailedInvestigationCheckFindsNothingElse(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void searchingRollsOnlyWhenItCouldMatter(GameTestHelper helper) {
        LoreScenarios.searchingRollsOnlyWhenItCouldMatter(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMadeLoreCheckRecallsAMobTypeForGood(GameTestHelper helper) {
        LoreScenarios.aMadeLoreCheckRecallsAMobTypeForGood(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aFailedLoreCheckWaitsBeforeTheNextTry(GameTestHelper helper) {
        LoreScenarios.aFailedLoreCheckWaitsBeforeTheNextTry(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void loreRollsOnlyForMobsInSightAndInRange(GameTestHelper helper) {
        LoreScenarios.loreRollsOnlyForMobsInSightAndInRange(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void everyVanillaMobHasAHintOfItsOwn(GameTestHelper helper) {
        LoreScenarios.everyVanillaMobHasAHintOfItsOwn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMadeHistoryCheckRecallsAStructureKindForGood(GameTestHelper helper) {
        LoreScenarios.aMadeHistoryCheckRecallsAStructureKindForGood(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void historyRollsOnlyInsideAStructureAndWaitsAfterAFailure(GameTestHelper helper) {
        LoreScenarios.historyRollsOnlyInsideAStructureAndWaitsAfterAFailure(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void loreSkillsComeFromTagsThenTheFallback(GameTestHelper helper) {
        LoreScenarios.loreSkillsComeFromTagsThenTheFallback(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aHintIsBuiltFromTheMobsData(GameTestHelper helper) {
        LoreScenarios.aHintIsBuiltFromTheMobsData(helper);
    }
}
