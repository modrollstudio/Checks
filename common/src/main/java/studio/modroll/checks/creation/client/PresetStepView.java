package studio.modroll.checks.creation.client;

import static studio.modroll.checks.creation.client.CreationUi.DOT_SPACE;
import static studio.modroll.checks.creation.client.CreationUi.GAP;
import static studio.modroll.checks.creation.client.CreationUi.KEY;
import static studio.modroll.checks.creation.client.CreationUi.LINE;
import static studio.modroll.checks.creation.client.CreationUi.PADDING;
import static studio.modroll.checks.creation.client.CreationUi.SCROLLBAR_WIDTH;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.body.SizeOption;
import studio.modroll.checks.creation.CreationDraft;
import studio.modroll.checks.creation.PresetOffer;
import studio.modroll.checks.preset.Background;
import studio.modroll.checks.preset.ClassPreset;
import studio.modroll.checks.preset.KitItem;
import studio.modroll.checks.preset.Preset;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.checks.preset.Species;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.sheet.SheetTooltips;
import studio.modroll.checks.ui.Area;
import studio.modroll.checks.ui.Frames;

/**
 * A species, background or class page. On the left, a tile per choice (Custom last) above what the
 * character has so far; on the right, the selected one under a banner, its details, then its flavor
 * text, scrolling when it does not fit.
 */
final class PresetStepView implements StepView {

    private static final float CHOICES_SHARE = 0.4f;
    private static final int TILE_GAP = 2;
    private static final ItemStack CUSTOM_ICON = new ItemStack(Items.NAME_TAG);
    private static final List<PresetKind> SUMMARY = List.of(PresetKind.values());

    private final CharacterCreationScreen host;
    private final PresetKind kind;
    private final ScrollRows rows = new ScrollRows();
    private List<Optional<ResourceLocation>> entries = List.of();
    private Area choicesPanel;
    private Area detailsPanel;
    private Area choices;
    private Area details;
    private int tile;
    private int tileSpace;
    private int perRow;
    private int gridLeft;
    private int summaryTop;
    private int detailsScroll;
    private int detailsContentHeight;
    private Optional<ResourceLocation> shown = Optional.empty();

    PresetStepView(CharacterCreationScreen host, PresetKind kind) {
        this.host = host;
        this.kind = kind;
    }

    @Override
    public void init(Area area) {
        entries = entries();
        choicesPanel = area.leftPart(CHOICES_SHARE, GAP);
        detailsPanel = area.rightPart(CHOICES_SHARE, GAP);
        choices = Frames.well(choicesPanel).inset(PADDING);
        details = Frames.well(detailsPanel).inset(PADDING);
        layoutGrid();
        layoutDetails();
    }

    /**
     * Large tiles when every choice fits in them, else normal ones: whole tiles across the well, leaving
     * room for a scrollbar, centered, the rows scrolling when even those do not fit.
     */
    private void layoutGrid() {
        summaryTop = choices.bottom() - summaryHeight();
        if (!layoutGrid(Frames.LARGE_TILE)) {
            layoutGrid(Frames.TILE);
        }
    }

    /** Lays the grid out in tiles {@code size} wide and returns whether every row fits. */
    private boolean layoutGrid(int size) {
        tile = size;
        tileSpace = size + TILE_GAP;
        int usable = choices.width() - SCROLLBAR_WIDTH - TILE_GAP;
        perRow = Math.max(1, (usable + TILE_GAP) / tileSpace);
        gridLeft = choices.left() + (usable - (perRow * tileSpace - TILE_GAP)) / 2;
        int totalRows = Mth.positiveCeilDiv(entries.size(), perRow);
        int fittingRows = (summaryTop - GAP - choices.top() + TILE_GAP) / tileSpace;
        rows.layout(totalRows, fittingRows);
        return totalRows <= fittingRows;
    }

