package studio.modroll.checks.gametest;

/**
 * Vanilla, and so Fabric, writes SavedData in place; NeoForge hands it to its IO worker, so its GameTests set a wait.
 */
public final class SavedDataWrites {

    private static volatile Runnable await = () -> {};

    private SavedDataWrites() {}

    public static void awaitWith(Runnable wait) {
        await = wait;
    }

    static void await() {
        await.run();
    }
}
