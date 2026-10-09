package studio.modroll.checks.score;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import studio.modroll.checks.Checks;
import studio.modroll.checks.PlayerSavedData;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.proficiency.Proficiencies;
import studio.modroll.checks.skill.Skills;

/**
 * Per-player values in a world-level {@link SavedData} keyed by UUID, which keeps this loader-agnostic.
 * Values for skills a datapack no longer defines are kept, and every key is optional: a player has only
 * the values set for them.
 */
public final class PlayerScoreStore extends SavedData {

    public static final String DATA_NAME = "checks_player_scores";
    private static final String SKILLS_KEY = "skills";
    private static final String PROFICIENCY_BONUS_KEY = "proficiency";
    private static final String SKILL_PROFICIENCIES_KEY = "skill_proficiencies";
    private static final String SAVE_PROFICIENCIES_KEY = "save_proficiencies";

    private final Map<UUID, EnumMap<Ability, Integer>> scores = new HashMap<>();
    private final Map<UUID, Map<ResourceLocation, Integer>> skillBonuses = new HashMap<>();
    private final Map<UUID, Integer> proficiencyBonuses = new HashMap<>();
    private final Map<UUID, Map<ResourceLocation, Proficiency>> skillProficiencies = new HashMap<>();
    private final Map<UUID, EnumMap<Ability, Proficiency>> saveProficiencies = new HashMap<>();

    public static PlayerScoreStore get(MinecraftServer server) {
        return PlayerSavedData.get(server, DATA_NAME, PlayerScoreStore::new, PlayerScoreStore::load);
    }

    public OptionalInt getScore(UUID player, Ability ability) {
        return lookup(scores.get(player), ability);
    }

    public void setScore(UUID player, Ability ability, int score) {
        scores.computeIfAbsent(player, id -> new EnumMap<>(Ability.class)).put(ability, Abilities.clamp(score));
        setDirty();
    }

    public OptionalInt getSkillBonus(UUID player, ResourceLocation skill) {
        return lookup(skillBonuses.get(player), skill);
    }

    public void setSkillBonus(UUID player, ResourceLocation skill, int bonus) {
        skillBonuses.computeIfAbsent(player, id -> new HashMap<>()).put(skill, Skills.clampBonus(bonus));
        setDirty();
    }

    public OptionalInt getProficiencyBonus(UUID player) {
        return lookup(proficiencyBonuses, player);
    }

    public void setProficiencyBonus(UUID player, int bonus) {
        proficiencyBonuses.put(player, Proficiencies.clampBonus(bonus));
        setDirty();
    }

    public Optional<Proficiency> getSkillProficiency(UUID player, ResourceLocation skill) {
        return lookupLevel(skillProficiencies.get(player), skill);
    }

    public void setSkillProficiency(UUID player, ResourceLocation skill, Proficiency proficiency) {
        skillProficiencies.computeIfAbsent(player, id -> new HashMap<>()).put(skill, proficiency);
        setDirty();
    }

    public Optional<Proficiency> getSaveProficiency(UUID player, Ability ability) {
        return lookupLevel(saveProficiencies.get(player), ability);
    }

    /** Callers must reject expertise first: saving throws are none or proficient. */
    public void setSaveProficiency(UUID player, Ability ability, Proficiency proficiency) {
        saveProficiencies
                .computeIfAbsent(player, id -> new EnumMap<>(Ability.class))
                .put(ability, proficiency);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        Map<UUID, CompoundTag> players = new HashMap<>();
        scores.forEach((uuid, playerScores) -> {
            CompoundTag entry = playerEntry(players, uuid);
            playerScores.forEach((ability, value) -> entry.putInt(ability.id(), value));
        });
        skillBonuses.forEach((uuid, bonuses) -> {
            CompoundTag skills = new CompoundTag();
            bonuses.forEach((skill, value) -> skills.putInt(skill.toString(), value));
            playerEntry(players, uuid).put(SKILLS_KEY, skills);
        });
        proficiencyBonuses.forEach((uuid, bonus) -> playerEntry(players, uuid).putInt(PROFICIENCY_BONUS_KEY, bonus));
        skillProficiencies.forEach((uuid, levels) ->
                playerEntry(players, uuid).put(SKILL_PROFICIENCIES_KEY, levelsTag(levels, ResourceLocation::toString)));
        saveProficiencies.forEach((uuid, levels) ->
                playerEntry(players, uuid).put(SAVE_PROFICIENCIES_KEY, levelsTag(levels, Ability::id)));
        return PlayerSavedData.write(tag, players, entry -> entry);
    }

