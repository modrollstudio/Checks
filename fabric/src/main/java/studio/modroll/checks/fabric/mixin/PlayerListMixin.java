package studio.modroll.checks.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.modroll.checks.body.PlayerBodies;

/**
 * Fabric API has no player-save event; NeoForge fires PlayerEvent.SaveToFile from this same method, which
 * vanilla calls on every autosave, logout and shutdown.
 */
@Mixin(PlayerList.class)
abstract class PlayerListMixin {

    @Inject(method = "save", at = @At("HEAD"))
    private void checks$saveHealth(ServerPlayer player, CallbackInfo ci) {
        PlayerBodies.onSave(player);
    }
}
