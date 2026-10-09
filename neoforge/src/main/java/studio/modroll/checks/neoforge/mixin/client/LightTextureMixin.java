package studio.modroll.checks.neoforge.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import studio.modroll.checks.trait.client.ClientDarkvision;

/**
 * Neither loader has a lightmap event. Darkvision reuses night vision's brightening at a lower strength:
 * the lightmap runs it whenever darkvision is on, at the stronger of the two.
 */
@Mixin(LightTexture.class)
abstract class LightTextureMixin {

    @ModifyExpressionValue(
            method = "updateLightTexture",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z",
                            ordinal = 0))
    private boolean checks$darkvisionOn(boolean nightVision) {
        return nightVision || ClientDarkvision.active();
    }

    @WrapOperation(
            method = "updateLightTexture",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/GameRenderer;getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"))
    private float checks$darkvisionScale(LivingEntity player, float partialTick, Operation<Float> original) {
        float nightVision = player.hasEffect(MobEffects.NIGHT_VISION) ? original.call(player, partialTick) : 0f;
        return ClientDarkvision.scale(nightVision);
    }
}
