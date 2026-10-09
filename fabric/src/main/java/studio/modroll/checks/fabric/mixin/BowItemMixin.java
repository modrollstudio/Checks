package studio.modroll.checks.fabric.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import studio.modroll.checks.body.BodyHooks;

/** Fabric API has no bow release event; NeoForge's ArrowLooseEvent changes the same draw time. */
@Mixin(BowItem.class)
abstract class BowItemMixin {

    @ModifyArg(
            method = "releaseUsing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"))
    private int checks$dexDraw(int charge, @Local(argsOnly = true) LivingEntity archer) {
        return BodyHooks.bowCharge(archer, charge);
    }
}
