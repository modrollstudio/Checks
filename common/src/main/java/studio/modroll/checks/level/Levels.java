package studio.modroll.checks.level;

public final class Levels {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 20;

    private Levels() {}

    public static boolean inRange(int level) {
        return level >= MIN_LEVEL && level <= MAX_LEVEL;
    }
}