    /** A newly selected entry starts at the top of its details. */
    private void layoutDetails() {
        Optional<ResourceLocation> selected = host.draft().choice(kind);
        if (!selected.equals(shown)) {
            shown = selected;
            detailsScroll = 0;
        }
        detailsContentHeight = Frames.BANNER_HEIGHT
                + GAP
                + detailBlocks().stream()
                        .mapToInt(block -> block.height(host.font(), blockWidth()))
                        .sum();
        detailsScroll = Mth.clamp(detailsScroll, 0, maxDetailsScroll());
    }

    /** The details' width, short of the scrollbar's column. */
    private int blockWidth() {
        return details.width() - SCROLLBAR_WIDTH - 1;
    }

    private int maxDetailsScroll() {
        return Math.max(0, detailsContentHeight - details.height());
    }

    private static int summaryHeight() {
        return Frames.HEADING_HEIGHT + SUMMARY.size() * LINE;
    }

    /** The offered presets by display name, then Custom. */
    private List<Optional<ResourceLocation>> entries() {
        Stream<Optional<ResourceLocation>> presets = host.draft().offer().presets().presets(kind).stream()
                .map(Preset::id)
                .sorted(Comparator.comparing(id -> name(Optional.of(id)).getString()))
                .map(Optional::of);
        return Stream.concat(presets, Stream.of(Optional.<ResourceLocation>empty()))
                .toList();
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        Frames.panel(graphics, choicesPanel);
        Frames.panel(graphics, detailsPanel);
    }

    @Override
    public void render(GuiGraphics graphics) {
        renderGrid(graphics);
        renderSummary(graphics);
        renderDetails(graphics);
    }

    private void renderGrid(GuiGraphics graphics) {
        int first = rows.first() * perRow;
        int end = Math.min(entries.size(), rows.end() * perRow);
        for (int index = first; index < end; index++) {
            int left = gridLeft + (index % perRow) * tileSpace;
            int top = choices.top() + (index / perRow - rows.first()) * tileSpace;
            Optional<ResourceLocation> entry = entries.get(index);
            boolean hovered = host.over(left, top, left + tile, top + tile);
            Frames.tile(graphics, left, top, tile, icon(entry), isSelected(entry), hovered);
            host.hover(left, top, left + tile, top + tile, () -> about(entry));
        }
        rows.renderScrollbar(graphics, choices.right() - SCROLLBAR_WIDTH, choices.top(), tileSpace);
    }

    /** What the character has so far, one line per kind of preset. */
    private void renderSummary(GuiGraphics graphics) {
        Font font = host.font();
        graphics.drawString(
                font,
                Frames.heading(Component.translatable(KEY + "summary.character")),
                choices.left(),
                summaryTop,
                Frames.TEXT);
        int y = summaryTop + Frames.HEADING_HEIGHT;
        for (PresetKind summarized : SUMMARY) {
            Component label = Component.translatable(KEY + "step." + summarized.id());
            graphics.drawString(font, label, choices.left(), y, Frames.MUTED);
            int valueSpace = choices.width() - font.width(label) - GAP;
            String value = font.plainSubstrByWidth(summaryValue(summarized).getString(), valueSpace);
            int color = host.draft().isChosen(summarized) ? Frames.GOLD_TEXT : Frames.DIM;
            graphics.drawString(font, value, choices.right() - font.width(value), y, color);
            y += LINE;
        }
    }

    private Component summaryValue(PresetKind summarized) {
        CreationDraft draft = host.draft();
        if (!draft.isChosen(summarized)) {
            return Component.translatable(KEY + "summary.unchosen");
        }
        return SheetText.presetName(summarized, draft.choice(summarized));
    }

    private void renderDetails(GuiGraphics graphics) {
        graphics.enableScissor(details.left(), details.top(), details.right(), details.bottom());
        int top = details.top() - detailsScroll;
        Area banner = new Area(details.left(), top, blockWidth(), Frames.BANNER_HEIGHT);
        if (host.draft().isChosen(kind)) {
            Frames.banner(graphics, host.font(), banner, icon(shown), name(shown));
        } else {
            Frames.banner(
                    graphics, host.font(), banner, icon(kind), Component.translatable(KEY + "choose." + kind.id()));
        }
        forEachBlock((block, area) -> block.render(graphics, this, area));
        graphics.disableScissor();
        renderDetailsScrollbar(graphics);
    }

