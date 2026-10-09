package studio.modroll.checks.check.client;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.ChatVisiblity;

/** Where the chat's visible lines are on screen, worked out as vanilla lays them out. */
public record ChatBox(int right, int top) {

    /** Vanilla's chat sits this far above the bottom of the screen. */
    private static final int BOTTOM_OFFSET = 40;
    /** Ticks a line stays up while the chat is closed. */
    private static final int SHOWN_TICKS = 200;
    /** Vanilla's chat background reaches this far past the text width. */
    private static final int BACKGROUND_PADDING = 12;

    private static final double LINE_HEIGHT = 9.0;

    /**
     * {@code lines} is vanilla's wrapped chat, newest first, read through each loader's accessor. Empty
     * while no line shows.
     */
    public static Optional<ChatBox> of(Minecraft minecraft, List<GuiMessage.Line> lines, int guiHeight) {
        if (minecraft.options.chatVisibility().get() == ChatVisiblity.HIDDEN) {
            return Optional.empty();
        }
        ChatComponent chat = minecraft.gui.getChat();
        int now = minecraft.gui.getGuiTicks();
        long shown = lines.stream()
                .limit(chat.getLinesPerPage())
                .filter(line -> chat.isChatFocused() || now - line.addedTime() < SHOWN_TICKS)
                .count();
        if (shown == 0) {
            return Optional.empty();
        }
        double scale = chat.getScale();
        int lineHeight =
                (int) (LINE_HEIGHT * (minecraft.options.chatLineSpacing().get() + 1.0));
        int bottom = Mth.floor((guiHeight - BOTTOM_OFFSET) / scale);
        int top = Mth.floor((bottom - shown * lineHeight) * scale);
        int right = Mth.ceil((Mth.ceil(chat.getWidth() / scale) + BACKGROUND_PADDING) * scale);
        return Optional.of(new ChatBox(right, top));
    }
}
