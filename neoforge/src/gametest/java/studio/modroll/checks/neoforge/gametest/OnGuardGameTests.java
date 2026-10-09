package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.OnGuardScenarios;

/**
 * NeoForge registration shim: delegates to the shared {@link OnGuardScenarios} bodies, each in a batch of
 * its own so no alarm reaches another test's villagers or golems.
 */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class OnGuardGameTests {

    private static final String TEMPLATE = "wide";

    @GameTest(template = TEMPLATE, batch = "on_guard_menu")
    public void onGuardVillagersOfferPleaAndLieOnce(GameTestHelper helper) {
        OnGuardScenarios.onGuardVillagersOfferPleaAndLieOnce(helper);
    }

    @GameTest(template = TEMPLATE, batch = "on_guard_plead")
    public void aSuccessfulPleaEndsTheAlarm(GameTestHelper helper) {
        OnGuardScenarios.aSuccessfulPleaEndsTheAlarm(helper);
    }

    @GameTest(template = TEMPLATE, batch = "on_guard_twenty")
    public void aNaturalTwentyPleaHalvesTheRefusals(GameTestHelper helper) {
        OnGuardScenarios.aNaturalTwentyPleaHalvesTheRefusals(helper);
    }

    @GameTest(template = TEMPLATE, batch = "on_guard_one")
    public void aNaturalOneLieRestartsTheAlarm(GameTestHelper helper) {
        OnGuardScenarios.aNaturalOneLieRestartsTheAlarm(helper);
    }

    @GameTest(template = TEMPLATE, batch = "on_guard_wary")
    public void waryVillagersSeeTheThiefComing(GameTestHelper helper) {
        OnGuardScenarios.waryVillagersSeeTheThiefComing(helper);
    }

    @GameTest(template = TEMPLATE, batch = "on_guard_off")
    public void onGuardAndWarinessSwitchOff(GameTestHelper helper) {
        OnGuardScenarios.onGuardAndWarinessSwitchOff(helper);
    }
}
