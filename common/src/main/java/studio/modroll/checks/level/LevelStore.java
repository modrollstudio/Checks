package studio.modroll.checks.level;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import studio.modroll.checks.api.Ability;

/**
 * Per-player levels in their own world-level {@link SavedData}; without a file, every player starts at
 * level 1. A level outside 1..20 is clamped. The per-level hit dice older versions recorded are not read.
 */
public final class LevelStore extends SavedData {

    public static final String DATA_NAME = "checks_levels";
    private static final String LEVEL_KEY = "level";
    private static final String XP_KEY = "xp";
    private static final String IMPROVEMENTS_KEY = "improvements";
    private static final String EXPERTISE_KEY = "expertise";
    private static final String HIT_DICE_SPENT_KEY = "hit_dice_spent";

    private final Map<UUID, PlayerLevel> levels = new HashMap<>();

    public static LevelStore get(MinecraftServer server) {
        return PlayerSavedData.get(server, DATA_NAME, LevelStore::new, LevelStore::load);
    }

    public PlayerLevel level(UUID player) {
        return levels.getOrDefault(player, PlayerLevel.START);
    }

    public void set(UUID player, PlayerLevel level) {
        levels.put(player, level);
        setDirty();
    }

    public void reset(UUID player) {
        levels.remove(player);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return PlayerSavedData.write(tag, levels, LevelStore::levelTag);
    }

    public static LevelStore load(CompoundTag tag, HolderLookup.Provider registries) {
        LevelStore store = new LevelStore();
        store.levels.putAll(PlayerSavedData.read(tag, "level", (players, key) -> parseLevel(players.getCompound(key))));
        return store;
    }

    private static CompoundTag levelTag(PlayerLevel level) {
        ListTag improvements = new ListTag();
        level.improvements().forEach(improvement -> improvements.add(improvementTag(improvement)));
        ListTag expertise = new ListTag();
        level.expertise().forEach(skill -> expertise.add(StringTag.valueOf(skill.toString())));
        CompoundTag tag = new CompoundTag();
        tag.putInt(LEVEL_KEY, level.level());
        tag.putInt(XP_KEY, level.xp());
        tag.put(IMPROVEMENTS_KEY, improvements);
        tag.put(EXPERTISE_KEY, expertise);
        tag.putInt(HIT_DICE_SPENT_KEY, level.hitDiceSpent());
        return tag;
    }

    private static CompoundTag improvementTag(Map<Ability, Integer> improvement) {
        CompoundTag tag = new CompoundTag();
        improvement.forEach((ability, increase) -> tag.putInt(ability.id(), increase));
        return tag;
    }

    private static PlayerLevel parseLevel(CompoundTag tag) {
        int level = Math.clamp(tag.getInt(LEVEL_KEY), Levels.MIN_LEVEL, Levels.MAX_LEVEL);
        List<Map<Ability, Integer>> improvements = new ArrayList<>();
        for (Tag improvement : tag.getList(IMPROVEMENTS_KEY, Tag.TAG_COMPOUND)) {
            improvements.add(parseImprovement((CompoundTag) improvement));
        }
        List<ResourceLocation> expertise = new ArrayList<>();
        for (Tag skill : tag.getList(EXPERTISE_KEY, Tag.TAG_STRING)) {
            Optional.ofNullable(ResourceLocation.tryParse(skill.getAsString()))
                    .ifPresentOrElse(
                            expertise::add,
                            () -> Checks.LOG.warn("Skipping expertise in invalid skill '{}'", skill.getAsString()));
        }
        return new PlayerLevel(
                level,
                Math.max(0, tag.getInt(XP_KEY)),
                improvements,
                expertise,
                Math.clamp(tag.getInt(HIT_DICE_SPENT_KEY), 0, level));
    }

    private static Map<Ability, Integer> parseImprovement(CompoundTag tag) {
        Map<Ability, Integer> improvement = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            if (tag.contains(ability.id())) {
                improvement.put(ability, tag.getInt(ability.id()));
            }
        }
        return improvement;
    }
}
