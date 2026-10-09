package studio.modroll.checks.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import studio.modroll.checks.level.Levelling;

/**
 * No event reports the points Mending spends from a collected orb; they never reach the player's XP
 * bar. Counting them here, and the remainder where it reaches the bar, credits the orb's full value once.
 */
@Mixin(ExperienceOrb.class)
abstract class ExperienceOrbMixin {

    @WrapOperation(
            method = "playerTouch",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/ExperienceOrb;repairPlayerItems(Lnet/minecraft/server/level/ServerPlayer;I)I"))
    private int checks$countMendingXp(
            ExperienceOrb orb, ServerPlayer player, int points, Operation<Integer> repairPlayerItems) {
        int remainder = repairPlayerItems.call(orb, player, points);
        Levelling.onVanillaXp(player, points - remainder);
        return remainder;
    }
}
