package studio.modroll.checks.neoforge.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import studio.modroll.checks.exploration.Taming;

/** As {@link WolfMixin}: a cat tames on a draw of 0. */
@Mixin(Cat.class)
abstract class CatMixin {

    @ModifyExpressionValue(
            method = "tryToTame",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int checks$animalHandling(int draw, @Local(argsOnly = true) Player tamer) {
        return Taming.made(tamer) ? 0 : draw;
    }
}
