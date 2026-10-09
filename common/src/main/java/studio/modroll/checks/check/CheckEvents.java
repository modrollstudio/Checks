package studio.modroll.checks.check;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.AfterCheckEvent;
import studio.modroll.checks.api.BeforeCheckEvent;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * The listeners registered through {@link studio.modroll.checks.api.ChecksApi}. A listener that throws is
 * logged and skipped, so one bad script cannot break checks for the server. While events are disabled no
 * listener is called.
 */
public final class CheckEvents {

    private static final List<Consumer<BeforeCheckEvent>> BEFORE = new CopyOnWriteArrayList<>();
    private static final List<Consumer<AfterCheckEvent>> AFTER = new CopyOnWriteArrayList<>();

    private CheckEvents() {}

    public static void addBeforeListener(Consumer<BeforeCheckEvent> listener) {
        BEFORE.add(listener);
    }

    public static void addAfterListener(Consumer<AfterCheckEvent> listener) {
        AFTER.add(listener);
    }

    static void fireBefore(BeforeCheckEvent event) {
        dispatch(BEFORE, event);
    }

    static void fireAfter(AfterCheckEvent event) {
        dispatch(AFTER, event);
    }

    /** For tests. */
    public static void clearListeners() {
        BEFORE.clear();
        AFTER.clear();
    }

    private static <T> void dispatch(List<Consumer<T>> listeners, T event) {
        if (!ScoresRuntime.config().extensions().events()) {
            return;
        }
        for (Consumer<T> listener : listeners) {
            try {
                listener.accept(event);
            } catch (RuntimeException e) {
                Checks.LOG.error("A check event listener threw; skipping it for this roll", e);
            }
        }
    }
}
