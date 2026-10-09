package studio.modroll.checks.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import studio.modroll.checks.exploration.Taming;

/**
 * As {@link WolfMixin}: a horse, donkey, mule or llama with a player on its back decides now and
 * then whether to accept them, tamed when a draw falls below its temper, so a made Animal Handling check
 * counts its temper as full.
 */
@Mixin(RunAroundLikeCrazyGoal.class)
abstract class RunAroundLikeCrazyGoalMixin {

    @Shadow
    @Final
    private AbstractHorse horse;

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/horse/AbstractHorse;getTemper()I"))
    private int checks$animalHandling(int temper) {
        return Taming.made(horse.getFirstPassenger()) ? Integer.MAX_VALUE : temper;
    }
}
