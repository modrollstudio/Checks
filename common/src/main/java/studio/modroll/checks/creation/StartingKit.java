package studio.modroll.checks.creation;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import studio.modroll.checks.preset.KitItem;

/** Hands a background's kit to a player; whatever does not fit drops at their feet, as with {@code /give}. */
final class StartingKit {

    private StartingKit() {}

    static void give(ServerPlayer player, List<KitItem> kit) {
        for (KitItem item : kit) {
            BuiltInRegistries.ITEM
                    .getOptional(item.item())
                    .ifPresent(registered -> give(player, new ItemStack(registered, item.count())));
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }
}
