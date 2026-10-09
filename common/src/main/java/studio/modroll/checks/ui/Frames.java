package studio.modroll.checks.ui;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * The storybook look, all drawn in code: light bevelled panels around a dark well, a gold banner with
 * an icon, parchment icon tiles, underlined headings, bullets and gold bracket caps around buttons; and
 * a calmer muted-parchment sheet with cards and tabs for the character sheet.
 */
public final class Frames {

    public static final int LINE = 10;
    public static final int PADDING = 4;

    /** The light frame around a panel's well. */
    public static final int FRAME = 4;

    /** A slim bevelled bar holding one line of text. */
    public static final int STRIP_HEIGHT = 18;
    /** From a strip's edge to its text. */
    public static final int STRIP_INSET = 5;

    /** A tile showing its item at normal size. */
    public static final int TILE = 20;
    /** A tile showing its item at twice the size, for picking from. */
    public static final int LARGE_TILE = 36;

    public static final int BANNER_HEIGHT = TILE;
    public static final int HEADING_HEIGHT = LINE + 2;
    public static final int BULLET_INDENT = 7;
    /** How far a button's bracket caps reach out from its sides. */
    public static final int CAP_REACH = 4;

    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFFC4C4C4;
    public static final int DIM = 0xFF8A8A8A;
    public static final int GOLD_TEXT = 0xFFFFCB4A;
    public static final int FLAVOR = 0xFFE0D3B5;
    /** Text on parchment. */
    public static final int INK = 0xFF3B2912;

    public static final int INK_FADED = 0xFF7A6240;
    /** Labels on the calm sheet: softer than {@link #INK}, firmer than {@link #INK_FADED}. */
    public static final int INK_SOFT = 0xFF5E4B33;
    /** Headings and key numbers on the calm sheet. */
    public static final int INK_GOLD = 0xFF8A5C12;

    private static final int OUTLINE = 0xFF151515;
    private static final int FRAME_FILL = 0xFFC6C6C6;
    private static final int FRAME_LIGHT = 0xFFFFFFFF;
    private static final int FRAME_SHADE = 0xFF5B5B5B;
    private static final int WELL = 0xFF464646;
    private static final int WELL_SHADE = 0xFF262626;
    private static final int WELL_LIGHT = 0xFF707070;

    private static final int GOLD = 0xFFB98A1C;
    private static final int GOLD_LIGHT = 0xFFF0C54A;
    private static final int GOLD_DARK = 0xFF6A4A08;
    private static final int GOLD_EDGE = 0xFF3A2804;

    private static final int PARCHMENT = 0xFFE4CF9C;
    private static final int PARCHMENT_HOVER = 0xFFF4E6BF;
    private static final int PARCHMENT_LIGHT = 0xFFF8EDCD;
    private static final int PARCHMENT_SHADE = 0xFFAE8F57;
    private static final int PARCHMENT_EDGE = 0xFF5A3E1C;

    private static final int SHEET = 0xFFD6C8A6;
    private static final int SHEET_EDGE = 0xFF9C8762;
    private static final int CARD = 0xFFCBBC97;
    private static final int TAB_IDLE = 0xFFBDAD88;
    private static final int SHEET_CORNER = 2;

    private static final int BAND_HEIGHT = 14;
    private static final int ICON_INSET = 2;
    private static final int ITEM_SIZE = 16;
    private static final int BULLET_SIZE = 2;
    private static final int BULLET_DROP = 3;

    private Frames() {}

    /** Draws a panel filling {@code area}; its content goes in its {@link #well}. */
    public static void panel(GuiGraphics graphics, Area area) {
        graphics.renderOutline(area.left(), area.top(), area.width(), area.height(), OUTLINE);
        bevel(graphics, area.inset(1), FRAME_FILL, FRAME_LIGHT, FRAME_SHADE);
        bevel(graphics, well(area), WELL, WELL_SHADE, WELL_LIGHT);
    }

