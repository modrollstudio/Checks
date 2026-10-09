package studio.modroll.checks.neoforge.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import studio.modroll.checks.body.BodyHooks;
import studio.modroll.checks.exploration.Hunger;

/** NeoForge has no food exhaustion event; every exhaustion a player gains passes through here. */
@Mixin(Player.class)
abstract class PlayerMixin {

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float checks$conAndSurvivalExhaustion(float exhaustion) {
        Player self = (Player) (Object) this;
        return Hunger.exhaustion(self, BodyHooks.exhaustion(self, exhaustion));
    }
}
