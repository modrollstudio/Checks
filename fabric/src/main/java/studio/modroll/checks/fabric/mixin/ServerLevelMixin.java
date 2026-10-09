package studio.modroll.checks.fabric.mixin;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.modroll.checks.social.Performances;

/** Fabric API has no game event hook; NeoForge's VanillaGameEvent fires at the same spot. */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {

    @Inject(method = "gameEvent", at = @At("HEAD"))
    private void checks$performance(
            Holder<GameEvent> event, Vec3 position, GameEvent.Context context, CallbackInfo ci) {
        Performances.onGameEvent(event, context);
    }
}
