package studio.modroll.checks.trait;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import studio.modroll.checks.PlayerSavedData;

/**
 * When each player last used each once-a-day trait, in game time, in its own world-level {@link SavedData}
 * so it survives relogs and restarts.
 */
public final class TraitUses extends SavedData {

    public static final String DATA_NAME = "checks_trait_uses";

    private final Map<UUID, Map<ResourceLocation, Long>> lastUses = new HashMap<>();

    public static TraitUses get(MinecraftServer server) {
        return PlayerSavedData.get(server, DATA_NAME, TraitUses::new, TraitUses::load);
    }

    public Optional<Long> lastUse(UUID player, ResourceLocation trait) {
        return Optional.ofNullable(lastUses.getOrDefault(player, Map.of()).get(trait));
    }

    public void use(UUID player, ResourceLocation trait, long gameTime) {
        lastUses.computeIfAbsent(player, uuid -> new HashMap<>()).put(trait, gameTime);
        setDirty();
    }

    /** Ready when never used, or {@code rechargeTicks} after the last use. */
    public static boolean ready(Optional<Long> lastUse, long now, int rechargeTicks) {
        return lastUse.map(last -> now - last >= rechargeTicks).orElse(true);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return PlayerSavedData.write(tag, lastUses, TraitUses::usesTag);
    }

    public static TraitUses load(CompoundTag tag, HolderLookup.Provider registries) {
        TraitUses uses = new TraitUses();
        uses.lastUses.putAll(
                PlayerSavedData.read(tag, "trait uses", (players, key) -> parseUses(players.getCompound(key))));
        return uses;
    }

    private static CompoundTag usesTag(Map<ResourceLocation, Long> uses) {
        CompoundTag tag = new CompoundTag();
        uses.forEach((trait, time) -> tag.putLong(trait.toString(), time));
        return tag;
    }

    private static Map<ResourceLocation, Long> parseUses(CompoundTag tag) {
        Map<ResourceLocation, Long> uses = new HashMap<>();
        for (String trait : tag.getAllKeys()) {
            Optional.ofNullable(ResourceLocation.tryParse(trait)).ifPresent(id -> uses.put(id, tag.getLong(trait)));
        }
        return uses;
    }
}
