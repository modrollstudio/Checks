package studio.modroll.checks.trait;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import studio.modroll.checks.ClientPayloads;
import studio.modroll.checks.score.ScoresRuntime;

/** Decided on the server and sent to the client when it changes; checked every tick, so any change shows within one. */
public final class Darkvision {

    private static final Map<UUID, Float> SENT = new ConcurrentHashMap<>();

    private Darkvision() {}

    public static float strength(ServerPlayer player) {
        if (Traits.effects(player, TraitEffect.Darkvision.class).isEmpty()) {
            return 0f;
        }
        return (float) ScoresRuntime.config().traits().darkvisionStrength();
    }

    public static void tick(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(Darkvision::sync);
    }

    /** A player who just joined has no strength yet, so the next tick sends it. */
    public static void onLogin(ServerPlayer player) {
        SENT.remove(player.getUUID());
    }

    public static void clear() {
        SENT.clear();
    }

    private static void sync(ServerPlayer player) {
        float strength = strength(player);
        Float sent = SENT.get(player.getUUID());
        if (sent == null || sent != strength) {
            ClientPayloads.send(player, new DarkvisionPayload(strength));
            SENT.put(player.getUUID(), strength);
        }
    }
}
