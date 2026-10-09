package studio.modroll.checks.creation.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** A list that only ever shows whole rows and scrolls by one row at a time. */
final class ScrollRows {

    private int first;
    private int visible;
    private int total;

    /** Keeps the scroll position across rebuilds, clamped to the new size. */
    void layout(int totalRows, int fittingRows) {
        total = totalRows;
        visible = Mth.clamp(fittingRows, 0, totalRows);
        first = Mth.clamp(first, 0, maxFirst());
    }

    int first() {
        return first;
    }

    int end() {
        return first + visible;
    }

    int visible() {
        return visible;
    }

    void scroll(double scrollY) {
        first = Mth.clamp(first - (int) Math.signum(scrollY), 0, maxFirst());
    }

    void renderScrollbar(GuiGraphics graphics, int left, int top, int rowHeight) {
        if (maxFirst() == 0) {
            return;
        }
        int track = visible * rowHeight;
        int thumb = Math.max(rowHeight, track * visible / total);
        int thumbTop = top + (track - thumb) * first / maxFirst();
        graphics.fill(left, thumbTop, left + CreationUi.SCROLLBAR_WIDTH, thumbTop + thumb, CreationUi.LABEL);
    }

    private int maxFirst() {
        return total - visible;
    }
}
