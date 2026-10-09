package studio.modroll.checks.exploration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.Checks;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * Acrobatics: fall damage to a player in survival or adventure rolls Acrobatics through the API against a DC
 * that grows with the damage, and a made check multiplies the damage by the success multiplier. A fall that
 * does no damage rolls nothing.
 */
public final class Landings {

    private static final ResourceLocation ACROBATICS = Checks.id("acrobatics");

    private Landings() {}

    public static float fallDamage(LivingEntity entity, DamageSource source, float amount) {
        ScoresConfig.LandingSettings settings =
                ScoresRuntime.config().exploration().landing();
        if (!settings.enabled()
                || amount <= 0
                || !source.is(DamageTypeTags.IS_FALL)
                || !(entity instanceof ServerPlayer player)
                || !ExplorationChecks.rollsFor(player)
                || player.isInvulnerableTo(source)
                || player.isDeadOrDying()) {
            return amount;
        }
        boolean made = ExplorationChecks.made(player, ACROBATICS, settings.dc(amount), "landing")
                .orElse(false);
        return made ? (float) (amount * settings.successMultiplier()) : amount;
    }
}
