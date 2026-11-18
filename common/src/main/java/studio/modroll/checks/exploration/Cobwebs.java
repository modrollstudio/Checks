package studio.modroll.checks.exploration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.phys.AABB;
import studio.modroll.checks.Checks;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/** Athletics to break free of a cobweb; the retry wait lives in memory only, so a restart ends it. */
public final class Cobwebs {

    private static final ResourceLocation ATHLETICS = Checks.id("athletics");
    /** As vanilla shrinks the box when it looks for blocks an entity is inside. */
    private static final double INSIDE = 1.0E-5;

    private static final Map<UUID, Long> RETRY_AT = new HashMap<>();

    private Cobwebs() {}

    public static void tick(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(Cobwebs::struggle);
        long now = server.getTickCount();
        RETRY_AT.values().removeIf(retryAt -> retryAt <= now);
    }

    public static void struggle(ServerPlayer player) {
        ScoresConfig.CobwebSettings settings =
                ScoresRuntime.config().exploration().cobwebs();
        long now = player.server.getTickCount();
        if (!settings.enabled()
                || !ExplorationChecks.rollsFor(player)
                || now < RETRY_AT.getOrDefault(player.getUUID(), now)) {
            return;
        }
        List<BlockPos> webs = websAround(player);
        if (webs.isEmpty() || !webs.stream().allMatch(web -> ExplorationChecks.mayBreak(player, web))) {
            return;
        }
        ExplorationChecks.made(player, ATHLETICS, settings.dc(), "cobweb").ifPresent(made -> {
            if (made) {
                webs.forEach(web -> player.level().destroyBlock(web, false, player));
            } else {
                RETRY_AT.put(player.getUUID(), now + settings.retryTicks());
            }
        });
    }

    public static void clear() {
        RETRY_AT.clear();
    }

    private static List<BlockPos> websAround(ServerPlayer player) {
        AABB box = player.getBoundingBox().deflate(INSIDE);
        Iterable<BlockPos> inside = BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ));
        return StreamSupport.stream(inside.spliterator(), false)
                .filter(pos -> player.level().getBlockState(pos).getBlock() instanceof WebBlock)
                .map(BlockPos::immutable)
                .toList();
    }
}
