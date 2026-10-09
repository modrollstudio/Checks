package studio.modroll.checks.gametest;

/**
 * How the shared scenarios wait for SavedData to reach the disk. Vanilla, and so Fabric, writes it in place;
 * NeoForge hands the write to its IO worker, so its GameTests set a wait for that worker.
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
