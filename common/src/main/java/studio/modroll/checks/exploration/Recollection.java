package studio.modroll.checks.exploration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * One kind of lore: a player rolls once to recall a subject, a mob type or a structure kind. A made check
 * shows its hint and is saved for good; after a failed one the subject waits out the retry time, which lives
 * in memory only, so a restart ends it.
 */
final class Recollection {

    private final LoreStore.Topic topic;
    private final String use;
    private final Map<UUID, Map<ResourceLocation, Long>> retryAt = new HashMap<>();

    /** Failure lines come from {@code checks.exploration.<use>.failure}. */
    Recollection(LoreStore.Topic topic, String use) {
        this.topic = topic;
        this.use = use;
    }

    /** Whether {@code player} could roll for {@code subject} now: not recalled, and not waiting to retry. */
    boolean ready(ServerPlayer player, ResourceLocation subject) {
        long retry = retryAt.getOrDefault(player.getUUID(), Map.of()).getOrDefault(subject, Long.MIN_VALUE);
        return player.server.getTickCount() >= retry
                && !LoreStore.get(player.server).knows(player.getUUID(), topic, subject);
    }

    void recall(
            ServerPlayer player,
            ResourceLocation subject,
            ResourceLocation skill,
            int dc,
            int retryTicks,
            Component hint) {
        ExplorationChecks.made(player, skill, dc, made -> made ? hint : ExplorationChecks.flavor(player, use, false))
                .ifPresent(made -> {
                    if (made) {
                        LoreStore.get(player.server).learn(player.getUUID(), topic, subject);
                    } else {
                        retryAt.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>())
                                .put(subject, (long) player.server.getTickCount() + retryTicks);
                    }
                });
    }

    /** Forgets every wait that is over. */
    void forgetWaited(long now) {
        retryAt.values().forEach(subjects -> subjects.values().removeIf(retry -> retry <= now));
        retryAt.values().removeIf(Map::isEmpty);
    }

    void clear() {
        retryAt.clear();
    }
}
