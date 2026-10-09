package studio.modroll.checks.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import studio.modroll.checks.Checks;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.body.Extra;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.PointBuy;
import studio.modroll.checks.level.Levels;
import studio.modroll.checks.proficiency.Proficiencies;
import studio.modroll.checks.save.VanillaSave;
import studio.modroll.checks.trait.TraitEffectType;
import studio.modroll.critfall.api.dice.DiceExpression;
import studio.modroll.critfall.api.dice.DiceParseException;

/**
 * Reads {@code config/checks/scores.json}, creating it with the defaults when missing. A bad value
 * falls back to its default with a warning; an unreadable file means all defaults.
 */
public final class ScoresConfigLoader {

    public static final String DEFAULT_FILE = """
            {
              "format_version": 1,
              "player_defaults": { "str": 10, "dex": 10, "con": 10, "int": 10, "wis": 10, "cha": 10 },
              "derivation": {
                "enabled": true,
                "strength_base": 8,
                "strength_per_attack_damage": 1.0,
                "dexterity_base": 6,
                "dexterity_per_speed": 24.0,
                "constitution_base": 8,
                "constitution_per_health": 0.2,
                "default_mental_score": 10
              },
              "profiles": { "enabled": true },
              "skills": { "enabled": true },
              "proficiency": { "enabled": true, "default_bonus": 2 },
              "critfall": {
                "enabled": true,
                "unprofiled_mobs": false,
                "save_abilities": { "critfall:spell": "dex" }
              },
              "stat_screen": { "enabled": true },
              "creation": {
                "enabled": true,
                "methods": ["standard_array", "point_buy", "roll", "hardcore"],
                "skill_choices": 2,
                "standard_array": [15, 14, 13, 12, 10, 8],
                "point_buy": {
                  "budget": 27,
                  "costs": { "8": 0, "9": 1, "10": 2, "11": 3, "12": 4, "13": 5, "14": 7, "15": 9 }
                },
                "roll_dice": "4d6kh3",
                "hardcore_dice": "3d6",
                "presets": {
                  "enabled": true,
                  "include_shipped": true,
                  "bonus_options": [[2, 1], [1, 1, 1]],
                  "bonus_max_score": 20
                }
              },
              "levelling": {
                "enabled": true,
                "xp_thresholds": [
                  0, 30, 90, 270, 650, 1400, 2300, 3400, 4800, 6400,
                  8500, 10000, 12000, 14000, 16500, 19500, 22500, 26500, 30500, 35500
                ],
                "proficiency_bonus": [2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6],
                "vanilla_xp": { "enabled": true, "xp_per_point": 1 },
                "advancements": { "enabled": true, "xp": 25 },
                "ability_improvements": {
                  "levels": [4, 8, 12, 16, 19],
                  "options": [[2], [1, 1]],
                  "max_score": 20
                }
              },
              "events": { "enabled": true },
              "bonus_sources": { "enabled": true, "passive_advantage": 5 },
              "triggers": { "enabled": true },
              "check_command": { "enabled": true },
              "stat_screen_sections": { "enabled": true },
              "health": {
                "enabled": true,
                "per_con_point": 2,
                "per_level": { "enabled": false, "multiplier": 0.2 }
              },
              "armor_class": { "enabled": true, "medium_max_dex": 2 },
              "size": { "enabled": true },
              "attribute_extras": {
                "knockback": { "enabled": true, "per_point": 0.1 },
                "mining_speed": { "enabled": true, "per_point": 0.05 },
                "bow_draw": { "enabled": true, "per_point": 0.1 },
                "crossbow_reload": { "enabled": true, "per_point": 0.1 },
                "breath": { "enabled": true, "per_point": 0.2 },
                "exhaustion": { "enabled": true, "per_point": 0.05 }
              },
              "vanilla_saves": {
                "profiled_mobs": false,
                "cooldown_ticks": 20,
                "explosion": { "enabled": true, "dc": 13, "success_multiplier": 0.5 },
                "poison": { "enabled": true, "dc": 12, "success_multiplier": 0.5 },
                "knockback": { "enabled": true, "dc": 13, "success_multiplier": 0.5, "min_strength": 0.7 },
                "fire": { "enabled": true, "dc": 12, "success_multiplier": 0.0 },
                "darkness": { "enabled": true, "dc": 13, "success_multiplier": 0.5 }
              },
              "traits": {
                "enabled": true,
                "recharge_ticks": 24000,
                "vanilla_save_advantage": { "enabled": true },
                "roll_bonus": { "enabled": true },
                "damage_resistance": { "enabled": true },
                "extra_health": { "enabled": true },
                "darkvision": { "enabled": true, "strength": 0.4 },
                "reroll": { "enabled": true },
                "last_stand": { "enabled": true },
                "ignored_by": { "enabled": true },
                "skill_choices": { "enabled": true },
                "attribute": { "enabled": true }
              },
              "social": {
                "enabled": true,
                "nearby_radius": 16,
                "gossip_radius": 32,
                "persuade": {
                  "enabled": true,
                  "dc": 12,
                  "cooldown_ticks": 24000,
                  "barely_margin": 2,
                  "critical_failure": {
                    "price_percent": 30,
                    "price_ticks": 48000,
                    "gossip": { "type": "minor_negative", "amount": 25, "nearby": true }
                  },
                  "failure": { "price_percent": 15, "price_ticks": 12000 },
                  "barely": { "price_percent": -10, "price_ticks": 12000 },
                  "success": { "price_percent": -20, "price_ticks": 12000 },
                  "critical_success": {
                    "price_percent": -35,
                    "price_ticks": 24000,
                    "gossip": { "type": "minor_positive", "amount": 10 }
                  }
                },
                "deceive": {
                  "enabled": true,
                  "cooldown_ticks": 24000,
                  "barely_margin": 2,
                  "critical_failure": {
                    "price_percent": 30,
                    "price_ticks": 48000,
                    "gossip": { "type": "minor_negative", "amount": 25, "nearby": true }
                  },
                  "failure": { "price_percent": 15, "price_ticks": 12000 },
                  "barely": { "price_percent": -10, "price_ticks": 12000 },
                  "success": { "price_percent": -20, "price_ticks": 12000 },
                  "critical_success": {
                    "price_percent": -35,
                    "price_ticks": 24000,
                    "gossip": { "type": "minor_positive", "amount": 10 }
                  },
                  "piglin": { "disguise_ticks": 2400, "critical_disguise_ticks": 6000, "anger_ticks": 600 },
                  "wandering_trader": { "refuse_ticks": 12000, "llamas_spit": true }
                },
                "intimidate": {
                  "enabled": true,
                  "dc": 13,
                  "cooldown_ticks": 72000,
                  "success": {
                    "price_percent": 15,
                    "price_ticks": 24000,
                    "gossip": { "type": "minor_negative", "amount": 20 }
                  },
                  "failure": {
                    "refuse_ticks": 48000,
                    "angers_golems": true,
                    "gossip": { "type": "major_negative", "amount": 19, "nearby": true }
                  },
                  "piglin_anger_ticks": 600
                },
                "pickpocket": {
                  "enabled": true,
                  "cooldown_ticks": 72000,
                  "behind_degrees": 60,
                  "failure": {
                    "refuse_ticks": 48000,
                    "angers_golems": true,
                    "gossip": { "type": "major_negative", "amount": 19, "nearby": true }
                  }
                },
                "passive_prices": {
                  "enabled": true,
                  "charisma_percent_per_point": 2,
                  "professions": {
                    "minecraft:cleric": { "skill": "checks:religion", "percent_per_point": 2 },
                    "minecraft:librarian": { "skill": "checks:history", "percent_per_point": 2 },
                    "minecraft:cartographer": { "skill": "checks:history", "percent_per_point": 2 }
                  }
                },
                "reactions": {
                  "enabled": true,
                  "flee_distance": 12,
                  "flee_ticks": 100,
                  "flee_speed": 0.75
                },
                "witnesses": { "enabled": true, "refuse_fraction": 0.5, "flee_distance": 6 },
                "golem_alarm": { "enabled": true, "radius": 48, "duration_ticks": 2400 },
                "on_guard": {
                  "enabled": true,
                  "plead": { "enabled": true, "dc": 15 },
                  "lie": { "enabled": true, "dc": 15 }
                },
                "wary": { "enabled": true, "max_reputation": -95 },
                "feel": {
                  "radius": 32,
                  "pickpocket_animation": { "enabled": true },
                  "speech_bubbles": { "enabled": true, "duration_ticks": 200 },
                  "voices": { "enabled": true }
                },
                "intimidate_mob": {
                  "enabled": true,
                  "dc": 12,
                  "cooldown_ticks": 1200,
                  "max_health": 20,
                  "exclude_tag": "checks:intimidation_immune",
                  "flee_ticks": 200,
                  "flee_distance": 16,
                  "flee_speed": 1.25,
                  "speed_boost_ticks": 100,
                  "speed_boost_amplifier": 1
                },
                "calm": { "enabled": true, "dc": 13, "cooldown_ticks": 1200, "tag": "checks:calmable" },
                "insight": {
                  "sense_hostility": { "enabled": true, "dc": 12, "range": 16 },
                  "creeper_warning": { "enabled": true, "dc": 12, "range": 16 }
                },
                "performance": {
                  "enabled": true,
                  "dc": 12,
                  "cooldown_ticks": 12000,
                  "range": 16,
                  "success": { "gossip": { "type": "minor_positive", "amount": 5 }, "piglin_ticks": 1200 },
                  "critical_success": { "gossip": { "type": "minor_positive", "amount": 10 }, "piglin_ticks": 3600 },
                  "critical_failure": { "gossip": { "type": "minor_negative", "amount": 5 } }
                }
              },
              "roll_messages": { "enabled": true, "duration_ticks": 240 },
              "death_messages": { "enabled": true, "window_ticks": 1200 },
              "exploration": {
                "leap": { "enabled": true, "dc": 12, "cooldown_ticks": 40, "boost": 0.15, "min_drop": 3 },
                "landing": { "enabled": true, "base_dc": 10, "dc_per_damage": 0.5, "success_multiplier": 0.5 },
                "cobwebs": { "enabled": true, "dc": 12, "retry_ticks": 60 },
                "climbing": { "enabled": true, "per_point": 0.1, "max_bonus": 0.5 },
                "sneak": { "enabled": true, "per_point": 0.05, "min_visibility": 0.25 },
                "spot_tripwires": { "enabled": true, "dc": 13, "range": 8, "interval_ticks": 20 },
                "disarm_tripwires": { "enabled": true, "dc": 13 },
                "search_chests": { "enabled": true, "dc": 13 },
                "monster_lore": { "enabled": true, "dc": 12, "range": 16, "retry_ticks": 6000, "interval_ticks": 20 },
                "structure_lore": { "enabled": true, "dc": 12, "retry_ticks": 6000, "interval_ticks": 20 },
                "taming": { "enabled": true, "dc": 12 },
                "hunger": { "enabled": true, "per_point": 0.05, "max_reduction": 0.25 },
                "ailments": { "enabled": true, "per_point": 0.05, "max_reduction": 0.25 }
              }
            }
            """;

