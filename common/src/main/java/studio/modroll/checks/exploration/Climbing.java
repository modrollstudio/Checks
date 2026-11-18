package studio.modroll.checks.exploration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import studio.modroll.checks.Checks;
import studio.modroll.checks.ClientPayloads;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/** Climbing is the client's own movement, so the server decides the speed and tells the client when it changes. */
public final class Climbing {

    private static final ResourceLocation ATHLETICS = Checks.id("athletics");
    private static final float UNCHANGED = 1f;

    private static final Map<UUID, Float> SENT = new ConcurrentHashMap<>();

    private Climbing() {}

    public static float speed(ServerPlayer player) {
        ScoresConfig.ClimbingSettings settings =
                ScoresRuntime.config().exploration().climbing();
        if (!settings.enabled()) {
            return UNCHANGED;
        }
        return speed(ExplorationChecks.modifier(player, ATHLETICS), settings.perPoint(), settings.maxBonus());
    }

    static float speed(int athleticsModifier, double perPoint, double maxBonus) {
        return (float) (UNCHANGED + ExplorationChecks.share(athleticsModifier, perPoint, maxBonus));
    }

    public static void tick(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(Climbing::sync);
    }

    /** A player who just joined has no speed yet, so the next tick sends it. */
    public static void onLogin(ServerPlayer player) {
        SENT.remove(player.getUUID());
    }

    public static void clear() {
        SENT.clear();
    }

    private static void sync(ServerPlayer player) {
        float speed = speed(player);
        Float sent = SENT.get(player.getUUID());
        if (sent == null || sent != speed) {
            ClientPayloads.send(player, new ClimbingPayload(speed));
            SENT.put(player.getUUID(), speed);
        }
    }
}
