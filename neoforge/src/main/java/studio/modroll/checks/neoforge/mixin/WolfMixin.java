package studio.modroll.checks.neoforge.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import studio.modroll.checks.exploration.Taming;

/**
 * No event lets a mod decide a taming attempt: NeoForge's AnimalTameEvent fires only once vanilla's own
 * chance came up, and can only cancel it. A wolf tames on a draw of 0, so a made Animal Handling check
 * draws 0.
 */
@Mixin(Wolf.class)
abstract class WolfMixin {

    @ModifyExpressionValue(
            method = "tryToTame",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int checks$animalHandling(int draw, @Local(argsOnly = true) Player tamer) {
        return Taming.made(tamer) ? 0 : draw;
    }
}
