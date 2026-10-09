package studio.modroll.checks.exploration;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import studio.modroll.checks.Checks;
import studio.modroll.checks.PlayerSavedData;

/**
 * Per player, the mob types and structure kinds whose lore they recalled, in their own world-level
 * {@link SavedData}; without a file, every player knows nothing yet.
 */
public final class LoreStore extends SavedData {

    public static final String DATA_NAME = "checks_lore";

    public enum Topic {
        MOBS,
        STRUCTURES;

        private String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final Map<UUID, Map<Topic, Set<ResourceLocation>>> known = new HashMap<>();

    public static LoreStore get(MinecraftServer server) {
        return PlayerSavedData.get(server, DATA_NAME, LoreStore::new, LoreStore::load);
    }

    public boolean knows(UUID player, Topic topic, ResourceLocation subject) {
        return known.getOrDefault(player, Map.of())
                .getOrDefault(topic, Set.of())
                .contains(subject);
    }

    public void learn(UUID player, Topic topic, ResourceLocation subject) {
        known.computeIfAbsent(player, uuid -> new EnumMap<>(Topic.class))
                .computeIfAbsent(topic, t -> new HashSet<>())
                .add(subject);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return PlayerSavedData.write(tag, known, LoreStore::topicsTag);
    }

    public static LoreStore load(CompoundTag tag, HolderLookup.Provider registries) {
        LoreStore store = new LoreStore();
        store.known.putAll(PlayerSavedData.read(tag, "lore", (players, key) -> parseTopics(players.getCompound(key))));
        return store;
    }

    private static CompoundTag topicsTag(Map<Topic, Set<ResourceLocation>> topics) {
        CompoundTag tag = new CompoundTag();
        topics.forEach((topic, subjects) -> {
            ListTag list = new ListTag();
            subjects.forEach(subject -> list.add(StringTag.valueOf(subject.toString())));
            tag.put(topic.key(), list);
        });
        return tag;
    }

    private static Map<Topic, Set<ResourceLocation>> parseTopics(CompoundTag tag) {
        Map<Topic, Set<ResourceLocation>> topics = new EnumMap<>(Topic.class);
        for (Topic topic : Topic.values()) {
            Set<ResourceLocation> subjects = new HashSet<>();
            for (Tag entry : tag.getList(topic.key(), Tag.TAG_STRING)) {
                ResourceLocation subject = ResourceLocation.tryParse(entry.getAsString());
                if (subject == null) {
                    Checks.LOG.warn("Skipping lore with invalid id '{}'", entry.getAsString());
                } else {
                    subjects.add(subject);
                }
            }
            topics.put(topic, subjects);
        }
        return topics;
    }
}
