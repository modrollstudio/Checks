package studio.modroll.checks.sheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.ui.Area;

class SheetLayoutTest {

    private static final int TAB_LABEL_WIDTH = 44;
    private static final int SAVES = 6;
    private static final int PASSIVES = 3;
    private static final int BODY_ROWS = 3;
    private static final int SKILLS = 18;
    private static final int MANY_SKILLS = 60;
    private static final int TALL_SECTION_ROWS = 40;
    private static final int MOD_SECTION_ROWS = 2;
    private static final int MIN_GUI_WIDTH = 320;
    private static final int MIN_GUI_HEIGHT = 240;

    private record Screen(int width, int height) {}

    /** Vanilla's Window.calculateScale: a GUI scale is lowered until the scaled window is at least 320×240. */
    private static Screen scaled(int width, int height, int requestedScale) {
        int scale = 1;
        while (scale != requestedScale
                && width / (scale + 1) >= MIN_GUI_WIDTH
                && height / (scale + 1) >= MIN_GUI_HEIGHT) {
            scale++;
        }
        return new Screen(width / scale, height / scale);
    }

    private static Set<Screen> screens() {
        Set<Screen> screens = new LinkedHashSet<>();
        for (int scale = 1; scale <= 4; scale++) {
            screens.add(scaled(1920, 1080, scale));
            screens.add(scaled(854, 480, scale));
        }
        return screens;
    }

    @Test
    void theScreensIncludeTheSmallestScaledWindows() {
        Set<Screen> screens = screens();
        assertTrue(screens.contains(new Screen(480, 270)), "1920×1080 at GUI scale 4");
        assertTrue(screens.contains(new Screen(427, 240)), "854×480 at GUI scales 2 to 4");
    }

    @Test
    void everyCombinationFitsInsideTheMarginsWithoutOverlaps() {
        int checked = 0;
        for (Screen screen : screens()) {
            for (int parts = 0; parts < 1 << 4; parts++) {
                boolean create = (parts & 1) != 0;
                boolean levelUp = (parts & 2) != 0;
                boolean body = (parts & 4) != 0;
                boolean modSection = (parts & 8) != 0;
                List<Integer> blocks = new ArrayList<>(List.of(SAVES, PASSIVES));
                if (body) {
                    blocks.add(BODY_ROWS);
                }
                if (modSection) {
                    blocks.add(MOD_SECTION_ROWS);
                }
                SheetLayout layout = SheetLayout.of(new SheetLayout.Inputs(
                        screen.width(),
                        screen.height(),
                        blocks,
                        SKILLS,
                        (create ? 1 : 0) + (levelUp ? 1 : 0),
                        TAB_LABEL_WIDTH));
                String context = screen + " create=" + create + " levelUp=" + levelUp + " body=" + body + " modSection="
                        + modSection;
                expectInsideMargins(layout.frame(), screen, context);
                expectNoOverlaps(layout.frame(), context);
                expectInsideTheSheet(layout, layout.overviewElements(), context);
                expectNoOverlaps(layout.overviewElements(), context);
                expectInsideTheScrollArea(layout, context);
                expectNoOverlaps(layout.overviewContent(), context);
                expectInsideTheSheet(layout, layout.skillsElements(), context);
                expectNoOverlaps(layout.skillsElements(), context);
                assertTrue(layout.visibleSkillRows() >= SheetLayout.MIN_SKILL_ROWS, "skill rows: " + context);
                checked++;
            }
        }
        assertEquals(screens().size() * 16, checked);
    }

    @Test
    void skillsShowWholeWhenThereIsRoomAndScrollWhenThereIsNot() {
        SheetLayout roomy = layout(new Screen(960, 540), SKILLS);
        assertEquals(SKILLS / SheetLayout.SKILL_COLUMNS, roomy.skillRows());
        assertEquals(roomy.skillRows(), roomy.visibleSkillRows());
        SheetLayout crowded = layout(new Screen(427, 240), MANY_SKILLS);
        assertTrue(crowded.visibleSkillRows() < crowded.skillRows());
        assertTrue(crowded.visibleSkillRows() >= SheetLayout.MIN_SKILL_ROWS);
    }

