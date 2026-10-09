package studio.modroll.checks.body;

import net.minecraft.world.entity.LivingEntity;

/**
 * The extras no attribute covers, applied where each loader hooks bow draws, crossbow loading and food
 * exhaustion. Anything but a server-side player is left unchanged.
 */
public final class BodyHooks {

    private BodyHooks() {}

    public static int bowCharge(LivingEntity archer, int charge) {
        return BodyRules.bowCharge(charge, BodyValues.extra(archer, Extra.BOW_DRAW));
    }

    public static int crossbowChargeDuration(LivingEntity shooter, int duration) {
        return BodyRules.crossbowChargeDuration(duration, BodyValues.extra(shooter, Extra.CROSSBOW_RELOAD));
    }

    public static float exhaustion(LivingEntity player, float exhaustion) {
        return BodyRules.exhaustion(exhaustion, BodyValues.extra(player, Extra.EXHAUSTION));
    }
}
