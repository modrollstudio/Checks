package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.LevelScenarios;

/** NeoForge registration shim: delegates to the shared {@link LevelScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class LevelGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void vanillaXpCountsWhenGained(GameTestHelper helper) {
        LevelScenarios.vanillaXpCountsWhenGained(helper);
    }

    @GameTest(template = TEMPLATE)
    public void dyingToAMobGivesNoXp(GameTestHelper helper) {
        LevelScenarios.dyingToAMobGivesNoXp(helper);
    }

    @GameTest(template = TEMPLATE)
    public void orbCountsInFullWithMending(GameTestHelper helper) {
        LevelScenarios.orbCountsInFullWithMending(helper);
    }

    @GameTest(template = TEMPLATE)
    public void advancementsGiveXpOnce(GameTestHelper helper) {
        LevelScenarios.advancementsGiveXpOnce(helper);
    }

    @GameTest(template = TEMPLATE)
    public void levelUpFiresOncePerLevel(GameTestHelper helper) {
        LevelScenarios.levelUpFiresOncePerLevel(helper);
    }

    @GameTest(template = TEMPLATE)
    public void proficiencyFollowsLevel(GameTestHelper helper) {
        LevelScenarios.proficiencyFollowsLevel(helper);
    }

    @GameTest(template = TEMPLATE)
    public void abilityScoreImprovements(GameTestHelper helper) {
        LevelScenarios.abilityScoreImprovements(helper);
    }

    @GameTest(template = TEMPLATE)
    public void levelCommands(GameTestHelper helper) {
        LevelScenarios.levelCommands(helper);
    }

    @GameTest(template = TEMPLATE)
    public void levellingOffRestoresPreviousRules(GameTestHelper helper) {
        LevelScenarios.levellingOffRestoresPreviousRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public void resetClearsLevel(GameTestHelper helper) {
        LevelScenarios.resetClearsLevel(helper);
    }

    @GameTest(template = TEMPLATE)
    public void levelUpRecordsTheClassHitDie(GameTestHelper helper) {
        LevelScenarios.levelUpRecordsTheClassHitDie(helper);
    }

    @GameTest(template = TEMPLATE)
    public void sheetShowsLevel(GameTestHelper helper) {
        LevelScenarios.sheetShowsLevel(helper);
    }

    @GameTest(template = TEMPLATE)
    public void levelSurvivesRelogAndSave(GameTestHelper helper) {
        LevelScenarios.levelSurvivesRelogAndSave(helper);
    }
}
