package studio.modroll.checks.trigger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/**
 * When each trigger can fire again, per player and per target, in server ticks. Kept in memory only:
 * a restart clears every cooldown.
 */
public final class TriggerCooldowns {

    private record PlayerKey(ResourceLocation trigger, UUID player) {}

    private record TargetKey(ResourceLocation trigger, String target) {}

    private final Map<PlayerKey, Long> players = new HashMap<>();
    private final Map<TargetKey, Long> targets = new HashMap<>();

    /** {@code target} is empty for an item use, which has no target. */
    public synchronized boolean ready(ResourceLocation trigger, UUID player, Optional<String> target, long now) {
        return now >= players.getOrDefault(new PlayerKey(trigger, player), now)
                && target.map(key -> now >= targets.getOrDefault(new TargetKey(trigger, key), now))
                        .orElse(true);
    }

    public synchronized void start(CheckTrigger trigger, UUID player, Optional<String> target, long now) {
        players.values().removeIf(readyAt -> readyAt <= now);
        targets.values().removeIf(readyAt -> readyAt <= now);
        CheckTrigger.Cooldown cooldown = trigger.cooldown();
        if (cooldown.playerTicks() > 0) {
            players.put(new PlayerKey(trigger.id(), player), now + cooldown.playerTicks());
        }
        if (cooldown.targetTicks() > 0) {
            target.ifPresent(key -> targets.put(new TargetKey(trigger.id(), key), now + cooldown.targetTicks()));
        }
    }

    public synchronized void clear() {
        players.clear();
        targets.clear();
    }
}
