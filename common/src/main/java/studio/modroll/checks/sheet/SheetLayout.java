package studio.modroll.checks.sheet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import studio.modroll.checks.ui.Area;

/**
 * Where everything on the stat screen goes, from plain sizes, so it is unit tested without a client. One
 * parchment sheet grows with the window up to a maximum width and is centered inside the margins, with
 * two tabs on its top edge and the buttons under it. Both tabs share the header: the face, name,
 * character line and level. The Overview tab has a card per ability, then its blocks (saves, passive
 * scores, body and the sections other mods add) packed into three columns, each block going under the
 * shortest column so far. The Skills tab lists the skills down two columns. When the window is too short,
 * either tab's content scrolls inside its view under the header: the Overview by pixels through
 * {@link #overview}, the skills by rows. The sheet is as tall as the taller tab, so switching never moves it.
 */
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
    /** From the sheet's edge to its content. */
    public static final int SHEET_PADDING = 10;
    /** The whitespace that separates the header, the ability cards and the blocks. */
    public static final int SPACING = 10;

    public static final int COLUMN_GAP = 16;
    public static final int BLOCK_GAP = 8;
    /** A block's heading and the room under it. */
    public static final int HEADING_HEIGHT = LINE + 3;

    public static final int TAB_HEIGHT = 14;
    /** From a tab's side to its label. */
    public static final int TAB_PADDING = 8;

    public static final int TAB_GAP = 2;

    /** The face and the two lines beside it. */
    public static final int HEADER_HEIGHT = 20;

    public static final int ABILITY_HEIGHT = 30;
    public static final int BUTTON_WIDTH = 150;
    public static final int BUTTON_HEIGHT = 20;
    /** Room around each button for its bracket caps. */
    public static final int BUTTON_SPACING = 12;

    public static final int COLUMNS = 3;
    public static final int SKILL_COLUMNS = 2;
    public static final int TAB_COUNT = 2;

    static final int MAX_SHEET_WIDTH = 480;
    static final int MIN_SKILL_ROWS = 3;
    /** The least the Overview's view shows, however short the window: the ability cards. */
    static final int MIN_OVERVIEW_VIEW = ABILITY_HEIGHT;

    public SheetLayout {
        tabs = List.copyOf(tabs);
        blocks = List.copyOf(blocks);
        buttons = List.copyOf(buttons);
    }

    /**
     * What the layout depends on: the screen size, the rows of each Overview block in order, how many
     * skills and buttons there are and the widest tab label.
     */
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

    /** The Overview tab's header and view, all inside the sheet. */
    public List<Area> overviewElements() {
        return List.of(header, overview);
    }

    /**
     * What scrolls in the Overview's view, placed as if unscrolled: from the view's top down to
     * {@link #overviewHeight} below it.
     */
    public List<Area> overviewContent() {
        List<Area> content = new ArrayList<>(List.of(abilities));
        content.addAll(blocks);
        return content;
    }

    /** How far the Overview scrolls: 0 when it all fits. */
    public int maxOverviewScroll() {
        return overviewHeight - overview.height();
    }

    /** Everything on the Skills tab, all inside the sheet. */
    public List<Area> skillsElements() {
        return List.of(header, skills);
    }

    /** What sits on the screen around the tabs' contents. */
    public List<Area> frame() {
        List<Area> frame = new ArrayList<>(List.of(sheet));
        frame.addAll(tabs);
        frame.addAll(buttons);
        return frame;
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

    /** Blocks packed into columns: each goes under whichever column is shortest so far, the leftmost on a tie. */
    private record Columns(List<Area> blocks, int height) {

        /** The blocks' rects with their tops relative to the columns' top. */
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

    /** One centered row, spaced for the bracket caps; buttons narrow only when the window cannot fit them. */
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
