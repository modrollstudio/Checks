package studio.modroll.checks.score;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.creation.CharacterBuild;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.creation.PresetGrants;
import studio.modroll.checks.data.EntityScoreProfile;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.level.PlayerLevel;
import studio.modroll.checks.trait.TraitEffectType;

class ScoreResolverTest {

    private static final ScoresConfig CONFIG = ScoresConfig.DEFAULTS;
    private static final AttributeValues ZOMBIE_ATTRS = new AttributeValues(3, 0.23, 20);
    private static final Skill STEALTH = new Skill(ResourceLocation.parse("checks:stealth"), Ability.DEXTERITY);
    private static final Skill ATHLETICS = new Skill(ResourceLocation.parse("checks:athletics"), Ability.STRENGTH);
    private static final ScoresConfig NO_PROFILES = new ScoresConfig(
            CONFIG.playerDefaults(),
            CONFIG.derivation(),
            false,
            true,
            CONFIG.proficiency(),
            CONFIG.critfall(),
            true,
            CONFIG.creation(),
            CONFIG.levelling(),
            CONFIG.extensions(),
            CONFIG.body(),
            CONFIG.saves(),
            CONFIG.traits(),
            CONFIG.social(),
            CONFIG.rollMessages(),
            CONFIG.deathMessages(),
            CONFIG.exploration());

    @Test
    void mobUsesProfileWhenPresent() {
        assertEquals(
                18,
                ScoreResolver.resolveScore(
                        Ability.STRENGTH,
                        abilities("{\"str\": 18}"),
                        false,
                        OptionalInt.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        ZOMBIE_ATTRS,
                        CONFIG));
    }

    @Test
    void mobDerivesUnprofiledAbility() {
        // 8 + 1.0 * 3 = 11
        assertEquals(
                11,
                ScoreResolver.resolveScore(
                        Ability.STRENGTH,
                        abilities("{\"dex\": 12}"),
                        false,
                        OptionalInt.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        ZOMBIE_ATTRS,
                        CONFIG));
    }

    @Test
    void mobWithNoProfileDerives() {
        assertEquals(
                11,
                ScoreResolver.resolveScore(
                        Ability.STRENGTH,
                        Optional.empty(),
                        false,
                        OptionalInt.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        ZOMBIE_ATTRS,
                        CONFIG));
    }

    @Test
    void derivationDisabledFallsToMentalDefault() {
        ScoresConfig noDerive = new ScoresConfig(
                CONFIG.playerDefaults(),
                new ScoresConfig.Derivation(false, 8, 1, 6, 24, 8, 0.2, 10),
                true,
                true,
                CONFIG.proficiency(),
                CONFIG.critfall(),
                true,
                CONFIG.creation(),
                CONFIG.levelling(),
                CONFIG.extensions(),
                CONFIG.body(),
                CONFIG.saves(),
                CONFIG.traits(),
                CONFIG.social(),
                CONFIG.rollMessages(),
                CONFIG.deathMessages(),
                CONFIG.exploration());
        assertEquals(
                10,
                ScoreResolver.resolveScore(
                        Ability.STRENGTH,
                        Optional.empty(),
                        false,
                        OptionalInt.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        ZOMBIE_ATTRS,
                        noDerive));
    }

    @Test
    void profilesDisabledIgnoresProfile() {
        assertEquals(
                11,
                ScoreResolver.resolveScore(
                        Ability.STRENGTH,
                        abilities("{\"str\": 30}"),
                        false,
                        OptionalInt.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        ZOMBIE_ATTRS,
                        NO_PROFILES));
    }

    @Test
    void playerPrefersStoredThenProfileThenDefault() {
        assertEquals(
                16,
                ScoreResolver.resolveScore(
                        Ability.STRENGTH,
                        abilities("{\"str\": 13}"),
                        true,
                        OptionalInt.of(16),
                        Optional.empty(),
                        Optional.empty(),
                        ZOMBIE_ATTRS,
                        CONFIG));
        assertEquals(
                13,
                ScoreResolver.resolveScore(
                        Ability.STRENGTH,
                        abilities("{\"str\": 13}"),
                        true,
                        OptionalInt.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        ZOMBIE_ATTRS,
                        CONFIG));
        assertEquals(
                10,
                ScoreResolver.resolveScore(
                        Ability.STRENGTH,
                        Optional.empty(),
                        true,
                        OptionalInt.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        ZOMBIE_ATTRS,
                        CONFIG));
    }

