package studio.modroll.checks.fabric.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla keeps a mob's goals protected; NeoForge opens them with its access transformer, Fabric does not. */
@Mixin(Mob.class)
public interface MobAccessor {

    @Accessor("goalSelector")
    GoalSelector checks$goalSelector();
}
