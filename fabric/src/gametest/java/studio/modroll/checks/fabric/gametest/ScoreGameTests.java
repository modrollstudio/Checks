package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.ScoreScenarios;

/** Fabric registration shim: delegates to the shared {@link ScoreScenarios} bodies. */
public class ScoreGameTests implements FabricGameTest {

    // A reload finishes off-thread; the GameTest server ticks fast enough to outrun it.
    private static final int ASYNC_TIMEOUT_TICKS = 2000;

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void profiledEntityBeatsDerivedScores(GameTestHelper helper) {
        ScoreScenarios.profiledEntityBeatsDerivedScores(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tagProfileAppliesOnlyToTaggedEntities(GameTestHelper helper) {
        ScoreScenarios.tagProfileAppliesOnlyToTaggedEntities(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void unprofiledMobsGetVariedDerivedScores(GameTestHelper helper) {
        ScoreScenarios.unprofiledMobsGetVariedDerivedScores(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void playerScoreResolutionOrder(GameTestHelper helper) {
        ScoreScenarios.playerScoreResolutionOrder(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void playerScoreSurvivesRelogAndSave(GameTestHelper helper) {
        ScoreScenarios.playerScoreSurvivesRelogAndSave(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = ASYNC_TIMEOUT_TICKS)
    public void reloadReplacesProfiles(GameTestHelper helper) {
        ScoreScenarios.reloadReplacesProfiles(helper);
    }
}
