package studio.modroll.checks.sheet.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import studio.modroll.checks.level.LevelGain;
import studio.modroll.checks.sheet.SheetTooltips;
import studio.modroll.checks.sheet.StatSheet;
import studio.modroll.checks.ui.Area;
import studio.modroll.checks.ui.Frames;

/**
 * The Level up! panel: what each level up to the current one gave, newest first, scrolling when it is long, then a
 * button for each choice waiting (an ability score improvement, Expertise) and Back.
 */
final class LevelUpScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.checks.level_up");
    private static final Component LEVELS_TITLE = Component.translatable("screen.checks.levels");
    private static final Component SEPARATOR = Component.translatable("screen.checks.tooltip.separator");

    private static final int CONTENT_WIDTH = 240;
    private static final int INSET = Frames.FRAME + Frames.PADDING + Frames.CAP_REACH;
    private static final int GAP = 4;
    private static final int LINE = Frames.LINE;
    private static final int LEVEL_GAP = 3;
    private static final int MAX_LIST_HEIGHT = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_GAP = 2 * (Frames.CAP_REACH + GAP);
    private static final ItemStack ICON = new ItemStack(Items.EXPERIENCE_BOTTLE);

    /** One level's heading and its wrapped gains. */
    private record Entry(Component heading, List<FormattedCharSequence> lines) {

        int height() {
            return LINE + lines.size() * LINE + LEVEL_GAP;
        }
    }

    private final Screen parent;
    private final StatSheet sheet;
    private final StatSheet.LevelLine line;
    private final List<Button> footer = new ArrayList<>();
    private List<Entry> entries = List.of();
    private Area panel;
    private Area content;
    private Area list;
    private int listHeight;
    private int scroll;

    LevelUpScreen(Screen parent, StatSheet sheet) {
        super(sheet.level().orElseThrow().choicesPending() ? TITLE : LEVELS_TITLE);
        this.parent = parent;
        this.sheet = sheet;
        this.line = sheet.level().orElseThrow();
    }

    @Override
    protected void init() {
        footer.clear();
        entries = entries();
        listHeight = entries.stream().mapToInt(Entry::height).sum();
        int listView = Math.min(listHeight, Math.min(MAX_LIST_HEIGHT, height - 2 * INSET - 3 * BUTTON_HEIGHT));
        int contentHeight = Frames.BANNER_HEIGHT + GAP + listView + GAP + BUTTON_HEIGHT;
        int panelWidth = CONTENT_WIDTH + 2 * INSET;
        int panelHeight = contentHeight + 2 * INSET;
        panel = new Area((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
        content = panel.inset(INSET);
        list = new Area(content.left(), content.top() + Frames.BANNER_HEIGHT + GAP, content.width(), listView);
        scroll = Mth.clamp(scroll, 0, maxScroll());

        List<Button.Builder> buttons = new ArrayList<>();
        int improvements = line.improvements().pending();
        if (improvements > 0) {
            buttons.add(Button.builder(
                    Component.translatable("screen.checks.level_up.improvement", improvements),
                    button -> minecraft.setScreen(new ImprovementScreen(this, sheet))));
        }
        int expertise = line.expertise().pending();
        if (expertise > 0) {
            buttons.add(Button.builder(
                    Component.translatable("screen.checks.level_up.expertise", expertise),
                    button -> minecraft.setScreen(new ExpertiseScreen(this, sheet))));
        }
        buttons.add(Button.builder(CommonComponents.GUI_BACK, button -> onClose()));
        int top = list.bottom() + GAP;
        int rowWidth = buttons.size() * BUTTON_WIDTH + (buttons.size() - 1) * BUTTON_GAP;
        int left = (width - rowWidth) / 2;
        for (Button.Builder builder : buttons) {
            footer.add(addRenderableWidget(
                    builder.bounds(left, top, BUTTON_WIDTH, BUTTON_HEIGHT).build()));
            left += BUTTON_WIDTH + BUTTON_GAP;
        }
    }

    private List<Entry> entries() {
        Optional<Integer> hitDie = sheet.body().hitDice().map(StatSheet.HitDiceLine::sides);
        List<Entry> built = new ArrayList<>();
        for (LevelGain gain : line.gains().reversed()) {
            List<Component> parts = SheetTooltips.levelGain(gain, hitDie);
            Component text = parts.isEmpty()
                    ? Component.translatable("screen.checks.level_up.nothing")
                    : ComponentUtils.formatList(parts, SEPARATOR);
            built.add(new Entry(
                    Component.translatable("screen.checks.level", gain.level()),
                    font.split(text, CONTENT_WIDTH - Frames.BULLET_INDENT)));
        }
        return built;
    }

    private int maxScroll() {
        return Math.max(0, listHeight - list.height());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        Frames.panel(graphics, panel);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Frames.banner(
                graphics,
                font,
                new Area(content.left(), content.top(), content.width(), Frames.BANNER_HEIGHT),
                ICON,
                title);
        graphics.enableScissor(list.left(), list.top(), list.right(), list.bottom());
        int y = list.top() - scroll;
        for (Entry entry : entries) {
            graphics.drawString(font, entry.heading(), list.left(), y, Frames.GOLD_TEXT);
            Frames.lines(graphics, font, entry.lines(), list.left() + Frames.BULLET_INDENT, y + LINE, Frames.MUTED);
            y += entry.height();
        }
        graphics.disableScissor();
        if (maxScroll() > 0) {
            int thumb = Math.max(LINE, list.height() * list.height() / listHeight);
            int thumbTop = list.top() + (list.height() - thumb) * scroll / maxScroll();
            graphics.fill(list.right() + GAP, thumbTop, list.right() + GAP + 1, thumbTop + thumb, Frames.DIM);
        }
        footer.forEach(button -> Frames.caps(graphics, button));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY) * LINE, 0, maxScroll());
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
