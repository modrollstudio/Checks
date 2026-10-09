package studio.modroll.checks.data;

import com.google.gson.JsonObject;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.proficiency.Proficiencies;
import studio.modroll.checks.skill.Skills;

/**
 * Datapack ability scores, skill bonuses and proficiencies for entity types, loaded from {@code
 * data/<ns>/checks/entity_profile/*.json}. Every value is optional; an absent one falls back on its own.
 */
public record EntityScoreProfile(
        ResourceLocation id,
        List<MatchEntry> matches,
        Map<Ability, Integer> abilities,
        Map<ResourceLocation, Integer> skillBonuses,
        int priority,
        OptionalInt proficiencyBonus,
        Map<ResourceLocation, Proficiency> skillProficiencies,
        Map<Ability, Proficiency> saveProficiencies) {

    public static final int FORMAT_VERSION = 1;

    public OptionalInt score(Ability ability) {
        return optionalInt(abilities.get(ability));
    }

    public OptionalInt skillBonus(Skill skill) {
        return optionalInt(skillBonuses.get(skill.id()));
    }

    public Optional<Proficiency> skillProficiency(Skill skill) {
        return Optional.ofNullable(skillProficiencies.get(skill.id()));
    }

    public Optional<Proficiency> saveProficiency(Ability ability) {
        return Optional.ofNullable(saveProficiencies.get(ability));
    }

    /** Skill keys resolve through {@code skills}; a key naming no loaded skill is skipped with a warning. */
    public static EntityScoreProfile parse(
            ResourceLocation id,
            JsonObject json,
            Function<ResourceLocation, Optional<Skill>> skills,
            Consumer<String> warn) {
        LenientJson j = new LenientJson(json, "entity_profile " + id, warn);
        j.checkFormatVersion(FORMAT_VERSION);

        List<MatchEntry> matches =
                j.stringList("matches").stream().map(MatchEntry::parse).toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("'matches' must list at least one entity id, #tag, or namespace:*");
        }

        Map<Ability, Integer> abilities = parseAbilities(j.object("abilities"));
        Map<ResourceLocation, Integer> skillBonuses = parseSkillBonuses(j.object("skills"), skills);
        int priority = j.getInt("priority", 0);
        LenientJson proficiency = j.object("proficiency");
        OptionalInt proficiencyBonus = parseProficiencyBonus(proficiency);
        Map<ResourceLocation, Proficiency> skillProficiencies =
                parseSkillProficiencies(proficiency.object("skills"), skills);
        Map<Ability, Proficiency> saveProficiencies = parseSaveProficiencies(proficiency.object("saves"));
        j.finish();
        return new EntityScoreProfile(
                id,
                matches,
                Map.copyOf(abilities),
                Map.copyOf(skillBonuses),
                priority,
                proficiencyBonus,
                Map.copyOf(skillProficiencies),
                Map.copyOf(saveProficiencies));
    }

    private static Map<Ability, Integer> parseAbilities(LenientJson abilitiesJson) {
        Map<Ability, Integer> parsed = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            if (!abilitiesJson.has(ability.id())) {
                continue;
            }
            parsed.put(
                    ability,
                    requireInt(abilitiesJson, ability.id(), ability.id(), Abilities.MIN_SCORE, Abilities.MAX_SCORE));
        }
        return parsed;
    }

    private static Map<ResourceLocation, Integer> parseSkillBonuses(
            LenientJson skillsJson, Function<ResourceLocation, Optional<Skill>> skills) {
        Map<ResourceLocation, Integer> parsed = new HashMap<>();
        for (String key : skillsJson.keys()) {
            int bonus = requireInt(skillsJson, key, key, Skills.MIN_BONUS, Skills.MAX_BONUS);
            knownSkill(skillsJson, key, skills).ifPresent(skill -> parsed.put(skill.id(), bonus));
        }
        return parsed;
    }

    private static OptionalInt parseProficiencyBonus(LenientJson proficiencyJson) {
        if (!proficiencyJson.has("bonus")) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(requireInt(
                proficiencyJson, "bonus", "proficiency.bonus", Proficiencies.MIN_BONUS, Proficiencies.MAX_BONUS));
    }

    private static Map<ResourceLocation, Proficiency> parseSkillProficiencies(
            LenientJson skillsJson, Function<ResourceLocation, Optional<Skill>> skills) {
        Map<ResourceLocation, Proficiency> parsed = new HashMap<>();
        for (String key : skillsJson.keys()) {
            Proficiency proficiency = requireProficiency(skillsJson, key);
            knownSkill(skillsJson, key, skills).ifPresent(skill -> parsed.put(skill.id(), proficiency));
        }
        return parsed;
    }

    private static Map<Ability, Proficiency> parseSaveProficiencies(LenientJson savesJson) {
        Map<Ability, Proficiency> parsed = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            if (!savesJson.has(ability.id())) {
                continue;
            }
            Proficiency proficiency = requireProficiency(savesJson, ability.id());
            if (!Proficiencies.allowedOnSave(proficiency)) {
                throw new IllegalArgumentException(
                        "save '" + ability.id() + "' must be none or proficient (no expertise on saves)");
            }
            parsed.put(ability, proficiency);
        }
        return parsed;
    }

    private static Optional<Skill> knownSkill(
            LenientJson json, String key, Function<ResourceLocation, Optional<Skill>> skills) {
        Optional<Skill> skill =
                Optional.ofNullable(ResourceLocation.tryParse(key)).flatMap(skills);
        if (skill.isEmpty()) {
            json.warn("unknown skill '" + key + "' (skipped)");
        }
        return skill;
    }

    private static Proficiency requireProficiency(LenientJson json, String key) {
        return json.optionalString(key)
                .flatMap(Proficiency::byId)
                .orElseThrow(() -> new IllegalArgumentException("'" + key + "' must be none, proficient or expertise"));
    }

    private static int requireInt(LenientJson json, String key, String label, int min, int max) {
        int value = json.optionalInt(key)
                .orElseThrow(() -> new IllegalArgumentException("'" + key + "' must be an integer"));
        if (value < min || value > max) {
            throw new IllegalArgumentException("'" + label + "' must be between " + min + " and " + max);
        }
        return value;
    }

    private static OptionalInt optionalInt(Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }
}
