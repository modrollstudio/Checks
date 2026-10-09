package studio.modroll.checks.social;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import studio.modroll.checks.Checks;
import studio.modroll.checks.ClientPayloads;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * Passive Insight, with no roll. While a player's passive Insight meets its DC, the mobs within range that
 * are after them get a marker above their head, and a creeper within range that starts its fuse gets one
 * at once, with a warning sound heard only by that player. The server decides, each tick, and tells the
 * player's client only when the set of marked mobs changes; the client draws the markers. The warning is a
 * sound event no registry knows, {@code checks:insight.warning}, so it is only sent to clients with Checks.
 */
public final class Insight {

    public static final ResourceLocation WARNING = Checks.id("insight.warning");

    private static final ResourceLocation INSIGHT = Checks.id("insight");
    private static final float WARNING_VOLUME = 1f;
    private static final float WARNING_PITCH = 1f;

    private static final Map<UUID, Set<Integer>> MARKED = new HashMap<>();
    /** Per player, the creepers whose current fuse they were warned about. */
    private static final Map<UUID, Set<UUID>> WARNED = new HashMap<>();

    private Insight() {}

    /** Called once per server tick. */
    public static void tick(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        players.forEach(Insight::update);
        Set<UUID> online = new HashSet<>();
        players.forEach(player -> online.add(player.getUUID()));
        MARKED.keySet().retainAll(online);
        WARNED.keySet().retainAll(online);
    }

    public static void clear() {
        MARKED.clear();
        WARNED.clear();
    }

    private static void update(ServerPlayer player) {
        ScoresConfig.SocialSettings social = ScoresRuntime.config().social();
        ScoresConfig.InsightSettings insight = social.insight();
        Set<Integer> marked = new HashSet<>();
        Set<UUID> fusing = new HashSet<>();
        sensed(player, social, insight.senseHostility(), Mob.class, mob -> mob.getTarget() == player)
                .forEach(mob -> marked.add(mob.getId()));
        Set<UUID> warned = WARNED.getOrDefault(player.getUUID(), Set.of());
        for (Creeper creeper :
                sensed(player, social, insight.creeperWarning(), Creeper.class, creeper -> creeper.getSwellDir() > 0)) {
            marked.add(creeper.getId());
            fusing.add(creeper.getUUID());
            if (!warned.contains(creeper.getUUID())) {
                warn(player, creeper);
            }
        }
        WARNED.put(player.getUUID(), fusing);
        if (!marked.equals(MARKED.getOrDefault(player.getUUID(), Set.of()))) {
            MARKED.put(player.getUUID(), marked);
            ClientPayloads.send(player, new HostilityPayload(List.copyOf(marked)));
        }
    }

    /**
     * The mobs within the sense's range that {@code filter} accepts, while the sense is on and the player's
     * passive Insight meets its DC; that is only worked out when there are any.
     */
    private static <T extends Mob> List<T> sensed(
            ServerPlayer player,
            ScoresConfig.SocialSettings social,
            ScoresConfig.InsightSense sense,
            Class<T> type,
            Predicate<T> filter) {
        if (!social.enabled() || !sense.enabled() || player.isSpectator()) {
            return List.of();
        }
        List<T> mobs =
                player.level().getEntitiesOfClass(type, player.getBoundingBox().inflate(sense.range()), filter);
        return mobs.isEmpty() || !meets(player, sense.dc()) ? List.of() : mobs;
    }

    private static boolean meets(ServerPlayer player, int dc) {
        return ChecksApi.skill(INSIGHT)
                .map(skill -> ChecksApi.passiveScore(player, skill) >= dc)
                .orElse(false);
    }

    /** From the creeper, so the player hears where it is. */
    private static void warn(ServerPlayer player, Creeper creeper) {
        if (!SocialFeel.checksClient().test(player)) {
            return;
        }
        player.connection.send(new ClientboundSoundPacket(
                Holder.direct(SoundEvent.createVariableRangeEvent(WARNING)),
                SoundSource.HOSTILE,
                creeper.getX(),
                creeper.getY(),
                creeper.getZ(),
                WARNING_VOLUME,
                WARNING_PITCH,
                player.getRandom().nextLong()));
    }
}
