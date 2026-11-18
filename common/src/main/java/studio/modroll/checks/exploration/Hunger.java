package studio.modroll.checks.exploration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.Checks;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/** Survival, passively: a positive modifier shrinks every bit of food exhaustion a player gains. */
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

    static float exhaustion(float exhaustion, int survivalModifier, ScoresConfig.ReductionSettings settings) {
        double reduction = ExplorationChecks.share(survivalModifier, settings.perPoint(), settings.maxReduction());
        return (float) (exhaustion * (1 - reduction));
    }
}
