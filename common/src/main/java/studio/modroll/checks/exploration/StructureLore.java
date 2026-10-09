package studio.modroll.checks.exploration;

import java.util.Comparator;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import studio.modroll.checks.Checks;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.text.FallbackText;

/**
 * History: a kind of structure is a structure tag {@code checks:history/<kind>}, so packs can add structures
 * or kinds. The first time a player in survival or adventure stands inside a piece of a structure of a kind,
 * they roll History. A made check shows a hint about that kind, from {@code checks.lore.structure.<kind>},
 * and is never rolled for again; a failed one may be tried again after the retry time. Each look rolls for
 * one kind at most.
 */
public final class StructureLore {

    private static final ResourceLocation HISTORY = Checks.id("history");
    private static final String KINDS = "history/";
    private static final String HINTS = "checks.lore.structure.";
    private static final String UNKNOWN_HINT = HINTS + "unknown";

    private static final Recollection RECOLLECTION = new Recollection(LoreStore.Topic.STRUCTURES, "structure_lore");

    private StructureLore() {}

    /** Called once per server tick; looks around for every player once per interval. */
    public static void tick(MinecraftServer server) {
        ScoresConfig.StructureLoreSettings settings =
                ScoresRuntime.config().exploration().structureLore();
        if (server.getTickCount() % settings.intervalTicks() != 0) {
            return;
        }
        server.getPlayerList().getPlayers().forEach(StructureLore::lookAround);
        RECOLLECTION.forgetWaited(server.getTickCount());
    }

    /** Rolls for a kind of structure {@code player} stands in and could roll for now. */
    public static void lookAround(ServerPlayer player) {
        ScoresConfig.StructureLoreSettings settings =
                ScoresRuntime.config().exploration().structureLore();
        if (!settings.enabled() || !ExplorationChecks.rollsFor(player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        kinds(level)
                .filter(kind -> RECOLLECTION.ready(player, kind.location()))
                .filter(kind -> level.structureManager()
                        .getStructureWithPieceAt(pos, kind)
                        .isValid())
                .findFirst()
                .ifPresent(kind -> RECOLLECTION.recall(
                        player, kind.location(), HISTORY, settings.dc(), settings.retryTicks(), hint(kind)));
    }

    public static void clear() {
        RECOLLECTION.clear();
    }

    private static Stream<TagKey<Structure>> kinds(ServerLevel level) {
        return level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .getTagNames()
                .filter(tag -> tag.location().getNamespace().equals(Checks.MOD_ID)
                        && tag.location().getPath().startsWith(KINDS))
                .sorted(Comparator.comparing(TagKey::location));
    }

    private static Component hint(TagKey<Structure> kind) {
        String name = kind.location().getPath().substring(KINDS.length()).replace('/', '.');
        return FallbackText.ofOr(HINTS + name, UNKNOWN_HINT);
    }
}
