package studio.modroll.checks.neoforge.mixin;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.modroll.checks.social.SocialPrices;

/**
 * Neither loader has an event for a player starting or stopping a trade. Villagers and wandering traders
 * both set their trading player here just before sending the offers, after villagers add their reputation
 * prices, and clear it when trading stops.
 */
@Mixin(AbstractVillager.class)
abstract class AbstractVillagerMixin {

    @Inject(method = "setTradingPlayer", at = @At("HEAD"))
    private void checks$takeBackSocialPrices(Player player, CallbackInfo ci) {
        SocialPrices.beforeTradingPlayer((AbstractVillager) (Object) this);
    }

    @Inject(method = "setTradingPlayer", at = @At("TAIL"))
    private void checks$addSocialPrices(Player player, CallbackInfo ci) {
        SocialPrices.afterTradingPlayer((AbstractVillager) (Object) this, player);
    }
}
