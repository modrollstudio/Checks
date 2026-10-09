package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.spawnCalm;
import static studio.modroll.checks.gametest.ScenarioSupport.standardSkill;
import static studio.modroll.checks.gametest.ScenarioSupport.withDice;
import static studio.modroll.checks.gametest.ScenarioSupport.withExtensions;
import static studio.modroll.checks.gametest.ScenarioSupport.withListeners;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.AfterCheckEvent;
import studio.modroll.checks.api.CheckKind;
import studio.modroll.checks.api.CheckResult;
import studio.modroll.checks.api.CheckRoll;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.ContestRoll;
import studio.modroll.checks.api.OpenRoll;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.critfall.api.combat.ContestResult;
import studio.modroll.critfall.api.combat.SaveResult;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.critfall.api.dice.RollMode;

/**
 * GameTest bodies for the check events. Every die is scripted with {@code withDice}, which fails the test
 * if a roll draws more dice than scripted, so a canceled roll is shown to draw none. Expected totals use
 * the entity's live modifier, so they hold for whatever scores the mob derives.
 */
public final class EventScenarios {

    private EventScenarios() {}

    /** A listener moves the DC, adds a bonus and grants advantage; the roll uses all three. */
    public static void beforeEventChangesDcBonusAndMode(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        int modifier = ChecksApi.modifier(mob, Ability.DEXTERITY);
        SaveResult result = withListeners(
                event -> {
                    event.dc(30);
                    event.addBonus(4);
                    event.grantAdvantage();
                },
                event -> {},
                () -> withDice(() -> ChecksApi.check(mob, Ability.DEXTERITY, 10).result(), 6, 15));
        expectEquals(helper, 30, result.dc(), "DC moved by the listener");
        expectEquals(helper, 15, result.natural(), "advantage keeps the higher face");
        expect(helper, result.roll().mode() == RollMode.ADVANTAGE, "mode must be advantage, was " + result.roll());
        expectEquals(helper, 15 + modifier + 4, result.saveTotal(), "total with the listener's bonus");
        helper.succeed();
    }

    /** An open roll asked for normally reports the advantage a listener granted, so its d20s read right. */
    public static void openRollReportsTheModeRolled(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        OpenRoll roll = withListeners(
                event -> event.grantAdvantage(),
                event -> {},
                () -> withDice(() -> ChecksApi.roll(mob, Ability.DEXTERITY), 6, 15));
        RollDetail d20s = RollDetail.of(roll.mode(), roll.result());
        expect(helper, !roll.canceled(), "a rolled check is not canceled");
        expect(helper, roll.mode() == RollMode.ADVANTAGE, "mode must be advantage, was " + roll.mode());
        expectEquals(helper, 15, d20s.kept(), "advantage keeps the higher face");
        expectEquals(helper, 6, d20s.dropped().orElseThrow(), "and drops the lower");
        helper.succeed();
    }

    /** A disadvantaged save given advantage by a listener rolls one die, the 5e way. */
    public static void advantageAndDisadvantageCancel(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        SaveResult result = withListeners(
                event -> event.grantAdvantage(),
                event -> {},
                () -> withDice(
                        () -> ChecksApi.savingThrow(mob, Ability.WISDOM, 10, RollMode.DISADVANTAGE)
                                .result(),
                        11));
        expect(helper, result.roll().mode() == RollMode.NORMAL, "mode must be normal, was " + result.roll());
        expectEquals(helper, 11, result.natural(), "the single die");
        helper.succeed();
    }

    /**
     * Every canceled roll is flagged canceled: a check fails, a contest goes to the opponent and an open
     * roll totals 0; none draws a die or fires an after event.
     */
    public static void cancelSkipsTheRoll(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        Mob other = spawnCalm(helper, EntityType.SKELETON);
        Skill athletics = standardSkill("athletics");
        List<AfterCheckEvent> after = new ArrayList<>();
        withListeners(
                event -> event.cancel(),
                after::add,
                () -> withDice(() -> {
                    CheckRoll check = ChecksApi.check(mob, athletics, -5);
                    expect(
                            helper,
                            check.canceled() && !check.result().saved(),
                            "canceled check must be flagged and fail: " + check);
                    CheckRoll save = ChecksApi.savingThrow(mob, Ability.CONSTITUTION, 10);
                    expect(
                            helper,
                            save.canceled() && !save.result().saved(),
                            "canceled save must be flagged and fail: " + save);
                    ContestRoll contest = ChecksApi.contest(mob, athletics, other, athletics);
                    expect(
                            helper,
                            contest.canceled() && !contest.result().initiatorWins(),
                            "canceled contest must be flagged and go to the opponent");
                    OpenRoll open = ChecksApi.roll(mob, Ability.STRENGTH);
                    expect(
                            helper,
                            open.canceled() && open.result().total() == 0,
                            "canceled open roll must be flagged and total 0: " + open);
                    return null;
                }));
        expect(helper, after.isEmpty(), "a canceled roll must fire no after event, fired " + after.size());
        helper.succeed();
    }

