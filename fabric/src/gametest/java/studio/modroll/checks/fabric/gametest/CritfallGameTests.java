package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.CritfallScenarios;

/** Fabric registration shim: delegates to the shared {@link CritfallScenarios} bodies. */
public class CritfallGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void playerAttackAndDamageUseAbilityAndProficiency(GameTestHelper helper) {
        CritfallScenarios.playerAttackAndDamageUseAbilityAndProficiency(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void unprofiledMobKeepsCritfallBonus(GameTestHelper helper) {
        CritfallScenarios.unprofiledMobKeepsCritfallBonus(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void profiledMobGetsChecksValues(GameTestHelper helper) {
        CritfallScenarios.profiledMobGetsChecksValues(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void unprofiledMobsOptionCoversEveryMob(GameTestHelper helper) {
        CritfallScenarios.unprofiledMobsOptionCoversEveryMob(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void spellSaveUsesMappedAbility(GameTestHelper helper) {
        CritfallScenarios.spellSaveUsesMappedAbility(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void disabledMatchesNoChecks(GameTestHelper helper) {
        CritfallScenarios.disabledMatchesNoChecks(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void toggleFreesAndRefillsTheSlot(GameTestHelper helper) {
        CritfallScenarios.toggleFreesAndRefillsTheSlot(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void toggleKeepsAnotherModsProvider(GameTestHelper helper) {
        CritfallScenarios.toggleKeepsAnotherModsProvider(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void damageFactorsStackWithAbilityModifier(GameTestHelper helper) {
        CritfallScenarios.damageFactorsStackWithAbilityModifier(helper);
    }
}
