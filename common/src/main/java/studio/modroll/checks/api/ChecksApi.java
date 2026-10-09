package studio.modroll.checks.api;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.bonus.BonusSources;
import studio.modroll.checks.check.CheckEvents;
import studio.modroll.checks.check.CheckRolls;
import studio.modroll.checks.data.SkillStore;
import studio.modroll.checks.level.Levelling;
import studio.modroll.checks.score.ScoreService;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.dice.RollMode;

/**
 * The public entry point for Checks: read an entity's scores and modifiers, and roll checks, saves
 * and contests on them. Checks only supplies modifiers; every die is rolled by Critfall's {@link
 * RollService}, so {@link RollService#setRoller} forces the faces here too. Call it with server-side
 * entities only.
 *
 * <p>Every roll adds the entity's datapack bonus sources and fires {@link #onBeforeCheck} and {@link
 * #onAfterCheck}, and returns Critfall's result wrapped with what only Checks knows: whether a listener
 * canceled the roll, and for an open roll the mode it was rolled with. A canceled roll rolls nothing.
 */
public final class ChecksApi {

    private ChecksApi() {}

    /** The effective score, {@code 1..30}. */
    public static int abilityScore(LivingEntity entity, Ability ability) {
        return ScoreService.abilityScore(entity, ability);
    }

    /** {@code floor((score - 10) / 2)}. */
    public static int abilityModifier(LivingEntity entity, Ability ability) {
        return ScoreService.modifier(entity, ability);
    }

    /** The entity's proficiency bonus, {@code 0..10}, or 0 while proficiency is disabled. */
    public static int proficiencyBonus(LivingEntity entity) {
        return ScoreService.proficiencyBonus(entity);
    }

    /** How proficient the entity is in {@code skill}; always none while proficiency is disabled. */
    public static Proficiency skillProficiency(LivingEntity entity, Skill skill) {
        return ScoreService.skillProficiency(entity, skill);
    }

    /** How proficient the entity is in the {@code ability} saving throw: none or proficient. */
    public static Proficiency saveProficiency(LivingEntity entity, Ability ability) {
        return ScoreService.saveProficiency(entity, ability);
    }

    /** The flat skill bonus, {@code -30..30}, or 0 while skills are disabled. */
    public static int skillBonus(LivingEntity entity, Skill skill) {
        return ScoreService.skillBonus(entity, skill);
    }

    /**
     * Governing ability modifier + proficiency bonus × (0, 1 or 2) + flat skill bonus. The flat bonus
     * is 0 while skills are disabled.
     */
    public static int skillModifier(LivingEntity entity, Skill skill) {
        return ScoreService.skillModifier(entity, skill);
    }

    /** Ability modifier, plus the proficiency bonus when the entity is proficient in this save. */
    public static int saveModifier(LivingEntity entity, Ability ability) {
        return ScoreService.saveModifier(entity, ability);
    }

    /**
     * {@code 10 +} the skill modifier, plus the entity's bonus sources on that skill's checks: their flat
     * bonuses, and the configured passive bonus for advantage or penalty for disadvantage (5 by default;
     * both cancel out).
     */
    public static int passiveScore(LivingEntity entity, Skill skill) {
        return BonusSources.passiveScore(entity, skill);
    }

    /** The player's character level, {@code 1..20}; always 1 while levelling is disabled. */
    public static int level(ServerPlayer player) {
        return Levelling.activeLevel(player).level();
    }

    /** The player's total character XP; always 0 while levelling is disabled. */
    public static int experience(ServerPlayer player) {
        return Levelling.activeLevel(player).xp();
    }

    /**
     * Calls {@code listener} on the server thread for each character level a player gains, after the new
     * level is stored. Register once, during mod construction.
     */
    public static void onLevelUp(Consumer<LevelUpEvent> listener) {
        Levelling.addListener(listener);
    }

    /**
     * Calls {@code listener} on the server thread before every check, save and contest side rolled
     * through this class; it may change the DC, add a bonus, grant advantage, impose disadvantage or
     * cancel. A listener that throws is logged and skipped. Register once, during mod construction.
     */
    public static void onBeforeCheck(Consumer<BeforeCheckEvent> listener) {
        CheckEvents.addBeforeListener(listener);
    }

    /** Calls {@code listener} on the server thread with the result of every roll made through this class. */
    public static void onAfterCheck(Consumer<AfterCheckEvent> listener) {
        CheckEvents.addAfterListener(listener);
    }

    /**
     * The modifier a check on {@code stat} adds: its ability modifier or its skill modifier. Bonus
     * sources are not included; they only apply when a check is rolled.
     */
    public static int modifier(LivingEntity entity, Stat stat) {
        return switch (stat) {
            case Ability ability -> abilityModifier(entity, ability);
            case Skill skill -> skillModifier(entity, skill);
        };
    }

    /**
     * A loaded skill; a namespace-less id ({@code stealth}) means the {@code checks:} skill. Skills
     * change on {@code /reload}, so look one up when needed instead of keeping it.
     */
    public static Optional<Skill> skill(ResourceLocation id) {
        return SkillStore.find(id);
    }

    /**
     * A check with no DC: d20 + modifier. The result's {@code modifier} is the check modifier plus bonus
     * sources and listener bonuses; its {@code mode} is the one rolled, which may differ from the one asked.
     */
    public static OpenRoll roll(LivingEntity entity, Stat stat) {
        return roll(entity, stat, RollMode.NORMAL);
    }

    public static OpenRoll roll(LivingEntity entity, Stat stat, RollMode mode) {
        return CheckRolls.open(entity, stat, modifier(entity, stat), mode);
    }

    /** An ability or skill check: d20 + modifier against {@code dc}; meeting the DC succeeds. */
    public static CheckRoll check(LivingEntity entity, Stat stat, int dc) {
        return check(entity, stat, dc, RollMode.NORMAL);
    }

    public static CheckRoll check(LivingEntity entity, Stat stat, int dc, RollMode mode) {
        return CheckRolls.againstDc(entity, stat, CheckKind.CHECK, modifier(entity, stat), dc, mode, Optional.empty());
    }

    /** d20 + {@link #saveModifier save modifier} against {@code dc}; meeting the DC saves. */
    public static CheckRoll savingThrow(LivingEntity entity, Ability ability, int dc) {
        return savingThrow(entity, ability, dc, RollMode.NORMAL);
    }

    public static CheckRoll savingThrow(LivingEntity entity, Ability ability, int dc, RollMode mode) {
        return CheckRolls.againstDc(
                entity, ability, CheckKind.SAVE, saveModifier(entity, ability), dc, mode, Optional.empty());
    }

    /** Each side rolls d20 + its own stat's modifier; ties go to the opponent. */
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

    /** An active check with the target's passive score as the DC; the target never rolls. */
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