    private ScoresConfigLoader() {}

    public static ScoresConfig load(Path file) {
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, DEFAULT_FILE);
                Checks.LOG.info("Wrote default scores config to {}", file);
                return ScoresConfig.DEFAULTS;
            }
            JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            return parse(json, Checks.LOG::warn);
        } catch (IOException | JsonParseException | IllegalStateException e) {
            Checks.LOG.error("Could not read {} — using default scores config: {}", file, e.toString());
            return ScoresConfig.DEFAULTS;
        }
    }

    public static ScoresConfig parse(JsonObject json, Consumer<String> warn) {
        LenientJson j = new LenientJson(json, "scores.json", warn);
        j.checkFormatVersion(ScoresConfig.FORMAT_VERSION);

        Map<Ability, Integer> playerDefaults = parsePlayerDefaults(j.object("player_defaults"), warn);

        LenientJson d = j.object("derivation");
        ScoresConfig.Derivation defaults = ScoresConfig.DEFAULTS.derivation();
        ScoresConfig.Derivation derivationConfig = new ScoresConfig.Derivation(
                d.getBool("enabled", defaults.enabled()),
                d.getDouble("strength_base", defaults.strengthBase()),
                d.getDouble("strength_per_attack_damage", defaults.strengthPerAttackDamage()),
                d.getDouble("dexterity_base", defaults.dexterityBase()),
                d.getDouble("dexterity_per_speed", defaults.dexterityPerSpeed()),
                d.getDouble("constitution_base", defaults.constitutionBase()),
                d.getDouble("constitution_per_health", defaults.constitutionPerHealth()),
                clampScore(
                        d.getInt("default_mental_score", defaults.defaultMentalScore()), "default_mental_score", warn));

        boolean profilesEnabled = j.object("profiles").getBool("enabled", ScoresConfig.DEFAULTS.profilesEnabled());
        boolean skillsEnabled = j.object("skills").getBool("enabled", ScoresConfig.DEFAULTS.skillsEnabled());
        ScoresConfig.ProficiencySettings proficiency = parseProficiency(j.object("proficiency"), warn);
        ScoresConfig.CritfallSettings critfall = parseCritfall(j.object("critfall"));
        boolean statScreenEnabled =
                j.object("stat_screen").getBool("enabled", ScoresConfig.DEFAULTS.statScreenEnabled());
        ScoresConfig.CreationSettings creation = parseCreation(j.object("creation"));
        ScoresConfig.LevellingSettings levelling = parseLevelling(j.object("levelling"));
        ScoresConfig.ExtensionSettings extensions = parseExtensions(j);
        ScoresConfig.BodySettings body = parseBody(j);
        ScoresConfig.SaveSettings saves = parseSaves(j.object("vanilla_saves"));
        ScoresConfig.TraitSettings traits = parseTraits(j.object("traits"));
        ScoresConfig.SocialSettings social = SocialConfigParser.parse(j.object("social"));
        ScoresConfig.RollMessageSettings rollMessages = parseRollMessages(j.object("roll_messages"));
        ScoresConfig.DeathMessageSettings deathMessages = parseDeathMessages(j.object("death_messages"));
        ScoresConfig.ExplorationSettings exploration = ExplorationConfigParser.parse(j.object("exploration"));

        j.finish();
        return new ScoresConfig(
                playerDefaults,
                derivationConfig,
                profilesEnabled,
                skillsEnabled,
                proficiency,
                critfall,
                statScreenEnabled,
                creation,
                levelling,
                extensions,
                body,
                saves,
                traits,
                social,
                rollMessages,
                deathMessages,
                exploration);
    }

    private static ScoresConfig.DeathMessageSettings parseDeathMessages(LenientJson json) {
        ScoresConfig.DeathMessageSettings defaults = ScoresConfig.DEFAULTS.deathMessages();
        return new ScoresConfig.DeathMessageSettings(
                json.getBool("enabled", defaults.enabled()), nonNegative(json, "window_ticks", defaults.windowTicks()));
    }

    private static ScoresConfig.RollMessageSettings parseRollMessages(LenientJson json) {
        ScoresConfig.RollMessageSettings defaults = ScoresConfig.DEFAULTS.rollMessages();
        return new ScoresConfig.RollMessageSettings(
                json.getBool("enabled", defaults.enabled()),
                atLeastOne(json, "duration_ticks", defaults.durationTicks()));
    }

    static int atLeastOne(LenientJson json, String key, int defaultValue) {
        int value = json.getInt(key, defaultValue);
        if (value < 1) {
            json.warn("'" + key + "' " + value + " must be at least 1, using " + defaultValue);
            return defaultValue;
        }
        return value;
    }

    private static ScoresConfig.TraitSettings parseTraits(LenientJson json) {
        ScoresConfig.TraitSettings defaults = ScoresConfig.DEFAULTS.traits();
        Map<TraitEffectType, Boolean> effects = new EnumMap<>(TraitEffectType.class);
        double darkvisionStrength = defaults.darkvisionStrength();
        for (TraitEffectType type : TraitEffectType.values()) {
            LenientJson effect = json.object(type.id());
            effects.put(type, effect.getBool("enabled", defaults.effects().get(type)));
            if (type == TraitEffectType.DARKVISION) {
                darkvisionStrength = fraction(effect, "strength", defaults.darkvisionStrength());
            }
        }
        return new ScoresConfig.TraitSettings(
                json.getBool("enabled", defaults.enabled()),
                nonNegative(json, "recharge_ticks", defaults.rechargeTicks()),
                darkvisionStrength,
                effects);
    }

    static double fraction(LenientJson json, String key, double defaultValue) {
        double value = json.getDouble(key, defaultValue);
        if (value < 0 || value > 1) {
            json.warn("'" + key + "' " + value + " is outside 0..1, using " + defaultValue);
            return defaultValue;
        }
        return value;
    }

    private static ScoresConfig.SaveSettings parseSaves(LenientJson json) {
        ScoresConfig.SaveSettings defaults = ScoresConfig.DEFAULTS.saves();
        Map<VanillaSave, ScoresConfig.SaveSetting> saves = new EnumMap<>(VanillaSave.class);
        double knockbackMinStrength = defaults.knockbackMinStrength();
        for (VanillaSave save : VanillaSave.values()) {
            LenientJson setting = json.object(save.id());
            saves.put(save, parseSave(setting, defaults.save(save)));
            if (save == VanillaSave.KNOCKBACK) {
                knockbackMinStrength = nonNegative(setting, "min_strength", defaults.knockbackMinStrength());
            }
        }
        return new ScoresConfig.SaveSettings(
                json.getBool("profiled_mobs", defaults.profiledMobs()),
                nonNegative(json, "cooldown_ticks", defaults.cooldownTicks()),
                knockbackMinStrength,
                saves);
    }

    private static ScoresConfig.SaveSetting parseSave(LenientJson json, ScoresConfig.SaveSetting defaults) {
        return new ScoresConfig.SaveSetting(
                json.getBool("enabled", defaults.enabled()),
                json.getInt("dc", defaults.dc()),
                nonNegative(json, "success_multiplier", defaults.successMultiplier()));
    }

    private static ScoresConfig.BodySettings parseBody(LenientJson json) {
        ScoresConfig.BodySettings defaults = ScoresConfig.DEFAULTS.body();
        return new ScoresConfig.BodySettings(
                parseHealth(json.object("health"), defaults.health()),
                parseArmorClass(json.object("armor_class"), defaults.armorClass()),
                enabled(json, "size", defaults.sizeEnabled()),
                parseExtras(json.object("attribute_extras"), defaults));
    }

    private static ScoresConfig.HealthSettings parseHealth(LenientJson json, ScoresConfig.HealthSettings defaults) {
        LenientJson perLevel = json.object("per_level");
        return new ScoresConfig.HealthSettings(
                json.getBool("enabled", defaults.enabled()),
                nonNegative(json, "per_con_point", defaults.perConPoint()),
                perLevel.getBool("enabled", defaults.perLevelEnabled()),
                nonNegative(perLevel, "multiplier", defaults.perLevelMultiplier()));
    }

    private static ScoresConfig.ArmorClassSettings parseArmorClass(
            LenientJson json, ScoresConfig.ArmorClassSettings defaults) {
        return new ScoresConfig.ArmorClassSettings(
                json.getBool("enabled", defaults.enabled()),
                nonNegative(json, "medium_max_dex", defaults.mediumMaxDex()));
    }

    private static Map<Extra, ScoresConfig.ExtraSetting> parseExtras(
            LenientJson json, ScoresConfig.BodySettings defaults) {
        Map<Extra, ScoresConfig.ExtraSetting> extras = new EnumMap<>(Extra.class);
        for (Extra extra : Extra.values()) {
            LenientJson setting = json.object(extra.id());
            ScoresConfig.ExtraSetting fallback = defaults.extra(extra);
            extras.put(
                    extra,
                    new ScoresConfig.ExtraSetting(
                            setting.getBool("enabled", fallback.enabled()),
                            nonNegative(setting, "per_point", fallback.perPoint())));
        }
        return extras;
    }

    static double nonNegative(LenientJson json, String key, double defaultValue) {
        double value = json.getDouble(key, defaultValue);
        if (value < 0) {
            json.warn("'" + key + "' " + value + " is negative, using " + defaultValue);
            return defaultValue;
        }
        return value;
    }

    private static ScoresConfig.ExtensionSettings parseExtensions(LenientJson json) {
        ScoresConfig.ExtensionSettings defaults = ScoresConfig.DEFAULTS.extensions();
        return new ScoresConfig.ExtensionSettings(
                enabled(json, "events", defaults.events()),
                parseBonusSources(json.object("bonus_sources"), defaults.bonusSources()),
                enabled(json, "triggers", defaults.triggers()),
                enabled(json, "check_command", defaults.checkCommand()),
                enabled(json, "stat_screen_sections", defaults.sheetSections()));
    }

    private static ScoresConfig.BonusSourceSettings parseBonusSources(
            LenientJson json, ScoresConfig.BonusSourceSettings defaults) {
        return new ScoresConfig.BonusSourceSettings(
                json.getBool("enabled", defaults.enabled()),
                nonNegative(json, "passive_advantage", defaults.passiveAdvantage()));
    }

    private static boolean enabled(LenientJson json, String feature, boolean defaultValue) {
        return json.object(feature).getBool("enabled", defaultValue);
    }

    private static ScoresConfig.LevellingSettings parseLevelling(LenientJson json) {
        ScoresConfig.LevellingSettings defaults = ScoresConfig.DEFAULTS.levelling();
        return new ScoresConfig.LevellingSettings(
                json.getBool("enabled", defaults.enabled()),
                parseXpThresholds(json, defaults.xpThresholds()),
                parseProficiencyBonuses(json, defaults.proficiencyBonuses()),
                parseXpSources(json, defaults.xpSources()),
                parseImprovements(json.object("ability_improvements"), defaults.improvements()));
    }

    /** One total per level, starting at 0 for level 1 and never decreasing. */
    private static List<Integer> parseXpThresholds(LenientJson json, List<Integer> defaultThresholds) {
        Optional<List<Integer>> thresholds = json.optionalIntList("xp_thresholds");
        if (thresholds.isEmpty()) {
            return defaultThresholds;
        }
        if (!perLevel(thresholds.get()) || thresholds.get().getFirst() != 0 || !nonDecreasing(thresholds.get())) {
            json.warn("'xp_thresholds' must be " + Levels.MAX_LEVEL
                    + " totals starting at 0 and never decreasing, using the default");
            return defaultThresholds;
        }
        return thresholds.get();
    }

    private static List<Integer> parseProficiencyBonuses(LenientJson json, List<Integer> defaultBonuses) {
        Optional<List<Integer>> bonuses = json.optionalIntList("proficiency_bonus");
        if (bonuses.isEmpty()) {
            return defaultBonuses;
        }
        boolean inRange = bonuses.get().stream()
                .allMatch(bonus -> bonus >= Proficiencies.MIN_BONUS && bonus <= Proficiencies.MAX_BONUS);
        if (!perLevel(bonuses.get()) || !inRange) {
            json.warn("'proficiency_bonus' must be " + Levels.MAX_LEVEL + " bonuses in " + Proficiencies.MIN_BONUS
                    + ".." + Proficiencies.MAX_BONUS + ", using the default");
            return defaultBonuses;
        }
        return bonuses.get();
    }

    private static boolean perLevel(List<Integer> values) {
        return values.size() == Levels.MAX_LEVEL;
    }

    private static boolean nonDecreasing(List<Integer> values) {
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) < values.get(i - 1)) {
                return false;
            }
        }
        return true;
    }

    private static ScoresConfig.XpSources parseXpSources(LenientJson json, ScoresConfig.XpSources defaults) {
        LenientJson vanilla = json.object("vanilla_xp");
        LenientJson advancements = json.object("advancements");
        return new ScoresConfig.XpSources(
                vanilla.getBool("enabled", defaults.vanillaXpEnabled()),
                nonNegative(vanilla, "xp_per_point", defaults.xpPerPoint()),
                advancements.getBool("enabled", defaults.advancementsEnabled()),
                nonNegative(advancements, "xp", defaults.xpPerAdvancement()));
    }

    static int nonNegative(LenientJson json, String key, int defaultValue) {
        int value = json.getInt(key, defaultValue);
        if (value < 0) {
            json.warn("'" + key + "' " + value + " is negative, using " + defaultValue);
            return defaultValue;
        }
        return value;
    }

    private static ScoresConfig.ImprovementSettings parseImprovements(
            LenientJson json, ScoresConfig.ImprovementSettings defaults) {
        return new ScoresConfig.ImprovementSettings(
                parseImprovementLevels(json, defaults.levels()),
                parseBonusOptions(json, "options", defaults.options()),
                parseMaxScore(json, "max_score", defaults.maxScore()));
    }

    /** A level outside the level range is skipped with a warning; repeats count once. */
    private static List<Integer> parseImprovementLevels(LenientJson json, List<Integer> defaultLevels) {
        Optional<List<Integer>> levels = json.optionalIntList("levels");
        if (levels.isEmpty()) {
            return defaultLevels;
        }
        Set<Integer> valid = new LinkedHashSet<>();
        for (int level : levels.get()) {
            if (Levels.inRange(level)) {
                valid.add(level);
            } else {
                json.warn("skipped improvement level " + level + ": outside " + Levels.MIN_LEVEL + ".."
                        + Levels.MAX_LEVEL);
            }
        }
        return List.copyOf(valid);
    }

    private static ScoresConfig.CreationSettings parseCreation(LenientJson json) {
        ScoresConfig.CreationSettings defaults = ScoresConfig.DEFAULTS.creation();
        return new ScoresConfig.CreationSettings(
                json.getBool("enabled", defaults.enabled()),
                parseMethods(json, defaults.methods()),
                nonNegative(json, "skill_choices", defaults.skillChoices()),
                parseStandardArray(json, defaults.standardArray()),
                parsePointBuy(json.object("point_buy"), defaults.pointBuy()),
                parseDice(json, "roll_dice", defaults.rollDice()),
                parseDice(json, "hardcore_dice", defaults.hardcoreDice()),
                parsePresets(json.object("presets"), defaults.presets()));
    }

    private static ScoresConfig.PresetSettings parsePresets(LenientJson json, ScoresConfig.PresetSettings defaults) {
        return new ScoresConfig.PresetSettings(
                json.getBool("enabled", defaults.enabled()),
                json.getBool("include_shipped", defaults.includeShipped()),
                parseBonusOptions(json, "bonus_options", defaults.bonusOptions()),
                parseMaxScore(json, "bonus_max_score", defaults.bonusMaxScore()));
    }

    /** Each option is a non-empty list of positive bonuses; a bad option is skipped with a warning. */
    private static List<List<Integer>> parseBonusOptions(
            LenientJson json, String key, List<List<Integer>> defaultOptions) {
        Optional<List<List<Integer>>> options = json.optionalIntLists(key);
        if (options.isEmpty()) {
            return defaultOptions;
        }
        List<List<Integer>> valid = new ArrayList<>();
        for (List<Integer> option : options.get()) {
            if (option.isEmpty() || !option.stream().allMatch(bonus -> bonus > 0)) {
                json.warn("skipped bonus option " + option + ": needs at least one bonus, each above 0");
                continue;
            }
            valid.add(option);
        }
        return valid;
    }

    private static int parseMaxScore(LenientJson json, String key, int defaultMax) {
        int max = json.getInt(key, defaultMax);
        if (!Abilities.inRange(max)) {
            json.warn("'" + key + "' " + max + " is outside " + Abilities.MIN_SCORE + ".." + Abilities.MAX_SCORE
                    + ", using " + defaultMax);
            return defaultMax;
        }
        return max;
    }

    /** Unknown ids are skipped; a list naming no known method allows them all. */
    private static List<CreationMethod> parseMethods(LenientJson json, List<CreationMethod> defaults) {
        if (!json.has("methods")) {
            return defaults;
        }
        Set<CreationMethod> methods = new LinkedHashSet<>();
        for (String id : json.stringList("methods")) {
            CreationMethod.byId(id)
                    .ifPresentOrElse(methods::add, () -> json.warn("unknown creation method '" + id + "', skipped"));
        }
        if (methods.isEmpty()) {
            json.warn("'methods' names no known method, allowing all");
            return defaults;
        }
        return List.copyOf(methods);
    }

    private static List<Integer> parseStandardArray(LenientJson json, List<Integer> defaultArray) {
        Optional<List<Integer>> array = json.optionalIntList("standard_array");
        if (array.isEmpty()) {
            return defaultArray;
        }
        if (array.get().size() != Ability.values().length
                || !array.get().stream().allMatch(Abilities::inRange)) {
            json.warn("'standard_array' must be " + Ability.values().length + " scores in " + Abilities.MIN_SCORE + ".."
                    + Abilities.MAX_SCORE + ", using the default");
            return defaultArray;
        }
        return array.get();
    }

    private static PointBuy parsePointBuy(LenientJson json, PointBuy defaults) {
        int budget = nonNegative(json, "budget", defaults.budget());
        if (!json.has("costs")) {
            return new PointBuy(budget, defaults.costs());
        }
        Map<Integer, Integer> costs = parseCosts(json.object("costs"));
        if (costs.isEmpty()) {
            json.warn("'costs' lists no valid score, using the default costs");
            return new PointBuy(budget, defaults.costs());
        }
        return new PointBuy(budget, costs);
    }

    /** Keys are scores, values their non-negative point cost; a bad entry is skipped with a warning. */
    private static Map<Integer, Integer> parseCosts(LenientJson json) {
        Map<Integer, Integer> costs = new HashMap<>();
        for (String key : json.keys()) {
            OptionalInt cost = json.optionalInt(key);
            Optional<Integer> score = parseScoreKey(key);
            if (score.isEmpty() || cost.isEmpty() || cost.getAsInt() < 0) {
                json.warn("skipped cost entry '" + key + "': needs a score in " + Abilities.MIN_SCORE + ".."
                        + Abilities.MAX_SCORE + " and a non-negative cost");
                continue;
            }
            costs.put(score.get(), cost.getAsInt());
        }
        return costs;
    }

    private static Optional<Integer> parseScoreKey(String key) {
        try {
            return Optional.of(Integer.parseInt(key)).filter(Abilities::inRange);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static DiceExpression parseDice(LenientJson json, String key, DiceExpression defaultDice) {
        Optional<String> text = json.optionalString(key);
        if (text.isEmpty()) {
            return defaultDice;
        }
        try {
            DiceExpression dice = DiceExpression.parse(text.get());
            if (dice.hasDice()) {
                return dice;
            }
            json.warn("'" + key + "' rolls no dice, using " + defaultDice);
        } catch (DiceParseException e) {
            json.warn("'" + key + "' is not a dice expression (" + e.getMessage() + "), using " + defaultDice);
        }
        return defaultDice;
    }

    private static ScoresConfig.CritfallSettings parseCritfall(LenientJson json) {
        ScoresConfig.CritfallSettings defaults = ScoresConfig.DEFAULTS.critfall();
        return new ScoresConfig.CritfallSettings(
                json.getBool("enabled", defaults.enabled()),
                json.getBool("unprofiled_mobs", defaults.unprofiledMobs()),
                parseSaveAbilities(json.object("save_abilities"), defaults.saveAbilities()));
    }

    /** Listed save keys override the defaults; an unknown ability is skipped with a warning. */
    private static Map<String, Ability> parseSaveAbilities(LenientJson json, Map<String, Ability> defaults) {
        Map<String, Ability> saveAbilities = new HashMap<>(defaults);
        for (String saveKey : json.keys()) {
            json.optionalString(saveKey).ifPresent(id -> Ability.byId(id)
                    .ifPresentOrElse(
                            ability -> saveAbilities.put(saveKey, ability),
                            () -> json.warn("'" + saveKey + "' names unknown ability '" + id + "', skipped")));
        }
        return Map.copyOf(saveAbilities);
    }

    private static ScoresConfig.ProficiencySettings parseProficiency(LenientJson json, Consumer<String> warn) {
        ScoresConfig.ProficiencySettings defaults = ScoresConfig.DEFAULTS.proficiency();
        int bonus = clamp(
                json.getInt("default_bonus", defaults.defaultBonus()),
                Proficiencies.MIN_BONUS,
                Proficiencies.MAX_BONUS,
                "proficiency.default_bonus",
                warn);
        return new ScoresConfig.ProficiencySettings(json.getBool("enabled", defaults.enabled()), bonus);
    }

    private static Map<Ability, Integer> parsePlayerDefaults(LenientJson json, Consumer<String> warn) {
        Map<Ability, Integer> defaults = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            int score = json.getInt(ability.id(), ScoresConfig.DEFAULTS.playerDefault(ability));
            defaults.put(ability, clampScore(score, "player_defaults." + ability.id(), warn));
        }
        return Map.copyOf(defaults);
    }

    private static int clampScore(int score, String key, Consumer<String> warn) {
        return clamp(score, Abilities.MIN_SCORE, Abilities.MAX_SCORE, key, warn);
    }

    private static int clamp(int value, int min, int max, String key, Consumer<String> warn) {
        int clamped = Math.clamp(value, min, max);
        if (clamped != value) {
            warn.accept(
                    "scores.json: '" + key + "' " + value + " is outside " + min + ".." + max + ", using " + clamped);
        }
        return clamped;
    }
}
