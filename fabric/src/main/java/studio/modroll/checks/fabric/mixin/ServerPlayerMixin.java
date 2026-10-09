package studio.modroll.checks.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import studio.modroll.checks.death.DeathStories;

/**
 * Neither loader has an event that changes a player's death message. Vanilla builds it once here and sends
 * that one message to the death screen and to chat.
 */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {

    @ModifyExpressionValue(
            method = "die",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/damagesource/CombatTracker;getDeathMessage()Lnet/minecraft/network/chat/Component;"))
    private Component checks$storyDeathMessage(Component vanilla, @Local(argsOnly = true) DamageSource source) {
        return DeathStories.deathMessage((ServerPlayer) (Object) this, source, vanilla);
    }
}
