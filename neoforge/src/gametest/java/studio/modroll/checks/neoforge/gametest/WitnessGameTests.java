package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.WitnessScenarios;

/** NeoForge registration shim: delegates to the shared {@link WitnessScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class WitnessGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void caughtPickpocketHasWitnesses(GameTestHelper helper) {
        WitnessScenarios.caughtPickpocketHasWitnesses(helper);
    }

    @GameTest(template = TEMPLATE)
    public void failedThreatHasWitnesses(GameTestHelper helper) {
        WitnessScenarios.failedThreatHasWitnesses(helper);
    }

    @GameTest(template = TEMPLATE)
    public void witnessesMustBeInGossipRange(GameTestHelper helper) {
        WitnessScenarios.witnessesMustBeInGossipRange(helper);
    }

    @GameTest(template = TEMPLATE)
    public void witnessRefusalIsHalfTheTargets(GameTestHelper helper) {
        WitnessScenarios.witnessRefusalIsHalfTheTargets(helper);
    }

    @GameTest(template = TEMPLATE)
    public void witnessesSwitchOff(GameTestHelper helper) {
        WitnessScenarios.witnessesSwitchOff(helper);
    }
}
