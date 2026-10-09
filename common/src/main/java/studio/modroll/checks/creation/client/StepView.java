package studio.modroll.checks.creation.client;

import net.minecraft.client.gui.GuiGraphics;
import studio.modroll.checks.ui.Area;

/** One page of the creation screen, drawn inside the area the screen gives it. */
interface StepView {

    /** Lays out the page and adds its widgets; called again whenever the page is rebuilt. */
    void init(Area area);

    void renderBackground(GuiGraphics graphics);

    void render(GuiGraphics graphics);

    default boolean mouseClicked(double x, double y) {
        return false;
    }

    default void mouseScrolled(double x, double y, double scrollY) {}
}
