package studio.modroll.checks.neoforge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import studio.modroll.checks.body.BodyHooks;

/**
 * No event lets a mod change how long a crossbow takes to load. Every use of the load time (the
 * charge, its sounds, the use duration) reads it here, so DEX shortens all of them alike.
 */
@Mixin(CrossbowItem.class)
abstract class CrossbowItemMixin {

    @ModifyReturnValue(method = "getChargeDuration", at = @At("RETURN"))
    private static int checks$dexReload(int duration, ItemStack crossbow, LivingEntity shooter) {
        return BodyHooks.crossbowChargeDuration(shooter, duration);
    }
}