    /** A slim panel {@link #STRIP_HEIGHT} tall for one line of text, which starts {@link #STRIP_INSET} in. */
    public static void strip(GuiGraphics graphics, Area area) {
        graphics.renderOutline(area.left(), area.top(), area.width(), area.height(), OUTLINE);
        bevel(graphics, area.inset(1), FRAME_FILL, FRAME_LIGHT, FRAME_SHADE);
        bevel(graphics, area.inset(3), WELL, WELL_SHADE, WELL_LIGHT);
    }

    /** The y at which a line of text sits centered in a strip starting at {@code top}. */
    public static int stripText(int top) {
        return top + STRIP_INSET;
    }

    /** The darker inside of a panel filling {@code area}. */
    public static Area well(Area area) {
        return area.inset(FRAME);
    }

    /** A sheet of parchment filling {@code area}, its corners clipped and its edge darkened; write on it in {@link #INK}. */
    public static void parchment(GuiGraphics graphics, Area area) {
        graphics.fill(area.left() + 1, area.top(), area.right() - 1, area.bottom(), PARCHMENT_EDGE);
        graphics.fill(area.left(), area.top() + 1, area.right(), area.bottom() - 1, PARCHMENT_EDGE);
        Area sheet = area.inset(1);
        bevel(graphics, sheet, PARCHMENT_SHADE, PARCHMENT, PARCHMENT_EDGE);
        bevel(graphics, sheet.inset(2), PARCHMENT, PARCHMENT_LIGHT, PARCHMENT_SHADE);
    }

    /** A calm sheet of muted parchment with a thin soft edge and rounded corners; write on it in ink. */
    public static void sheet(GuiGraphics graphics, Area area) {
        rounded(graphics, area, SHEET_EDGE, SHEET_CORNER);
        rounded(graphics, area.inset(1), SHEET, 1);
    }

    /** A slightly darker, borderless patch of a {@link #sheet}. */
    public static void card(GuiGraphics graphics, Area area) {
        rounded(graphics, area, CARD, 1);
    }

    /**
     * A tab standing on a sheet whose top edge is {@code area.bottom()}; the open one takes the sheet's
     * colour and opens into it, the others stay darker behind its edge.
     */
    public static void tab(GuiGraphics graphics, Area area, boolean open) {
        int left = area.left();
        int top = area.top();
        int right = area.right();
        int bottom = area.bottom();
        roundedTop(graphics, left, top, right, bottom + 1, SHEET_EDGE);
        roundedTop(graphics, left + 1, top + 1, right - 1, open ? bottom + 1 : bottom, open ? SHEET : TAB_IDLE);
    }

    /** A gold band {@code row.width()} wide with an icon tile at its left and {@code name} on it. */
    public static void banner(GuiGraphics graphics, Font font, Area row, ItemStack icon, Component name) {
        int bandLeft = row.left() + TILE / 2;
        int bandTop = row.top() + (BANNER_HEIGHT - BAND_HEIGHT) / 2;
        Area band = new Area(bandLeft, bandTop, row.right() - bandLeft, BAND_HEIGHT);
        graphics.renderOutline(band.left(), band.top(), band.width(), band.height(), GOLD_EDGE);
        bevel(graphics, band.inset(1), GOLD, GOLD_LIGHT, GOLD_DARK);
        tile(graphics, row.left(), row.top(), TILE, icon, true, false);
        int textLeft = row.left() + TILE + PADDING;
        String shown = font.plainSubstrByWidth(name.getString(), band.right() - PADDING - textLeft);
        graphics.drawString(font, shown, textLeft, bandTop + (BAND_HEIGHT - font.lineHeight) / 2 + 1, TEXT);
    }

