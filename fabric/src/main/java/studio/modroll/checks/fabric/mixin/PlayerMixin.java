package studio.modroll.checks.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.modroll.checks.body.BodyHooks;
import studio.modroll.checks.exploration.Hunger;
import studio.modroll.checks.exploration.Leaps;
import studio.modroll.checks.level.Levelling;

/**
 * Fabric API has no XP-gain event; NeoForge fires PlayerXpEvent.XpChange at this same spot. Enchanting,
 * anvils and death never pass through here, so they never take character XP. An orb's points reach
 * here minus what Mending spent, which {@link ExperienceOrbMixin} counts. Neither loader has a food
 * exhaustion event; every exhaustion a player gains passes through {@code causeFoodExhaustion}. Fabric API has
 * no jump event; NeoForge fires LivingJumpEvent within this same jump.
 */
@Mixin(Player.class)
abstract class PlayerMixin {

    @Inject(method = "giveExperiencePoints", at = @At("HEAD"))
    private void checks$countVanillaXp(int points, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            Levelling.onVanillaXp(player, points);
        }
    }

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float checks$conAndSurvivalExhaustion(float exhaustion) {
        Player self = (Player) (Object) this;
        return Hunger.exhaustion(self, BodyHooks.exhaustion(self, exhaustion));
    }

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void checks$athleticsLeap(CallbackInfo ci) {
        Leaps.onJump((Player) (Object) this);
    }
}
