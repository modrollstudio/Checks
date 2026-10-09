package studio.modroll.checks.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import studio.modroll.checks.exploration.Ailments;
import studio.modroll.checks.exploration.Landings;
import studio.modroll.checks.exploration.Sneaking;
import studio.modroll.checks.save.VanillaSaves;
import studio.modroll.checks.trait.TraitHooks;

/**
 * Fabric API has no event that changes incoming damage, knockback strength or an effect's duration;
 * NeoForge fires LivingIncomingDamageEvent and LivingKnockBackEvent at these spots, and neither loader
 * has an event for the effect duration. Every goal and brain targeting check asks {@code canAttack}, which
 * also makes a mob drop a target it may no longer attack; NeoForge's target event covers only new targets.
 * Fabric API has no visibility event either; NeoForge fires LivingVisibilityEvent where this one returns.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float checks$resistanceSavesAndLanding(float amount, @Local(argsOnly = true) DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        float saved = VanillaSaves.explosionDamage(self, source, TraitHooks.incomingDamage(self, source, amount));
        return Landings.fallDamage(self, source, saved);
    }

    @ModifyReturnValue(method = "getVisibilityPercent", at = @At("RETURN"))
    private double checks$stealth(double visibility, @Local(argsOnly = true) Entity looker) {
        return visibility * Sneaking.visibility((LivingEntity) (Object) this, looker);
    }

    @ModifyReturnValue(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"))
    private boolean checks$ignoredBy(boolean canAttack, @Local(argsOnly = true) LivingEntity target) {
        return canAttack && TraitHooks.canTarget((LivingEntity) (Object) this, target);
    }

    @ModifyVariable(method = "knockback", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double checks$knockbackSave(double strength) {
        return VanillaSaves.knockback((LivingEntity) (Object) this, strength);
    }

    @ModifyVariable(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            argsOnly = true)
    private MobEffectInstance checks$effectSaveAndMedicine(MobEffectInstance effect) {
        LivingEntity self = (LivingEntity) (Object) this;
        return Ailments.effect(self, VanillaSaves.effect(self, effect));
    }
}
