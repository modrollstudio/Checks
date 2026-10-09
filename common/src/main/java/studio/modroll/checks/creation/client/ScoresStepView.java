package studio.modroll.checks.creation.client;

import static studio.modroll.checks.creation.client.CreationUi.BUTTON_HEIGHT;
import static studio.modroll.checks.creation.client.CreationUi.CHIP;
import static studio.modroll.checks.creation.client.CreationUi.GAP;
import static studio.modroll.checks.creation.client.CreationUi.KEY;
import static studio.modroll.checks.creation.client.CreationUi.LINE;
import static studio.modroll.checks.creation.client.CreationUi.PADDING;
import static studio.modroll.checks.creation.client.CreationUi.ROW;
import static studio.modroll.checks.creation.client.CreationUi.SELECTED;
import static studio.modroll.checks.creation.client.CreationUi.SMALL_BUTTON;
import static studio.modroll.checks.creation.client.CreationUi.VALUE;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.creation.CreationDraft;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.CreationRollPayload;
import studio.modroll.checks.creation.RollAnnouncement;
import studio.modroll.checks.creation.RolledScore;
import studio.modroll.checks.preset.Background;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.ui.Area;
import studio.modroll.checks.ui.Frames;

/**
 * The scores page. The left panel holds one row per ability: standard array and roll values are chips
 * placed onto abilities, and a background's bonus sits beside the placed score with its own - / +
 * buttons, followed by the total, e.g. {@code 15 +2 = 17}. The right panel holds the method choice and
 * its description, the chips still to place, and what is left of the background bonus.
 */
final class ScoresStepView implements StepView {

    private static final float ABILITIES_SHARE = 0.5f;
    private static final int ABILITY_COUNT = Ability.values().length;
    private static final int METHODS_PER_ROW = 2;
    private static final int MAX_ROW = 22;
    private static final int CHIP_HEIGHT = 14;
    private static final int MAX_CHIP_WIDTH = 24;
    private static final int ABBREVIATION_WIDTH = 24;
    private static final int SLOT_WIDTH = 24;
    private static final int BONUS_WIDTH = 18;
    private static final int TOTAL_WIDTH = 30;
    private static final int MODIFIER_WIDTH = 24;
    private static final int SUGGEST_BUTTON_WIDTH = 90;

    private final CharacterCreationScreen host;
    private Area abilitiesPanel;
    private Area methodPanel;
    private Area abilities;
    private Area method;
    private int rowsTop;
    private int rowHeight;
    private int descriptionTop;
    private int poolTop;
    private int chipsTop;
    private int bonusTop;
    private int chipWidth;
    private List<FormattedCharSequence> description = List.of();
    private List<FormattedCharSequence> bonusHelp = List.of();

    ScoresStepView(CharacterCreationScreen host) {
        this.host = host;
    }

    @Override
    public void init(Area area) {
        abilitiesPanel = area.leftPart(ABILITIES_SHARE, GAP);
        methodPanel = area.rightPart(ABILITIES_SHARE, GAP);
        abilities = Frames.well(abilitiesPanel).inset(PADDING);
        method = Frames.well(methodPanel).inset(PADDING);
        rowsTop = abilities.top() + SMALL_BUTTON + GAP;
        rowHeight = Mth.clamp((abilities.bottom() - rowsTop) / ABILITY_COUNT, ROW, MAX_ROW);
        layoutMethod();
        addMethodButtons();
        addSuggestButton();
        addAbilityControls();
    }

    /** The method buttons, two to a row, then the description, the chips and the background bonus. */
    private void layoutMethod() {
        CreationDraft draft = host.draft();
        int methodRows = Mth.positiveCeilDiv(draft.offer().methods().size(), METHODS_PER_ROW);
        descriptionTop = method.top() + Frames.HEADING_HEIGHT + methodRows * (BUTTON_HEIGHT + GAP);
        description = host.font().split(methodDescription(), method.width());
        poolTop = descriptionTop + description.size() * LINE + GAP;
        chipsTop = poolTop + Frames.HEADING_HEIGHT;
        int chipCount = Math.max(1, draft.pool().size());
        chipWidth = Math.min(MAX_CHIP_WIDTH, (method.width() - (chipCount - 1) * GAP) / chipCount);
        bonusTop = poolTop + poolHeight();
        bonusHelp = bonusHelp(method.width());
    }

