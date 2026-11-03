package studio.modroll.checks.sheet.client;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import studio.modroll.checks.creation.client.CharacterCreationScreen;
import studio.modroll.checks.level.ExpertisePayload;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.sheet.StatSheet;
import studio.modroll.checks.ui.Area;
import studio.modroll.checks.ui.Frames;

/**
 * Chooses skills for Expertise, on a panel under a banner: a toggle per skill the player is proficient in, two
 * columns, up to the number waiting, then Confirm / Cancel. The server validates the choice and answers with a fresh
 * stat sheet, which replaces this screen.
 */
final class ExpertiseScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.checks.expertise.title");
    private static final Component CONFIRM = Component.translatable("screen.checks.improvement.confirm");

    private static final int COLUMNS = 2;
    private static final int CONTENT_WIDTH = 240;
    private static final int INSET = Frames.FRAME + Frames.PADDING + Frames.CAP_REACH;
    private static final int GAP = 4;
    private static final int LINE = Frames.LINE;
    private static final int ROW = 22;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 2 * (Frames.CAP_REACH + GAP);
    private static final int SKILL_WIDTH = (CONTENT_WIDTH - BUTTON_GAP) / COLUMNS;
    private static final ItemStack ICON = new ItemStack(Items.BOOK);

    private final Screen parent;
    private final StatSheet.ExpertiseChoice choice;
    private final Set<ResourceLocation> chosen = new LinkedHashSet<>();
    private final List<Button> skillButtons = new ArrayList<>();
    private final List<Button> footer = new ArrayList<>();
    private Button confirm;
    private List<FormattedCharSequence> help = List.of();
    private Area panel;
    private Area content;
    private int helpTop;

    ExpertiseScreen(Screen parent, StatSheet sheet) {
        super(TITLE);
        this.parent = parent;
        this.choice = sheet.level().orElseThrow().expertise();
    }

    @Override
    protected void init() {
        skillButtons.clear();
        footer.clear();
        Component helpText = choice.options().isEmpty()
                ? Component.translatable("screen.checks.expertise.none", choice.pending())
                : Component.translatable("screen.checks.expertise.help", choice.pending());
        help = font.split(helpText, CONTENT_WIDTH);
        int rows = (choice.options().size() + COLUMNS - 1) / COLUMNS;
        int contentHeight = Frames.BANNER_HEIGHT + GAP + help.size() * LINE + GAP + rows * ROW + GAP + BUTTON_HEIGHT;
        int panelWidth = CONTENT_WIDTH + 2 * INSET;
        int panelHeight = contentHeight + 2 * INSET;
        panel = new Area((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
        content = panel.inset(INSET);
        helpTop = content.top() + Frames.BANNER_HEIGHT + GAP;
        int rowsTop = helpTop + help.size() * LINE + GAP;
        for (int i = 0; i < choice.options().size(); i++) {
            ResourceLocation skill = choice.options().get(i);
            int left = content.left() + (i % COLUMNS) * (SKILL_WIDTH + BUTTON_GAP);
            int top = rowsTop + (i / COLUMNS) * ROW;
            skillButtons.add(addRenderableWidget(Button.builder(label(skill), button -> toggle(skill))
                    .bounds(left, top, SKILL_WIDTH, BUTTON_HEIGHT)
                    .build()));
        }
        int buttonsTop = rowsTop + rows * ROW + GAP;
        int buttonsLeft = (width - 2 * BUTTON_WIDTH - BUTTON_GAP) / 2;
        confirm = addRenderableWidget(Button.builder(CONFIRM, button -> submit())
                .bounds(buttonsLeft, buttonsTop, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        footer.add(confirm);
        footer.add(addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(buttonsLeft + BUTTON_WIDTH + BUTTON_GAP, buttonsTop, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()));
        updateButtons();
    }

    private Component label(ResourceLocation skill) {
        Component name = SheetText.skillName(skill);
        return chosen.contains(skill) ? Component.translatable("screen.checks.expertise.chosen", name) : name;
    }

    private void toggle(ResourceLocation skill) {
        if (!chosen.remove(skill) && chosen.size() < choice.pending()) {
            chosen.add(skill);
        }
        updateButtons();
    }

    private void updateButtons() {
        boolean full = chosen.size() >= choice.pending();
        for (int i = 0; i < skillButtons.size(); i++) {
            ResourceLocation skill = choice.options().get(i);
            Button button = skillButtons.get(i);
            button.setMessage(label(skill));
            button.active = chosen.contains(skill) || !full;
        }
        int wanted = Math.min(choice.pending(), choice.options().size());
        confirm.active = !chosen.isEmpty() && chosen.size() == wanted;
    }

    private void submit() {
        confirm.active = false;
        CharacterCreationScreen.sendToServer(new ExpertisePayload(List.copyOf(chosen)));
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
        Frames.lines(graphics, font, help, content.left(), helpTop, Frames.MUTED);
        footer.forEach(button -> Frames.caps(graphics, button));
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
