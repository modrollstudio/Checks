package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.CheckScenarios;

/** NeoForge registration shim: delegates to the shared {@link CheckScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class CheckGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void apiReadsScoresAndModifiers(GameTestHelper helper) {
        CheckScenarios.apiReadsScoresAndModifiers(helper);
    }

    @GameTest(template = TEMPLATE)
    public void abilityCheckMeetsBeatsAndMisses(GameTestHelper helper) {
        CheckScenarios.abilityCheckMeetsBeatsAndMisses(helper);
    }

    @GameTest(template = TEMPLATE)
    public void skillCheckAddsSkillModifier(GameTestHelper helper) {
        CheckScenarios.skillCheckAddsSkillModifier(helper);
    }

    @GameTest(template = TEMPLATE)
    public void savingThrowUsesAbilityModifier(GameTestHelper helper) {
        CheckScenarios.savingThrowUsesAbilityModifier(helper);
    }

    @GameTest(template = TEMPLATE)
    public void contestGoesEitherWay(GameTestHelper helper) {
        CheckScenarios.contestGoesEitherWay(helper);
    }

    @GameTest(template = TEMPLATE)
    public void contestTieGoesToOpponent(GameTestHelper helper) {
        CheckScenarios.contestTieGoesToOpponent(helper);
    }

    @GameTest(template = TEMPLATE)
    public void checkAgainstPassiveScore(GameTestHelper helper) {
        CheckScenarios.checkAgainstPassiveScore(helper);
    }

    @GameTest(template = TEMPLATE)
    public void rollModesPassThroughToCritfall(GameTestHelper helper) {
        CheckScenarios.rollModesPassThroughToCritfall(helper);
    }

    @GameTest(template = TEMPLATE)
    public void skillsDisabledUseAbilityModifier(GameTestHelper helper) {
        CheckScenarios.skillsDisabledUseAbilityModifier(helper);
    }

    @GameTest(template = TEMPLATE)
    public void openRollAddsModifier(GameTestHelper helper) {
        CheckScenarios.openRollAddsModifier(helper);
    }

    @GameTest(template = TEMPLATE)
    public void rollCommandReturnsTotal(GameTestHelper helper) {
        CheckScenarios.rollCommandReturnsTotal(helper);
    }

    @GameTest(template = TEMPLATE)
    public void commandResultsWorkWithExecuteStore(GameTestHelper helper) {
        CheckScenarios.commandResultsWorkWithExecuteStore(helper);
    }
}
