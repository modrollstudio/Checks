package studio.modroll.checks.score;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.creation.CharacterBuild;
import studio.modroll.checks.creation.PresetGrants;
import studio.modroll.checks.data.EntityScoreProfile;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.level.LevelRules;
import studio.modroll.checks.level.PlayerLevel;
import studio.modroll.checks.proficiency.Proficiencies;
import studio.modroll.checks.skill.Skills;
import studio.modroll.checks.trait.TraitEffectType;

/**
 * The precedence rules, free of Minecraft entities so they are unit-testable. Every value prefers a
 * persisted player value, then a datapack profile, then a fallback: the config default for player
 * scores and the proficiency bonus, attribute derivation for other entities' scores, 0 for skill
 * bonuses and none for proficiency levels. A confirmed character slots in between the command value
 * and the profile for scores and skill and save proficiency, while character creation is enabled; what
 * its presets grant (background bonuses and skills, class saves) counts only while presets are on too.
 * While levelling is enabled, a player's chosen ability score improvements add to their score below the
 * command value, and their level's proficiency bonus replaces the profile and config default. Stored
 * player values, characters and levels are empty for non-players.
 */
public final class ScoreResolver {

    private ScoreResolver() {}

    public static int resolveScore(
            Ability ability,
            Optional<EntityScoreProfile> profile,
            boolean isPlayer,
            OptionalInt storedPlayerScore,
            Optional<CharacterBuild> character,
            Optional<PlayerLevel> level,
            AttributeValues attrs,
            ScoresConfig config) {
        OptionalInt profileScore = fromProfile(profile, config, p -> p.score(ability));
        if (isPlayer) {
            int base = activeCharacter(character, config)
                    .map(build ->
                            build.score(ability) + activeGrants(build, config).bonus(ability))
                    .orElseGet(() -> profileScore.orElse(config.playerDefault(ability)));
            int improved = base
                    + activeLevel(level, config)
                            .map(stored -> stored.improvementBonus(ability))
                            .orElse(0);
            return Abilities.clamp(storedPlayerScore.orElse(improved));
        }
        if (profileScore.isPresent()) {
            return Abilities.clamp(profileScore.getAsInt());
        }
        if (config.derivation().enabled()) {
            return AbilityDerivation.derive(ability, config.derivation(), attrs);
        }
        return Abilities.clamp(config.derivation().defaultMentalScore());
    }

    public static int resolveSkillBonus(
            Skill skill, Optional<EntityScoreProfile> profile, OptionalInt storedPlayerBonus, ScoresConfig config) {
        if (!config.skillsEnabled()) {
            return 0;
        }
        return Skills.clampBonus(storedPlayerBonus.orElse(
                fromProfile(profile, config, p -> p.skillBonus(skill)).orElse(0)));
    }

    public static int resolveProficiencyBonus(
            Optional<EntityScoreProfile> profile,
            OptionalInt storedPlayerBonus,
            Optional<PlayerLevel> level,
            ScoresConfig config) {
        if (!config.proficiency().enabled()) {
            return 0;
        }
        if (storedPlayerBonus.isPresent()) {
            return Proficiencies.clampBonus(storedPlayerBonus.getAsInt());
        }
        return Proficiencies.clampBonus(activeLevel(level, config)
                .map(stored -> LevelRules.proficiencyBonus(stored.level(), config.levelling()))
                .orElseGet(() -> fromProfile(profile, config, EntityScoreProfile::proficiencyBonus)
                        .orElse(config.proficiency().defaultBonus())));
    }

    public static Proficiency resolveSkillProficiency(
            Skill skill,
            Optional<EntityScoreProfile> profile,
            Optional<Proficiency> storedPlayerLevel,
            Optional<CharacterBuild> character,
            ScoresConfig config) {
        Optional<Proficiency> characterLevel = activeCharacter(character, config)
                .filter(build -> build.skills().contains(skill.id())
                        || activeGrants(build, config).skills().contains(skill.id())
                        || activeTraitSkills(build, config).contains(skill.id()))
                .map(build -> Proficiency.PROFICIENT);
        return resolveLevel(
                profile, storedPlayerLevel.or(() -> characterLevel), config, p -> p.skillProficiency(skill));
    }

    public static Proficiency resolveSaveProficiency(
            Ability ability,
            Optional<EntityScoreProfile> profile,
            Optional<Proficiency> storedPlayerLevel,
            Optional<CharacterBuild> character,
            ScoresConfig config) {
        Optional<Proficiency> characterLevel = activeCharacter(character, config)
                .filter(build -> activeGrants(build, config).saves().contains(ability))
                .map(build -> Proficiency.PROFICIENT);
        return resolveLevel(
                profile, storedPlayerLevel.or(() -> characterLevel), config, p -> p.saveProficiency(ability));
    }

    private static Proficiency resolveLevel(
            Optional<EntityScoreProfile> profile,
            Optional<Proficiency> storedPlayerLevel,
            ScoresConfig config,
            Function<EntityScoreProfile, Optional<Proficiency>> level) {
        if (!config.proficiency().enabled()) {
            return Proficiency.NONE;
        }
        if (storedPlayerLevel.isPresent()) {
            return storedPlayerLevel.get();
        }
        return config.profilesEnabled() ? profile.flatMap(level).orElse(Proficiency.NONE) : Proficiency.NONE;
    }

    private static Optional<CharacterBuild> activeCharacter(Optional<CharacterBuild> character, ScoresConfig config) {
        return config.creation().enabled() ? character : Optional.empty();
    }

    private static Optional<PlayerLevel> activeLevel(Optional<PlayerLevel> level, ScoresConfig config) {
        return config.levelling().enabled() ? level : Optional.empty();
    }

    private static PresetGrants activeGrants(CharacterBuild build, ScoresConfig config) {
        return config.creation().presets().enabled() ? build.grants() : PresetGrants.NONE;
    }

    private static Set<ResourceLocation> activeTraitSkills(CharacterBuild build, ScoresConfig config) {
        return config.traits().active(TraitEffectType.SKILL_CHOICES)
                ? activeGrants(build, config).traitSkills()
                : Set.of();
    }

    private static OptionalInt fromProfile(
            Optional<EntityScoreProfile> profile,
            ScoresConfig config,
            Function<EntityScoreProfile, OptionalInt> value) {
        if (!config.profilesEnabled()) {
            return OptionalInt.empty();
        }
        return profile.map(value).orElse(OptionalInt.empty());
    }
}
