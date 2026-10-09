package studio.modroll.checks.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.commands.EffectCommands;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import studio.modroll.checks.save.VanillaSaves;

/** Neither loader tells an effect's cause; {@code /effect give}, from chat or a function, adds effects here. */
@Mixin(EffectCommands.class)
abstract class EffectCommandsMixin {

    @WrapOperation(
            method = "giveEffect",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean checks$noSaveFromCommands(
            LivingEntity entity, MobEffectInstance effect, Entity source, Operation<Boolean> original) {
        return VanillaSaves.fromCommand(() -> original.call(entity, effect, source));
    }
}
