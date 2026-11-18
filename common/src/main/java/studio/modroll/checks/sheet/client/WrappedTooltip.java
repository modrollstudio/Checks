package studio.modroll.checks.sheet.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2ic;

/**
 * Vanilla puts the title gap after the first wrapped part, splitting the title; this puts it after all of the title's
 * lines.
 */
public final class WrappedTooltip {

    static final int LINE_HEIGHT = 10;
    static final int TITLE_GAP = 2;
    static final int GLYPH_HEIGHT = 8;
    private static final int Z = 400;
    private static final int WHITE = 0xFFFFFFFF;

    private WrappedTooltip() {}

    public static void render(
            GuiGraphics graphics, Font font, List<Component> lines, int width, int mouseX, int mouseY) {
        if (lines.isEmpty()) {
            return;
        }
        int titleLines = font.split(lines.getFirst(), width).size();
        List<FormattedCharSequence> wrapped =
                lines.stream().flatMap(line -> font.split(line, width).stream()).toList();
        int boxWidth = wrapped.stream().mapToInt(font::width).max().orElse(0);
        int boxHeight = height(wrapped.size(), titleLines);
        Vector2ic at = DefaultTooltipPositioner.INSTANCE.positionTooltip(
                graphics.guiWidth(), graphics.guiHeight(), mouseX, mouseY, boxWidth, boxHeight);
        graphics.pose().pushPose();
        TooltipRenderUtil.renderTooltipBackground(graphics, at.x(), at.y(), boxWidth, boxHeight, Z);
        graphics.pose().translate(0, 0, Z);
        for (int i = 0; i < wrapped.size(); i++) {
            graphics.drawString(font, wrapped.get(i), at.x(), at.y() + lineTop(i, titleLines), WHITE);
        }
        graphics.pose().popPose();
    }

    static int lineTop(int index, int titleLines) {
        return index * LINE_HEIGHT + (index >= titleLines ? TITLE_GAP : 0);
    }

    static int height(int lineCount, int titleLines) {
        return lineTop(lineCount - 1, titleLines) + GLYPH_HEIGHT;
    }
}
