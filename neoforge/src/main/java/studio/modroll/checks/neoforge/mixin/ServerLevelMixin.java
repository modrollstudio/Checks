package studio.modroll.checks.neoforge.mixin;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.modroll.checks.rest.Rests;

/**
 * Vanilla wakes everyone here only when a night is slept through, while they are still asleep. NeoForge's
 * SleepFinishedTimeEvent fires only with the daylight cycle on.
 */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {

    @Inject(method = "wakeUpAllPlayers", at = @At("HEAD"))
    private void checks$longRest(CallbackInfo ci) {
        Rests.onNightSlept((ServerLevel) (Object) this);
    }
}
