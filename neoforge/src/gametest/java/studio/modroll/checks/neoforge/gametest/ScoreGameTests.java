package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.ScoreScenarios;

/** NeoForge registration shim: delegates to the shared {@link ScoreScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class ScoreGameTests {

    private static final String TEMPLATE = "empty";
    // A reload finishes off-thread; the GameTest server ticks fast enough to outrun it.
    private static final int ASYNC_TIMEOUT_TICKS = 2000;

    @GameTest(template = TEMPLATE)
    public void profiledEntityBeatsDerivedScores(GameTestHelper helper) {
        ScoreScenarios.profiledEntityBeatsDerivedScores(helper);
    }

    @GameTest(template = TEMPLATE)
    public void tagProfileAppliesOnlyToTaggedEntities(GameTestHelper helper) {
        ScoreScenarios.tagProfileAppliesOnlyToTaggedEntities(helper);
    }

    @GameTest(template = TEMPLATE)
    public void unprofiledMobsGetVariedDerivedScores(GameTestHelper helper) {
        ScoreScenarios.unprofiledMobsGetVariedDerivedScores(helper);
    }

    @GameTest(template = TEMPLATE)
    public void playerScoreResolutionOrder(GameTestHelper helper) {
        ScoreScenarios.playerScoreResolutionOrder(helper);
    }

    @GameTest(template = TEMPLATE)
    public void playerScoreSurvivesRelogAndSave(GameTestHelper helper) {
        ScoreScenarios.playerScoreSurvivesRelogAndSave(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = ASYNC_TIMEOUT_TICKS)
    public void reloadReplacesProfiles(GameTestHelper helper) {
        ScoreScenarios.reloadReplacesProfiles(helper);
    }
}
