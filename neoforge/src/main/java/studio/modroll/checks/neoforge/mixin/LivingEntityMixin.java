package studio.modroll.checks.neoforge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import studio.modroll.checks.exploration.Ailments;
import studio.modroll.checks.save.VanillaSaves;
import studio.modroll.checks.trait.TraitHooks;

/**
 * NeoForge's mob effect events cannot change an effect's duration; every effect added passes through here.
 * LivingChangeTargetEvent fires only when a target is set, while every goal and brain targeting check asks
 * {@code canAttack}, which also makes a mob drop a target it may no longer attack, as on Fabric.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {

    @ModifyVariable(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            argsOnly = true)
    private MobEffectInstance checks$effectSaveAndMedicine(MobEffectInstance effect) {
        LivingEntity self = (LivingEntity) (Object) this;
        return Ailments.effect(self, VanillaSaves.effect(self, effect));
    }

    @ModifyReturnValue(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"))
    private boolean checks$ignoredBy(boolean canAttack, @Local(argsOnly = true) LivingEntity target) {
        return canAttack && TraitHooks.canTarget((LivingEntity) (Object) this, target);
    }
}
