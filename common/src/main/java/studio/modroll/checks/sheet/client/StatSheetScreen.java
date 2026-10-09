package studio.modroll.checks.sheet.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.api.SheetSection;
import studio.modroll.checks.creation.client.CharacterCreationScreen;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.checks.sheet.SheetLayout;
import studio.modroll.checks.sheet.SheetSectionRegistry;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.sheet.SheetTooltips;
import studio.modroll.checks.sheet.StatSheet;
import studio.modroll.checks.ui.Area;
import studio.modroll.checks.ui.Frames;

/**
 * The character sheet: one parchment sheet with two tabs under a shared header (the player's face and
 * name, species, background and class, level, XP bar and proficiency bonus). Overview has a card per
 * ability, then the saves, passive scores, body and the sections other mods add; Skills lists every skill.
 * Gold is kept for headings and ability modifiers; breakdowns and the proficiency legend are in tooltips.
 * Its only actions are the tabs and the footer buttons.
 */
public final class StatSheetScreen extends Screen {

    public static final KeyMapping KEY = new KeyMapping(
            "key.checks.stat_sheet", InputConstants.Type.KEYSYM, InputConstants.KEY_K, "key.categories.checks");

    private static final Component TITLE = Component.translatable("screen.checks.stat_sheet");
    private static final Component PROFICIENCY_BONUS = Component.translatable("screen.checks.proficiency_bonus");
    private static final Component SAVES = Component.translatable("screen.checks.saving_throws");
    private static final Component PASSIVES = Component.translatable("screen.checks.passive_scores");
    private static final Component CREATE_CHARACTER = Component.translatable("screen.checks.create_character");
    private static final Component IDENTITY_SEPARATOR = Component.translatable("screen.checks.identity.separator");
    private static final Component LEVEL_UP = Component.translatable("screen.checks.level_up");

    private static final int GAP = SheetLayout.GAP;
    private static final int LINE = SheetLayout.LINE;
    private static final float LARGE_TEXT = 2.0f;
    private static final int LARGE_TEXT_HEIGHT = 16;
    private static final int FACE_INSET = 2;
    private static final int DOT = 5;
    private static final int DOTS_WIDTH = 2 * DOT + 1 + GAP;
    private static final int TOOLTIP_WIDTH = 200;
    private static final int XP_BAR_WIDTH = 100;
    private static final int XP_BAR_HEIGHT = 4;
    /** Centers the tab labels, which have no descenders worth room. */
    private static final int TAB_TEXT_DROP = 4;

    private static final int XP_TRACK = 0xFFBFAF8A;
    private static final int XP_FILL = 0xFF7D9A4C;

    /** The tab the sheet last showed, so it reopens there. */
    private static Tab tab = Tab.OVERVIEW;

    private final StatSheet sheet;
    private final List<StatSheet.SkillLine> skills;
    private final List<Block> blocks;
    private final List<Button> buttons = new ArrayList<>();
    private SheetLayout layout;
    private int firstSkillRow;
    private int overviewScroll;
    /** While the Overview draws: hovers count only inside its view, against its scrolled content. */
    private Optional<Area> hoverView = Optional.empty();

    private int mouseX;
    private int mouseY;
    private List<Component> tooltip = List.of();

    private enum Tab {
        OVERVIEW(Component.translatable("screen.checks.tab.overview")),
        SKILLS(Component.translatable("screen.checks.skills"));

        private final Component label;

        Tab(Component label) {
            this.label = label;
        }
    }

    /** A heading and its rows on the Overview tab; hovering the heading shows {@code about}, when it has lines. */
    private record Block(Component title, Supplier<List<Component>> about, List<Row> rows) {}

    /** A label and value; {@code dots} shows a proficiency before the label. */
    private record Row(Optional<Proficiency> dots, Component label, String value, Supplier<List<Component>> tooltip) {}

    private StatSheetScreen(StatSheet sheet) {
        super(TITLE);
        this.sheet = sheet;
        this.skills = sheet.skills().stream()
                .sorted(Comparator.comparing(
                        line -> SheetText.skillName(line.id()).getString()))
                .toList();
        this.blocks = blocks(sheet);
    }

