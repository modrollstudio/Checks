package studio.modroll.checks.save;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The last result of each save per entity, kept until its cooldown ends, in server ticks. Kept in memory
 * only: a restart clears every cooldown.
 */
public final class SaveCooldowns {

    private record Key(VanillaSave save, UUID entity) {}

    private record Last(long readyAt, boolean saved) {}

    private final Map<Key, Last> results = new HashMap<>();

    /** Whether the last such save succeeded, while it is cooling down; empty when a new roll is due. */
    public synchronized Optional<Boolean> recent(VanillaSave save, UUID entity, long now) {
        return Optional.ofNullable(results.get(new Key(save, entity)))
                .filter(last -> now < last.readyAt())
                .map(Last::saved);
    }

    public synchronized void start(VanillaSave save, UUID entity, long now, int cooldownTicks, boolean saved) {
        results.values().removeIf(last -> last.readyAt() <= now);
        if (cooldownTicks > 0) {
            results.put(new Key(save, entity), new Last(now + cooldownTicks, saved));
        }
    }

    public synchronized void clear() {
        results.clear();
    }
}
