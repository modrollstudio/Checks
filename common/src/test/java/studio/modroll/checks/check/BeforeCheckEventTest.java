package studio.modroll.checks.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.BeforeCheckEvent;
import studio.modroll.checks.api.CheckKind;
import studio.modroll.checks.api.CheckResult;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.critfall.api.dice.RollMode;

class BeforeCheckEventTest {

    @AfterEach
    void reset() {
        CheckEvents.clearListeners();
        ScoresRuntime.setConfig(ScoresConfig.DEFAULTS);
    }

    private static BeforeCheckEvent event(OptionalInt dc, boolean advantage, boolean disadvantage) {
        return new BeforeCheckEvent(
                null, Ability.DEXTERITY, CheckKind.CHECK, Optional.empty(), dc, 2, 1, advantage, disadvantage);
    }

    @Test
    void advantageAndDisadvantageCancelOutHoweverManyOfEach() {
        BeforeCheckEvent event = event(OptionalInt.of(10), false, false);
        assertEquals(RollMode.NORMAL, event.mode());

        event.grantAdvantage();
        event.grantAdvantage();
        assertEquals(RollMode.ADVANTAGE, event.mode());

        event.imposeDisadvantage();
        assertEquals(RollMode.NORMAL, event.mode());
        assertTrue(event.hasAdvantage());
        assertTrue(event.hasDisadvantage());
    }

    @Test
    void disadvantageAloneRollsWithDisadvantage() {
        assertEquals(
                RollMode.DISADVANTAGE, event(OptionalInt.of(10), false, true).mode());
    }

    @Test
    void listenersChangeTheDcAndAddToTheBonus() {
        BeforeCheckEvent event = event(OptionalInt.of(10), false, false);

        event.dc(14);
        event.addBonus(3);
        event.addBonus(-1);

        assertEquals(OptionalInt.of(14), event.dc());
        assertEquals(2, event.modifier());
        assertEquals(3, event.bonus());
    }

    @Test
    void aRollWithoutADcCannotBeGivenOne() {
        assertThrows(IllegalStateException.class, () -> event(OptionalInt.empty(), false, false)
                .dc(10));
    }

    @Test
    void cancelIsRemembered() {
        BeforeCheckEvent event = event(OptionalInt.of(10), false, false);
        assertFalse(event.isCanceled());

        event.cancel();

        assertTrue(event.isCanceled());
    }

    @Test
    void aThrowingListenerIsSkippedAndTheRestStillRun() {
        List<String> calls = new ArrayList<>();
        CheckEvents.addBeforeListener(event -> {
            calls.add("first");
            throw new IllegalStateException("bad script");
        });
        CheckEvents.addBeforeListener(event -> calls.add("second"));

        CheckEvents.fireBefore(event(OptionalInt.of(10), false, false));

        assertEquals(List.of("first", "second"), calls);
    }

    @Test
    void disabledEventsCallNoListener() {
        List<String> calls = new ArrayList<>();
        CheckEvents.addBeforeListener(event -> calls.add("called"));
        ScoresConfig defaults = ScoresConfig.DEFAULTS;
        ScoresRuntime.setConfig(new ScoresConfig(
                defaults.playerDefaults(),
                defaults.derivation(),
                defaults.profilesEnabled(),
                defaults.skillsEnabled(),
                defaults.proficiency(),
                defaults.critfall(),
                defaults.statScreenEnabled(),
                defaults.creation(),
                defaults.levelling(),
                new ScoresConfig.ExtensionSettings(
                        false, ScoresConfig.DEFAULTS.extensions().bonusSources(), true, true, true),
                defaults.body(),
                defaults.saves(),
                defaults.traits(),
                defaults.social(),
                defaults.rollMessages(),
                defaults.deathMessages(),
                defaults.exploration()));

        CheckEvents.fireBefore(event(OptionalInt.of(10), false, false));

        assertEquals(List.of(), calls);
    }

    @Test
    void resultNamesNaturalOnesAndTwenties() {
        CheckResult twenty = new CheckResult(RollDetail.normal(20), 2, 0, 22, OptionalInt.of(25), false);
        CheckResult one = new CheckResult(RollDetail.normal(1), 2, 0, 3, OptionalInt.empty(), false);

        assertTrue(twenty.isNatural20());
        assertFalse(twenty.isNatural1());
        assertTrue(one.isNatural1());
        assertEquals(RollMode.NORMAL, one.mode());
        assertEquals(1, one.natural());
    }
}