    @Test
    void skillBonusPrefersStoredPlayerBonusThenProfileThenZero() {
        Optional<EntityScoreProfile> profiled = skillProfile(3);
        assertEquals(7, ScoreResolver.resolveSkillBonus(STEALTH, profiled, OptionalInt.of(7), CONFIG));
        assertEquals(3, ScoreResolver.resolveSkillBonus(STEALTH, profiled, OptionalInt.empty(), CONFIG));
        assertEquals(0, ScoreResolver.resolveSkillBonus(STEALTH, Optional.empty(), OptionalInt.empty(), CONFIG));
    }

    @Test
    void skillBonusIgnoresProfileWhenProfilesDisabled() {
        assertEquals(0, ScoreResolver.resolveSkillBonus(STEALTH, skillProfile(3), OptionalInt.empty(), NO_PROFILES));
        assertEquals(5, ScoreResolver.resolveSkillBonus(STEALTH, skillProfile(3), OptionalInt.of(5), NO_PROFILES));
    }

    @Test
    void skillsDisabledZeroesEveryBonus() {
        ScoresConfig noSkills = new ScoresConfig(
                CONFIG.playerDefaults(),
                CONFIG.derivation(),
                true,
                false,
                CONFIG.proficiency(),
                CONFIG.critfall(),
                true,
                CONFIG.creation(),
                CONFIG.levelling(),
                CONFIG.extensions(),
                CONFIG.body(),
                CONFIG.saves(),
                CONFIG.traits(),
                CONFIG.social(),
                CONFIG.rollMessages(),
                CONFIG.deathMessages(),
                CONFIG.exploration());
        assertEquals(0, ScoreResolver.resolveSkillBonus(STEALTH, skillProfile(3), OptionalInt.of(5), noSkills));
    }

    @Test
    void proficiencyBonusPrefersStoredPlayerBonusThenProfileThenConfigDefault() {
        Optional<EntityScoreProfile> profiled = proficiencyProfile("{\"bonus\": 4}");
        assertEquals(6, ScoreResolver.resolveProficiencyBonus(profiled, OptionalInt.of(6), Optional.empty(), CONFIG));
        assertEquals(4, ScoreResolver.resolveProficiencyBonus(profiled, OptionalInt.empty(), Optional.empty(), CONFIG));
        assertEquals(
                2,
                ScoreResolver.resolveProficiencyBonus(Optional.empty(), OptionalInt.empty(), Optional.empty(), CONFIG));
        assertEquals(
                5,
                ScoreResolver.resolveProficiencyBonus(
                        Optional.empty(), OptionalInt.empty(), Optional.empty(), withProficiency(true, 5)));
    }

    @Test
    void proficiencyBonusIgnoresProfileWhenProfilesDisabled() {
        Optional<EntityScoreProfile> profiled = proficiencyProfile("{\"bonus\": 4}");
        assertEquals(
                2, ScoreResolver.resolveProficiencyBonus(profiled, OptionalInt.empty(), Optional.empty(), NO_PROFILES));
        assertEquals(
                6, ScoreResolver.resolveProficiencyBonus(profiled, OptionalInt.of(6), Optional.empty(), NO_PROFILES));
    }

    @Test
    void skillProficiencyPrefersStoredPlayerLevelThenProfileThenNone() {
        Optional<EntityScoreProfile> profiled =
                proficiencyProfile("{\"skills\": {\"checks:stealth\": \"proficient\"}}");
        assertEquals(
                Proficiency.EXPERTISE,
                ScoreResolver.resolveSkillProficiency(
                        STEALTH, profiled, Optional.of(Proficiency.EXPERTISE), Optional.empty(), CONFIG));
        assertEquals(
                Proficiency.NONE,
                ScoreResolver.resolveSkillProficiency(
                        STEALTH, profiled, Optional.of(Proficiency.NONE), Optional.empty(), CONFIG));
        assertEquals(
                Proficiency.PROFICIENT,
                ScoreResolver.resolveSkillProficiency(STEALTH, profiled, Optional.empty(), Optional.empty(), CONFIG));
        assertEquals(
                Proficiency.NONE,
                ScoreResolver.resolveSkillProficiency(
                        STEALTH, Optional.empty(), Optional.empty(), Optional.empty(), CONFIG));
    }

