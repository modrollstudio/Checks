package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.RollMessageScenarios;

/** NeoForge registration shim: delegates to the shared {@link RollMessageScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class RollMessageGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void checksClientsGetTheLineWithItsDuration(GameTestHelper helper) {
        RollMessageScenarios.checksClientsGetTheLineWithItsDuration(helper);
    }

    @GameTest(template = TEMPLATE)
    public void rollMessagesSwitchOff(GameTestHelper helper) {
        RollMessageScenarios.rollMessagesSwitchOff(helper);
    }

    @GameTest(template = TEMPLATE)
    public void socialRollsSendTheFlavorOnALineOfItsOwn(GameTestHelper helper) {
        RollMessageScenarios.socialRollsSendTheFlavorOnALineOfItsOwn(helper);
    }

    @GameTest(template = TEMPLATE)
    public void clientsWithoutChecksGetOnlyTheRoll(GameTestHelper helper) {
        RollMessageScenarios.clientsWithoutChecksGetOnlyTheRoll(helper);
    }
}
