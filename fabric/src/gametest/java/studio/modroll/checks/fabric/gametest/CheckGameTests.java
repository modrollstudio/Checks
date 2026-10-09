package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.CheckScenarios;

/** Fabric registration shim: delegates to the shared {@link CheckScenarios} bodies. */
public class CheckGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void apiReadsScoresAndModifiers(GameTestHelper helper) {
        CheckScenarios.apiReadsScoresAndModifiers(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void abilityCheckMeetsBeatsAndMisses(GameTestHelper helper) {
        CheckScenarios.abilityCheckMeetsBeatsAndMisses(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void skillCheckAddsSkillModifier(GameTestHelper helper) {
        CheckScenarios.skillCheckAddsSkillModifier(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void savingThrowUsesAbilityModifier(GameTestHelper helper) {
        CheckScenarios.savingThrowUsesAbilityModifier(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void contestGoesEitherWay(GameTestHelper helper) {
        CheckScenarios.contestGoesEitherWay(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void contestTieGoesToOpponent(GameTestHelper helper) {
        CheckScenarios.contestTieGoesToOpponent(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void checkAgainstPassiveScore(GameTestHelper helper) {
        CheckScenarios.checkAgainstPassiveScore(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void rollModesPassThroughToCritfall(GameTestHelper helper) {
        CheckScenarios.rollModesPassThroughToCritfall(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void skillsDisabledUseAbilityModifier(GameTestHelper helper) {
        CheckScenarios.skillsDisabledUseAbilityModifier(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void openRollAddsModifier(GameTestHelper helper) {
        CheckScenarios.openRollAddsModifier(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void rollCommandReturnsTotal(GameTestHelper helper) {
        CheckScenarios.rollCommandReturnsTotal(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void commandResultsWorkWithExecuteStore(GameTestHelper helper) {
        CheckScenarios.commandResultsWorkWithExecuteStore(helper);
    }
}