    public static PlayerScoreStore load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerScoreStore store = new PlayerScoreStore();
        PlayerSavedData.read(tag, "player scores", CompoundTag::getCompound).forEach(store::loadPlayer);
        return store;
    }

    private static <K> OptionalInt lookup(Map<K, Integer> values, K key) {
        if (values == null || !values.containsKey(key)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(values.get(key));
    }

    private static <K> Optional<Proficiency> lookupLevel(Map<K, Proficiency> levels, K key) {
        return levels == null ? Optional.empty() : Optional.ofNullable(levels.get(key));
    }

    private static <K> CompoundTag levelsTag(Map<K, Proficiency> levels, Function<K, String> keyName) {
        CompoundTag tag = new CompoundTag();
        levels.forEach((key, level) -> tag.putString(keyName.apply(key), level.id()));
        return tag;
    }

    private static CompoundTag playerEntry(Map<UUID, CompoundTag> players, UUID uuid) {
        return players.computeIfAbsent(uuid, id -> new CompoundTag());
    }

    private void loadPlayer(UUID uuid, CompoundTag entry) {
        for (Ability ability : Ability.values()) {
            if (entry.contains(ability.id())) {
                scores.computeIfAbsent(uuid, id -> new EnumMap<>(Ability.class))
                        .put(ability, Abilities.clamp(entry.getInt(ability.id())));
            }
        }
        CompoundTag skills = entry.getCompound(SKILLS_KEY);
        for (String key : skills.getAllKeys()) {
            parseSkillId(key).ifPresent(skill -> skillBonuses
                    .computeIfAbsent(uuid, id -> new HashMap<>())
                    .put(skill, Skills.clampBonus(skills.getInt(key))));
        }
        if (entry.contains(PROFICIENCY_BONUS_KEY)) {
            proficiencyBonuses.put(uuid, Proficiencies.clampBonus(entry.getInt(PROFICIENCY_BONUS_KEY)));
        }
        loadSkillProficiencies(uuid, entry.getCompound(SKILL_PROFICIENCIES_KEY));
        loadSaveProficiencies(uuid, entry.getCompound(SAVE_PROFICIENCIES_KEY));
    }

    private void loadSkillProficiencies(UUID uuid, CompoundTag levels) {
        for (String key : levels.getAllKeys()) {
            Optional<ResourceLocation> skill = parseSkillId(key);
            Optional<Proficiency> level = parseLevel(levels, key);
            if (skill.isPresent() && level.isPresent()) {
                skillProficiencies.computeIfAbsent(uuid, id -> new HashMap<>()).put(skill.get(), level.get());
            }
        }
    }

    private void loadSaveProficiencies(UUID uuid, CompoundTag levels) {
        for (Ability ability : Ability.values()) {
            if (!levels.contains(ability.id())) {
                continue;
            }
            parseLevel(levels, ability.id()).ifPresent(level -> {
                if (Proficiencies.allowedOnSave(level)) {
                    saveProficiencies
                            .computeIfAbsent(uuid, id -> new EnumMap<>(Ability.class))
                            .put(ability, level);
                } else {
                    Checks.LOG.warn("Skipping {} save proficiency '{}'", ability.id(), level.id());
                }
            });
        }
    }

    private static Optional<Proficiency> parseLevel(CompoundTag levels, String key) {
        Optional<Proficiency> level = Proficiency.byId(levels.getString(key));
        if (level.isEmpty()) {
            Checks.LOG.warn("Skipping unknown proficiency '{}' under '{}'", levels.getString(key), key);
        }
        return level;
    }

    private static Optional<ResourceLocation> parseSkillId(String key) {
        ResourceLocation id = ResourceLocation.tryParse(key);
        if (id == null) {
            Checks.LOG.warn("Skipping player skill entry under invalid skill id '{}'", key);
        }
        return Optional.ofNullable(id);
    }
}
