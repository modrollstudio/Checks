package studio.modroll.checks.api;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.bonus.BonusSources;
import studio.modroll.checks.check.CheckEvents;
import studio.modroll.checks.check.CheckRolls;
import studio.modroll.checks.data.SkillStore;
import studio.modroll.checks.level.LevelRules;
import studio.modroll.checks.level.Levelling;
import studio.modroll.checks.rest.Rests;
import studio.modroll.checks.score.ScoreService;
import studio.modroll.critfall.api.dice.RollMode;

public final class ChecksApi {

    private ChecksApi() {}

    public static int abilityScore(LivingEntity entity, Ability ability) {
        return ScoreService.abilityScore(entity, ability);
    }

    public static int abilityModifier(LivingEntity entity, Ability ability) {
        return ScoreService.modifier(entity, ability);
    }

    public static int proficiencyBonus(LivingEntity entity) {
        return ScoreService.proficiencyBonus(entity);
    }

    public static Proficiency skillProficiency(LivingEntity entity, Skill skill) {
        return ScoreService.skillProficiency(entity, skill);
    }

    public static Proficiency saveProficiency(LivingEntity entity, Ability ability) {
        return ScoreService.saveProficiency(entity, ability);
    }

    public static int skillBonus(LivingEntity entity, Skill skill) {
        return ScoreService.skillBonus(entity, skill);
    }

    public static int skillModifier(LivingEntity entity, Skill skill) {
        return ScoreService.skillModifier(entity, skill);
    }

    public static int saveModifier(LivingEntity entity, Ability ability) {
        return ScoreService.saveModifier(entity, ability);
    }

    public static int passiveScore(LivingEntity entity, Skill skill) {
        return BonusSources.passiveScore(entity, skill);
    }

    public static int level(ServerPlayer player) {
        return Levelling.activeLevel(player).level();
    }

    public static int attacksPerAction(LivingEntity entity) {
        return entity instanceof ServerPlayer player ? Levelling.attacksPerAction(player) : LevelRules.BASE_ATTACKS;
    }

    public static int experience(ServerPlayer player) {
        return Levelling.activeLevel(player).xp();
    }

    public static void onLevelUp(Consumer<LevelUpEvent> listener) {
        Levelling.addListener(listener);
    }

    public static void addRestBlocker(Predicate<ServerPlayer> blocker) {
        Rests.addBlocker(blocker);
    }

    public static int hitDiceLeft(ServerPlayer player) {
        return Rests.hitDiceLeft(player);
    }

    public static void onBeforeCheck(Consumer<BeforeCheckEvent> listener) {
        CheckEvents.addBeforeListener(listener);
    }

    public static void onAfterCheck(Consumer<AfterCheckEvent> listener) {
        CheckEvents.addAfterListener(listener);
    }

    public static int modifier(LivingEntity entity, Stat stat) {
        return switch (stat) {
            case Ability ability -> abilityModifier(entity, ability);
            case Skill skill -> skillModifier(entity, skill);
        };
    }

    public static Optional<Skill> skill(ResourceLocation id) {
        return SkillStore.find(id);
    }

    public static OpenRoll roll(LivingEntity entity, Stat stat) {
        return roll(entity, stat, RollMode.NORMAL);
    }

    public static OpenRoll roll(LivingEntity entity, Stat stat, RollMode mode) {
        return CheckRolls.open(entity, stat, modifier(entity, stat), mode);
    }

    public static CheckRoll check(LivingEntity entity, Stat stat, int dc) {
        return check(entity, stat, dc, RollMode.NORMAL);
    }

    public static CheckRoll check(LivingEntity entity, Stat stat, int dc, RollMode mode) {
        return CheckRolls.againstDc(entity, stat, CheckKind.CHECK, modifier(entity, stat), dc, mode, Optional.empty());
    }

    public static CheckRoll savingThrow(LivingEntity entity, Ability ability, int dc) {
        return savingThrow(entity, ability, dc, RollMode.NORMAL);
    }

    public static CheckRoll savingThrow(LivingEntity entity, Ability ability, int dc, RollMode mode) {
        return CheckRolls.againstDc(
                entity, ability, CheckKind.SAVE, saveModifier(entity, ability), dc, mode, Optional.empty());
    }

    public static ContestRoll contest(
            LivingEntity initiator, Stat initiatorStat, LivingEntity opponent, Stat opponentStat) {
        return contest(initiator, initiatorStat, RollMode.NORMAL, opponent, opponentStat, RollMode.NORMAL);
    }

    public static ContestRoll contest(
            LivingEntity initiator,
            Stat initiatorStat,
            RollMode initiatorMode,
            LivingEntity opponent,
            Stat opponentStat,
            RollMode opponentMode) {
        return CheckRolls.contest(
                initiator,
                initiatorStat,
                modifier(initiator, initiatorStat),
                initiatorMode,
                opponent,
                opponentStat,
                modifier(opponent, opponentStat),
                opponentMode);
    }

    public static CheckRoll checkAgainstPassive(
            LivingEntity actor, Stat stat, LivingEntity target, Skill passiveSkill) {
        return checkAgainstPassive(actor, stat, target, passiveSkill, RollMode.NORMAL);
    }

    public static CheckRoll checkAgainstPassive(
            LivingEntity actor, Stat stat, LivingEntity target, Skill passiveSkill, RollMode mode) {
        return CheckRolls.againstDc(
                actor,
                stat,
                CheckKind.CHECK,
                modifier(actor, stat),
                passiveScore(target, passiveSkill),
                mode,
                Optional.of(target));
    }
}