    @Test
    void saveProficiencyPrefersStoredPlayerLevelThenProfileThenNone() {
        Optional<EntityScoreProfile> profiled = proficiencyProfile("{\"saves\": {\"con\": \"proficient\"}}");
        assertEquals(
                Proficiency.NONE,
                ScoreResolver.resolveSaveProficiency(
                        Ability.CONSTITUTION, profiled, Optional.of(Proficiency.NONE), Optional.empty(), CONFIG));
        assertEquals(
                Proficiency.PROFICIENT,
                ScoreResolver.resolveSaveProficiency(
                        Ability.CONSTITUTION, profiled, Optional.empty(), Optional.empty(), CONFIG));
        assertEquals(
                Proficiency.NONE,
                ScoreResolver.resolveSaveProficiency(
                        Ability.DEXTERITY, profiled, Optional.empty(), Optional.empty(), CONFIG));
    }

    @Test
    void proficiencyDisabledZeroesTheBonusAndClearsEveryLevel() {
        ScoresConfig off = withProficiency(false, 2);
        Optional<EntityScoreProfile> profiled = proficiencyProfile(
                "{\"bonus\": 4, \"skills\": {\"checks:stealth\": \"expertise\"}, \"saves\": {\"con\": \"proficient\"}}");
        assertEquals(0, ScoreResolver.resolveProficiencyBonus(profiled, OptionalInt.of(6), Optional.empty(), off));
        assertEquals(
                Proficiency.NONE,
                ScoreResolver.resolveSkillProficiency(
                        STEALTH, profiled, Optional.of(Proficiency.EXPERTISE), Optional.empty(), off));
        assertEquals(
                Proficiency.NONE,
                ScoreResolver.resolveSaveProficiency(
                        Ability.CONSTITUTION, profiled, Optional.empty(), Optional.empty(), off));
    }

    @Test
    void levelProficiencyReplacesTheDefaultForPlayers() {
        assertEquals(
                3, ScoreResolver.resolveProficiencyBonus(Optional.empty(), OptionalInt.empty(), atLevel(5), CONFIG));
        assertEquals(
                6, ScoreResolver.resolveProficiencyBonus(Optional.empty(), OptionalInt.empty(), atLevel(17), CONFIG));
    }

    @Test
    void levelProficiencyBeatsAPlayerProfile() {
        Optional<EntityScoreProfile> profiled = proficiencyProfile("{\"bonus\": 4}");
        assertEquals(6, ScoreResolver.resolveProficiencyBonus(profiled, OptionalInt.empty(), atLevel(17), CONFIG));
    }

    @Test
    void levellingOffRestoresThePlayerProfileBonus() {
        Optional<EntityScoreProfile> profiled = proficiencyProfile("{\"bonus\": 4}");
        assertEquals(
                4,
                ScoreResolver.resolveProficiencyBonus(
                        profiled, OptionalInt.empty(), atLevel(17), withLevelling(false)));
    }

    @Test
    void commandProficiencyBeatsTheLevel() {
        assertEquals(
                9, ScoreResolver.resolveProficiencyBonus(Optional.empty(), OptionalInt.of(9), atLevel(17), CONFIG));
    }

    @Test
    void mobProficiencyIgnoresLevelling() {
        Optional<EntityScoreProfile> profiled = proficiencyProfile("{\"bonus\": 4}");
        assertEquals(4, ScoreResolver.resolveProficiencyBonus(profiled, OptionalInt.empty(), Optional.empty(), CONFIG));
    }

    @Test
    void levellingOffRestoresTheFlatProficiencyBonus() {
        ScoresConfig off = withLevelling(false);
        assertEquals(2, ScoreResolver.resolveProficiencyBonus(Optional.empty(), OptionalInt.empty(), atLevel(17), off));
    }

