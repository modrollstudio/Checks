package studio.modroll.checks.social;

import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;

/**
 * The kinds of entity social actions work on. Players, baby villagers and piglins, and everything else are
 * never targets. A hostile mob is one that walks with goals, so it can be sent running; a neutral one is a
 * mob Calm works on, angry or not.
 */
public enum SocialTarget {
    VILLAGER("villager"),
    WANDERING_TRADER("wandering_trader"),
    PIGLIN("piglin"),
    HOSTILE("hostile"),
    NEUTRAL("neutral");

    private final String id;

    SocialTarget(String id) {
        this.id = id;
    }

    /** The key in the lang file and in the voice sound ids. */
    public String id() {
        return id;
    }

    public static Optional<SocialTarget> of(Entity entity) {
        return switch (entity) {
            case Villager villager when !villager.isBaby() -> Optional.of(VILLAGER);
            case WanderingTrader trader -> Optional.of(WANDERING_TRADER);
            case Piglin piglin when !piglin.isBaby() -> Optional.of(PIGLIN);
            case Piglin piglin -> Optional.empty();
            case Entity mob when SocialMobs.calmable(mob) -> Optional.of(NEUTRAL);
            case PathfinderMob mob when mob instanceof Enemy -> Optional.of(HOSTILE);
            default -> Optional.empty();
        };
    }

    public boolean trades() {
        return this == VILLAGER || this == WANDERING_TRADER;
    }

    /** Hostile and neutral mobs: they have no voice, no speech bubbles and nothing to trade. */
    public boolean mob() {
        return this == HOSTILE || this == NEUTRAL;
    }
}
