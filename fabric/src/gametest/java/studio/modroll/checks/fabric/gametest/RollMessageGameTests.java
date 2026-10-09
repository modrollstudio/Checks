package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.RollMessageScenarios;

/** Fabric registration shim: delegates to the shared {@link RollMessageScenarios} bodies. */
public class RollMessageGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void checksClientsGetTheLineWithItsDuration(GameTestHelper helper) {
        RollMessageScenarios.checksClientsGetTheLineWithItsDuration(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void rollMessagesSwitchOff(GameTestHelper helper) {
        RollMessageScenarios.rollMessagesSwitchOff(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void socialRollsSendTheFlavorOnALineOfItsOwn(GameTestHelper helper) {
        RollMessageScenarios.socialRollsSendTheFlavorOnALineOfItsOwn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void clientsWithoutChecksGetOnlyTheRoll(GameTestHelper helper) {
        RollMessageScenarios.clientsWithoutChecksGetOnlyTheRoll(helper);
    }
}
