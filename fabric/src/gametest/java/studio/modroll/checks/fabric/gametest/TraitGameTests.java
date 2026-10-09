package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.TraitScenarios;

/** Fabric registration shim: delegates to the shared {@link TraitScenarios} bodies. */
public class TraitGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void vanillaSaveAdvantage(GameTestHelper helper) {
        TraitScenarios.vanillaSaveAdvantage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void rollBonusGrantsAdvantage(GameTestHelper helper) {
        TraitScenarios.rollBonusGrantsAdvantage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void damageResistanceHalvesMatchingDamage(GameTestHelper helper) {
        TraitScenarios.damageResistanceHalvesMatchingDamage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void extraHealthFollowsTheLevel(GameTestHelper helper) {
        TraitScenarios.extraHealthFollowsTheLevel(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void darkvisionIsSentToTheClient(GameTestHelper helper) {
        TraitScenarios.darkvisionIsSentToTheClient(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void luckRerollsNaturalOnes(GameTestHelper helper) {
        TraitScenarios.luckRerollsNaturalOnes(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void resourcefulRerollsOnceADay(GameTestHelper helper) {
        TraitScenarios.resourcefulRerollsOnceADay(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void lastStandOnceADay(GameTestHelper helper) {
        TraitScenarios.lastStandOnceADay(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void lastStandBeforeTheTotem(GameTestHelper helper) {
        TraitScenarios.lastStandBeforeTheTotem(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void totemWhenLastStandIsOff(GameTestHelper helper) {
        TraitScenarios.totemWhenLastStandIsOff(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void ignoredByMobs(GameTestHelper helper) {
        TraitScenarios.ignoredByMobs(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void skillChoicesAtCreation(GameTestHelper helper) {
        TraitScenarios.skillChoicesAtCreation(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void attributeModifiers(GameTestHelper helper) {
        TraitScenarios.attributeModifiers(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void masterSwitchTurnsEveryTraitOff(GameTestHelper helper) {
        TraitScenarios.masterSwitchTurnsEveryTraitOff(helper);
    }
}
