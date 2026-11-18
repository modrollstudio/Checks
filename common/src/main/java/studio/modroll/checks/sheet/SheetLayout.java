package studio.modroll.checks.sheet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import studio.modroll.checks.ui.Area;

public record SheetLayout(
        Area sheet,
        List<Area> tabs,
        Area header,
        Area overview,
        int overviewHeight,
        Area abilities,
        List<Area> blocks,
        Area skills,
        int skillRows,
        int visibleSkillRows,
        List<Area> buttons) {

    public static final int MARGIN = 12;
    public static final int GAP = 4;
    public static final int LINE = 10;
    public static final int SHEET_PADDING = 10;
    public static final int SPACING = 10;

    public static final int COLUMN_GAP = 16;
    public static final int BLOCK_GAP = 8;
    public static final int HEADING_HEIGHT = LINE + 3;

    public static final int TAB_HEIGHT = 14;
    public static final int TAB_PADDING = 8;

    public static final int TAB_GAP = 2;

    public static final int HEADER_HEIGHT = 20;

    public static final int ABILITY_HEIGHT = 30;
    public static final int BUTTON_WIDTH = 150;
    public static final int BUTTON_HEIGHT = 20;
    public static final int BUTTON_SPACING = 12;

    public static final int COLUMNS = 3;
    public static final int SKILL_COLUMNS = 2;
    public static final int TAB_COUNT = 2;

    static final int MAX_SHEET_WIDTH = 480;
    static final int MIN_SKILL_ROWS = 3;
    static final int MIN_OVERVIEW_VIEW = ABILITY_HEIGHT;

    public SheetLayout {
        tabs = List.copyOf(tabs);
        blocks = List.copyOf(blocks);
        buttons = List.copyOf(buttons);
    }

    public record Inputs(
            int width, int height, List<Integer> blockRows, int skillCount, int buttonCount, int tabLabelWidth) {

        public Inputs {
            blockRows = List.copyOf(blockRows);
        }
    }

    public static SheetLayout of(Inputs in) {
        int sheetWidth = Math.min(MAX_SHEET_WIDTH, in.width() - 2 * MARGIN);
        int sheetLeft = (in.width() - sheetWidth) / 2;
        int innerLeft = sheetLeft + SHEET_PADDING;
        int innerWidth = sheetWidth - 2 * SHEET_PADDING;
        Columns columns = Columns.pack(in.blockRows(), innerWidth);

        int skillRows = Math.ceilDiv(in.skillCount(), SKILL_COLUMNS);
        int belowHeader = HEADER_HEIGHT + SPACING;
        int overviewHeight = ABILITY_HEIGHT + SPACING + columns.height();
        int footer = in.buttonCount() == 0 ? 0 : BLOCK_GAP + BUTTON_HEIGHT;
        int chrome = TAB_HEIGHT + 2 * SHEET_PADDING + footer;
        int room = in.height() - 2 * MARGIN - chrome - belowHeader;
        int visibleSkillRows = Math.min(skillRows, Math.max(Math.min(skillRows, MIN_SKILL_ROWS), room / LINE));
        int overviewView = Math.min(overviewHeight, Math.max(MIN_OVERVIEW_VIEW, room));
        int content = belowHeader + Math.max(overviewView, visibleSkillRows * LINE);
        int available = in.height() - 2 * MARGIN;

        int top = MARGIN + Math.max(0, (available - chrome - content) / 2);
        Area sheet = new Area(sheetLeft, top + TAB_HEIGHT, sheetWidth, content + 2 * SHEET_PADDING);
        Area header = new Area(innerLeft, sheet.top() + SHEET_PADDING, innerWidth, HEADER_HEIGHT);
        Area overview = new Area(
                innerLeft, header.bottom() + SPACING, innerWidth, Math.min(overviewHeight, content - belowHeader));
        Area abilities = new Area(innerLeft, overview.top(), innerWidth, ABILITY_HEIGHT);
        Area skills = new Area(innerLeft, header.bottom() + SPACING, innerWidth, visibleSkillRows * LINE);
        return new SheetLayout(
                sheet,
                tabs(innerLeft, top, in.tabLabelWidth()),
                header,
                overview,
                overviewHeight,
                abilities,
                columns.place(innerLeft, abilities.bottom() + SPACING),
                skills,
                skillRows,
                visibleSkillRows,
                buttons(in, sheet.bottom() + BLOCK_GAP));
    }

    public int maxOverviewScroll() {
        return overviewHeight - overview.height();
    }

    public static int blockHeight(int rows) {
        return HEADING_HEIGHT + rows * LINE;
    }

    private static List<Area> tabs(int left, int top, int labelWidth) {
        int width = labelWidth + 2 * TAB_PADDING;
        List<Area> tabs = new ArrayList<>();
        for (int i = 0; i < TAB_COUNT; i++) {
            tabs.add(new Area(left + i * (width + TAB_GAP), top, width, TAB_HEIGHT));
        }
        return tabs;
    }

    private record Columns(List<Area> blocks, int height) {

        static Columns pack(List<Integer> blockRows, int innerWidth) {
            int width = (innerWidth - (COLUMNS - 1) * COLUMN_GAP) / COLUMNS;
            int[] heights = new int[COLUMNS];
            List<Area> blocks = new ArrayList<>();
            for (int rows : blockRows) {
                int shortest = 0;
                for (int i = 1; i < COLUMNS; i++) {
                    if (heights[i] < heights[shortest]) {
                        shortest = i;
                    }
                }
                int top = heights[shortest] == 0 ? 0 : heights[shortest] + BLOCK_GAP;
                blocks.add(new Area(shortest * (width + COLUMN_GAP), top, width, blockHeight(rows)));
                heights[shortest] = top + blockHeight(rows);
            }
            return new Columns(blocks, Arrays.stream(heights).max().orElse(0));
        }

        List<Area> place(int left, int top) {
            return blocks.stream()
                    .map(block -> new Area(left + block.left(), top + block.top(), block.width(), block.height()))
                    .toList();
        }
    }

    private static List<Area> buttons(Inputs in, int top) {
        int count = in.buttonCount();
        if (count == 0) {
            return List.of();
        }
        int buttonWidth = Math.min(BUTTON_WIDTH, (in.width() - 2 * MARGIN - (count + 1) * BUTTON_SPACING) / count);
        int left = (in.width() - (count * (buttonWidth + BUTTON_SPACING) - BUTTON_SPACING)) / 2;
        List<Area> buttons = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            buttons.add(new Area(left + i * (buttonWidth + BUTTON_SPACING), top, buttonWidth, BUTTON_HEIGHT));
        }
        return buttons;
    }
}
