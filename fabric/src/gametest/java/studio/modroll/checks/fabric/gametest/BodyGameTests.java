package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.BodyScenarios;

/** Fabric registration shim: delegates to the shared {@link BodyScenarios} bodies. */
public class BodyGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void conSetsMaxHealthAndKeepsHealthWithinIt(GameTestHelper helper) {
        BodyScenarios.conSetsMaxHealthAndKeepsHealthWithinIt(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void levelsAddHitDieHealthWhileOn(GameTestHelper helper) {
        BodyScenarios.levelsAddHitDieHealthWhileOn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void relogKeepsHealthAboveTheVanillaMax(GameTestHelper helper) {
        BodyScenarios.relogKeepsHealthAboveTheVanillaMax(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void switchesOffLeaveNoModifiersAfterRelog(GameTestHelper helper) {
        BodyScenarios.switchesOffLeaveNoModifiersAfterRelog(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void removedChecksLeavesVanillaAttributes(GameTestHelper helper) {
        BodyScenarios.removedChecksLeavesVanillaAttributes(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void respawnStartsAtFullModifiedHealth(GameTestHelper helper) {
        BodyScenarios.respawnStartsAtFullModifiedHealth(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void armorClassFollowsTheHeaviestArmor(GameTestHelper helper) {
        BodyScenarios.armorClassFollowsTheHeaviestArmor(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void armorClassReachesCritfallRolls(GameTestHelper helper) {
        BodyScenarios.armorClassReachesCritfallRolls(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void onlyProfiledMobsGetArmorClass(GameTestHelper helper) {
        BodyScenarios.onlyProfiledMobsGetArmorClass(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void speciesAndChoiceSetTheSize(GameTestHelper helper) {
        BodyScenarios.speciesAndChoiceSetTheSize(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void creationValidatesTheSizeChoice(GameTestHelper helper) {
        BodyScenarios.creationValidatesTheSizeChoice(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void sizeSwitchesOff(GameTestHelper helper) {
        BodyScenarios.sizeSwitchesOff(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void strengthExtras(GameTestHelper helper) {
        BodyScenarios.strengthExtras(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dexterityExtras(GameTestHelper helper) {
        BodyScenarios.dexterityExtras(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void constitutionExtras(GameTestHelper helper) {
        BodyScenarios.constitutionExtras(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void healthAndArmorClassSwitchOff(GameTestHelper helper) {
        BodyScenarios.healthAndArmorClassSwitchOff(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void sheetShowsTheBody(GameTestHelper helper) {
        BodyScenarios.sheetShowsTheBody(helper);
    }
}
