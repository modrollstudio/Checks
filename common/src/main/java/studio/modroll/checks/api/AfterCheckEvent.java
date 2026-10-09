package studio.modroll.checks.api;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fired on the server after a check, save or contest side made through {@link ChecksApi} is rolled,
 * with the full result. A canceled roll fires none.
 *
 * @param opponent the other side of a contest, or the entity whose passive score was the DC
 */
public record AfterCheckEvent(
        LivingEntity entity, Stat stat, CheckKind kind, Optional<LivingEntity> opponent, CheckResult result) {}
