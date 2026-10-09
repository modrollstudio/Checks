package studio.modroll.checks.creation.client;

import static studio.modroll.checks.creation.client.CreationUi.GAP;
import static studio.modroll.checks.creation.client.CreationUi.KEY;
import static studio.modroll.checks.creation.client.CreationUi.LINE;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.creation.CharacterStory;
import studio.modroll.checks.creation.CreationDraft;
import studio.modroll.checks.preset.Background;
import studio.modroll.checks.preset.ClassPreset;
import studio.modroll.checks.preset.KitItem;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.ui.Area;
import studio.modroll.checks.ui.Frames;

/**
 * The last page, on a sheet of parchment: the character told as a short story, then its abilities,
 * saving throws and starting kit side by side, and a reminder that confirming locks it.
 */
final class SummaryStepView implements StepView {

    private static final int MARGIN = 10;
    private static final int COLUMNS = 3;
    private static final int COLUMN_GAP = 12;
    private static final int ABILITY_TABLE_WIDTH = 72;

    private final CharacterCreationScreen host;
    private Area sheet;
    private Area inner;
    private List<FormattedCharSequence> story = List.of();
    private List<FormattedCharSequence> intro = List.of();
    private int columnsTop;
    private int columnWidth;

    SummaryStepView(CharacterCreationScreen host) {
        this.host = host;
    }

    @Override
    public void init(Area area) {
        sheet = area;
        inner = area.inset(MARGIN);
        story = host.font().split(story(), inner.width());
        intro = host.font().split(Frames.flavor(Component.translatable(KEY + "summary.intro")), inner.width());
        columnsTop = inner.top() + Frames.HEADING_HEIGHT + story.size() * LINE + 2 * GAP;
        columnWidth = (inner.width() - (COLUMNS - 1) * COLUMN_GAP) / COLUMNS;
    }

    private Component story() {
        CreationDraft draft = host.draft();
        Map<PresetKind, Optional<ResourceLocation>> presets = new EnumMap<>(PresetKind.class);
        if (draft.offer().presets().enabled()) {
            Arrays.stream(PresetKind.values()).forEach(kind -> presets.put(kind, draft.choice(kind)));
        }
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        Arrays.stream(Ability.values())
                .forEach(ability -> draft.finalScore(ability).ifPresent(score -> scores.put(ability, score)));
        Component name = Minecraft.getInstance().player == null
                ? Component.empty()
                : Minecraft.getInstance().player.getName();
        return CharacterStory.of(name, presets, scores, proficientSkills());
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        Frames.parchment(graphics, sheet);
    }

    @Override
    public void render(GuiGraphics graphics) {
        int y = inner.top();
        centered(graphics, Frames.heading(Component.translatable(KEY + "story.heading")), inner, y);
        y += Frames.HEADING_HEIGHT;
        for (FormattedCharSequence line : story) {
            graphics.drawString(host.font(), line, inner.left(), y, Frames.INK, false);
            y += LINE;
        }
        renderAbilities(graphics, column(0));
        renderSaves(graphics, column(1));
        renderKit(graphics, column(2));
        int introTop = inner.bottom() - intro.size() * LINE;
        for (FormattedCharSequence line : intro) {
            int left = inner.left() + (inner.width() - host.font().width(line)) / 2;
            graphics.drawString(host.font(), line, left, introTop, Frames.INK_FADED, false);
            introTop += LINE;
        }
    }

    private Area column(int index) {
        int left = inner.left() + index * (columnWidth + COLUMN_GAP);
        return new Area(left, columnsTop, columnWidth, inner.bottom() - columnsTop);
    }

    private void centered(GuiGraphics graphics, Component text, Area area, int y) {
        int left = area.left() + (area.width() - host.font().width(text)) / 2;
        graphics.drawString(host.font(), text, left, y, Frames.INK, false);
    }

    /** One line per ability: its abbreviation, then the final score and modifier at the right. */
    private void renderAbilities(GuiGraphics graphics, Area column) {
        CreationDraft draft = host.draft();
        centered(graphics, Frames.heading(Component.translatable(KEY + "abilities")), column, column.top());
        int tableWidth = Math.min(column.width(), ABILITY_TABLE_WIDTH);
        column = new Area(column.left() + (column.width() - tableWidth) / 2, column.top(), tableWidth, column.height());
        int y = column.top() + Frames.HEADING_HEIGHT;
        for (Ability ability : Ability.values()) {
            graphics.drawString(
                    host.font(), SheetText.abilityAbbreviation(ability), column.left(), y, Frames.INK_FADED, false);
            Component value = draft.finalScore(ability).stream()
                    .mapToObj(score -> Component.translatable(
                            KEY + "summary.score", score, SheetText.signed(Abilities.modifier(score))))
                    .findFirst()
                    .orElse(Component.empty());
            graphics.drawString(host.font(), value, column.right() - host.font().width(value), y, Frames.INK, false);
            host.hover(
                    column.left(),
                    y,
                    column.right(),
                    y + LINE,
                    () -> List.of(SheetText.abilityName(ability), SheetText.abilityDescription(ability)));
            y += LINE;
        }
    }

    private void renderSaves(GuiGraphics graphics, Area column) {
        centered(graphics, Frames.heading(Component.translatable(KEY + "saves")), column, column.top());
        int y = column.top() + Frames.HEADING_HEIGHT;
        for (Ability save : saves()) {
            centered(graphics, SheetText.abilityName(save), column, y);
            y += LINE;
        }
    }

    private void renderKit(GuiGraphics graphics, Area column) {
        if (kit().isEmpty()) {
            return;
        }
        centered(graphics, Frames.heading(Component.translatable(KEY + "kit")), column, column.top());
        int kitWidth = Math.min(column.width(), kit().size() * CreationUi.ITEM_SPACE);
        int left = column.left() + (column.width() - kitWidth) / 2;
        CreationUi.kit(graphics, kit(), new Area(left, column.top() + Frames.HEADING_HEIGHT, kitWidth, 0), host);
    }

    /** The background's skills and the picked ones, the species' included, by name. */
    private List<ResourceLocation> proficientSkills() {
        CreationDraft draft = host.draft();
        return Stream.of(draft.skillPlan().fixed(), draft.picks(), draft.speciesPicks())
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(skill -> SheetText.skillName(skill).getString()))
                .toList();
    }

    private List<Ability> saves() {
        return host.draft().classPreset().map(ClassPreset::saves).orElse(List.of());
    }

    private List<KitItem> kit() {
        return host.draft().background().map(Background::kit).orElse(List.of());
    }
}
