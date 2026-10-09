package studio.modroll.checks.check.client;

import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import studio.modroll.checks.check.RollLinePayload;

/**
 * Roll lines above the hotbar, where the action bar sits, each loader calling {@link #render} from its HUD.
 * Showing one makes an empty marker of its own vanilla's action bar message, with the line's duration as
 * its timer: the lines are drawn, faded as vanilla fades the action bar, only while that marker is still
 * the action bar message, so any newer one hides them at once. A line wider than the screen wraps, and
 * the lines move up above the chat wherever they would cover it.
 */
public final class ClientRollLines {

    private static final int SIDE_MARGIN = 8;
    /** As vanilla draws the action bar, around its anchor. */
    private static final int TEXT_OFFSET = -4;

    private static final int BACKDROP_PADDING = 2;
    private static final float FADE_TICKS = 20;
    private static final int OPAQUE = 255;
    private static final int MIN_VISIBLE_ALPHA = 8;
    private static final int WHITE = -1;
    private static final float NO_DEFAULT_BACKGROUND = 0f;
    private static final int CHAT_CLEARANCE = 2;

    private static Component marker = Component.empty();
    private static List<Component> lines = List.of();

    private ClientRollLines() {}

    /** {@code keepFor} sets the action bar timer, through each loader's accessor. */
    public static void show(RollLinePayload payload, IntConsumer keepFor) {
        marker = Component.literal("");
        lines = payload.lines();
        Minecraft.getInstance().gui.setOverlayMessage(marker, false);
        keepFor.accept(payload.durationTicks());
    }

    /**
     * Draws the lines centered, the last one on {@code anchorY} and the rest stacked above it, while
     * vanilla's action bar message ({@code overlay}, with {@code overlayTicks} left) is the marker.
     */
    public static void render(
            GuiGraphics graphics,
            float partialTick,
            Component overlay,
            int overlayTicks,
            int anchorY,
            Optional<ChatBox> chat) {
        Minecraft minecraft = Minecraft.getInstance();
        if (overlay != marker || overlayTicks <= 0 || minecraft.options.hideGui) {
            return;
        }
        int alpha = Math.min(OPAQUE, (int) ((overlayTicks - partialTick) * OPAQUE / FADE_TICKS));
        if (alpha <= MIN_VISIBLE_ALPHA) {
            return;
        }
        Font font = minecraft.font;
        int maxWidth = graphics.guiWidth() - 2 * SIDE_MARGIN;
        List<FormattedCharSequence> wrapped = lines.stream()
                .flatMap(line -> font.split(line, maxWidth).stream())
                .toList();
        int color = FastColor.ARGB32.color(alpha, WHITE);
        int lineSpacing = font.lineHeight + 2 * BACKDROP_PADDING;
        int widest = wrapped.stream().mapToInt(font::width).max().orElse(0);
        int left = (graphics.guiWidth() - widest) / 2 - BACKDROP_PADDING;
        int top = clearOf(
                chat,
                left,
                anchorY + TEXT_OFFSET - (wrapped.size() - 1) * lineSpacing,
                wrapped.size() * lineSpacing - BACKDROP_PADDING);
        for (int i = 0; i < wrapped.size(); i++) {
            drawWithBackdrop(graphics, font, wrapped.get(i), top + i * lineSpacing, color);
        }
    }

    /**
     * The top of a block of lines {@code height} tall starting at {@code top}, raised above the chat
     * when it would otherwise cover the chat's lines; {@code left} is the block's left edge.
     */
    static int clearOf(Optional<ChatBox> chat, int left, int top, int height) {
        return chat.filter(box -> box.right() > left && box.top() < top + height)
                .map(box -> box.top() - CHAT_CLEARANCE - height)
                .orElse(top);
    }

    /** As vanilla draws the action bar: a backdrop only when the text background setting asks for one. */
    private static void drawWithBackdrop(
            GuiGraphics graphics, Font font, FormattedCharSequence text, int y, int color) {
        int width = font.width(text);
        int x = (graphics.guiWidth() - width) / 2;
        int background = Minecraft.getInstance().options.getBackgroundColor(NO_DEFAULT_BACKGROUND);
        if (background != 0) {
            graphics.fill(
                    x - BACKDROP_PADDING,
                    y - BACKDROP_PADDING,
                    x + width + BACKDROP_PADDING,
                    y + font.lineHeight + BACKDROP_PADDING,
                    FastColor.ARGB32.multiply(background, color));
        }
        graphics.drawString(font, text, x, y, color, true);
    }
}
