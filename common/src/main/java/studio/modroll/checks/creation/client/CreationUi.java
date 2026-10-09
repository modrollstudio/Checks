package studio.modroll.checks.creation.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import studio.modroll.checks.preset.KitItem;
import studio.modroll.checks.ui.Area;

/** Sizes, colours and drawing shared by the creation pages; the panels and banners are {@code Frames}. */
final class CreationUi {

    static final String KEY = "screen.checks.creation.";

    static final int GAP = 4;
    static final int PADDING = 4;
    static final int LINE = 10;
    static final int ROW = 16;
    static final int BUTTON_HEIGHT = 20;
    static final int SMALL_BUTTON = 14;
    static final int DOT = 5;
    static final int DOT_SPACE = DOT + PADDING;
    static final int SCROLLBAR_WIDTH = 2;
    static final int ITEM_SIZE = 16;
    // Room for the stack count, which vanilla draws past the item's right edge.
    static final int ITEM_SPACE = ITEM_SIZE + 4;

    static final int CHIP = 0xFF303030;
    static final int SELECTED = 0xFFFFFF55;
    static final int LABEL = 0xFFA0A0A0;
    static final int VALUE = 0xFFFFFFFF;
    static final int DIM = 0xFF606060;

    private CreationUi() {}

    /** A proficiency dot: filled, or an empty outline. */
    static void dot(GuiGraphics graphics, Font font, int left, int y, boolean filled, int color) {
        int top = y + (font.lineHeight - DOT) / 2 - 1;
        if (filled) {
            graphics.fill(left, top, left + DOT, top + DOT, color);
        } else {
            graphics.renderOutline(left, top, DOT, DOT, color);
        }
    }

    static Button smallButton(String label, int left, int top, Runnable onPress, CharacterCreationScreen host) {
        return Button.builder(Component.literal(label), button -> {
                    onPress.run();
                    host.refresh();
                })
                .bounds(left, top, SMALL_BUTTON, SMALL_BUTTON)
                .build();
    }

    /** One icon per kit stack with its count, wrapped to {@code width}, each naming its item on hover. */
    static void kit(GuiGraphics graphics, List<KitItem> kit, Area area, CharacterCreationScreen host) {
        int perRow = kitPerRow(area.width());
        for (int i = 0; i < kit.size(); i++) {
            int x = area.left() + (i % perRow) * ITEM_SPACE;
            int y = area.top() + (i / perRow) * ITEM_SPACE;
            KitItem item = kit.get(i);
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(item.item()), item.count());
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(host.font(), stack, x, y);
            host.hover(x, y, x + ITEM_SIZE, y + ITEM_SIZE, () -> List.of(stack.getHoverName()));
        }
    }

    static int kitHeight(List<KitItem> kit, int width) {
        int perRow = kitPerRow(width);
        return (kit.size() + perRow - 1) / perRow * ITEM_SPACE;
    }

    private static int kitPerRow(int width) {
        return Math.max(1, width / ITEM_SPACE);
    }
}