    /** The Roll button before rolling, the chips after; their place stays once all are placed. */
    private int poolHeight() {
        CreationDraft draft = host.draft();
        if (draft.awaitingRoll()) {
            return BUTTON_HEIGHT + GAP;
        }
        return draft.usesPool() ? Frames.HEADING_HEIGHT + CHIP_HEIGHT + GAP : 0;
    }

    private Component methodDescription() {
        CreationDraft draft = host.draft();
        String key = KEY + "description." + draft.method().id();
        if (draft.method() == CreationMethod.POINT_BUY) {
            return Component.translatable(
                    key,
                    draft.offer().pointBuy().min(),
                    draft.offer().pointBuy().budget());
        }
        return Component.translatable(key);
    }

    private List<FormattedCharSequence> bonusHelp(int width) {
        Optional<Background> background = host.draft().background();
        if (background.isEmpty()) {
            return List.of();
        }
        Component help = Component.translatable(
                KEY + "bonus_help",
                SheetText.presetName(
                        PresetKind.BACKGROUND, Optional.of(background.get().id())),
                ComponentUtils.formatList(
                        background.get().abilities().stream()
                                .map(SheetText::abilityAbbreviation)
                                .toList(),
                        Component.translatable("checks.list.comma")),
                SheetText.bonusSplits(
                        host.draft().bonusOptions(),
                        background.get().abilities().size()));
        return host.font().split(help, width);
    }

    private void addMethodButtons() {
        CreationDraft draft = host.draft();
        List<CreationMethod> methods = draft.offer().methods();
        int buttonWidth = (method.width() - (METHODS_PER_ROW - 1) * GAP) / METHODS_PER_ROW;
        int buttonsTop = method.top() + Frames.HEADING_HEIGHT;
        for (int i = 0; i < methods.size(); i++) {
            CreationMethod choice = methods.get(i);
            boolean selected = choice == draft.method();
            Component label = Component.translatable(KEY + "method." + choice.id());
            int left = method.left() + (i % METHODS_PER_ROW) * (buttonWidth + GAP);
            int top = buttonsTop + (i / METHODS_PER_ROW) * (BUTTON_HEIGHT + GAP);
            Button button = host.addControl(
                    Button.builder(selected ? label.copy().withStyle(ChatFormatting.YELLOW) : label, b -> {
                                draft.select(choice);
                                host.refresh();
                            })
                            .bounds(left, top, buttonWidth, BUTTON_HEIGHT)
                            .tooltip(Tooltip.create(Component.translatable(KEY + "method." + choice.id() + ".tooltip")))
                            .build());
            button.active = !selected && draft.offer().allows(choice);
        }
    }

    private void addSuggestButton() {
        CreationDraft draft = host.draft();
        if (!draft.usesPool() || draft.classPreset().isEmpty()) {
            return;
        }
        List<Ability> order = draft.classPreset().orElseThrow().suggestedOrder();
        Component tooltip = Component.translatable(
                KEY + "use_suggested.tooltip",
                SheetText.abilityName(order.getFirst()),
                SheetText.abilityName(order.get(1)),
                SheetText.abilityName(order.getLast()));
        host.addControl(Button.builder(Component.translatable(KEY + "use_suggested"), button -> {
                    draft.useSuggested();
                    host.refresh();
                })
                .bounds(right() - SUGGEST_BUTTON_WIDTH, abilities.top(), SUGGEST_BUTTON_WIDTH, SMALL_BUTTON)
                .tooltip(Tooltip.create(tooltip))
                .build());
    }

