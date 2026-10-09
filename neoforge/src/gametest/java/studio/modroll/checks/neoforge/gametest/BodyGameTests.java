package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.BodyScenarios;

/** NeoForge registration shim: delegates to the shared {@link BodyScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class BodyGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void conSetsMaxHealthAndKeepsHealthWithinIt(GameTestHelper helper) {
        BodyScenarios.conSetsMaxHealthAndKeepsHealthWithinIt(helper);
    }

    @GameTest(template = TEMPLATE)
    public void levelsAddHitDieHealthWhileOn(GameTestHelper helper) {
        BodyScenarios.levelsAddHitDieHealthWhileOn(helper);
    }

    @GameTest(template = TEMPLATE)
    public void relogKeepsHealthAboveTheVanillaMax(GameTestHelper helper) {
        BodyScenarios.relogKeepsHealthAboveTheVanillaMax(helper);
    }

    @GameTest(template = TEMPLATE)
    public void switchesOffLeaveNoModifiersAfterRelog(GameTestHelper helper) {
        BodyScenarios.switchesOffLeaveNoModifiersAfterRelog(helper);
    }

    @GameTest(template = TEMPLATE)
    public void removedChecksLeavesVanillaAttributes(GameTestHelper helper) {
        BodyScenarios.removedChecksLeavesVanillaAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public void respawnStartsAtFullModifiedHealth(GameTestHelper helper) {
        BodyScenarios.respawnStartsAtFullModifiedHealth(helper);
    }

    @GameTest(template = TEMPLATE)
    public void armorClassFollowsTheHeaviestArmor(GameTestHelper helper) {
        BodyScenarios.armorClassFollowsTheHeaviestArmor(helper);
    }

    @GameTest(template = TEMPLATE)
    public void armorClassReachesCritfallRolls(GameTestHelper helper) {
        BodyScenarios.armorClassReachesCritfallRolls(helper);
    }

    @GameTest(template = TEMPLATE)
    public void onlyProfiledMobsGetArmorClass(GameTestHelper helper) {
        BodyScenarios.onlyProfiledMobsGetArmorClass(helper);
    }

    @GameTest(template = TEMPLATE)
    public void speciesAndChoiceSetTheSize(GameTestHelper helper) {
        BodyScenarios.speciesAndChoiceSetTheSize(helper);
    }

    @GameTest(template = TEMPLATE)
    public void creationValidatesTheSizeChoice(GameTestHelper helper) {
        BodyScenarios.creationValidatesTheSizeChoice(helper);
    }

    @GameTest(template = TEMPLATE)
    public void sizeSwitchesOff(GameTestHelper helper) {
        BodyScenarios.sizeSwitchesOff(helper);
    }

    @GameTest(template = TEMPLATE)
    public void strengthExtras(GameTestHelper helper) {
        BodyScenarios.strengthExtras(helper);
    }

    @GameTest(template = TEMPLATE)
    public void dexterityExtras(GameTestHelper helper) {
        BodyScenarios.dexterityExtras(helper);
    }

    @GameTest(template = TEMPLATE)
    public void constitutionExtras(GameTestHelper helper) {
        BodyScenarios.constitutionExtras(helper);
    }

    @GameTest(template = TEMPLATE)
    public void healthAndArmorClassSwitchOff(GameTestHelper helper) {
        BodyScenarios.healthAndArmorClassSwitchOff(helper);
    }

    @GameTest(template = TEMPLATE)
    public void sheetShowsTheBody(GameTestHelper helper) {
        BodyScenarios.sheetShowsTheBody(helper);
    }
}
