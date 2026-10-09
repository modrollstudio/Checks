package studio.modroll.checks.body;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import studio.modroll.checks.PlayerSavedData;

/**
 * Each player's health as of their last save, in Checks' own world-level {@link SavedData}. Vanilla
 * caps the health it loads at the unmodified max, before Checks' modifiers are back, so this is what
 * lets health above it survive a relog. Without Checks the file is simply never read.
 */
public final class HealthStore extends SavedData {

    public static final String DATA_NAME = "checks_health";

    private final Map<UUID, Float> health = new HashMap<>();

    public static HealthStore get(MinecraftServer server) {
        return PlayerSavedData.get(server, DATA_NAME, HealthStore::new, HealthStore::load);
    }

    public Optional<Float> health(UUID player) {
        return Optional.ofNullable(health.get(player));
    }

    public void set(UUID player, float value) {
        Float previous = health.put(player, value);
        if (previous == null || previous != value) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return PlayerSavedData.write(tag, health, FloatTag::valueOf);
    }

    public static HealthStore load(CompoundTag tag, HolderLookup.Provider registries) {
        HealthStore store = new HealthStore();
        store.health.putAll(PlayerSavedData.read(tag, "health", CompoundTag::getFloat));
        return store;
    }
}
