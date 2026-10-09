package studio.modroll.checks.death;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;

/**
 * A Checks event that can explain a player's death shortly after it, with a pool of story death messages
 * ({@code checks.death.<id>} in the lang file). When several could, the first in this order is told.
 */
public enum DeathStory {
    CAUGHT_PICKPOCKETING("caught_pickpocketing"),
    INTIMIDATE_NATURAL_ONE("intimidate_natural_one"),
    FAILED_INTIMIDATE("failed_intimidate"),
    FAILED_EXPLOSION_SAVE("failed_explosion_save"),
    LAST_STAND_SPENT("last_stand_spent");

    private final String id;

    DeathStory(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Whether a death from {@code source} is the one this story tells. */
    public boolean fits(DamageSource source) {
        return switch (this) {
            case CAUGHT_PICKPOCKETING, FAILED_INTIMIDATE -> source.getEntity() instanceof IronGolem;
            case INTIMIDATE_NATURAL_ONE -> source.getEntity() instanceof Mob;
            case FAILED_EXPLOSION_SAVE -> source.is(DamageTypeTags.IS_EXPLOSION);
            case LAST_STAND_SPENT -> true;
        };
    }
}
