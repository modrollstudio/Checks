package studio.modroll.checks.sheet.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WrappedTooltipTest {

    @Test
    void theTitleGapFollowsEveryWrappedLineOfTheTitle() {
        int titleLines = 2;
        assertEquals(0, WrappedTooltip.lineTop(0, titleLines));
        assertEquals(WrappedTooltip.LINE_HEIGHT, WrappedTooltip.lineTop(1, titleLines));
        assertEquals(2 * WrappedTooltip.LINE_HEIGHT + WrappedTooltip.TITLE_GAP, WrappedTooltip.lineTop(2, titleLines));
        assertEquals(3 * WrappedTooltip.LINE_HEIGHT + WrappedTooltip.TITLE_GAP, WrappedTooltip.lineTop(3, titleLines));
    }

    @Test
    void heightMatchesVanillaForAnUnwrappedTitle() {
        assertEquals(WrappedTooltip.GLYPH_HEIGHT, WrappedTooltip.height(1, 1));
        assertEquals(3 * WrappedTooltip.LINE_HEIGHT, WrappedTooltip.height(3, 1));
    }

    @Test
    void aTitleAloneHasNoGap() {
        assertEquals(WrappedTooltip.LINE_HEIGHT + WrappedTooltip.GLYPH_HEIGHT, WrappedTooltip.height(2, 2));
    }
}