    /** The after event carries the kind, the dice, the total, the DC and the outcome. */
    public static void afterEventSeesTheResult(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        int modifier = ChecksApi.saveModifier(mob, Ability.DEXTERITY);
        List<AfterCheckEvent> after = new ArrayList<>();
        withListeners(
                event -> event.addBonus(1),
                after::add,
                () -> withDice(
                        () -> ChecksApi.savingThrow(mob, Ability.DEXTERITY, 50, RollMode.ADVANTAGE)
                                .result(),
                        20,
                        3));
        expectEquals(helper, 1, after.size(), "after events");
        AfterCheckEvent event = after.getFirst();
        CheckResult result = event.result();
        expect(helper, event.entity() == mob && event.kind() == CheckKind.SAVE, "save by the mob, was " + event);
        expect(helper, event.stat() == Ability.DEXTERITY, "stat must be DEX, was " + event.stat());
        expect(helper, result.isNatural20() && !result.isNatural1(), "natural 20 must be flagged: " + result);
        expect(helper, result.roll().dropped().equals(OptionalInt.of(3)), "dropped face 3: " + result.roll());
        expectEquals(helper, modifier, result.modifier(), "save modifier");
        expectEquals(helper, 1, result.bonus(), "listener bonus");
        expectEquals(helper, 20 + modifier + 1, result.total(), "total");
        expect(helper, result.dc().equals(OptionalInt.of(50)) && !result.success(), "DC 50 fails: " + result);
        helper.succeed();
    }

    /** A contest fires one after event per side; exactly the winner succeeds. */
    public static void contestFiresOneEventPerSide(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        Mob other = spawnCalm(helper, EntityType.SKELETON);
        Skill athletics = standardSkill("athletics");
        List<AfterCheckEvent> after = new ArrayList<>();
        List<CheckKind> before = new ArrayList<>();
        ContestResult contest = withListeners(
                event -> before.add(event.kind()),
                after::add,
                () -> withDice(
                        () -> ChecksApi.contest(mob, athletics, other, Ability.STRENGTH)
                                .result(),
                        20,
                        1));
        expect(helper, before.equals(List.of(CheckKind.CONTEST, CheckKind.CONTEST)), "two before events: " + before);
        expectEquals(helper, 2, after.size(), "after events");
        expect(helper, after.get(0).entity() == mob && after.get(1).entity() == other, "initiator first");
        expect(helper, after.get(0).opponent().orElseThrow() == other, "the initiator's opponent");
        expect(
                helper,
                after.get(0).result().success() == contest.initiatorWins()
                        && after.get(1).result().success() != contest.initiatorWins(),
                "exactly the winner succeeds");
        expect(helper, after.get(0).result().dc().isEmpty(), "a contest side has no DC");
        helper.succeed();
    }

    /** With events switched off, listeners are not called and the roll is untouched. */
    public static void disabledEventsCallNoListener(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        List<String> calls = new ArrayList<>();
        SaveResult result = withExtensions(
                new ScoresConfig.ExtensionSettings(
                        false, ScoresConfig.DEFAULTS.extensions().bonusSources(), true, true, true),
                () -> withListeners(
                        event -> {
                            calls.add("before");
                            event.cancel();
                        },
                        event -> calls.add("after"),
                        () -> withDice(
                                () -> ChecksApi.check(mob, Ability.STRENGTH, 10).result(), 12)));
        expect(helper, calls.isEmpty(), "no listener may run, ran " + calls);
        expectEquals(helper, 12, result.natural(), "the roll still happens");
        helper.succeed();
    }
}
