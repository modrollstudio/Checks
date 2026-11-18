package studio.modroll.checks.social;

import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;

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

    public boolean mob() {
        return this == HOSTILE || this == NEUTRAL;
    }
}
