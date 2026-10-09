package studio.modroll.checks.fabric.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import studio.modroll.checks.save.VanillaSaves;

/** Neither loader has an ignite event; fire, fire aspect, burning arrows and lava all set entities alight here. */
@Mixin(Entity.class)
abstract class EntityMixin {

    @ModifyVariable(method = "igniteForTicks", at = @At("HEAD"), argsOnly = true)
    private int checks$fireSave(int ticks) {
        return VanillaSaves.igniteTicks((Entity) (Object) this, ticks);
    }
}
