package studio.modroll.checks.creation.client;

import net.minecraft.client.gui.GuiGraphics;
import studio.modroll.checks.ui.Area;

interface StepView {

    /** Called again whenever the page is rebuilt. */
    void init(Area area);

    void renderBackground(GuiGraphics graphics);

    void render(GuiGraphics graphics);

    default boolean mouseClicked(double x, double y) {
        return false;
    }

    default void mouseScrolled(double x, double y, double scrollY) {}
}
