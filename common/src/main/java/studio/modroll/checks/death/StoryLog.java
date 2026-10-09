package studio.modroll.checks.death;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;

/**
 * The latest time each player went through each {@link DeathStory}, in server ticks, with the item it was
 * about. Kept in memory only: a restart forgets every story.
 */
public final class StoryLog {

    /** A story to tell, about {@code item} (empty when it is about none). */
    public record Told(DeathStory story, Component item) {}

    private record Entry(long tick, Component item) {}

    private final Map<UUID, Map<DeathStory, Entry>> entries = new HashMap<>();

    public synchronized void record(UUID player, DeathStory story, Component item, long now) {
        entries.computeIfAbsent(player, uuid -> new EnumMap<>(DeathStory.class)).put(story, new Entry(now, item));
    }

    /**
     * The first story, in {@link DeathStory} order, the player went through at most {@code windowTicks} ago
     * and that {@code fits} the death. A death is told once: the player's stories are forgotten either way.
     */
    public synchronized Optional<Told> take(UUID player, long now, int windowTicks, Predicate<DeathStory> fits) {
        Map<DeathStory, Entry> stories = entries.remove(player);
        if (stories == null) {
            return Optional.empty();
        }
        return stories.entrySet().stream()
                .filter(story -> now - story.getValue().tick() <= windowTicks)
                .filter(story -> fits.test(story.getKey()))
                .findFirst()
                .map(story -> new Told(story.getKey(), story.getValue().item()));
    }

    public synchronized void clear() {
        entries.clear();
    }
}