    @Test
    void aTallSectionScrollsTheOverviewInsteadOfLeavingTheWindow() {
        Screen small = new Screen(427, 240);
        SheetLayout layout = SheetLayout.of(new SheetLayout.Inputs(
                small.width(),
                small.height(),
                List.of(SAVES, PASSIVES, BODY_ROWS, TALL_SECTION_ROWS),
                SKILLS,
                2,
                TAB_LABEL_WIDTH));
        String context = "tall section at " + small;

        expectInsideMargins(layout.frame(), small, context);
        expectInsideTheSheet(layout, layout.overviewElements(), context);
        expectInsideTheScrollArea(layout, context);
        expectNoOverlaps(layout.overviewContent(), context);
        Area section = layout.blocks().getLast();
        assertTrue(section.bottom() > layout.overview().bottom(), "the section runs past the view");
        assertEquals(
                layout.overview().top() + layout.overviewHeight(),
                section.bottom(),
                "scrolling to the end shows the section's last row");
        assertTrue(layout.maxOverviewScroll() > 0);
    }

    @Test
    void theOverviewDoesNotScrollWhenItFits() {
        assertEquals(0, layout(new Screen(427, 240), SKILLS).maxOverviewScroll());
        assertEquals(0, layout(new Screen(960, 540), SKILLS).maxOverviewScroll());
    }

    @Test
    void eachBlockGoesUnderTheShortestColumn() {
        List<Area> blocks = layout(new Screen(960, 540), SKILLS).blocks();
        Area saves = blocks.get(0);
        Area passives = blocks.get(1);
        Area body = blocks.get(2);
        Area section = blocks.get(3);
        assertEquals(saves.top(), passives.top());
        assertEquals(saves.top(), body.top());
        assertTrue(saves.left() < passives.left() && passives.left() < body.left());
        assertEquals(passives.left(), section.left());
        assertEquals(passives.bottom() + SheetLayout.BLOCK_GAP, section.top());
    }

    @Test
    void theTabsSitOnTheSheetAndKeepItsSize() {
        SheetLayout layout = layout(new Screen(427, 240), SKILLS);
        assertEquals(SheetLayout.TAB_COUNT, layout.tabs().size());
        for (Area tab : layout.tabs()) {
            assertEquals(layout.sheet().top(), tab.bottom());
        }
        int contentBottom = layout.sheet().bottom() - SheetLayout.SHEET_PADDING;
        int overviewBottom =
                layout.blocks().stream().mapToInt(Area::bottom).max().orElseThrow();
        assertEquals(contentBottom, Math.max(overviewBottom, layout.skills().bottom()));
    }

    @Test
    void theSheetGrowsWithTheWindowUpToAMaximum() {
        assertEquals(
                427 - 2 * SheetLayout.MARGIN,
                layout(new Screen(427, 240), SKILLS).sheet().width());
        assertEquals(
                SheetLayout.MAX_SHEET_WIDTH,
                layout(new Screen(960, 540), SKILLS).sheet().width());
    }

    private static SheetLayout layout(Screen screen, int skills) {
        return SheetLayout.of(new SheetLayout.Inputs(
                screen.width(),
                screen.height(),
                List.of(SAVES, PASSIVES, BODY_ROWS, MOD_SECTION_ROWS),
                skills,
                2,
                TAB_LABEL_WIDTH));
    }

    private static void expectInsideMargins(List<Area> rects, Screen screen, String context) {
        Area inside = new Area(
                SheetLayout.MARGIN,
                SheetLayout.MARGIN,
                screen.width() - 2 * SheetLayout.MARGIN,
                screen.height() - 2 * SheetLayout.MARGIN);
        for (Area rect : rects) {
            assertTrue(inside.contains(rect), rect + " leaves the margins: " + context);
        }
    }

    private static void expectInsideTheSheet(SheetLayout layout, List<Area> rects, String context) {
        Area sheet = layout.sheet();
        Area inner = new Area(
                sheet.left() + SheetLayout.SHEET_PADDING,
                sheet.top() + SheetLayout.SHEET_PADDING,
                sheet.width() - 2 * SheetLayout.SHEET_PADDING,
                sheet.height() - 2 * SheetLayout.SHEET_PADDING);
        for (Area rect : rects) {
            assertTrue(inner.contains(rect), rect + " leaves the sheet: " + context);
        }
    }

    /** The Overview's content stays within its view's width and the height it scrolls through. */
    private static void expectInsideTheScrollArea(SheetLayout layout, String context) {
        Area view = layout.overview();
        Area scrollArea = new Area(view.left(), view.top(), view.width(), layout.overviewHeight());
        for (Area rect : layout.overviewContent()) {
            assertTrue(scrollArea.contains(rect), rect + " leaves the Overview's scroll area: " + context);
        }
    }

    private static void expectNoOverlaps(List<Area> elements, String context) {
        for (int i = 0; i < elements.size(); i++) {
            for (int j = i + 1; j < elements.size(); j++) {
                assertFalse(
                        elements.get(i).overlaps(elements.get(j)),
                        elements.get(i) + " overlaps " + elements.get(j) + ": " + context);
            }
        }
    }
}
