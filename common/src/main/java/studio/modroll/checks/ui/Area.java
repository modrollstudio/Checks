package studio.modroll.checks.ui;

public record Area(int left, int top, int width, int height) {

    public int right() {
        return left + width;
    }

    public int bottom() {
        return top + height;
    }

    public boolean contains(double x, double y) {
        return x >= left && x < right() && y >= top && y < bottom();
    }

    public Area inset(int padding) {
        return new Area(left + padding, top + padding, width - 2 * padding, height - 2 * padding);
    }

    public Area leftPart(float share, int gap) {
        return new Area(left, top, (int) ((width - gap) * share), height);
    }

    public Area rightPart(float share, int gap) {
        int leftWidth = (int) ((width - gap) * share);
        return new Area(left + leftWidth + gap, top, width - leftWidth - gap, height);
    }
}
