package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.LoreScenarios;

/** NeoForge registration shim: delegates to the shared {@link LoreScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class LoreGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void aMadeInvestigationCheckFindsOneMoreItem(GameTestHelper helper) {
        LoreScenarios.aMadeInvestigationCheckFindsOneMoreItem(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aFailedInvestigationCheckFindsNothingElse(GameTestHelper helper) {
        LoreScenarios.aFailedInvestigationCheckFindsNothingElse(helper);
    }

    @GameTest(template = TEMPLATE)
    public void searchingRollsOnlyWhenItCouldMatter(GameTestHelper helper) {
        LoreScenarios.searchingRollsOnlyWhenItCouldMatter(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aMadeLoreCheckRecallsAMobTypeForGood(GameTestHelper helper) {
        LoreScenarios.aMadeLoreCheckRecallsAMobTypeForGood(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aFailedLoreCheckWaitsBeforeTheNextTry(GameTestHelper helper) {
        LoreScenarios.aFailedLoreCheckWaitsBeforeTheNextTry(helper);
    }

    @GameTest(template = TEMPLATE)
    public void loreRollsOnlyForMobsInSightAndInRange(GameTestHelper helper) {
        LoreScenarios.loreRollsOnlyForMobsInSightAndInRange(helper);
    }

    @GameTest(template = TEMPLATE)
    public void everyVanillaMobHasAHintOfItsOwn(GameTestHelper helper) {
        LoreScenarios.everyVanillaMobHasAHintOfItsOwn(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aMadeHistoryCheckRecallsAStructureKindForGood(GameTestHelper helper) {
        LoreScenarios.aMadeHistoryCheckRecallsAStructureKindForGood(helper);
    }

    @GameTest(template = TEMPLATE)
    public void historyRollsOnlyInsideAStructureAndWaitsAfterAFailure(GameTestHelper helper) {
        LoreScenarios.historyRollsOnlyInsideAStructureAndWaitsAfterAFailure(helper);
    }

    @GameTest(template = TEMPLATE)
    public void loreSkillsComeFromTagsThenTheFallback(GameTestHelper helper) {
        LoreScenarios.loreSkillsComeFromTagsThenTheFallback(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aHintIsBuiltFromTheMobsData(GameTestHelper helper) {
        LoreScenarios.aHintIsBuiltFromTheMobsData(helper);
    }
}
