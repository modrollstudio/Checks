package studio.modroll.checks.creation;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import studio.modroll.checks.PlayerSavedData;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.body.Size;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.critfall.api.dice.DieRoll;

/**
 * Per-player characters in their own world-level {@link SavedData}. A malformed entry, a build missing any
 * part included, loads as "no character" with a warning.
 */
public final class CharacterStore extends SavedData {

    public static final String DATA_NAME = "checks_characters";
    private static final String PROMPTED_KEY = "prompted";
    private static final String ROLLS_KEY = "rolls";
    private static final String BUILD_KEY = "build";
    private static final String METHOD_KEY = "method";
    private static final String SCORES_LIST_KEY = "rolled";
    private static final String TOTAL_KEY = "total";
    private static final String DICE_KEY = "dice";
    private static final String SIDES_KEY = "sides";
    private static final String VALUE_KEY = "value";
    private static final String KEPT_KEY = "kept";
    private static final String SCORES_KEY = "scores";
    private static final String SKILLS_KEY = "skills";
    private static final String CHOICES_KEY = "presets";
    private static final String GRANTS_KEY = "grants";
    private static final String BONUSES_KEY = "bonuses";
    private static final String SAVES_KEY = "saves";
    private static final String SIZE_KEY = "size";
    private static final String TRAIT_SKILLS_KEY = "trait_skills";

    private final Map<UUID, PlayerCharacter> characters = new HashMap<>();

    public static CharacterStore get(MinecraftServer server) {
        return PlayerSavedData.get(server, DATA_NAME, CharacterStore::new, CharacterStore::load);
    }

    public PlayerCharacter character(UUID player) {
        return characters.getOrDefault(player, PlayerCharacter.NONE);
    }

    public void update(UUID player, UnaryOperator<PlayerCharacter> change) {
        characters.put(player, change.apply(character(player)));
        setDirty();
    }

    public void reset(UUID player) {
        characters.remove(player);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return PlayerSavedData.write(tag, characters, CharacterStore::characterTag);
    }

    public static CharacterStore load(CompoundTag tag, HolderLookup.Provider registries) {
        CharacterStore store = new CharacterStore();
        store.characters.putAll(
                PlayerSavedData.read(tag, "character", (players, key) -> parseCharacter(players.getCompound(key))));
        return store;
    }

