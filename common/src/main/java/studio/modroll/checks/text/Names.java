package studio.modroll.checks.text;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Lines never start a sentence with a mob's name, so the lowercase "the" always reads right. */
public final class Names {

    private Names() {}

    public static Component of(Entity entity) {
        if (entity instanceof Player || entity.hasCustomName()) {
            return entity.getDisplayName();
        }
        return FallbackText.of("checks.name.unnamed", entity.getDisplayName());
    }
}