    @Test
    void improvementsAddToAPlayersScoreBelowTheCommand() {
        Optional<PlayerLevel> improved =
                Optional.of(PlayerLevel.START.withLevelAndXp(4, 0).withImprovement(Map.of(Ability.STRENGTH, 2)));
        assertEquals(17, playerStrength(OptionalInt.empty(), Optional.of(character(15, Set.of())), improved, CONFIG));
        assertEquals(12, playerStrength(OptionalInt.empty(), Optional.empty(), improved, CONFIG));
        assertEquals(14, playerStrength(OptionalInt.of(14), Optional.empty(), improved, CONFIG));
        assertEquals(10, playerStrength(OptionalInt.empty(), Optional.empty(), improved, withLevelling(false)));
    }

    private static int playerStrength(
            OptionalInt stored, Optional<CharacterBuild> character, Optional<PlayerLevel> level, ScoresConfig config) {
        return ScoreResolver.resolveScore(
                Ability.STRENGTH, Optional.empty(), true, stored, character, level, ZOMBIE_ATTRS, config);
    }

    private static Optional<PlayerLevel> atLevel(int level) {
        return Optional.of(PlayerLevel.START.withLevelAndXp(level, 0));
    }

    private static ScoresConfig withLevelling(boolean enabled) {
        ScoresConfig.LevellingSettings live = CONFIG.levelling();
        return new ScoresConfig(
                CONFIG.playerDefaults(),
                CONFIG.derivation(),
                true,
                true,
                CONFIG.proficiency(),
                CONFIG.critfall(),
                true,
                CONFIG.creation(),
                new ScoresConfig.LevellingSettings(
                        enabled, live.xpThresholds(), live.proficiencyBonuses(), live.xpSources(), live.improvements()),
                CONFIG.extensions(),
                CONFIG.body(),
                CONFIG.saves(),
                CONFIG.traits(),
                CONFIG.social(),
                CONFIG.rollMessages(),
                CONFIG.deathMessages(),
                CONFIG.exploration());
    }

    private static ScoresConfig withProficiency(boolean enabled, int defaultBonus) {
        return new ScoresConfig(
                CONFIG.playerDefaults(),
                CONFIG.derivation(),
                true,
                true,
                new ScoresConfig.ProficiencySettings(enabled, defaultBonus),
                CONFIG.critfall(),
                true,
                CONFIG.creation(),
                CONFIG.levelling(),
                CONFIG.extensions(),
                CONFIG.body(),
                CONFIG.saves(),
                CONFIG.traits(),
                CONFIG.social(),
                CONFIG.rollMessages(),
                CONFIG.deathMessages(),
                CONFIG.exploration());
    }

    @Test
    void characterRanksBelowTheCommandAndAboveTheProfile() {
        Optional<CharacterBuild> character = Optional.of(character(15, Set.of()));
        assertEquals(
                17,
                ScoreResolver.resolveScore(
                        Ability.STRENGTH,
                        abilities("{\"str\": 13}"),
                        true,
                        OptionalInt.of(17),
                        character,
                        Optional.empty(),
                        ZOMBIE_ATTRS,
                        CONFIG));
        assertEquals(
                15,
                ScoreResolver.resolveScore(
                        Ability.STRENGTH,
                        abilities("{\"str\": 13}"),
                        true,
                        OptionalInt.empty(),
                        character,
                        Optional.empty(),
                        ZOMBIE_ATTRS,
                        CONFIG));
    }

    @Test
    void characterIsIgnoredWhileCreationIsDisabled() {
        Optional<CharacterBuild> character = Optional.of(character(15, Set.of(STEALTH.id())));
        ScoresConfig off = withCreation(false);
        assertEquals(
                13,
                ScoreResolver.resolveScore(
                        Ability.STRENGTH,
                        abilities("{\"str\": 13}"),
                        true,
                        OptionalInt.empty(),
                        character,
                        Optional.empty(),
                        ZOMBIE_ATTRS,
                        off));
        assertEquals(
                Proficiency.NONE,
                ScoreResolver.resolveSkillProficiency(STEALTH, Optional.empty(), Optional.empty(), character, off));
    }

