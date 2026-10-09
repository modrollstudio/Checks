package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.LevelScenarios;

/** Fabric registration shim: delegates to the shared {@link LevelScenarios} bodies. */
public class LevelGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void vanillaXpCountsWhenGained(GameTestHelper helper) {
        LevelScenarios.vanillaXpCountsWhenGained(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dyingToAMobGivesNoXp(GameTestHelper helper) {
        LevelScenarios.dyingToAMobGivesNoXp(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void orbCountsInFullWithMending(GameTestHelper helper) {
        LevelScenarios.orbCountsInFullWithMending(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void advancementsGiveXpOnce(GameTestHelper helper) {
        LevelScenarios.advancementsGiveXpOnce(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void levelUpFiresOncePerLevel(GameTestHelper helper) {
        LevelScenarios.levelUpFiresOncePerLevel(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void proficiencyFollowsLevel(GameTestHelper helper) {
        LevelScenarios.proficiencyFollowsLevel(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void abilityScoreImprovements(GameTestHelper helper) {
        LevelScenarios.abilityScoreImprovements(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void levelCommands(GameTestHelper helper) {
        LevelScenarios.levelCommands(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void levellingOffRestoresPreviousRules(GameTestHelper helper) {
        LevelScenarios.levellingOffRestoresPreviousRules(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void resetClearsLevel(GameTestHelper helper) {
        LevelScenarios.resetClearsLevel(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void levelUpRecordsTheClassHitDie(GameTestHelper helper) {
        LevelScenarios.levelUpRecordsTheClassHitDie(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void sheetShowsLevel(GameTestHelper helper) {
        LevelScenarios.sheetShowsLevel(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void levelSurvivesRelogAndSave(GameTestHelper helper) {
        LevelScenarios.levelSurvivesRelogAndSave(helper);
    }
}
