package studio.modroll.checks.uishots;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;

final class ShotScript {

    private static final int WAIT_TIMEOUT_TICKS = 600;

    private final Deque<Step> steps = new ArrayDeque<>();
    private int ticksOnStep;

    @FunctionalInterface
    interface Step {
        boolean run(int tick);
    }

    ShotScript then(Runnable action) {
        steps.add(tick -> {
            action.run();
            return true;
        });
        return this;
    }

    ShotScript waitTicks(int ticks) {
        steps.add(tick -> tick >= ticks);
        return this;
    }

    ShotScript waitFor(String what, BooleanSupplier condition) {
        steps.add(tick -> {
            if (condition.getAsBoolean()) {
                return true;
            }
            if (tick > WAIT_TIMEOUT_TICKS) {
                throw new IllegalStateException("UI shots timed out waiting for " + what);
            }
            return false;
        });
        return this;
    }

    boolean finished() {
        return steps.isEmpty();
    }

    void tick() {
        Step step = steps.peek();
        if (step == null) {
            return;
        }
        if (step.run(ticksOnStep++)) {
            steps.poll();
            ticksOnStep = 0;
        }
    }
}
