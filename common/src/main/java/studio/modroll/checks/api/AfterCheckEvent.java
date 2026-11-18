package studio.modroll.checks.api;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;

public record AfterCheckEvent(
        LivingEntity entity, Stat stat, CheckKind kind, Optional<LivingEntity> opponent, CheckResult result) {}