    /**
     * An item on a square parchment tile {@code size} wide with clipped corners, gold-edged when selected;
     * the item is scaled to fill it, so a size of {@link #TILE} or {@link #LARGE_TILE} keeps it crisp.
     */
    public static void tile(
            GuiGraphics graphics, int left, int top, int size, ItemStack icon, boolean selected, boolean hovered) {
        int edge = selected ? GOLD_LIGHT : PARCHMENT_EDGE;
        graphics.fill(left + 1, top, left + size - 1, top + size, edge);
        graphics.fill(left, top + 1, left + size, top + size - 1, edge);
        Area paper = new Area(left + 1, top + 1, size - 2, size - 2);
        if (selected) {
            paper = paper.inset(1);
        }
        bevel(graphics, paper, hovered ? PARCHMENT_HOVER : PARCHMENT, PARCHMENT_LIGHT, PARCHMENT_SHADE);
        float scale = (size - 2f * ICON_INSET) / ITEM_SIZE;
        graphics.pose().pushPose();
        graphics.pose().translate(left + ICON_INSET, top + ICON_INSET, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.renderItem(icon, 0, 0);
        graphics.pose().popPose();
    }

    public static Component heading(Component text) {
        return text.copy().withStyle(ChatFormatting.UNDERLINE);
    }

    public static Component flavor(Component text) {
        return text.copy().withStyle(ChatFormatting.ITALIC);
    }

    /** A small square bullet and {@code lines} beside it; returns the y below them. */
    public static int bullet(
            GuiGraphics graphics, Font font, List<FormattedCharSequence> lines, int left, int top, int color) {
        graphics.fill(left + 1, top + BULLET_DROP, left + 1 + BULLET_SIZE, top + BULLET_DROP + BULLET_SIZE, MUTED);
        return lines(graphics, font, lines, left + BULLET_INDENT, top, color);
    }

    /** Draws wrapped lines from {@code top} and returns the y below them. */
    public static int lines(
            GuiGraphics graphics, Font font, List<FormattedCharSequence> lines, int left, int top, int color) {
        int y = top;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, left, y, color);
            y += LINE;
        }
        return y;
    }

    /** Gold brackets hugging the button's sides, dimmed while it is inactive. */
    public static void caps(GuiGraphics graphics, AbstractWidget button) {
        int color = button.active ? GOLD_LIGHT : FRAME_SHADE;
        int top = button.getY() - 1;
        int bottom = button.getBottom() + 1;
        int left = button.getX();
        int right = button.getRight();
        graphics.fill(left - CAP_REACH, top, left - CAP_REACH + 2, bottom, color);
        graphics.fill(left - CAP_REACH, top, left + 1, top + 1, color);
        graphics.fill(left - CAP_REACH, bottom - 1, left + 1, bottom, color);
        graphics.fill(right + CAP_REACH - 2, top, right + CAP_REACH, bottom, color);
        graphics.fill(right - 1, top, right + CAP_REACH, top + 1, color);
        graphics.fill(right - 1, bottom - 1, right + CAP_REACH, bottom, color);
    }

    /** A filled box whose corners are cut {@code radius} pixels deep, one step per row. */
    private static void rounded(GuiGraphics graphics, Area area, int color, int radius) {
        for (int i = 0; i < radius; i++) {
            int cut = radius - i;
            graphics.fill(area.left() + cut, area.top() + i, area.right() - cut, area.top() + i + 1, color);
            graphics.fill(area.left() + cut, area.bottom() - i - 1, area.right() - cut, area.bottom() - i, color);
        }
        graphics.fill(area.left(), area.top() + radius, area.right(), area.bottom() - radius, color);
    }

    private static void roundedTop(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left + 1, top, right - 1, top + 1, color);
        graphics.fill(left, top + 1, right, bottom, color);
    }

    /** A filled box lit from the top left: {@code light} along the top and left, {@code shade} along the rest. */
    private static void bevel(GuiGraphics graphics, Area area, int fill, int light, int shade) {
        graphics.fill(area.left(), area.top(), area.right(), area.bottom(), fill);
        graphics.fill(area.left(), area.top(), area.right() - 1, area.top() + 1, light);
        graphics.fill(area.left(), area.top(), area.left() + 1, area.bottom() - 1, light);
        graphics.fill(area.left() + 1, area.bottom() - 1, area.right(), area.bottom(), shade);
        graphics.fill(area.right() - 1, area.top() + 1, area.right(), area.bottom(), shade);
    }
}
