package studio.modroll.checks.text;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * How flavor lines name an entity: a player or a mob with a custom name by that name ("Bob"), any other mob
 * by its type with {@code checks.name.unnamed} around it ("the Iron Golem", "the Farmer"). Lines never
 * start a sentence with a mob's name, so the lowercase "the" always reads right.
 */
public final class Names {

    private Names() {}

    public static Component of(Entity entity) {
        if (entity instanceof Player || entity.hasCustomName()) {
            return entity.getDisplayName();
        }
        return FallbackText.of("checks.name.unnamed", entity.getDisplayName());
    }
}