    private void addAbilityControls() {
        CreationDraft draft = host.draft();
        if (draft.awaitingRoll()) {
            host.addControl(Button.builder(Component.translatable(KEY + "roll"), button -> {
                        button.active = false;
                        CharacterCreationScreen.sendToServer(new CreationRollPayload(draft.method()));
                    })
                    .bounds(method.left(), poolTop, method.width(), BUTTON_HEIGHT)
                    .build());
            return;
        }
        for (Ability ability : Ability.values()) {
            int top = rowTop(ability) + (rowHeight - SMALL_BUTTON) / 2;
            if (draft.method() == CreationMethod.POINT_BUY) {
                addStepButtons(
                        top,
                        controlsLeft(),
                        draft.canLower(ability),
                        draft.canRaise(ability),
                        () -> draft.lower(ability),
                        () -> draft.raise(ability),
                        SLOT_WIDTH);
            }
            if (draft.takesBonus(ability)) {
                Button raise = addStepButtons(
                        top,
                        bonusLeft(),
                        draft.canLowerBonus(ability),
                        draft.canRaiseBonus(ability),
                        () -> draft.lowerBonus(ability),
                        () -> draft.raiseBonus(ability),
                        BONUS_WIDTH);
                draft.raiseLimit(ability)
                        .ifPresent(limit -> raise.setTooltip(Tooltip.create(limitText(limit, ability))));
            }
        }
    }

    /** A - and a + button with {@code valueWidth} between them; returns the + button. */
    private Button addStepButtons(
            int top, int left, boolean canLower, boolean canRaise, Runnable lower, Runnable raise, int valueWidth) {
        host.addControl(CreationUi.smallButton("-", left, top, lower, host)).active = canLower;
        Button plus = host.addControl(CreationUi.smallButton("+", left + SMALL_BUTTON + valueWidth, top, raise, host));
        plus.active = canRaise;
        return plus;
    }

    /** Why a bonus + is disabled, in plain words. */
    private Component limitText(CreationDraft.BonusLimit limit, Ability ability) {
        String key = KEY + "bonus_limit." + limit.name().toLowerCase(Locale.ROOT);
        return switch (limit) {
            case ALL_USED -> Component.translatable(key);
            case NO_SPLIT ->
                Component.translatable(
                        key,
                        SheetText.bonusSplits(
                                host.draft().bonusOptions(),
                                host.draft()
                                        .background()
                                        .orElseThrow()
                                        .abilities()
                                        .size()));
            case OVER_MAX ->
                Component.translatable(
                        key,
                        SheetText.abilityName(ability),
                        host.draft().offer().presets().bonusMaxScore());
        };
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        Frames.panel(graphics, abilitiesPanel);
        Frames.panel(graphics, methodPanel);
    }

    @Override
    public void render(GuiGraphics graphics) {
        renderAbilities(graphics);
        renderMethod(graphics);
    }

    private void renderAbilities(GuiGraphics graphics) {
        CreationDraft draft = host.draft();
        int headingTop = abilities.top() + (SMALL_BUTTON - host.font().lineHeight) / 2 + 1;
        graphics.drawString(
                host.font(),
                Frames.heading(Component.translatable(KEY + "abilities")),
                left(),
                headingTop,
                Frames.TEXT);
        if (draft.method() == CreationMethod.POINT_BUY) {
            Component points = Component.translatable(
                    KEY + "points", draft.pointsLeft(), draft.offer().pointBuy().budget());
            graphics.drawString(host.font(), points, right() - host.font().width(points), headingTop, Frames.GOLD_TEXT);
        }
        for (Ability ability : Ability.values()) {
            renderAbilityRow(graphics, ability);
        }
    }

