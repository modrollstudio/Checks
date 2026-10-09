package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.CreationScenarios;

/** Fabric registration shim: delegates to the shared {@link CreationScenarios} bodies. */
public class CreationGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void eachMethodProducesValidScores(GameTestHelper helper) {
        CreationScenarios.eachMethodProducesValidScores(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void serverRejectsInvalidSubmissions(GameTestHelper helper) {
        CreationScenarios.serverRejectsInvalidSubmissions(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void rollsDoNotChangeOnReopenOrRelog(GameTestHelper helper) {
        CreationScenarios.rollsDoNotChangeOnReopenOrRelog(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void hardcoreAssignsInOrder(GameTestHelper helper) {
        CreationScenarios.hardcoreAssignsInOrder(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void rollChatListsEveryDie(GameTestHelper helper) {
        CreationScenarios.rollChatListsEveryDie(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void resetReopensCreation(GameTestHelper helper) {
        CreationScenarios.resetReopensCreation(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void creationToggleOffSkipsIt(GameTestHelper helper) {
        CreationScenarios.creationToggleOffSkipsIt(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void characterRanksBetweenCommandAndProfile(GameTestHelper helper) {
        CreationScenarios.characterRanksBetweenCommandAndProfile(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void characterPersistsInTheWorldSave(GameTestHelper helper) {
        CreationScenarios.characterPersistsInTheWorldSave(helper);
    }
}
