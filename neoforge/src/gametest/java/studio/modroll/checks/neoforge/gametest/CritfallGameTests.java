package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.CritfallScenarios;

/** NeoForge registration shim: delegates to the shared {@link CritfallScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class CritfallGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void playerAttackAndDamageUseAbilityAndProficiency(GameTestHelper helper) {
        CritfallScenarios.playerAttackAndDamageUseAbilityAndProficiency(helper);
    }

    @GameTest(template = TEMPLATE)
    public void unprofiledMobKeepsCritfallBonus(GameTestHelper helper) {
        CritfallScenarios.unprofiledMobKeepsCritfallBonus(helper);
    }

    @GameTest(template = TEMPLATE)
    public void profiledMobGetsChecksValues(GameTestHelper helper) {
        CritfallScenarios.profiledMobGetsChecksValues(helper);
    }

    @GameTest(template = TEMPLATE)
    public void unprofiledMobsOptionCoversEveryMob(GameTestHelper helper) {
        CritfallScenarios.unprofiledMobsOptionCoversEveryMob(helper);
    }

    @GameTest(template = TEMPLATE)
    public void spellSaveUsesMappedAbility(GameTestHelper helper) {
        CritfallScenarios.spellSaveUsesMappedAbility(helper);
    }

    @GameTest(template = TEMPLATE)
    public void disabledMatchesNoChecks(GameTestHelper helper) {
        CritfallScenarios.disabledMatchesNoChecks(helper);
    }

    @GameTest(template = TEMPLATE)
    public void toggleFreesAndRefillsTheSlot(GameTestHelper helper) {
        CritfallScenarios.toggleFreesAndRefillsTheSlot(helper);
    }

    @GameTest(template = TEMPLATE)
    public void toggleKeepsAnotherModsProvider(GameTestHelper helper) {
        CritfallScenarios.toggleKeepsAnotherModsProvider(helper);
    }

    @GameTest(template = TEMPLATE)
    public void damageFactorsStackWithAbilityModifier(GameTestHelper helper) {
        CritfallScenarios.damageFactorsStackWithAbilityModifier(helper);
    }
}