    private void renderMethod(GuiGraphics graphics) {
        CreationDraft draft = host.draft();
        graphics.drawString(
                host.font(),
                Frames.heading(Component.translatable(KEY + "method")),
                method.left(),
                method.top(),
                Frames.TEXT);
        Frames.lines(graphics, host.font(), description, method.left(), descriptionTop, Frames.MUTED);
        if (draft.usesPool()) {
            graphics.drawString(
                    host.font(),
                    Frames.heading(Component.translatable(KEY + "pool")),
                    method.left(),
                    poolTop,
                    Frames.TEXT);
            renderChips(graphics);
        }
        if (bonusHelp.isEmpty()) {
            return;
        }
        graphics.drawString(
                host.font(),
                Frames.heading(Component.translatable(KEY + "bonus")),
                method.left(),
                bonusTop,
                Frames.TEXT);
        int y = bonusTop + Frames.HEADING_HEIGHT;
        if (!draft.awaitingRoll()) {
            graphics.drawString(
                    host.font(), SheetText.bonusesLeft(draft.bonusesLeft()), method.left(), y, Frames.GOLD_TEXT);
            y += LINE;
        }
        Frames.lines(graphics, host.font(), bonusHelp, method.left(), y, Frames.MUTED);
    }

    /** Placed values leave a gap, so the remaining chips never shift under the cursor; none left says so. */
    private void renderChips(GuiGraphics graphics) {
        CreationDraft draft = host.draft();
        List<Integer> pool = draft.pool();
        if (IntStream.range(0, pool.size()).noneMatch(draft::inPool)) {
            graphics.drawString(
                    host.font(),
                    Component.translatable(KEY + "pool.placed"),
                    method.left(),
                    textTop(chipsTop),
                    Frames.DIM);
            return;
        }
        for (int chip = 0; chip < pool.size(); chip++) {
            if (!draft.inPool(chip)) {
                continue;
            }
            int left = chipLeft(chip);
            int right = left + chipWidth;
            int bottom = chipsTop + CHIP_HEIGHT;
            boolean selected = draft.selectedChip().equals(OptionalInt.of(chip));
            graphics.fill(left, chipsTop, right, bottom, CHIP);
            int outline = selected ? SELECTED : host.over(left, chipsTop, right, bottom) ? VALUE : Frames.DIM;
            graphics.renderOutline(left, chipsTop, chipWidth, CHIP_HEIGHT, outline);
            graphics.drawCenteredString(
                    host.font(), Integer.toString(pool.get(chip)), left + chipWidth / 2, textTop(chipsTop), VALUE);
            rolledScore(chip)
                    .ifPresent(score -> host.hover(left, chipsTop, right, bottom, () -> List.of(rollText(score))));
        }
    }

    private void renderAbilityRow(GuiGraphics graphics, Ability ability) {
        CreationDraft draft = host.draft();
        int rowTop = rowTop(ability);
        int boxTop = rowTop + (rowHeight - CHIP_HEIGHT) / 2;
        int textTop = textTop(boxTop);
        graphics.drawString(host.font(), SheetText.abilityAbbreviation(ability), left(), textTop, Frames.MUTED);
        host.hover(
                left(),
                rowTop,
                controlsLeft(),
                rowTop + rowHeight,
                () -> List.of(SheetText.abilityName(ability), SheetText.abilityDescription(ability)));
        renderSlot(graphics, ability, boxTop);
        if (draft.takesBonus(ability)) {
            graphics.drawCenteredString(
                    host.font(),
                    SheetText.signed(draft.bonus(ability)),
                    bonusLeft() + SMALL_BUTTON + BONUS_WIDTH / 2,
                    textTop,
                    draft.bonus(ability) > 0 ? Frames.GOLD_TEXT : Frames.MUTED);
            host.hover(
                    bonusLeft(),
                    rowTop,
                    totalLeft(),
                    rowTop + rowHeight,
                    () -> List.of(Component.translatable(KEY + "bonus.tooltip")));
        }
        OptionalInt score = draft.finalScore(ability);
        if (score.isEmpty()) {
            return;
        }
        if (draft.bonus(ability) > 0) {
            graphics.drawString(
                    host.font(),
                    Component.translatable(KEY + "total", score.getAsInt()),
                    totalLeft(),
                    textTop,
                    Frames.TEXT);
        }
        graphics.drawString(
                host.font(),
                SheetText.signed(Abilities.modifier(score.getAsInt())),
                modifierLeft(),
                textTop,
                Frames.MUTED);
        int diceLeft = modifierLeft() + MODIFIER_WIDTH;
        placedRoll(ability)
                .ifPresent(rolled -> graphics.drawString(
                        host.font(),
                        clip(RollAnnouncement.dice(rolled.dice()), right() - diceLeft),
                        diceLeft,
                        textTop,
                        Frames.DIM));
    }

