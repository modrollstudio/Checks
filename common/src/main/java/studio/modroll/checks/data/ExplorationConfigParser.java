package studio.modroll.checks.data;

import static studio.modroll.checks.data.ScoresConfigLoader.atLeastOne;
import static studio.modroll.checks.data.ScoresConfigLoader.fraction;
import static studio.modroll.checks.data.ScoresConfigLoader.nonNegative;

/** Reads the {@code exploration} object of {@code scores.json}; a bad value falls back to its default with a warning. */
final class ExplorationConfigParser {

    private ExplorationConfigParser() {}

    static ScoresConfig.ExplorationSettings parse(LenientJson json) {
        ScoresConfig.ExplorationSettings defaults = ScoresConfig.DEFAULTS.exploration();
        return new ScoresConfig.ExplorationSettings(
                parseLeap(json.object("leap"), defaults.leap()),
                parseLanding(json.object("landing"), defaults.landing()),
                parseCobwebs(json.object("cobwebs"), defaults.cobwebs()),
                parseClimbing(json.object("climbing"), defaults.climbing()),
                parseSneak(json.object("sneak"), defaults.sneak()),
                parseSpotTripwires(json.object("spot_tripwires"), defaults.spotTripwires()),
                parseDisarmTripwires(json.object("disarm_tripwires"), defaults.disarmTripwires()),
                parseSearch(json.object("search_chests"), defaults.searchChests()),
                parseMonsterLore(json.object("monster_lore"), defaults.monsterLore()),
                parseStructureLore(json.object("structure_lore"), defaults.structureLore()),
                parseTaming(json.object("taming"), defaults.taming()),
                parseReduction(json.object("hunger"), defaults.hunger()),
                parseReduction(json.object("ailments"), defaults.ailments()));
    }

    private static ScoresConfig.LeapSettings parseLeap(LenientJson json, ScoresConfig.LeapSettings defaults) {
        return new ScoresConfig.LeapSettings(
                json.getBool("enabled", defaults.enabled()),
                json.getInt("dc", defaults.dc()),
                nonNegative(json, "cooldown_ticks", defaults.cooldownTicks()),
                nonNegative(json, "boost", defaults.boost()),
                atLeastOne(json, "min_drop", defaults.minDrop()));
    }

    private static ScoresConfig.LandingSettings parseLanding(LenientJson json, ScoresConfig.LandingSettings defaults) {
        return new ScoresConfig.LandingSettings(
                json.getBool("enabled", defaults.enabled()),
                json.getInt("base_dc", defaults.baseDc()),
                nonNegative(json, "dc_per_damage", defaults.dcPerDamage()),
                nonNegative(json, "success_multiplier", defaults.successMultiplier()));
    }

    private static ScoresConfig.CobwebSettings parseCobwebs(LenientJson json, ScoresConfig.CobwebSettings defaults) {
        return new ScoresConfig.CobwebSettings(
                json.getBool("enabled", defaults.enabled()),
                json.getInt("dc", defaults.dc()),
                nonNegative(json, "retry_ticks", defaults.retryTicks()));
    }

    private static ScoresConfig.ClimbingSettings parseClimbing(
            LenientJson json, ScoresConfig.ClimbingSettings defaults) {
        return new ScoresConfig.ClimbingSettings(
                json.getBool("enabled", defaults.enabled()),
                nonNegative(json, "per_point", defaults.perPoint()),
                nonNegative(json, "max_bonus", defaults.maxBonus()));
    }

    private static ScoresConfig.SneakSettings parseSneak(LenientJson json, ScoresConfig.SneakSettings defaults) {
        return new ScoresConfig.SneakSettings(
                json.getBool("enabled", defaults.enabled()),
                nonNegative(json, "per_point", defaults.perPoint()),
                fraction(json, "min_visibility", defaults.minVisibility()));
    }

    private static ScoresConfig.SpotTripwireSettings parseSpotTripwires(
            LenientJson json, ScoresConfig.SpotTripwireSettings defaults) {
        return new ScoresConfig.SpotTripwireSettings(
                json.getBool("enabled", defaults.enabled()),
                json.getInt("dc", defaults.dc()),
                nonNegative(json, "range", defaults.range()),
                atLeastOne(json, "interval_ticks", defaults.intervalTicks()));
    }

    private static ScoresConfig.DisarmTripwireSettings parseDisarmTripwires(
            LenientJson json, ScoresConfig.DisarmTripwireSettings defaults) {
        return new ScoresConfig.DisarmTripwireSettings(
                json.getBool("enabled", defaults.enabled()), json.getInt("dc", defaults.dc()));
    }

    private static ScoresConfig.SearchSettings parseSearch(LenientJson json, ScoresConfig.SearchSettings defaults) {
        return new ScoresConfig.SearchSettings(
                json.getBool("enabled", defaults.enabled()), json.getInt("dc", defaults.dc()));
    }

    private static ScoresConfig.MonsterLoreSettings parseMonsterLore(
            LenientJson json, ScoresConfig.MonsterLoreSettings defaults) {
        return new ScoresConfig.MonsterLoreSettings(
                json.getBool("enabled", defaults.enabled()),
                json.getInt("dc", defaults.dc()),
                nonNegative(json, "range", defaults.range()),
                nonNegative(json, "retry_ticks", defaults.retryTicks()),
                atLeastOne(json, "interval_ticks", defaults.intervalTicks()));
    }

    private static ScoresConfig.StructureLoreSettings parseStructureLore(
            LenientJson json, ScoresConfig.StructureLoreSettings defaults) {
        return new ScoresConfig.StructureLoreSettings(
                json.getBool("enabled", defaults.enabled()),
                json.getInt("dc", defaults.dc()),
                nonNegative(json, "retry_ticks", defaults.retryTicks()),
                atLeastOne(json, "interval_ticks", defaults.intervalTicks()));
    }

    private static ScoresConfig.TamingSettings parseTaming(LenientJson json, ScoresConfig.TamingSettings defaults) {
        return new ScoresConfig.TamingSettings(
                json.getBool("enabled", defaults.enabled()), json.getInt("dc", defaults.dc()));
    }

    private static ScoresConfig.ReductionSettings parseReduction(
            LenientJson json, ScoresConfig.ReductionSettings defaults) {
        return new ScoresConfig.ReductionSettings(
                json.getBool("enabled", defaults.enabled()),
                nonNegative(json, "per_point", defaults.perPoint()),
                fraction(json, "max_reduction", defaults.maxReduction()));
    }
}