    /**
     * Opens on arrival unless the player has opened another screen since pressing the key; a sheet sent
     * after an improvement replaces the open sheet or choice screen.
     */
    public static void open(StatSheet sheet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null
                || minecraft.screen instanceof StatSheetScreen
                || minecraft.screen instanceof ImprovementScreen) {
            minecraft.setScreen(new StatSheetScreen(sheet));
        }
    }

    /** Saves, passive scores, the body when it has rows, then the sections other mods add. */
    private static List<Block> blocks(StatSheet sheet) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block(
                SAVES,
                SheetTooltips::legend,
                sheet.abilities().stream()
                        .map(line -> new Row(
                                Optional.of(line.saveProficiency()),
                                SheetText.abilityName(line.ability()),
                                SheetText.signed(line.saveModifier()),
                                () -> SheetTooltips.save(line, sheet.proficiencyBonus())))
                        .toList()));
        blocks.add(new Block(
                PASSIVES,
                List::of,
                sheet.passives().stream()
                        .map(line -> new Row(
                                Optional.empty(),
                                SheetText.skillName(line.skill()),
                                Integer.toString(line.score()),
                                SheetTooltips::passive))
                        .toList()));
        SheetTooltips.body(sheet.body()).ifPresent(body -> blocks.add(block(body)));
        SheetSectionRegistry.sectionsFor(sheet).forEach(section -> blocks.add(block(section)));
        return blocks;
    }

    private static Block block(SheetSection section) {
        return new Block(
                section.title(),
                List::of,
                section.rows().stream()
                        .map(row -> new Row(
                                Optional.empty(), row.label(), row.value().getString(), row::tooltip))
                        .toList());
    }

    @Override
    protected void init() {
        List<FooterButton> footer = footerButtons();
        layout = SheetLayout.of(new SheetLayout.Inputs(
                width,
                height,
                blocks.stream().map(block -> block.rows().size()).toList(),
                skills.size(),
                footer.size(),
                tabLabelWidth()));
        firstSkillRow = Mth.clamp(firstSkillRow, 0, maxFirstSkillRow());
        overviewScroll = Mth.clamp(overviewScroll, 0, layout.maxOverviewScroll());
        buttons.clear();
        for (int i = 0; i < footer.size(); i++) {
            Area bounds = layout.buttons().get(i);
            buttons.add(addRenderableWidget(
                    Button.builder(footer.get(i).label(), footer.get(i).onPress())
                            .bounds(bounds.left(), bounds.top(), bounds.width(), bounds.height())
                            .build()));
        }
    }

    private record FooterButton(Component label, Button.OnPress onPress) {}

    private List<FooterButton> footerButtons() {
        List<FooterButton> buttons = new ArrayList<>();
        if (sheet.creationAvailable()) {
            buttons.add(new FooterButton(CREATE_CHARACTER, button -> CharacterCreationScreen.requestOpen()));
        }
        if (pendingImprovements() > 0) {
            buttons.add(new FooterButton(LEVEL_UP, button -> minecraft.setScreen(new ImprovementScreen(this, sheet))));
        }
        return buttons;
    }

    private int pendingImprovements() {
        return sheet.level().map(line -> line.improvements().pending()).orElse(0);
    }

    private int tabLabelWidth() {
        int widest = 0;
        for (Tab each : Tab.values()) {
            widest = Math.max(widest, font.width(each.label));
        }
        return widest;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        tooltip = List.of();
        buttons.forEach(button -> Frames.caps(graphics, button));
        Frames.sheet(graphics, area(layout.sheet()));
        renderTabs(graphics);
        renderHeader(graphics);
        if (tab == Tab.OVERVIEW) {
            renderOverview(graphics);
        } else {
            renderSkills(graphics);
        }
        WrappedTooltip.render(graphics, font, tooltip, TOOLTIP_WIDTH, mouseX, mouseY);
    }

    /** The ability cards and blocks, clipped to the view and shifted up by the scroll. */
    private void renderOverview(GuiGraphics graphics) {
        Area view = layout.overview();
        graphics.enableScissor(view.left(), view.top(), view.right(), view.bottom());
        graphics.pose().pushPose();
        graphics.pose().translate(0, -overviewScroll, 0);
        hoverView = Optional.of(view);
        renderAbilities(graphics);
        for (int i = 0; i < blocks.size(); i++) {
            renderBlock(graphics, blocks.get(i), layout.blocks().get(i));
        }
        hoverView = Optional.empty();
        graphics.pose().popPose();
        graphics.disableScissor();
        renderScrollbar(graphics, view, overviewScroll, layout.maxOverviewScroll(), layout.overviewHeight());
    }

    private void renderTabs(GuiGraphics graphics) {
        for (Tab each : Tab.values()) {
            Area bounds = layout.tabs().get(each.ordinal());
            boolean open = each == tab;
            Frames.tab(graphics, area(bounds), open);
            int color = open ? Frames.INK : bounds.contains(mouseX, mouseY) ? Frames.INK_SOFT : Frames.INK_FADED;
            int left = bounds.left() + (bounds.width() - font.width(each.label)) / 2;
            graphics.drawString(font, each.label, left, bounds.top() + TAB_TEXT_DROP, color, false);
        }
    }

    /** The face and name with the character line under them; the level, XP bar and proficiency bonus at the right. */
    private void renderHeader(GuiGraphics graphics) {
        Area header = layout.header();
        Frames.card(graphics, new Area(header.left(), header.top(), Frames.TILE, Frames.TILE));
        PlayerFaceRenderer.draw(
                graphics,
                minecraft.player.getSkin(),
                header.left() + FACE_INSET,
                header.top() + FACE_INSET,
                Frames.TILE - 2 * FACE_INSET);
        int textLeft = header.left() + Frames.TILE + GAP + GAP;
        int nameTop = header.top() + 1;
        int lineTop = nameTop + LINE;
        graphics.drawString(font, minecraft.player.getName(), textLeft, nameTop, Frames.INK_GOLD, false);
        sheet.level().ifPresent(line -> renderLevel(graphics, line, header.right(), nameTop));
        sheet.identity().ifPresent(identity -> renderIdentity(graphics, identity, textLeft, lineTop));
        renderProficiencyBonus(graphics, header.right(), lineTop);
    }

    private static Area area(Area rect) {
        return new Area(rect.left(), rect.top(), rect.width(), rect.height());
    }

    /** Species, background and class on one line, each explained on hover; the species with its traits. */
    private void renderIdentity(GuiGraphics graphics, StatSheet.Identity identity, int left, int identityTop) {
        List<PresetKind> kinds = List.of(PresetKind.values());
        for (PresetKind kind : kinds) {
            if (kind != kinds.getFirst()) {
                graphics.drawString(font, IDENTITY_SEPARATOR, left, identityTop, Frames.INK_FADED, false);
                left += font.width(IDENTITY_SEPARATOR);
            }
            Optional<ResourceLocation> choice = identity.choices().get(kind);
            Component name = SheetText.presetName(kind, choice);
            graphics.drawString(font, name, left, identityTop, Frames.INK_SOFT, false);
            List<ResourceLocation> traits = kind == PresetKind.SPECIES ? identity.traits() : List.of();
            hover(
                    left,
                    identityTop,
                    left + font.width(name),
                    identityTop + LINE,
                    () -> SheetTooltips.preset(kind, choice, traits));
            left += font.width(name);
        }
    }

    /** {@code Level 5} and an XP bar towards the next level, ending at {@code right}; hovering shows the XP. */
    private void renderLevel(GuiGraphics graphics, StatSheet.LevelLine line, int right, int levelTop) {
        Component label = Component.translatable("screen.checks.level", line.level());
        int lineWidth = font.width(label) + GAP + XP_BAR_WIDTH;
        int left = right - lineWidth;
        graphics.drawString(font, label, left, levelTop, Frames.INK, false);
        int barLeft = left + font.width(label) + GAP;
        int barTop = levelTop + (font.lineHeight - XP_BAR_HEIGHT) / 2 - 1;
        graphics.fill(barLeft, barTop, barLeft + XP_BAR_WIDTH, barTop + XP_BAR_HEIGHT, XP_TRACK);
        graphics.fill(
                barLeft, barTop, barLeft + Math.round(XP_BAR_WIDTH * line.progress()), barTop + XP_BAR_HEIGHT, XP_FILL);
        hover(left, levelTop, left + lineWidth, levelTop + LINE, () -> SheetTooltips.level(line));
    }

    private void renderProficiencyBonus(GuiGraphics graphics, int right, int top) {
        String value = SheetText.signed(sheet.proficiencyBonus());
        int valueLeft = right - font.width(value);
        int left = valueLeft - GAP - font.width(PROFICIENCY_BONUS);
        graphics.drawString(font, PROFICIENCY_BONUS, left, top, Frames.INK_SOFT, false);
        graphics.drawString(font, value, valueLeft, top, Frames.INK, false);
        hover(left, top, right, top + LINE, SheetTooltips::proficiencyBonus);
    }

    private void renderAbilities(GuiGraphics graphics) {
        List<StatSheet.AbilityLine> abilities = sheet.abilities();
        Area area = layout.abilities();
        int cardWidth = (area.width() - (abilities.size() - 1) * GAP) / abilities.size();
        for (int i = 0; i < abilities.size(); i++) {
            Area card = new Area(area.left() + i * (cardWidth + GAP), area.top(), cardWidth, area.height());
            renderAbility(graphics, abilities.get(i), card);
        }
    }

    /** The modifier large and gold on the left; the abbreviation over the score on the right. */
    private void renderAbility(GuiGraphics graphics, StatSheet.AbilityLine line, Area card) {
        Frames.card(graphics, card);
        hover(
                card.left(),
                card.top(),
                card.right(),
                card.bottom(),
                () -> SheetTooltips.ability(line, sheet.body().extras()));
        String modifier = SheetText.signed(line.modifier());
        graphics.pose().pushPose();
        graphics.pose()
                .translate(
                        card.left() + card.width() / 4f - font.width(modifier) * LARGE_TEXT / 2,
                        card.top() + (card.height() - LARGE_TEXT_HEIGHT) / 2f + 1,
                        0);
        graphics.pose().scale(LARGE_TEXT, LARGE_TEXT, 1);
        graphics.drawString(font, modifier, 0, 0, Frames.INK_GOLD, false);
        graphics.pose().popPose();
        int center = card.left() + card.width() * 3 / 4;
        int textTop = card.top() + (card.height() - LINE - font.lineHeight) / 2 + 1;
        centered(graphics, SheetText.abilityAbbreviation(line.ability()), center, textTop, Frames.INK_SOFT);
        centered(graphics, Component.literal(Integer.toString(line.score())), center, textTop + LINE, Frames.INK_FADED);
    }

    private void centered(GuiGraphics graphics, Component text, int center, int top, int color) {
        graphics.drawString(font, text, center - font.width(text) / 2, top, color, false);
    }

    /** The gold heading, then each row with its tooltip on hover. */
    private void renderBlock(GuiGraphics graphics, Block block, Area area) {
        graphics.drawString(font, block.title(), area.left(), area.top(), Frames.INK_GOLD, false);
        List<Component> about = block.about().get();
        if (!about.isEmpty()) {
            hover(area.left(), area.top(), area.left() + font.width(block.title()), area.top() + LINE, () -> about);
        }
        int y = area.top() + SheetLayout.HEADING_HEIGHT;
        for (Row row : block.rows()) {
            renderRow(graphics, row, area.left(), area.right(), y, Frames.INK_SOFT, Frames.INK);
            y += LINE;
        }
    }

    /** The dots, if any, then the label cut to fit before the value, which is right-aligned at {@code right}. */
    private void renderRow(GuiGraphics graphics, Row row, int left, int right, int y, int labelColor, int valueColor) {
        hover(left, y, right, y + LINE, row.tooltip());
        int labelLeft = left;
        if (row.dots().isPresent()) {
            dots(graphics, row.dots().get(), left, y);
            hover(left, y, left + DOTS_WIDTH, y + LINE, SheetTooltips::legend);
            labelLeft += DOTS_WIDTH;
        }
        int valueLeft = right - font.width(row.value());
        String label = font.plainSubstrByWidth(row.label().getString(), valueLeft - GAP - labelLeft);
        graphics.drawString(font, label, labelLeft, y, labelColor, false);
        graphics.drawString(font, row.value(), valueLeft, y, valueColor, false);
    }

    /** Down two columns, quieter than the Overview: skills without proficiency are faded. */
    private void renderSkills(GuiGraphics graphics) {
        Area area = layout.skills();
        int columnWidth =
                (area.width() - (SheetLayout.SKILL_COLUMNS - 1) * SheetLayout.COLUMN_GAP) / SheetLayout.SKILL_COLUMNS;
        for (int column = 0; column < SheetLayout.SKILL_COLUMNS; column++) {
            int left = area.left() + column * (columnWidth + SheetLayout.COLUMN_GAP);
            for (int row = 0; row < layout.visibleSkillRows(); row++) {
                int index = column * layout.skillRows() + firstSkillRow + row;
                if (index < skills.size()) {
                    renderSkill(graphics, skills.get(index), left, left + columnWidth, area.top() + row * LINE);
                }
            }
        }
        renderScrollbar(graphics, area, firstSkillRow * LINE, maxFirstSkillRow() * LINE, layout.skillRows() * LINE);
    }

    private void renderSkill(GuiGraphics graphics, StatSheet.SkillLine line, int left, int right, int y) {
        Row row = new Row(
                Optional.of(line.proficiency()),
                SheetText.skillName(line.id()),
                SheetText.signed(line.modifier()),
                () -> SheetTooltips.skill(line, sheet));
        int color = line.proficiency() == Proficiency.NONE ? Frames.INK_FADED : Frames.INK_SOFT;
        renderRow(graphics, row, left, right, y, color, color);
    }

    /** A thin line just right of {@code view}, only when what it shows, {@code total} tall, scrolls. */
    private void renderScrollbar(GuiGraphics graphics, Area view, int scroll, int maxScroll, int total) {
        if (maxScroll <= 0) {
            return;
        }
        int track = view.height();
        int thumb = Math.max(LINE, track * track / total);
        int thumbTop = view.top() + (track - thumb) * scroll / maxScroll;
        int left = view.right() + GAP;
        graphics.fill(left, thumbTop, left + 1, thumbTop + thumb, Frames.INK_FADED);
    }

    /** Hollow (not proficient), one filled (proficient) or two filled (expertise). */
    private void dots(GuiGraphics graphics, Proficiency proficiency, int left, int y) {
        int top = y + (font.lineHeight - DOT) / 2 - 1;
        if (proficiency == Proficiency.NONE) {
            graphics.renderOutline(left, top, DOT, DOT, Frames.INK_FADED);
            return;
        }
        graphics.fill(left, top, left + DOT, top + DOT, Frames.INK_SOFT);
        if (proficiency == Proficiency.EXPERTISE) {
            graphics.fill(left + DOT + 1, top, left + 2 * DOT + 1, top + DOT, Frames.INK_SOFT);
        }
    }

    /** Shows {@code lines} this frame when the mouse is over the area; a later, smaller area wins. */
    private void hover(int left, int top, int right, int bottom, Supplier<List<Component>> lines) {
        if (hoverView.isPresent() && !hoverView.get().contains(mouseX, mouseY)) {
            return;
        }
        int scroll = hoverView.isPresent() ? overviewScroll : 0;
        if (new Area(left, top, right - left, bottom - top).contains(mouseX, mouseY + scroll)) {
            tooltip = lines.get();
        }
    }

    private int maxFirstSkillRow() {
        return layout.skillRows() - layout.visibleSkillRows();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Tab each : Tab.values()) {
            Area bounds = layout.tabs().get(each.ordinal());
            if (each != tab && bounds.contains(mouseX, mouseY)) {
                tab = each;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Scrolls the Overview a line at a time, and the skills by whole rows so only complete rows are drawn. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = -(int) Math.signum(scrollY);
        if (tab == Tab.SKILLS) {
            firstSkillRow = Mth.clamp(firstSkillRow + step, 0, maxFirstSkillRow());
        } else {
            overviewScroll = Mth.clamp(overviewScroll + step * LINE, 0, layout.maxOverviewScroll());
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KEY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
