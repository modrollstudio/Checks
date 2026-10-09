package studio.modroll.checks.check;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.api.AfterCheckEvent;
import studio.modroll.checks.api.BeforeCheckEvent;
import studio.modroll.checks.api.CheckKind;
import studio.modroll.checks.api.CheckResult;
import studio.modroll.checks.api.CheckRoll;
import studio.modroll.checks.api.ContestRoll;
import studio.modroll.checks.api.OpenRoll;
import studio.modroll.checks.api.Stat;
import studio.modroll.checks.bonus.BonusSources;
import studio.modroll.checks.bonus.SituationalBonus;
import studio.modroll.checks.trait.TraitHooks;
import studio.modroll.critfall.api.ContestContext;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.ContestResult;
import studio.modroll.critfall.api.combat.SaveResult;
import studio.modroll.critfall.api.dice.DiceExpression;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.critfall.api.dice.RollResult;

/**
 * Every roll {@link studio.modroll.checks.api.ChecksApi} makes: the stat's modifier plus the entity's bonus
 * sources, then the before event, Critfall's roll and the after event. A canceled roll rolls nothing. A trait
 * may roll a check or save once more; contests are never rerolled, since Critfall rolls both sides at once.
 */
public final class CheckRolls {

    private static final int NOT_ROLLED = 0;

    private CheckRolls() {}

    public static CheckRoll againstDc(
            LivingEntity entity,
            Stat stat,
            CheckKind kind,
            int modifier,
            int dc,
            RollMode mode,
            Optional<LivingEntity> opponent) {
        BeforeCheckEvent before = before(entity, stat, kind, opponent, OptionalInt.of(dc), modifier, mode);
        if (before.isCanceled()) {
            return new CheckRoll(canceledSave(dc), true);
        }
        int finalDc = before.dc().getAsInt();
        SaveResult result = RollService.savingThrow(entity, total(before), finalDc, before.mode());
        if (TraitHooks.reroll(entity, result.natural(), Optional.of(!result.saved()))
                .isPresent()) {
            result = RollService.savingThrow(entity, total(before), finalDc, before.mode());
        }
        after(before, result.roll(), result.saveTotal(), OptionalInt.of(finalDc), result.saved());
        return new CheckRoll(result, false);
    }

    public static OpenRoll open(LivingEntity entity, Stat stat, int modifier, RollMode mode) {
        BeforeCheckEvent before =
                before(entity, stat, CheckKind.CHECK, Optional.empty(), OptionalInt.empty(), modifier, mode);
        if (before.isCanceled()) {
            return new OpenRoll(new RollResult(NOT_ROLLED, List.of(), NOT_ROLLED), before.mode(), true);
        }
        DiceExpression dice = before.mode().d20Expression().plus(DiceExpression.parse(Integer.toString(total(before))));
        RollResult result = RollService.roll(dice);
        if (TraitHooks.reroll(entity, RollDetail.of(before.mode(), result).kept(), Optional.empty())
                .isPresent()) {
            result = RollService.roll(dice);
        }
        RollDetail roll = RollDetail.of(before.mode(), result);
        after(before, roll, result.total(), OptionalInt.empty(), false);
        return new OpenRoll(result, before.mode(), false);
    }

    /** Both sides' events fire before either rolls, initiator first; canceling either cancels the contest. */
    public static ContestRoll contest(
            LivingEntity initiator,
            Stat initiatorStat,
            int initiatorModifier,
            RollMode initiatorMode,
            LivingEntity opponent,
            Stat opponentStat,
            int opponentModifier,
            RollMode opponentMode) {
        BeforeCheckEvent first = contestSide(initiator, initiatorStat, initiatorModifier, initiatorMode, opponent);
        if (first.isCanceled()) {
            return canceledContest();
        }
        BeforeCheckEvent second = contestSide(opponent, opponentStat, opponentModifier, opponentMode, initiator);
        if (second.isCanceled()) {
            return canceledContest();
        }
        ContestResult result = RollService.contest(
                initiator, opponent, new ContestContext(total(first), first.mode(), total(second), second.mode()));
        after(first, result.initiatorRoll(), result.initiatorTotal(), OptionalInt.empty(), result.initiatorWins());
        after(second, result.opponentRoll(), result.opponentTotal(), OptionalInt.empty(), !result.initiatorWins());
        return new ContestRoll(result, false);
    }

    private static BeforeCheckEvent contestSide(
            LivingEntity entity, Stat stat, int modifier, RollMode mode, LivingEntity other) {
        return before(entity, stat, CheckKind.CONTEST, Optional.of(other), OptionalInt.empty(), modifier, mode);
    }

    private static BeforeCheckEvent before(
            LivingEntity entity,
            Stat stat,
            CheckKind kind,
            Optional<LivingEntity> opponent,
            OptionalInt dc,
            int modifier,
            RollMode mode) {
        SituationalBonus situational = BonusSources.forCheck(entity, kind, stat);
        BeforeCheckEvent event = new BeforeCheckEvent(
                entity,
                stat,
                kind,
                opponent,
                dc,
                modifier,
                situational.bonus(),
                mode == RollMode.ADVANTAGE || situational.advantage(),
                mode == RollMode.DISADVANTAGE || situational.disadvantage());
        CheckEvents.fireBefore(event);
        return event;
    }

    private static void after(BeforeCheckEvent before, RollDetail roll, int total, OptionalInt dc, boolean success) {
        CheckResult result = new CheckResult(roll, before.modifier(), before.bonus(), total, dc, success);
        CheckEvents.fireAfter(
                new AfterCheckEvent(before.entity(), before.stat(), before.kind(), before.opponent(), result));
    }

    private static int total(BeforeCheckEvent event) {
        return event.modifier() + event.bonus();
    }

    /** Below the DC even for a DC of 0 or less, so a canceled check never succeeds. */
    private static SaveResult canceledSave(int dc) {
        return new SaveResult(NOT_ROLLED, Math.min(NOT_ROLLED, dc - 1), dc);
    }

    /** All zero, so the tie goes to the opponent. */
    private static ContestRoll canceledContest() {
        return new ContestRoll(new ContestResult(NOT_ROLLED, NOT_ROLLED, NOT_ROLLED, NOT_ROLLED), true);
    }
}