    private void renderDetailsScrollbar(GuiGraphics graphics) {
        if (maxDetailsScroll() == 0) {
            return;
        }
        int thumb = Math.max(LINE, details.height() * details.height() / detailsContentHeight);
        int thumbTop = details.top() + (details.height() - thumb) * detailsScroll / maxDetailsScroll();
        graphics.fill(details.right() - SCROLLBAR_WIDTH, thumbTop, details.right(), thumbTop + thumb, Frames.MUTED);
    }

    /** Each block below the banner with the area it is drawn in, top to bottom. */
    private void forEachBlock(BiConsumer<Block, Area> action) {
        int y = details.top() - detailsScroll + Frames.BANNER_HEIGHT + GAP;
        for (Block block : detailBlocks()) {
            int height = block.height(host.font(), blockWidth());
            action.accept(block, new Area(details.left(), y, blockWidth(), height));
            y += height;
        }
    }

    /** Hover only over the part of {@code area} the details well shows. */
    private void hoverVisible(Area area, Supplier<List<Component>> lines) {
        int top = Math.max(area.top(), details.top());
        int bottom = Math.min(area.bottom(), details.bottom());
        if (top < bottom) {
            host.hover(area.left(), top, area.right(), bottom, lines);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y) {
        return clickTile(x, y) || clickSize(x, y);
    }

    private boolean clickTile(double x, double y) {
        int column = Mth.floor((x - gridLeft) / tileSpace);
        int row = Mth.floor((y - choices.top()) / tileSpace);
        boolean onTile = column >= 0
                && column < perRow
                && row >= 0
                && row < rows.visible()
                && x - gridLeft - column * tileSpace < tile
                && y - choices.top() - row * tileSpace < tile;
        int index = (rows.first() + row) * perRow + column;
        if (!onTile || index >= entries.size()) {
            return false;
        }
        host.draft().choose(kind, entries.get(index));
        return true;
    }

    private boolean clickSize(double x, double y) {
        if (!details.contains(x, y)) {
            return false;
        }
        AtomicBoolean clicked = new AtomicBoolean();
        forEachBlock((block, area) -> {
            if (block instanceof SizeChoice choice && area.contains(x, y)) {
                host.draft().chooseSize(choice.option().size());
                clicked.set(true);
            }
        });
        return clicked.get();
    }

    @Override
    public void mouseScrolled(double x, double y, double scrollY) {
        if (detailsPanel.contains(x, y)) {
            detailsScroll = Mth.clamp(detailsScroll - (int) Math.signum(scrollY) * LINE, 0, maxDetailsScroll());
        } else {
            rows.scroll(scrollY);
        }
    }

    private boolean isSelected(Optional<ResourceLocation> entry) {
        CreationDraft draft = host.draft();
        return draft.isChosen(kind) && draft.choice(kind).equals(entry);
    }

    private Component name(Optional<ResourceLocation> entry) {
        return SheetText.presetName(kind, entry);
    }

    private ItemStack icon(Optional<ResourceLocation> entry) {
        PresetOffer presets = host.draft().offer().presets();
        return entry.flatMap(id -> presets.presets(kind).stream()
                        .filter(preset -> preset.id().equals(id))
                        .findFirst())
                .map(preset -> stack(preset.icon()))
                .orElse(CUSTOM_ICON);
    }

    private static ItemStack icon(PresetKind kind) {
        return stack(kind.defaultIcon());
    }

    private static ItemStack stack(ResourceLocation item) {
        return new ItemStack(BuiltInRegistries.ITEM.get(item));
    }

    private List<Component> about(Optional<ResourceLocation> entry) {
        List<Component> lines = new ArrayList<>();
        lines.add(name(entry));
        SheetText.presetDescription(kind, entry).ifPresent(lines::add);
        return lines;
    }

    /** Nothing until a choice is made; then its details, and its description last as flavor text. */
    private List<Block> detailBlocks() {
        CreationDraft draft = host.draft();
        if (!draft.isChosen(kind)) {
            return List.of();
        }
        Optional<ResourceLocation> entry = draft.choice(kind);
        List<Block> blocks = new ArrayList<>();
        PresetOffer presets = draft.offer().presets();
        switch (kind) {
            case SPECIES -> presets.species(entry).ifPresent(species -> addSpecies(blocks, species, draft));
            case BACKGROUND -> presets.background(entry).ifPresent(background -> addBackground(blocks, background));
            case CLASS -> presets.classPreset(entry).ifPresent(classPreset -> addClass(blocks, classPreset));
        }
        SheetText.presetDescription(kind, entry).ifPresent(description -> {
            blocks.add(new Gap());
            blocks.add(new Text(Frames.flavor(description), Frames.FLAVOR));
        });
        return blocks;
    }

    private static void addSpecies(List<Block> blocks, Species species, CreationDraft draft) {
        addSizes(blocks, species, draft.size());
        if (species.traits().isEmpty()) {
            return;
        }
        addHeading(blocks, "traits");
        for (ResourceLocation trait : species.traits()) {
            blocks.add(new Bullet(SheetText.traitName(trait), Frames.GOLD_TEXT, () -> traitTooltip(trait)));
        }
    }

    /** A single size is only stated; several can be chosen between, the species' default first. */
    private static void addSizes(List<Block> blocks, Species species, Optional<SizeOption> current) {
        if (species.sizes().isEmpty()) {
            return;
        }
        addHeading(blocks, "size");
        if (species.sizes().size() == 1) {
            blocks.add(new Bullet(sizeLabel(species.sizes().getFirst()), Frames.TEXT));
            return;
        }
        for (SizeOption option : species.sizes()) {
            blocks.add(new SizeChoice(option, current.equals(Optional.of(option))));
        }
    }

    private static Component sizeLabel(SizeOption option) {
        return Component.translatable(
                KEY + "size.option", SheetText.sizeName(option.size()), SheetText.scale(option.scale()));
    }

    private void addBackground(List<Block> blocks, Background background) {
        addHeading(blocks, "ability_bonuses");
        blocks.add(new Bullet(abbreviations(background.abilities()), Frames.GOLD_TEXT));
        blocks.add(new Bullet(
                SheetText.bonusSplits(
                        host.draft().offer().presets().bonusOptionsFor(background),
                        background.abilities().size()),
                Frames.MUTED));
        if (!background.skills().isEmpty()) {
            addHeading(blocks, "background_skills");
            for (ResourceLocation skill : background.skills()) {
                blocks.add(new Bullet(SheetText.skillName(skill), Frames.GOLD_TEXT, () -> skillTooltip(skill)));
            }
        }
        if (!background.kit().isEmpty()) {
            addHeading(blocks, "kit");
            blocks.add(new Kit(background.kit()));
        }
    }

    private static void addClass(List<Block> blocks, ClassPreset classPreset) {
        if (!classPreset.saves().isEmpty()) {
            addHeading(blocks, "saves");
            blocks.add(new Bullet(abbreviations(classPreset.saves()), Frames.GOLD_TEXT));
        }
        if (classPreset.skillChoices() > 0) {
            addHeading(blocks, "class_skills");
            blocks.add(new Text(
                    Component.translatable(KEY + "class_skills.choose", classPreset.skillChoices()), Frames.MUTED));
            blocks.add(new Bullet(
                    ComponentUtils.formatList(
                            classPreset.skillOptions().stream()
                                    .map(SheetText::skillName)
                                    .toList(),
                            Component.literal(", ")),
                    Frames.TEXT));
        }
        addHeading(blocks, "suggested");
        blocks.add(new Bullet(
                SheetText.suggestedOrder(classPreset.suggestedOrder()),
                Frames.TEXT,
                () -> List.of(Component.translatable(KEY + "suggested.tooltip"))));
    }

    private static void addHeading(List<Block> blocks, String key) {
        blocks.add(new Gap());
        blocks.add(new Heading(Component.translatable(KEY + key)));
    }

    private static Component abbreviations(List<Ability> abilities) {
        return ComponentUtils.formatList(
                abilities.stream().map(SheetText::abilityAbbreviation).toList(), Component.literal(", "));
    }

    private static List<Component> traitTooltip(ResourceLocation trait) {
        List<Component> lines = new ArrayList<>();
        lines.add(SheetText.traitName(trait));
        SheetText.traitDescription(trait).ifPresent(lines::add);
        return lines;
    }

    private List<Component> skillTooltip(ResourceLocation skill) {
        List<Component> lines = new ArrayList<>();
        lines.add(SheetText.skillName(skill));
        host.draft().offer().skills().stream()
                .filter(offered -> offered.id().equals(skill))
                .findFirst()
                .ifPresent(offered -> lines.addAll(SheetTooltips.about(skill, offered.ability())));
        return lines;
    }

    /** One piece of the details below the banner. */
    private sealed interface Block permits Heading, Text, Bullet, Gap, Kit, SizeChoice {

        int height(Font font, int width);

        /** Draws at the area's top left, {@code area.width()} wide. */
        void render(GuiGraphics graphics, PresetStepView view, Area area);
    }

    private record Heading(Component text) implements Block {

        @Override
        public int height(Font font, int width) {
            return Frames.HEADING_HEIGHT;
        }

        @Override
        public void render(GuiGraphics graphics, PresetStepView view, Area area) {
            graphics.drawString(view.host.font(), Frames.heading(text), area.left(), area.top(), Frames.TEXT);
        }
    }

    /** Wrapped text; hovering it shows {@code tooltip} when there is one. */
    private record Text(Component text, int color, Supplier<List<Component>> tooltip) implements Block {

        Text(Component text, int color) {
            this(text, color, List::of);
        }

        @Override
        public int height(Font font, int width) {
            return font.split(text, width).size() * LINE;
        }

        @Override
        public void render(GuiGraphics graphics, PresetStepView view, Area area) {
            Frames.lines(
                    graphics,
                    view.host.font(),
                    view.host.font().split(text, area.width()),
                    area.left(),
                    area.top(),
                    color);
            view.hoverVisible(area, tooltip);
        }
    }

    /** A bulleted line, its wrapped lines indented past the bullet. */
    private record Bullet(Component text, int color, Supplier<List<Component>> tooltip) implements Block {

        Bullet(Component text, int color) {
            this(text, color, List::of);
        }

        @Override
        public int height(Font font, int width) {
            return font.split(text, width - Frames.BULLET_INDENT).size() * LINE;
        }

        @Override
        public void render(GuiGraphics graphics, PresetStepView view, Area area) {
            Font font = view.host.font();
            List<FormattedCharSequence> lines = font.split(text, area.width() - Frames.BULLET_INDENT);
            Frames.bullet(graphics, font, lines, area.left(), area.top(), color);
            view.hoverVisible(area, tooltip);
        }
    }

    /** One size to pick: a dot, filled when it is the character's size, then its name and scale. */
    private record SizeChoice(SizeOption option, boolean selected) implements Block {

        @Override
        public int height(Font font, int width) {
            return LINE;
        }

        @Override
        public void render(GuiGraphics graphics, PresetStepView view, Area area) {
            boolean hovered = view.host.over(area.left(), area.top(), area.right(), area.bottom());
            int color = selected ? Frames.GOLD_TEXT : hovered ? Frames.TEXT : Frames.MUTED;
            CreationUi.dot(graphics, view.host.font(), area.left(), area.top(), selected, color);
            graphics.drawString(view.host.font(), sizeLabel(option), area.left() + DOT_SPACE, area.top(), color);
            view.hoverVisible(area, () -> List.of(Component.translatable(KEY + "size.choose")));
        }
    }

    private record Gap() implements Block {

        @Override
        public int height(Font font, int width) {
            return GAP;
        }

        @Override
        public void render(GuiGraphics graphics, PresetStepView view, Area area) {}
    }

    private record Kit(List<KitItem> items) implements Block {

        @Override
        public int height(Font font, int width) {
            return CreationUi.kitHeight(items, width);
        }

        @Override
        public void render(GuiGraphics graphics, PresetStepView view, Area area) {
            CreationUi.kit(graphics, items, area, view.host);
        }
    }
}
