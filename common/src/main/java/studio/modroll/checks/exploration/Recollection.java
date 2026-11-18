package studio.modroll.checks.exploration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

final class Recollection {

    private final LoreStore.Topic topic;
    private final String use;
    private final Map<UUID, Map<ResourceLocation, Long>> retryAt = new HashMap<>();

    Recollection(LoreStore.Topic topic, String use) {
        this.topic = topic;
        this.use = use;
    }

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

    void forgetWaited(long now) {
        retryAt.values().forEach(subjects -> subjects.values().removeIf(retry -> retry <= now));
        retryAt.values().removeIf(Map::isEmpty);
    }

    void clear() {
        retryAt.clear();
    }
}
