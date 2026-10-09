package studio.modroll.checks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Checks' world-level {@link SavedData} files, each keeping its players under a {@code players} compound by UUID. */
public final class PlayerSavedData {

    private static final String PLAYERS_KEY = "players";

    private PlayerSavedData() {}

    // A null DataFixTypes is safe on both loaders: NeoForge patches it and Fabric API's
    // PersistentStateManagerMixin skips the fixer for null.
    public static <T extends SavedData> T get(
            MinecraftServer server,
            String name,
            Supplier<T> create,
            BiFunction<CompoundTag, HolderLookup.Provider, T> load) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(create, load, null), name);
    }

    public static <V> CompoundTag write(CompoundTag tag, Map<UUID, V> players, Function<V, Tag> entry) {
        CompoundTag entries = new CompoundTag();
        players.forEach((uuid, value) -> entries.put(uuid.toString(), entry.apply(value)));
        tag.put(PLAYERS_KEY, entries);
        return tag;
    }

    /**
     * Each player's entry, read by {@code entry} from the players compound and the entry's key. An entry
     * under an invalid UUID, or one {@code entry} rejects with an {@link IllegalArgumentException}, is skipped
     * with a warning.
     */
    public static <V> Map<UUID, V> read(CompoundTag tag, String what, BiFunction<CompoundTag, String, V> entry) {
        CompoundTag entries = tag.getCompound(PLAYERS_KEY);
        Map<UUID, V> players = new HashMap<>();
        for (String key : entries.getAllKeys()) {
            try {
                players.put(UUID.fromString(key), entry.apply(entries, key));
            } catch (IllegalArgumentException e) {
                Checks.LOG.warn("Skipping {} under '{}': {}", what, key, e.getMessage());
            }
        }
        return players;
    }
}
