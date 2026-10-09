package studio.modroll.checks.sheet.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.creation.BonusSplits;
import studio.modroll.checks.creation.client.CharacterCreationScreen;
import studio.modroll.checks.level.ImprovementPayload;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.sheet.StatSheet;
import studio.modroll.checks.ui.Area;
import studio.modroll.checks.ui.Frames;

/**
 * Chooses one ability score improvement, on a panel under a banner: - / + per ability, limited to the
 * allowed splits and the max score, then what is left and Confirm / Cancel. The server validates the
 * choice and answers with a fresh stat sheet, which replaces this screen.
 */
final class ImprovementScreen extends Screen {

    private static final Component TITLE = Component.translatable("screen.checks.improvement.title");
    private static final Component CONFIRM = Component.translatable("screen.checks.improvement.confirm");

    private static final int ABILITY_COUNT = Ability.values().length;
    private static final int CONTENT_WIDTH = 220;
    /** The panel's frame and padding, plus room for the buttons' bracket caps. */
    private static final int INSET = Frames.FRAME + Frames.PADDING + Frames.CAP_REACH;

    private static final int GAP = 4;
    private static final int LINE = 10;
    private static final int ROW = 16;
    private static final int SMALL_BUTTON = 14;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 2 * (Frames.CAP_REACH + GAP);
    private static final ItemStack ICON = new ItemStack(Items.EXPERIENCE_BOTTLE);

    private final Screen parent;
    private final StatSheet sheet;
    private final StatSheet.Improvements rules;
    private final int[] increases = new int[ABILITY_COUNT];
    private final List<Button> minusButtons = new ArrayList<>();
    private final List<Button> plusButtons = new ArrayList<>();
    private final List<Button> footer = new ArrayList<>();
    private Button confirm;
    private List<FormattedCharSequence> help = List.of();
    private Area panel;
    private Area content;
    private int helpTop;
    private int rowsTop;
    private int statusTop;

    ImprovementScreen(Screen parent, StatSheet sheet) {
        super(TITLE);
        this.parent = parent;
        this.sheet = sheet;
        this.rules = sheet.level().orElseThrow().improvements();
    }

    @Override
    protected void init() {
        minusButtons.clear();
        plusButtons.clear();
        footer.clear();
        help = font.split(helpText(), CONTENT_WIDTH);
        int contentHeight = Frames.BANNER_HEIGHT
                + GAP
                + help.size() * LINE
                + GAP
                + ABILITY_COUNT * ROW
                + GAP
                + LINE
                + GAP
                + BUTTON_HEIGHT;
        int panelWidth = CONTENT_WIDTH + 2 * INSET;
        int panelHeight = contentHeight + 2 * INSET;
        panel = new Area((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
        content = panel.inset(INSET);
        helpTop = content.top() + Frames.BANNER_HEIGHT + GAP;
        rowsTop = helpTop + help.size() * LINE + GAP;
        statusTop = rowsTop + ABILITY_COUNT * ROW + GAP;
        for (Ability ability : Ability.values()) {
            addAbilityButtons(ability, rowsTop + ability.ordinal() * ROW);
        }
        int buttonsTop = statusTop + LINE + GAP;
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

    private Component helpText() {
        Component splits = SheetText.bonusSplits(rules.options(), ABILITY_COUNT);
        Component max = Component.translatable("screen.checks.improvement.max", rules.maxScore());
        if (rules.pending() > 1) {
            return Component.translatable("screen.checks.improvement.help.pending", splits, max, rules.pending());
        }
        return Component.translatable("screen.checks.improvement.help", splits, max);
    }

    private void addAbilityButtons(Ability ability, int top) {
        int plusLeft = content.right() - SMALL_BUTTON;
        int minusLeft = plusLeft - GAP - SMALL_BUTTON;
        int buttonTop = top + (ROW - SMALL_BUTTON) / 2;
        minusButtons.add(addRenderableWidget(Button.builder(Component.literal("-"), button -> change(ability, -1))
                .bounds(minusLeft, buttonTop, SMALL_BUTTON, SMALL_BUTTON)
                .build()));
        plusButtons.add(addRenderableWidget(Button.builder(Component.literal("+"), button -> change(ability, 1))
                .bounds(plusLeft, buttonTop, SMALL_BUTTON, SMALL_BUTTON)
                .build()));
    }

    private void change(Ability ability, int delta) {
        increases[ability.ordinal()] += delta;
        updateButtons();
    }

    private void updateButtons() {
        for (Ability ability : Ability.values()) {
            minusButtons.get(ability.ordinal()).active = increases[ability.ordinal()] > 0;
            plusButtons.get(ability.ordinal()).active = canRaise(ability);
        }
        confirm.active = complete();
    }

    private boolean canRaise(Ability ability) {
        if (score(ability) + increases[ability.ordinal()] + 1 > rules.maxScore()) {
            return false;
        }
        int[] raised = increases.clone();
        raised[ability.ordinal()]++;
        return BonusSplits.reachable(given(raised), rules.options());
    }

    private boolean complete() {
        return BonusSplits.remaining(given(increases), rules.options()).contains(List.of());
    }

    private static List<Integer> given(int[] values) {
        return Arrays.stream(values).filter(value -> value > 0).boxed().toList();
    }

    private int score(Ability ability) {
        return sheet.ability(ability).score();
    }

    private void submit() {
        confirm.active = false;
        CharacterCreationScreen.sendToServer(
                new ImprovementPayload(Arrays.stream(increases).boxed().toList()));
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
        for (Ability ability : Ability.values()) {
            renderRow(graphics, ability, rowsTop + ability.ordinal() * ROW);
        }
        graphics.drawCenteredString(
                font,
                SheetText.bonusesLeft(BonusSplits.remaining(given(increases), rules.options())),
                width / 2,
                statusTop,
                Frames.GOLD_TEXT);
        footer.forEach(button -> Frames.caps(graphics, button));
    }

    /** {@code Strength   13} or, once raised, {@code Strength   13 → 15} in gold. */
    private void renderRow(GuiGraphics graphics, Ability ability, int top) {
        int textTop = top + (ROW - font.lineHeight) / 2 + 1;
        graphics.drawString(font, SheetText.abilityName(ability), content.left(), textTop, Frames.MUTED);
        int increase = increases[ability.ordinal()];
        Component value = increase == 0
                ? Component.literal(Integer.toString(score(ability)))
                : Component.translatable("screen.checks.improvement.score", score(ability), score(ability) + increase);
        int valueRight = content.right() - 2 * SMALL_BUTTON - 2 * GAP;
        graphics.drawString(
                font, value, valueRight - font.width(value), textTop, increase == 0 ? Frames.TEXT : Frames.GOLD_TEXT);
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
