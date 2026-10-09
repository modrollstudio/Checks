package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.MobSocialScenarios;

/** NeoForge registration shim: delegates to the shared {@link MobSocialScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class MobSocialGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void intimidateSendsAHostileMobRunning(GameTestHelper helper) {
        MobSocialScenarios.intimidateSendsAHostileMobRunning(helper);
    }

    @GameTest(template = TEMPLATE)
    public void aFailedThreatDoesNothingAndANaturalOneEnrages(GameTestHelper helper) {
        MobSocialScenarios.aFailedThreatDoesNothingAndANaturalOneEnrages(helper);
    }

    @GameTest(template = TEMPLATE)
    public void strongBossAndExcludedMobsAreFearless(GameTestHelper helper) {
        MobSocialScenarios.strongBossAndExcludedMobsAreFearless(helper);
    }

    @GameTest(template = TEMPLATE)
    public void calmClearsAngerForEachListedMob(GameTestHelper helper) {
        MobSocialScenarios.calmClearsAngerForEachListedMob(helper);
    }

    @GameTest(template = TEMPLATE)
    public void calmFailsLeavesAngerAndCoolsDown(GameTestHelper helper) {
        MobSocialScenarios.calmFailsLeavesAngerAndCoolsDown(helper);
    }

    @GameTest(template = TEMPLATE)
    public void mobActionsSwitchOff(GameTestHelper helper) {
        MobSocialScenarios.mobActionsSwitchOff(helper);
    }
}
