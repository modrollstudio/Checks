package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.CreationScenarios;

/** NeoForge registration shim: delegates to the shared {@link CreationScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class CreationGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void eachMethodProducesValidScores(GameTestHelper helper) {
        CreationScenarios.eachMethodProducesValidScores(helper);
    }

    @GameTest(template = TEMPLATE)
    public void serverRejectsInvalidSubmissions(GameTestHelper helper) {
        CreationScenarios.serverRejectsInvalidSubmissions(helper);
    }

    @GameTest(template = TEMPLATE)
    public void rollsDoNotChangeOnReopenOrRelog(GameTestHelper helper) {
        CreationScenarios.rollsDoNotChangeOnReopenOrRelog(helper);
    }

    @GameTest(template = TEMPLATE)
    public void hardcoreAssignsInOrder(GameTestHelper helper) {
        CreationScenarios.hardcoreAssignsInOrder(helper);
    }

    @GameTest(template = TEMPLATE)
    public void rollChatListsEveryDie(GameTestHelper helper) {
        CreationScenarios.rollChatListsEveryDie(helper);
    }

    @GameTest(template = TEMPLATE)
    public void resetReopensCreation(GameTestHelper helper) {
        CreationScenarios.resetReopensCreation(helper);
    }

    @GameTest(template = TEMPLATE)
    public void creationToggleOffSkipsIt(GameTestHelper helper) {
        CreationScenarios.creationToggleOffSkipsIt(helper);
    }

    @GameTest(template = TEMPLATE)
    public void characterRanksBetweenCommandAndProfile(GameTestHelper helper) {
        CreationScenarios.characterRanksBetweenCommandAndProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public void characterPersistsInTheWorldSave(GameTestHelper helper) {
        CreationScenarios.characterPersistsInTheWorldSave(helper);
    }
}
