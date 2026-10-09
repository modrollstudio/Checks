package studio.modroll.checks.exploration;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.Checks;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.save.SaveRules;
import studio.modroll.checks.save.VanillaSaves;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * Medicine, passively and with no roll: a positive Medicine modifier makes the effects in the
 * {@code checks:shortened_by_medicine} tag (poison, wither, hunger and nausea) wear off sooner on a player.
 * Effects a command gives, infinite ones, and effects on anything but a server-side player are left unchanged.
 */
public final class Ailments {

    private static final ResourceLocation MEDICINE = Checks.id("medicine");
    private static final TagKey<MobEffect> AILMENTS =
            TagKey.create(Registries.MOB_EFFECT, Checks.id("shortened_by_medicine"));

    private Ailments() {}

    /** Returns {@code effect} itself when nothing changes. */
    public static MobEffectInstance effect(LivingEntity entity, MobEffectInstance effect) {
        ScoresConfig.ReductionSettings settings =
                ScoresRuntime.config().exploration().ailments();
        if (!settings.enabled()
                || !(entity instanceof ServerPlayer player)
                || !effect.getEffect().is(AILMENTS)
                || effect.isInfiniteDuration()
                || VanillaSaves.givenByCommand()) {
            return effect;
        }
        int duration = duration(effect.getDuration(), ExplorationChecks.modifier(player, MEDICINE), settings);
        if (duration == effect.getDuration()) {
            return effect;
        }
        return new MobEffectInstance(
                effect.getEffect(),
                duration,
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon());
    }

    /** {@code ticks} less the Medicine share, rounded down. */
    static int duration(int ticks, int medicineModifier, ScoresConfig.ReductionSettings settings) {
        double reduction = ExplorationChecks.share(medicineModifier, settings.perPoint(), settings.maxReduction());
        return SaveRules.scaledTicks(ticks, 1 - reduction);
    }
}
