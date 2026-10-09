package studio.modroll.checks.fabric.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import studio.modroll.checks.save.VanillaSaves;

/**
 * Fabric API has no explosion knockback event; NeoForge fires ExplosionKnockbackEvent at this spot. The
 * push is changed where it is stored, so both the entity's motion and the knockback sent to a player's
 * client use it.
 */
@Mixin(Explosion.class)
abstract class ExplosionMixin {

    @ModifyVariable(method = "explode", at = @At("STORE"), ordinal = 1)
    private Vec3 checks$knockbackSave(Vec3 push, @Local Entity entity) {
        return VanillaSaves.explosionKnockback(entity, push);
    }
}