    /** Point buy draws its value between the - / + buttons; the other methods draw a slot that holds a chip. */
    private void renderSlot(GuiGraphics graphics, Ability ability, int top) {
        CreationDraft draft = host.draft();
        int slotLeft = controlsLeft() + SMALL_BUTTON;
        if (draft.method() != CreationMethod.POINT_BUY) {
            boolean placing = draft.selectedChip().isPresent();
            boolean hovered = draft.usesPool() && host.over(left(), top, bonusLeft(), top + CHIP_HEIGHT);
            graphics.fill(slotLeft, top, slotLeft + SLOT_WIDTH, top + CHIP_HEIGHT, CHIP);
            graphics.renderOutline(
                    slotLeft, top, SLOT_WIDTH, CHIP_HEIGHT, placing ? SELECTED : hovered ? VALUE : Frames.DIM);
        }
        draft.score(ability)
                .ifPresent(score -> graphics.drawCenteredString(
                        host.font(), Integer.toString(score), slotLeft + SLOT_WIDTH / 2, textTop(top), VALUE));
    }

    @Override
    public boolean mouseClicked(double x, double y) {
        CreationDraft draft = host.draft();
        if (!draft.usesPool()) {
            return false;
        }
        return clickChip(x, y) || clickSlot(x, y);
    }

    private boolean clickChip(double x, double y) {
        CreationDraft draft = host.draft();
        if (x < method.left() || y < chipsTop || y >= chipsTop + CHIP_HEIGHT) {
            return false;
        }
        int chip = (int) (x - method.left()) / (chipWidth + GAP);
        boolean onChip = chip < draft.pool().size() && x < chipLeft(chip) + chipWidth && draft.inPool(chip);
        if (onChip) {
            draft.selectChip(chip);
        }
        return onChip;
    }

    private boolean clickSlot(double x, double y) {
        int row = Mth.floor((y - rowsTop) / rowHeight);
        boolean onSlots = x >= left() && x < bonusLeft() && y >= rowsTop && row < ABILITY_COUNT;
        return onSlots && host.draft().clickAbility(Ability.values()[row]);
    }

    private Optional<RolledScore> rolledScore(int chip) {
        CreationDraft draft = host.draft();
        return draft.offer().rolledScores(draft.method()).map(scores -> scores.get(chip));
    }

    private Optional<RolledScore> placedRoll(Ability ability) {
        OptionalInt chip = host.draft().chip(ability);
        return chip.isPresent() ? rolledScore(chip.getAsInt()) : Optional.empty();
    }

    private static Component rollText(RolledScore score) {
        return Component.translatable(KEY + "roll_result", RollAnnouncement.dice(score.dice()), score.total());
    }

    private FormattedCharSequence clip(Component text, int maxWidth) {
        return Language.getInstance().getVisualOrder(host.font().substrByWidth(text, maxWidth));
    }

    private int rowTop(Ability ability) {
        return rowsTop + ability.ordinal() * rowHeight;
    }

    private int textTop(int boxTop) {
        return boxTop + (CHIP_HEIGHT - host.font().lineHeight) / 2 + 1;
    }

    private int chipLeft(int chip) {
        return method.left() + chip * (chipWidth + GAP);
    }

    private int left() {
        return abilities.left();
    }

    private int right() {
        return abilities.right();
    }

    private int controlsLeft() {
        return left() + ABBREVIATION_WIDTH;
    }

    private int bonusLeft() {
        return controlsLeft() + 2 * SMALL_BUTTON + SLOT_WIDTH + GAP;
    }

    private int totalLeft() {
        return bonusLeft() + 2 * SMALL_BUTTON + BONUS_WIDTH + GAP;
    }

    private int modifierLeft() {
        return totalLeft() + TOTAL_WIDTH;
    }
}
