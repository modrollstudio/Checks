package studio.modroll.checks.neoforge.mixin.client;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import studio.modroll.checks.exploration.client.ClientClimbing;

/** Neither loader has a climbing event, and climbing speed is no attribute. */
@Mixin(LivingEntity.class)
abstract class LivingEntityClimbingMixin {

    @ModifyConstant(method = "handleRelativeFrictionAndCalculateMovement", constant = @Constant(doubleValue = 0.2))
    private double checks$athleticsClimbSpeed(double vanilla) {
        return ClientClimbing.climbSpeed((LivingEntity) (Object) this, vanilla);
    }
}