    @Test
    void pickedSkillsAreProficientBelowTheCommandLevel() {
        Optional<CharacterBuild> character = Optional.of(character(10, Set.of(STEALTH.id())));
        assertEquals(
                Proficiency.PROFICIENT,
                ScoreResolver.resolveSkillProficiency(STEALTH, Optional.empty(), Optional.empty(), character, CONFIG));
        assertEquals(
                Proficiency.EXPERTISE,
                ScoreResolver.resolveSkillProficiency(
                        STEALTH, Optional.empty(), Optional.of(Proficiency.EXPERTISE), character, CONFIG));
    }

    private static CharacterBuild character(int strength, Set<ResourceLocation> skills) {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            scores.put(ability, ability == Ability.STRENGTH ? strength : 10);
        }
        return new CharacterBuild(
                CreationMethod.STANDARD_ARRAY, scores, skills, PresetChoices.CUSTOM, PresetGrants.NONE);
    }

    /** A Soldier-Fighter: base STR 15 with +2, Athletics from the background, STR and CON saves from the class. */
    private static Optional<CharacterBuild> presetCharacter() {
        CharacterBuild base = character(15, Set.of());
        return Optional.of(new CharacterBuild(
                base.method(),
                base.scores(),
                Set.of(),
                new PresetChoices(
                        Optional.empty(),
                        Optional.of(ResourceLocation.parse("checks:soldier")),
                        Optional.of(ResourceLocation.parse("checks:fighter")),
                        Optional.empty()),
                new PresetGrants(
                        Map.of(Ability.STRENGTH, 2),
                        Set.of(ATHLETICS.id()),
                        Set.of(Ability.STRENGTH, Ability.CONSTITUTION),
                        Set.of(STEALTH.id()))));
    }

    @Test
    void presetGrantsAddTheBonusTheBackgroundSkillsAndTheClassSaves() {
        Optional<CharacterBuild> character = presetCharacter();
        assertEquals(17, playerStrength(character, CONFIG));
        assertEquals(
                Proficiency.PROFICIENT,
                ScoreResolver.resolveSkillProficiency(
                        ATHLETICS, Optional.empty(), Optional.empty(), character, CONFIG));
        assertEquals(
                Proficiency.PROFICIENT,
                ScoreResolver.resolveSaveProficiency(
                        Ability.CONSTITUTION, Optional.empty(), Optional.empty(), character, CONFIG));
        assertEquals(
                Proficiency.NONE,
                ScoreResolver.resolveSaveProficiency(
                        Ability.WISDOM, Optional.empty(), Optional.empty(), character, CONFIG));
    }

    @Test
    void classSavesRankBelowTheCommandLevelAndAboveTheProfile() {
        Optional<EntityScoreProfile> profiled = proficiencyProfile("{\"saves\": {\"wis\": \"proficient\"}}");
        assertEquals(
                Proficiency.NONE,
                ScoreResolver.resolveSaveProficiency(
                        Ability.STRENGTH, profiled, Optional.of(Proficiency.NONE), presetCharacter(), CONFIG));
        assertEquals(
                Proficiency.PROFICIENT,
                ScoreResolver.resolveSaveProficiency(
                        Ability.STRENGTH, profiled, Optional.empty(), presetCharacter(), CONFIG));
    }

    @Test
    void presetGrantsAreIgnoredWhilePresetsAreOff() {
        Optional<CharacterBuild> character = presetCharacter();
        ScoresConfig off = withPresets(false);
        assertEquals(15, playerStrength(character, off));
        assertEquals(
                Proficiency.NONE,
                ScoreResolver.resolveSkillProficiency(ATHLETICS, Optional.empty(), Optional.empty(), character, off));
        assertEquals(
                Proficiency.NONE,
                ScoreResolver.resolveSaveProficiency(
                        Ability.CONSTITUTION, Optional.empty(), Optional.empty(), character, off));
    }

    @Test
    void traitSkillPicksCountOnlyWhileTraitSkillChoicesAreOn() {
        Optional<CharacterBuild> character = presetCharacter();
        assertEquals(
                Proficiency.PROFICIENT,
                ScoreResolver.resolveSkillProficiency(STEALTH, Optional.empty(), Optional.empty(), character, CONFIG));
        Map<TraitEffectType, Boolean> effects = new EnumMap<>(CONFIG.traits().effects());
        effects.put(TraitEffectType.SKILL_CHOICES, false);
        ScoresConfig off = withTraits(new ScoresConfig.TraitSettings(
                true, CONFIG.traits().rechargeTicks(), CONFIG.traits().darkvisionStrength(), effects));
        assertEquals(
                Proficiency.NONE,
                ScoreResolver.resolveSkillProficiency(STEALTH, Optional.empty(), Optional.empty(), character, off));
        assertEquals(
                Proficiency.PROFICIENT,
                ScoreResolver.resolveSkillProficiency(ATHLETICS, Optional.empty(), Optional.empty(), character, off));
    }

    private static ScoresConfig withTraits(ScoresConfig.TraitSettings traits) {
        return new ScoresConfig(
                CONFIG.playerDefaults(),
                CONFIG.derivation(),
                true,
                true,
                CONFIG.proficiency(),
                CONFIG.critfall(),
                true,
                CONFIG.creation(),
                CONFIG.levelling(),
                CONFIG.extensions(),
                CONFIG.body(),
                CONFIG.saves(),
                traits,
                CONFIG.social(),
                CONFIG.rollMessages(),
                CONFIG.deathMessages(),
                CONFIG.exploration());
    }

    private static int playerStrength(Optional<CharacterBuild> character, ScoresConfig config) {
        return ScoreResolver.resolveScore(
                Ability.STRENGTH,
                Optional.empty(),
                true,
                OptionalInt.empty(),
                character,
                Optional.empty(),
                ZOMBIE_ATTRS,
                config);
    }

    private static ScoresConfig withPresets(boolean enabled) {
        ScoresConfig.CreationSettings live = CONFIG.creation();
        ScoresConfig.PresetSettings presets = live.presets();
        return withCreationSettings(new ScoresConfig.CreationSettings(
                live.enabled(),
                live.methods(),
                live.skillChoices(),
                live.standardArray(),
                live.pointBuy(),
                live.rollDice(),
                live.hardcoreDice(),
                new ScoresConfig.PresetSettings(
                        enabled, presets.includeShipped(), presets.bonusOptions(), presets.bonusMaxScore())));
    }

    private static ScoresConfig withCreation(boolean enabled) {
        ScoresConfig.CreationSettings live = CONFIG.creation();
        return withCreationSettings(new ScoresConfig.CreationSettings(
                enabled,
                live.methods(),
                live.skillChoices(),
                live.standardArray(),
                live.pointBuy(),
                live.rollDice(),
                live.hardcoreDice(),
                live.presets()));
    }

    private static ScoresConfig withCreationSettings(ScoresConfig.CreationSettings creation) {
        return new ScoresConfig(
                CONFIG.playerDefaults(),
                CONFIG.derivation(),
                true,
                true,
                CONFIG.proficiency(),
                CONFIG.critfall(),
                true,
                creation,
                CONFIG.levelling(),
                CONFIG.extensions(),
                CONFIG.body(),
                CONFIG.saves(),
                CONFIG.traits(),
                CONFIG.social(),
                CONFIG.rollMessages(),
                CONFIG.deathMessages(),
                CONFIG.exploration());
    }

    private static Optional<EntityScoreProfile> abilities(String abilities) {
        return profile("\"abilities\": " + abilities);
    }

    private static Optional<EntityScoreProfile> proficiencyProfile(String proficiency) {
        return profile("\"proficiency\": " + proficiency);
    }

    private static Optional<EntityScoreProfile> skillProfile(int stealthBonus) {
        return profile("\"skills\": {\"checks:stealth\": " + stealthBonus + "}");
    }

    private static Optional<EntityScoreProfile> profile(String body) {
        return Optional.of(EntityScoreProfile.parse(
                ResourceLocation.parse("pack:test"),
                JsonParser.parseString("{\"matches\": [\"minecraft:zombie\"], " + body + "}")
                        .getAsJsonObject(),
                id -> Optional.of(STEALTH).filter(skill -> skill.id().equals(id)),
                w -> {}));
    }
}
