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
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.creation.CreationDraft;
import studio.modroll.checks.creation.SkillPlan;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.sheet.SheetTooltips;
import studio.modroll.checks.ui.Area;
import studio.modroll.checks.ui.Frames;

/**
 * The skills page, grouped by where each proficiency comes from: the species' skill choices, the
 * background's fixed skills, the class's list to choose from, and any other skill while overlap or a Custom
 * background allows one.
 * Skills sit two to a row; the whole page scrolls by row.
 */
final class SkillsStepView implements StepView {

    private static final int COLUMNS = 2;
    private static final int ROW_HEIGHT = LINE + 2;
    private static final int COLUMN_GAP = 16;
    private static final int DIVIDER = 0xFF5E5E5E;

    private final CharacterCreationScreen host;
    private final ScrollRows scroll = new ScrollRows();
    private List<Row> rows = List.of();
    private Area panel;
    private Area inner;
    private int listTop;
    private int columnWidth;

    SkillsStepView(CharacterCreationScreen host) {
        this.host = host;
    }

    /** Where a listed skill comes from, which decides whether it can be clicked and its tooltip. */
    private enum Source {
        SPECIES,
        BACKGROUND,
        CLASS_LIST,
        ANY;

        String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private sealed interface Row permits Heading, SkillRow {}

    private record Heading(FormattedCharSequence text, int color) implements Row {}

    private record SkillRow(List<Skill> skills, Source source) implements Row {}

    /** Two columns of skills a clear gap apart, a divider between them, scrolling when they do not fit. */
    @Override
    public void init(Area area) {
        panel = area;
        inner = Frames.well(area).inset(PADDING);
        rows = rows(inner.width() - SCROLLBAR_WIDTH);
        listTop = inner.top() + Frames.HEADING_HEIGHT + GAP;
        columnWidth = (inner.width() - SCROLLBAR_WIDTH - GAP - COLUMN_GAP) / COLUMNS;
        scroll.layout(rows.size(), (inner.bottom() - listTop) / ROW_HEIGHT);
    }

    private List<Row> rows(int width) {
        CreationDraft draft = host.draft();
        SkillPlan plan = draft.skillPlan();
        List<Row> rows = new ArrayList<>();
        if (plan.speciesRequired() > 0) {
            addHeading(
                    rows,
                    width,
                    Frames.GOLD_TEXT,
                    text("skills.from_species", plan.speciesRequired(), presetName(PresetKind.SPECIES)));
            addSkills(rows, plan.speciesOptions()::contains, Source.SPECIES);
        }
        if (!plan.fixed().isEmpty()) {
            addHeading(
                    rows, width, Frames.GOLD_TEXT, text("skills.from_background", presetName(PresetKind.BACKGROUND)));
            addSkills(rows, plan.fixed()::contains, Source.BACKGROUND);
        }
        if (draft.classPreset().isPresent()) {
            addClassGroups(rows, width, plan);
        } else if (plan.required() > 0) {
            addHeading(rows, width, Frames.GOLD_TEXT, text("skills.choose_any", plan.required()));
            addSkills(rows, skill -> !plan.fixed().contains(skill), Source.ANY);
        } else {
            addHeading(rows, width, Frames.MUTED, text("skills.none_to_pick"));
        }
        return rows;
    }

    private void addClassGroups(List<Row> rows, int width, SkillPlan plan) {
        addHeading(
                rows,
                width,
                Frames.GOLD_TEXT,
                text("skills.from_class", plan.classPicks(), presetName(PresetKind.CLASS)));
        addSkills(rows, skill -> plan.onClassList(skill) && !plan.fixed().contains(skill), Source.CLASS_LIST);
        if (plan.overlapPicks() > 0) {
            String key = plan.overlapPicks() == 1 ? "skills.overlap.one" : "skills.overlap.many";
            addHeading(rows, width, Frames.MUTED, text(key, skillNames(plan.overlap()), plan.overlapPicks()));
        }
        if (plan.freePicks() > 0) {
            addHeading(rows, width, Frames.MUTED, text("skills.custom_background", plan.freePicks()));
        }
        if (plan.offListAllowance() > 0) {
            int anyPicks = Math.min(plan.offListAllowance(), plan.required());
            addHeading(rows, width, Frames.GOLD_TEXT, text("skills.any_other", anyPicks));
            addSkills(rows, skill -> !plan.onClassList(skill) && !plan.fixed().contains(skill), Source.ANY);
        }
    }

    private void addHeading(List<Row> rows, int width, int color, Component text) {
        host.font().split(text, width).forEach(line -> rows.add(new Heading(line, color)));
    }

