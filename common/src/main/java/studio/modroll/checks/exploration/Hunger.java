package studio.modroll.checks.exploration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.Checks;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * Survival, passively and with no roll: a positive Survival modifier makes a player's hunger drain slower, by
 * shrinking every bit of food exhaustion they gain. Anything but a server-side player is left unchanged.
 */
public final class Hunger {

    private static final ResourceLocation SURVIVAL = Checks.id("survival");

    private Hunger() {}

    public static float exhaustion(LivingEntity entity, float exhaustion) {
        ScoresConfig.ReductionSettings settings =
                ScoresRuntime.config().exploration().hunger();
        if (!settings.enabled() || !(entity instanceof ServerPlayer player)) {
            return exhaustion;
        }
        return exhaustion(exhaustion, ExplorationChecks.modifier(player, SURVIVAL), settings);
    }

    /** {@code exhaustion} less the Survival share. */
    static float exhaustion(float exhaustion, int survivalModifier, ScoresConfig.ReductionSettings settings) {
        double reduction = ExplorationChecks.share(survivalModifier, settings.perPoint(), settings.maxReduction());
        return (float) (exhaustion * (1 - reduction));
    }
}
