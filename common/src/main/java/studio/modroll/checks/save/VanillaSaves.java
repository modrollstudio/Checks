package studio.modroll.checks.save;

import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.api.CheckRoll;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.check.RollMessages;
import studio.modroll.checks.check.RollText;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.death.DeathStories;
import studio.modroll.checks.death.DeathStory;
import studio.modroll.checks.score.ScoreService;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.text.FallbackText;
import studio.modroll.checks.trait.TraitHooks;
import studio.modroll.critfall.api.combat.SaveResult;

/**
 * Saving throws against vanilla hazards, called from where each loader hooks them. A made save multiplies
 * the damage, effect duration, knockback or burn time by its configured success multiplier. Players in
 * survival or adventure save, and mobs with an entity profile when that is switched on; anything else,
 * and anything client-side, is left unchanged.
 */
public final class VanillaSaves {

    private static final double UNCHANGED = 1.0;
    private static final SaveCooldowns COOLDOWNS = new SaveCooldowns();
    private static final Map<Holder<MobEffect>, VanillaSave> EFFECT_SAVES = Map.of(
            MobEffects.POISON, VanillaSave.POISON,
            MobEffects.WITHER, VanillaSave.POISON,
            MobEffects.HUNGER, VanillaSave.POISON,
            MobEffects.DARKNESS, VanillaSave.DARKNESS);

    private static final ThreadLocal<Boolean> FROM_COMMAND = ThreadLocal.withInitial(() -> false);

    private VanillaSaves() {}

    /** Runs {@code addEffect} for a command or function: an effect a command gives never calls for a save. */
    public static boolean fromCommand(BooleanSupplier addEffect) {
        FROM_COMMAND.set(true);
        try {
            return addEffect.getAsBoolean();
        } finally {
            FROM_COMMAND.set(false);
        }
    }

    /** Whether the effect being added right now comes from a command or function. */
    public static boolean givenByCommand() {
        return FROM_COMMAND.get();
    }

    /** DEX: explosion damage. */
    public static float explosionDamage(LivingEntity entity, DamageSource source, float amount) {
        if (amount <= 0
                || !source.is(DamageTypeTags.IS_EXPLOSION)
                || entity.isInvulnerableTo(source)
                || entity.isDeadOrDying()) {
            return amount;
        }
        return (float) (amount * factor(entity, VanillaSave.EXPLOSION));
    }

    /**
     * CON: poison, wither and hunger; WIS: darkness. Effects from mobs, potions, food and the world call for
     * a save, those a command gives do not. Returns {@code effect} itself when nothing changes.
     */
    public static MobEffectInstance effect(LivingEntity entity, MobEffectInstance effect) {
        Optional<VanillaSave> save = Optional.ofNullable(EFFECT_SAVES.get(effect.getEffect()));
        if (save.isEmpty() || effect.isInfiniteDuration() || givenByCommand()) {
            return effect;
        }
        double factor = factor(entity, save.get());
        if (factor == UNCHANGED) {
            return effect;
        }
        return new MobEffectInstance(
                effect.getEffect(),
                SaveRules.scaledTicks(effect.getDuration(), factor),
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon());
    }

    /** STR: a hit's knockback, when it is above the configured minimum strength. */
    public static double knockback(LivingEntity entity, double strength) {
        double minStrength = ScoresRuntime.config().saves().knockbackMinStrength();
        if (!SaveRules.bigKnockback(strength, minStrength)) {
            return strength;
        }
        return strength * factor(entity, VanillaSave.KNOCKBACK);
    }

    /** STR: an explosion's or wind charge's push. */
    public static Vec3 explosionKnockback(Entity entity, Vec3 push) {
        if (!(entity instanceof LivingEntity living) || push.lengthSqr() == 0) {
            return push;
        }
        return push.scale(factor(living, VanillaSave.KNOCKBACK));
    }

    /** DEX: catching fire, unless in lava or the ignition would not change the burn time anyway. */
    public static int igniteTicks(Entity entity, int ticks) {
        if (!(entity instanceof LivingEntity living)
                || ticks <= entity.getRemainingFireTicks()
                || entity.fireImmune()
                || inLava(entity)) {
            return ticks;
        }
        return SaveRules.scaledTicks(ticks, factor(living, VanillaSave.FIRE));
    }

    public static void clear() {
        COOLDOWNS.clear();
    }

    private static double factor(LivingEntity entity, VanillaSave save) {
        ScoresConfig.SaveSettings settings = ScoresRuntime.config().saves();
        ScoresConfig.SaveSetting setting = settings.save(save);
        if (!setting.enabled() || !makesSaves(entity, settings)) {
            return UNCHANGED;
        }
        return saved(entity, save, setting, settings.cooldownTicks()) ? setting.successMultiplier() : UNCHANGED;
    }

    private static boolean makesSaves(LivingEntity entity, ScoresConfig.SaveSettings settings) {
        if (entity.level().isClientSide()) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return settings.profiledMobs() && ScoreService.hasProfile(entity);
    }

    /** A save still cooling down reuses its result; a canceled roll counts as failed and starts no cooldown. */
    private static boolean saved(
            LivingEntity entity, VanillaSave save, ScoresConfig.SaveSetting setting, int cooldown) {
        long now = entity.getServer().getTickCount();
        Optional<Boolean> recent = COOLDOWNS.recent(save, entity.getUUID(), now);
        if (recent.isPresent()) {
            return recent.get();
        }
        CheckRoll roll =
                ChecksApi.savingThrow(entity, save.ability(), setting.dc(), TraitHooks.vanillaSaveMode(entity, save));
        if (roll.canceled()) {
            return false;
        }
        SaveResult result = roll.result();
        COOLDOWNS.start(save, entity.getUUID(), now, cooldown, result.saved());
        if (entity instanceof ServerPlayer player) {
            announce(player, save, result);
            if (save == VanillaSave.EXPLOSION && !result.saved()) {
                DeathStories.record(player, DeathStory.FAILED_EXPLOSION_SAVE);
            }
        }
        return result.saved();
    }

    private static void announce(ServerPlayer player, VanillaSave save, SaveResult result) {
        RollMessages.showResult(
                player,
                FallbackText.of(
                        result.saved() ? "checks.save.success" : "checks.save.failure",
                        SheetText.statName(save.ability()),
                        FallbackText.of("checks.save." + save.id()),
                        result.dc(),
                        RollText.d20(result.roll()),
                        SheetText.signed(result.saveBonus()),
                        result.saveTotal()));
    }

    /** Lava sets entities alight through the same call as fire; standing in it, or in a lava cauldron, never saves. */
    private static boolean inLava(Entity entity) {
        return entity.isInLava()
                || entity.level()
                        .getBlockStatesIfLoaded(entity.getBoundingBox())
                        .anyMatch(VanillaSaves::isLava);
    }

    private static boolean isLava(BlockState state) {
        return state.getFluidState().is(FluidTags.LAVA) || state.is(Blocks.LAVA_CAULDRON);
    }
}
