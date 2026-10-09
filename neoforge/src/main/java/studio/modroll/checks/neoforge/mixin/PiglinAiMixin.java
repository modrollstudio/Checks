package studio.modroll.checks.neoforge.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.modroll.checks.social.Socials;

/** Neither loader has a hook for whether piglins see gold on an entity; a Deception disguise counts as gold. */
@Mixin(PiglinAi.class)
abstract class PiglinAiMixin {

    @Inject(method = "isWearingGold", at = @At("HEAD"), cancellable = true)
    private static void checks$disguise(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Socials.disguised(entity)) {
            cir.setReturnValue(true);
        }
    }
}
