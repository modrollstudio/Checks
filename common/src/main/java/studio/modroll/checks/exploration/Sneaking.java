package studio.modroll.checks.exploration;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.Checks;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

public final class Sneaking {

    private static final ResourceLocation STEALTH = Checks.id("stealth");
    private static final ResourceLocation PERCEPTION = Checks.id("perception");
    private static final double UNCHANGED = 1.0;

    private Sneaking() {}

    public static double visibility(LivingEntity seen, @Nullable Entity looker) {
        ScoresConfig.SneakSettings settings =
                ScoresRuntime.config().exploration().sneak();
        if (!settings.enabled()
                || !(seen instanceof ServerPlayer player)
                || !player.isShiftKeyDown()
                || !(looker instanceof LivingEntity mob)) {
            return UNCHANGED;
        }
        int margin = ExplorationChecks.passive(player, STEALTH) - ExplorationChecks.passive(mob, PERCEPTION);
        return visibility(margin, settings.perPoint(), settings.minVisibility());
    }

    static double visibility(int margin, double perPoint, double min) {
        if (margin <= 0) {
            return UNCHANGED;
        }
        return Math.max(min, UNCHANGED - margin * perPoint);
    }
}