    /** The offered skills that {@code include} accepts, by name, two to a row. */
    private void addSkills(List<Row> rows, Predicate<ResourceLocation> include, Source source) {
        List<Skill> skills = host.draft().offer().skills().stream()
                .filter(skill -> include.test(skill.id()))
                .sorted(Comparator.comparing(
                        skill -> SheetText.skillName(skill.id()).getString()))
                .toList();
        for (int i = 0; i < skills.size(); i += COLUMNS) {
            rows.add(new SkillRow(skills.subList(i, Math.min(i + COLUMNS, skills.size())), source));
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        Frames.panel(graphics, panel);
    }

    @Override
    public void render(GuiGraphics graphics) {
        CreationDraft draft = host.draft();
        int top = inner.top();
        graphics.drawString(host.font(), Frames.heading(text("skills")), left(), top, Frames.TEXT);
        Component count = text("skills.count", draft.pickedCount(), draft.requiredCount());
        graphics.drawString(host.font(), count, right() - host.font().width(count), top, Frames.GOLD_TEXT);
        graphics.enableScissor(inner.left(), inner.top(), inner.right(), inner.bottom());
        for (int index = scroll.first(); index < scroll.end(); index++) {
            int y = listTop + (index - scroll.first()) * ROW_HEIGHT;
            switch (rows.get(index)) {
                case Heading heading -> graphics.drawString(host.font(), heading.text(), left(), y, heading.color());
                case SkillRow row -> renderRow(graphics, row, y);
            }
        }
        graphics.disableScissor();
        scroll.renderScrollbar(graphics, right() - SCROLLBAR_WIDTH, listTop, ROW_HEIGHT);
    }

    private void renderRow(GuiGraphics graphics, SkillRow row, int y) {
        int divider = left() + columnWidth + COLUMN_GAP / 2;
        graphics.fill(divider, y - 1, divider + 1, y + ROW_HEIGHT - 1, DIVIDER);
        for (int column = 0; column < row.skills().size(); column++) {
            int columnLeft = left() + column * (columnWidth + COLUMN_GAP);
            renderSkill(graphics, row.skills().get(column), row.source(), columnLeft, columnLeft + columnWidth, y);
        }
    }

    private void renderSkill(GuiGraphics graphics, Skill skill, Source source, int left, int right, int y) {
        boolean fixed = source == Source.BACKGROUND;
        boolean forSpecies = source == Source.SPECIES;
        boolean picked = forSpecies
                ? host.draft().speciesPicked(skill.id())
                : host.draft().picked(skill.id());
        int color = color(skill.id(), fixed, picked, forSpecies);
        CreationUi.dot(graphics, host.font(), left, y, fixed || picked, color);
        Component ability = SheetText.abilityAbbreviation(skill.ability());
        int abilityLeft = right - host.font().width(ability);
        graphics.drawString(host.font(), ability, abilityLeft, y, Frames.DIM);
        Component name = SheetText.skillName(skill.id());
        String shown = host.font().plainSubstrByWidth(name.getString(), abilityLeft - PADDING - left - DOT_SPACE);
        graphics.drawString(host.font(), shown, left + DOT_SPACE, y, color);
        host.hover(left, y, right, y + ROW_HEIGHT, () -> Stream.of(
                        Stream.of(name),
                        SheetTooltips.about(skill.id(), skill.ability()).stream(),
                        Stream.of(text("skills.source." + source.id())))
                .flatMap(lines -> lines)
                .toList());
    }

    /** Background skills grey, picked and pickable skills bright, the rest dim. */
    private int color(ResourceLocation skill, boolean fixed, boolean picked, boolean forSpecies) {
        if (fixed) {
            return Frames.MUTED;
        }
        boolean pickable = forSpecies
                ? host.draft().canPickForSpecies(skill)
                : host.draft().canPick(skill);
        return picked || pickable ? Frames.TEXT : Frames.DIM;
    }

    /** Only skills the player picks respond; background skills are fixed. */
    @Override
    public boolean mouseClicked(double x, double y) {
        int index = scroll.first() + Mth.floor((y - listTop) / ROW_HEIGHT);
        int column = (int) (x - left()) / (columnWidth + COLUMN_GAP);
        boolean inColumn = (x - left()) % (columnWidth + COLUMN_GAP) < columnWidth;
        boolean overList = x >= left() && inColumn && y >= listTop && index < scroll.end();
        if (!overList
                || !(rows.get(index) instanceof SkillRow row)
                || row.source() == Source.BACKGROUND
                || column >= row.skills().size()) {
            return false;
        }
        ResourceLocation skill = row.skills().get(column).id();
        if (row.source() == Source.SPECIES) {
            host.draft().toggleSpeciesSkill(skill);
        } else {
            host.draft().toggleSkill(skill);
        }
        return true;
    }

    @Override
    public void mouseScrolled(double x, double y, double scrollY) {
        scroll.scroll(scrollY);
    }

    private Component presetName(PresetKind kind) {
        return SheetText.presetName(kind, host.draft().choice(kind));
    }

    private static Component skillNames(Set<ResourceLocation> ids) {
        return ComponentUtils.formatList(
                ids.stream()
                        .map(SheetText::skillName)
                        .sorted(Comparator.comparing(Component::getString))
                        .toList(),
                Component.translatable("checks.list.comma"));
    }

    private static Component text(String key, Object... args) {
        return Component.translatable(KEY + key, args);
    }

    private int left() {
        return inner.left();
    }

    private int right() {
        return inner.right();
    }
}
