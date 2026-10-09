package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.WitnessScenarios;

/** Fabric registration shim: delegates to the shared {@link WitnessScenarios} bodies. */
public class WitnessGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void caughtPickpocketHasWitnesses(GameTestHelper helper) {
        WitnessScenarios.caughtPickpocketHasWitnesses(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void failedThreatHasWitnesses(GameTestHelper helper) {
        WitnessScenarios.failedThreatHasWitnesses(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void witnessesMustBeInGossipRange(GameTestHelper helper) {
        WitnessScenarios.witnessesMustBeInGossipRange(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void witnessRefusalIsHalfTheTargets(GameTestHelper helper) {
        WitnessScenarios.witnessRefusalIsHalfTheTargets(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void witnessesSwitchOff(GameTestHelper helper) {
        WitnessScenarios.witnessesSwitchOff(helper);
    }
}
