package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.GolemAlarmScenarios;

/**
 * Fabric registration shim: delegates to the shared {@link GolemAlarmScenarios} bodies, each in a batch of
 * its own so no alarm reaches another test's golems.
 */
public class GolemAlarmGameTests implements FabricGameTest {

    private static final String TEMPLATE = "checks:wide";

    @GameTest(template = TEMPLATE, batch = "golem_alarm_backup")
    public void guardsCallForBackup(GameTestHelper helper) {
        GolemAlarmScenarios.guardsCallForBackup(helper);
    }

    @GameTest(template = TEMPLATE, batch = "golem_alarm_radius")
    public void backupReachesItsRadiusAndSpreads(GameTestHelper helper) {
        GolemAlarmScenarios.backupReachesItsRadiusAndSpreads(helper);
    }

    @GameTest(template = TEMPLATE, batch = "golem_alarm_expiry")
    public void alarmExpires(GameTestHelper helper) {
        GolemAlarmScenarios.alarmExpires(helper);
    }

    @GameTest(template = TEMPLATE, batch = "golem_alarm_death")
    public void alarmEndsWhenThePlayerDies(GameTestHelper helper) {
        GolemAlarmScenarios.alarmEndsWhenThePlayerDies(helper);
    }

    @GameTest(template = TEMPLATE, batch = "golem_alarm_off")
    public void golemAlarmSwitchesOff(GameTestHelper helper) {
        GolemAlarmScenarios.golemAlarmSwitchesOff(helper);
    }

    @GameTest(template = TEMPLATE, batch = "golem_alarm_peaceful")
    public void noBackupOnPeaceful(GameTestHelper helper) {
        GolemAlarmScenarios.noBackupOnPeaceful(helper);
    }

    @GameTest(template = TEMPLATE, batch = "golem_alarm_calm")
    public void aCalmedGolemSitsOutTheAlarm(GameTestHelper helper) {
        GolemAlarmScenarios.aCalmedGolemSitsOutTheAlarm(helper);
    }
}
