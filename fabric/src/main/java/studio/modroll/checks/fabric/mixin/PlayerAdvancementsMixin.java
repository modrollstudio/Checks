package studio.modroll.checks.fabric.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.modroll.checks.level.Levelling;

/**
 * Fabric API has no advancement-earned event. Rewards are granted exactly once, when an advancement
 * becomes done, which is where NeoForge fires AdvancementEarnEvent.
 */
@Mixin(PlayerAdvancements.class)
abstract class PlayerAdvancementsMixin {

    @Shadow
    private ServerPlayer player;

    @Inject(
            method = "award",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void checks$countAdvancement(
            AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
        Levelling.onAdvancement(player, advancement);
    }
}