    private static CompoundTag characterTag(PlayerCharacter character) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(PROMPTED_KEY, character.prompted());
        character.rolls().ifPresent(rolls -> tag.put(ROLLS_KEY, rollsTag(rolls)));
        character.build().ifPresent(build -> tag.put(BUILD_KEY, buildTag(build)));
        return tag;
    }

    private static CompoundTag rollsTag(CharacterRolls rolls) {
        ListTag scores = new ListTag();
        rolls.scores().forEach(score -> scores.add(rolledScoreTag(score)));
        CompoundTag tag = new CompoundTag();
        tag.putString(METHOD_KEY, rolls.method().id());
        tag.put(SCORES_LIST_KEY, scores);
        return tag;
    }

    private static CompoundTag rolledScoreTag(RolledScore score) {
        ListTag dice = new ListTag();
        for (DieRoll die : score.dice()) {
            CompoundTag dieTag = new CompoundTag();
            dieTag.putInt(SIDES_KEY, die.sides());
            dieTag.putInt(VALUE_KEY, die.value());
            dieTag.putBoolean(KEPT_KEY, die.kept());
            dice.add(dieTag);
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt(TOTAL_KEY, score.total());
        tag.put(DICE_KEY, dice);
        return tag;
    }

    private static CompoundTag buildTag(CharacterBuild build) {
        CompoundTag scores = new CompoundTag();
        build.scores().forEach((ability, score) -> scores.putInt(ability.id(), score));
        ListTag skills = idList(build.skills());
        CompoundTag tag = new CompoundTag();
        tag.putString(METHOD_KEY, build.method().id());
        tag.put(SCORES_KEY, scores);
        tag.put(SKILLS_KEY, skills);
        tag.put(CHOICES_KEY, choicesTag(build.choices()));
        tag.put(GRANTS_KEY, grantsTag(build.grants()));
        return tag;
    }

    private static CompoundTag choicesTag(PresetChoices choices) {
        CompoundTag tag = new CompoundTag();
        for (PresetKind kind : PresetKind.values()) {
            choices.get(kind).ifPresent(id -> tag.putString(kind.id(), id.toString()));
        }
        choices.size().ifPresent(size -> tag.putString(SIZE_KEY, size.id()));
        return tag;
    }

    private static CompoundTag grantsTag(PresetGrants grants) {
        CompoundTag bonuses = new CompoundTag();
        grants.bonuses().forEach((ability, bonus) -> bonuses.putInt(ability.id(), bonus));
        CompoundTag tag = new CompoundTag();
        tag.put(BONUSES_KEY, bonuses);
        tag.put(SKILLS_KEY, idList(grants.skills()));
        tag.put(SAVES_KEY, abilityList(grants.saves()));
        tag.put(TRAIT_SKILLS_KEY, idList(grants.traitSkills()));
        return tag;
    }

    private static ListTag idList(Set<ResourceLocation> ids) {
        ListTag list = new ListTag();
        ids.forEach(id -> list.add(StringTag.valueOf(id.toString())));
        return list;
    }

    private static ListTag abilityList(Set<Ability> abilities) {
        ListTag list = new ListTag();
        abilities.forEach(ability -> list.add(StringTag.valueOf(ability.id())));
        return list;
    }

    private static PlayerCharacter parseCharacter(CompoundTag tag) {
        Optional<CharacterRolls> rolls =
                tag.contains(ROLLS_KEY) ? Optional.of(parseRolls(tag.getCompound(ROLLS_KEY))) : Optional.empty();
        Optional<CharacterBuild> build =
                tag.contains(BUILD_KEY) ? Optional.of(parseBuild(tag.getCompound(BUILD_KEY))) : Optional.empty();
        return new PlayerCharacter(tag.getBoolean(PROMPTED_KEY), rolls, build);
    }

    private static CharacterRolls parseRolls(CompoundTag tag) {
        List<RolledScore> scores = tag.getList(SCORES_LIST_KEY, Tag.TAG_COMPOUND).stream()
                .map(score -> parseRolledScore((CompoundTag) score))
                .toList();
        if (scores.size() != Ability.values().length) {
            throw new IllegalArgumentException("expected " + Ability.values().length + " rolled scores");
        }
        return new CharacterRolls(parseMethod(tag), scores);
    }

    private static RolledScore parseRolledScore(CompoundTag tag) {
        List<DieRoll> dice = tag.getList(DICE_KEY, Tag.TAG_COMPOUND).stream()
                .map(die -> (CompoundTag) die)
                .map(die -> new DieRoll(die.getInt(SIDES_KEY), die.getInt(VALUE_KEY), die.getBoolean(KEPT_KEY)))
                .toList();
        return new RolledScore(tag.getInt(TOTAL_KEY), dice);
    }

    private static CharacterBuild parseBuild(CompoundTag tag) {
        requireKeys(tag, METHOD_KEY, SCORES_KEY, SKILLS_KEY, CHOICES_KEY, GRANTS_KEY);
        CompoundTag scoresTag = tag.getCompound(SCORES_KEY);
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            if (!scoresTag.contains(ability.id())) {
                throw new IllegalArgumentException("build has no " + ability.id() + " score");
            }
            scores.put(ability, Abilities.clamp(scoresTag.getInt(ability.id())));
        }
        return new CharacterBuild(
                parseMethod(tag),
                scores,
                parseIds(tag, SKILLS_KEY),
                parseChoices(tag.getCompound(CHOICES_KEY)),
                parseGrants(tag.getCompound(GRANTS_KEY)));
    }

    private static PresetChoices parseChoices(CompoundTag tag) {
        return new PresetChoices(
                parseChoice(tag, PresetKind.SPECIES),
                parseChoice(tag, PresetKind.BACKGROUND),
                parseChoice(tag, PresetKind.CLASS),
                Size.byId(tag.getString(SIZE_KEY)));
    }

    private static Optional<ResourceLocation> parseChoice(CompoundTag tag, PresetKind kind) {
        return tag.contains(kind.id())
                ? Optional.ofNullable(ResourceLocation.tryParse(tag.getString(kind.id())))
                : Optional.empty();
    }

    private static PresetGrants parseGrants(CompoundTag tag) {
        requireKeys(tag, BONUSES_KEY, SKILLS_KEY, SAVES_KEY, TRAIT_SKILLS_KEY);
        CompoundTag bonusesTag = tag.getCompound(BONUSES_KEY);
        Map<Ability, Integer> bonuses = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            if (bonusesTag.contains(ability.id())) {
                bonuses.put(ability, bonusesTag.getInt(ability.id()));
            }
        }
        Set<Ability> saves = EnumSet.noneOf(Ability.class);
        for (Tag save : tag.getList(SAVES_KEY, Tag.TAG_STRING)) {
            Ability.byId(save.getAsString()).ifPresent(saves::add);
        }
        return new PresetGrants(bonuses, parseIds(tag, SKILLS_KEY), saves, parseIds(tag, TRAIT_SKILLS_KEY));
    }

    private static void requireKeys(CompoundTag tag, String... keys) {
        for (String key : keys) {
            if (!tag.contains(key)) {
                throw new IllegalArgumentException("missing '" + key + "'");
            }
        }
    }

    private static Set<ResourceLocation> parseIds(CompoundTag tag, String key) {
        Set<ResourceLocation> ids = new HashSet<>();
        for (Tag id : tag.getList(key, Tag.TAG_STRING)) {
            Optional.ofNullable(ResourceLocation.tryParse(id.getAsString())).ifPresent(ids::add);
        }
        return ids;
    }

    private static CreationMethod parseMethod(CompoundTag tag) {
        String id = tag.getString(METHOD_KEY);
        return CreationMethod.byId(id)
                .orElseThrow(() -> new IllegalArgumentException("unknown creation method '" + id + "'"));
    }
}
