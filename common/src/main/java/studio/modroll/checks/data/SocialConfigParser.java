package studio.modroll.checks.data;

import static studio.modroll.checks.data.ScoresConfigLoader.atLeastOne;
import static studio.modroll.checks.data.ScoresConfigLoader.nonNegative;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.gossip.GossipType;
import studio.modroll.checks.social.SocialEffect;
import studio.modroll.checks.social.SocialOutcome;

/** Reads the {@code social} object of {@code scores.json}; a bad value falls back to its default with a warning. */
final class SocialConfigParser {

    private static final double MAX_BEHIND_DEGREES = 180;

    private SocialConfigParser() {}

    static ScoresConfig.SocialSettings parse(LenientJson json) {
        ScoresConfig.SocialSettings defaults = ScoresConfig.DEFAULTS.social();
        return new ScoresConfig.SocialSettings(
                json.getBool("enabled", defaults.enabled()),
                nonNegative(json, "nearby_radius", defaults.nearbyRadius()),
                nonNegative(json, "gossip_radius", defaults.gossipRadius()),
                parsePersuade(json.object("persuade"), defaults.persuade()),
                parseDeceive(json.object("deceive"), defaults.deceive()),
                parseIntimidate(json.object("intimidate"), defaults.intimidate()),
                parsePickpocket(json.object("pickpocket"), defaults.pickpocket()),
                parsePassivePrices(json.object("passive_prices"), defaults.passivePrices()),
                parseReactions(json.object("reactions"), defaults.reactions()),
                parseWitnesses(json.object("witnesses"), defaults.witnesses()),
                parseGolemAlarm(json.object("golem_alarm"), defaults.golemAlarm()),
                parseOnGuard(json.object("on_guard"), defaults.onGuard()),
                parseWary(json.object("wary"), defaults.wary()),
                parseFeel(json.object("feel"), defaults.feel()),
                parseIntimidateMob(json.object("intimidate_mob"), defaults.intimidateMob()),
                parseCalm(json.object("calm"), defaults.calm()),
                parseInsight(json.object("insight"), defaults.insight()),
                parsePerformance(json.object("performance"), defaults.performance()));
    }

    private static ScoresConfig.IntimidateMobSettings parseIntimidateMob(
            LenientJson json, ScoresConfig.IntimidateMobSettings defaults) {
        return new ScoresConfig.IntimidateMobSettings(
                json.getBool("enabled", defaults.enabled()),
                json.getInt("dc", defaults.dc()),
                nonNegative(json, "cooldown_ticks", defaults.cooldownTicks()),
                nonNegative(json, "max_health", defaults.maxHealth()),
                parseId(json, "exclude_tag", defaults.excludeTag()),
                nonNegative(json, "flee_ticks", defaults.fleeTicks()),
                nonNegative(json, "flee_distance", defaults.fleeDistance()),
                nonNegative(json, "flee_speed", defaults.fleeSpeed()),
                nonNegative(json, "speed_boost_ticks", defaults.speedBoostTicks()),
                nonNegative(json, "speed_boost_amplifier", defaults.speedBoostAmplifier()));
    }

    private static ScoresConfig.CalmSettings parseCalm(LenientJson json, ScoresConfig.CalmSettings defaults) {
        return new ScoresConfig.CalmSettings(
                json.getBool("enabled", defaults.enabled()),
                json.getInt("dc", defaults.dc()),
                nonNegative(json, "cooldown_ticks", defaults.cooldownTicks()),
                parseId(json, "tag", defaults.tag()));
    }

    private static ScoresConfig.InsightSettings parseInsight(LenientJson json, ScoresConfig.InsightSettings defaults) {
        return new ScoresConfig.InsightSettings(
                parseSense(json.object("sense_hostility"), defaults.senseHostility()),
                parseSense(json.object("creeper_warning"), defaults.creeperWarning()));
    }

    private static ScoresConfig.InsightSense parseSense(LenientJson json, ScoresConfig.InsightSense defaults) {
        return new ScoresConfig.InsightSense(
                json.getBool("enabled", defaults.enabled()),
                json.getInt("dc", defaults.dc()),
                nonNegative(json, "range", defaults.range()));
    }

    private static ScoresConfig.PerformanceSettings parsePerformance(
            LenientJson json, ScoresConfig.PerformanceSettings defaults) {
        return new ScoresConfig.PerformanceSettings(
                json.getBool("enabled", defaults.enabled()),
                json.getInt("dc", defaults.dc()),
                nonNegative(json, "cooldown_ticks", defaults.cooldownTicks()),
                nonNegative(json, "range", defaults.range()),
                parsePerformanceEffect(json.object("success"), defaults.success()),
                parsePerformanceEffect(json.object("critical_success"), defaults.criticalSuccess()),
                parsePerformanceEffect(json.object("critical_failure"), defaults.criticalFailure()));
    }

    private static ScoresConfig.PerformanceEffect parsePerformanceEffect(
            LenientJson json, ScoresConfig.PerformanceEffect defaults) {
        return new ScoresConfig.PerformanceEffect(
                parseGossip(json, defaults.gossip()), nonNegative(json, "piglin_ticks", defaults.piglinTicks()));
    }

    private static ResourceLocation parseId(LenientJson json, String key, ResourceLocation defaultId) {
        return json.optionalString(key)
                .map(text -> Optional.ofNullable(ResourceLocation.tryParse(text))
                        .orElseGet(() -> {
                            json.warn("'" + key + "' " + text + " is not an id, using " + defaultId);
                            return defaultId;
                        }))
                .orElse(defaultId);
    }

    private static ScoresConfig.FeelSettings parseFeel(LenientJson json, ScoresConfig.FeelSettings defaults) {
        LenientJson bubbles = json.object("speech_bubbles");
        ScoresConfig.SpeechBubbleSettings bubbleDefaults = defaults.speechBubbles();
        return new ScoresConfig.FeelSettings(
                nonNegative(json, "radius", defaults.radius()),
                json.object("pickpocket_animation").getBool("enabled", defaults.pickpocketAnimation()),
                new ScoresConfig.SpeechBubbleSettings(
                        bubbles.getBool("enabled", bubbleDefaults.enabled()),
                        atLeastOne(bubbles, "duration_ticks", bubbleDefaults.durationTicks())),
                json.object("voices").getBool("enabled", defaults.voices()));
    }

    private static ScoresConfig.ReactionSettings parseReactions(
            LenientJson json, ScoresConfig.ReactionSettings defaults) {
        return new ScoresConfig.ReactionSettings(
                json.getBool("enabled", defaults.enabled()),
                nonNegative(json, "flee_distance", defaults.fleeDistance()),
                nonNegative(json, "flee_ticks", defaults.fleeTicks()),
                nonNegative(json, "flee_speed", defaults.fleeSpeed()));
    }

    private static ScoresConfig.WitnessSettings parseWitnesses(
            LenientJson json, ScoresConfig.WitnessSettings defaults) {
        return new ScoresConfig.WitnessSettings(
                json.getBool("enabled", defaults.enabled()),
                nonNegative(json, "refuse_fraction", defaults.refuseFraction()),
                nonNegative(json, "flee_distance", defaults.fleeDistance()));
    }

    private static ScoresConfig.GolemAlarmSettings parseGolemAlarm(
            LenientJson json, ScoresConfig.GolemAlarmSettings defaults) {
        return new ScoresConfig.GolemAlarmSettings(
                json.getBool("enabled", defaults.enabled()),
                nonNegative(json, "radius", defaults.radius()),
                nonNegative(json, "duration_ticks", defaults.durationTicks()));
    }

    private static ScoresConfig.OnGuardSettings parseOnGuard(LenientJson json, ScoresConfig.OnGuardSettings defaults) {
        return new ScoresConfig.OnGuardSettings(
                json.getBool("enabled", defaults.enabled()),
                parseDeEscalation(json.object("plead"), defaults.plead()),
                parseDeEscalation(json.object("lie"), defaults.lie()));
    }

    private static ScoresConfig.DeEscalation parseDeEscalation(LenientJson json, ScoresConfig.DeEscalation defaults) {
        return new ScoresConfig.DeEscalation(
                json.getBool("enabled", defaults.enabled()), json.getInt("dc", defaults.dc()));
    }

    private static ScoresConfig.WarySettings parseWary(LenientJson json, ScoresConfig.WarySettings defaults) {
        return new ScoresConfig.WarySettings(
                json.getBool("enabled", defaults.enabled()), json.getInt("max_reputation", defaults.maxReputation()));
    }

    private static ScoresConfig.PersuadeSettings parsePersuade(
            LenientJson json, ScoresConfig.PersuadeSettings defaults) {
        return new ScoresConfig.PersuadeSettings(
                json.getBool("enabled", defaults.enabled()),
                json.getInt("dc", defaults.dc()),
                nonNegative(json, "cooldown_ticks", defaults.cooldownTicks()),
                parseDeal(json, defaults.deal()));
    }

    private static ScoresConfig.DeceiveSettings parseDeceive(LenientJson json, ScoresConfig.DeceiveSettings defaults) {
        LenientJson piglin = json.object("piglin");
        ScoresConfig.PiglinDeceit piglinDefaults = defaults.piglin();
        LenientJson trader = json.object("wandering_trader");
        ScoresConfig.TraderDeceit traderDefaults = defaults.wanderingTrader();
        return new ScoresConfig.DeceiveSettings(
                json.getBool("enabled", defaults.enabled()),
                nonNegative(json, "cooldown_ticks", defaults.cooldownTicks()),
                parseDeal(json, defaults.deal()),
                new ScoresConfig.PiglinDeceit(
                        nonNegative(piglin, "disguise_ticks", piglinDefaults.disguiseTicks()),
                        nonNegative(piglin, "critical_disguise_ticks", piglinDefaults.criticalDisguiseTicks()),
                        nonNegative(piglin, "anger_ticks", piglinDefaults.angerTicks())),
                new ScoresConfig.TraderDeceit(
                        nonNegative(trader, "refuse_ticks", traderDefaults.refuseTicks()),
                        trader.getBool("llamas_spit", traderDefaults.llamasSpit())));
    }

    private static ScoresConfig.Deal parseDeal(LenientJson json, ScoresConfig.Deal defaults) {
        Map<SocialOutcome, SocialEffect> outcomes = new EnumMap<>(SocialOutcome.class);
        for (SocialOutcome outcome : SocialOutcome.values()) {
            outcomes.put(outcome, parseEffect(json.object(outcome.id()), defaults.effect(outcome)));
        }
        return new ScoresConfig.Deal(nonNegative(json, "barely_margin", defaults.barelyMargin()), outcomes);
    }

    private static ScoresConfig.IntimidateSettings parseIntimidate(
            LenientJson json, ScoresConfig.IntimidateSettings defaults) {
        return new ScoresConfig.IntimidateSettings(
                json.getBool("enabled", defaults.enabled()),
                json.getInt("dc", defaults.dc()),
                nonNegative(json, "cooldown_ticks", defaults.cooldownTicks()),
                parseEffect(json.object("success"), defaults.success()),
                parseEffect(json.object("failure"), defaults.failure()),
                nonNegative(json, "piglin_anger_ticks", defaults.piglinAngerTicks()));
    }

    private static ScoresConfig.PickpocketSettings parsePickpocket(
            LenientJson json, ScoresConfig.PickpocketSettings defaults) {
        return new ScoresConfig.PickpocketSettings(
                json.getBool("enabled", defaults.enabled()),
                nonNegative(json, "cooldown_ticks", defaults.cooldownTicks()),
                parseBehindDegrees(json, defaults.behindDegrees()),
                parseEffect(json.object("failure"), defaults.failure()));
    }

    private static double parseBehindDegrees(LenientJson json, double defaultDegrees) {
        double degrees = json.getDouble("behind_degrees", defaultDegrees);
        if (degrees < 0 || degrees > MAX_BEHIND_DEGREES) {
            json.warn("'behind_degrees' " + degrees + " is outside 0.." + MAX_BEHIND_DEGREES + ", using "
                    + defaultDegrees);
            return defaultDegrees;
        }
        return degrees;
    }

    private static SocialEffect parseEffect(LenientJson json, SocialEffect defaults) {
        return new SocialEffect(
                json.getDouble("price_percent", defaults.pricePercent()),
                nonNegative(json, "price_ticks", defaults.priceTicks()),
                parseGossip(json, defaults.gossip()),
                nonNegative(json, "refuse_ticks", defaults.refuseTicks()),
                json.getBool("angers_golems", defaults.angersGolems()));
    }

    /** An amount of 0 spreads no gossip; {@code nearby} tells every villager in the gossip radius. */
    private static Optional<SocialEffect.Gossip> parseGossip(
            LenientJson parent, Optional<SocialEffect.Gossip> defaults) {
        if (!parent.has("gossip")) {
            return defaults;
        }
        LenientJson json = parent.object("gossip");
        GossipType defaultType = defaults.map(SocialEffect.Gossip::type).orElse(GossipType.MINOR_NEGATIVE);
        GossipType type = json.optionalString("type")
                .map(id -> gossipType(json, id, defaultType))
                .orElse(defaultType);
        int amount = nonNegative(
                json, "amount", defaults.map(SocialEffect.Gossip::amount).orElse(0));
        boolean nearby =
                json.getBool("nearby", defaults.map(SocialEffect.Gossip::nearby).orElse(false));
        return amount == 0 ? Optional.empty() : Optional.of(new SocialEffect.Gossip(type, amount, nearby));
    }

    private static GossipType gossipType(LenientJson json, String id, GossipType defaultType) {
        return Arrays.stream(GossipType.values())
                .filter(type -> type.getSerializedName().equals(id))
                .findFirst()
                .orElseGet(() -> {
                    json.warn("unknown gossip type '" + id + "', using " + defaultType.getSerializedName());
                    return defaultType;
                });
    }

    private static ScoresConfig.PassivePriceSettings parsePassivePrices(
            LenientJson json, ScoresConfig.PassivePriceSettings defaults) {
        return new ScoresConfig.PassivePriceSettings(
                json.getBool("enabled", defaults.enabled()),
                json.getDouble("charisma_percent_per_point", defaults.charismaPercentPerPoint()),
                parseProfessions(json, defaults.professions()));
    }

    /** A listed {@code professions} object replaces the defaults; a bad entry is skipped with a warning. */
    private static Map<ResourceLocation, ScoresConfig.ProfessionPrice> parseProfessions(
            LenientJson parent, Map<ResourceLocation, ScoresConfig.ProfessionPrice> defaults) {
        if (!parent.has("professions")) {
            return defaults;
        }
        LenientJson json = parent.object("professions");
        Map<ResourceLocation, ScoresConfig.ProfessionPrice> professions = new HashMap<>();
        for (String key : json.keys()) {
            LenientJson entry = json.object(key);
            Optional<ResourceLocation> profession = Optional.ofNullable(ResourceLocation.tryParse(key));
            Optional<ResourceLocation> skill = entry.optionalString("skill").map(ResourceLocation::tryParse);
            double percent = entry.getDouble("percent_per_point", 0);
            if (profession.isEmpty() || skill.isEmpty()) {
                json.warn("skipped profession '" + key + "': needs a profession id and a 'skill' id");
                continue;
            }
            professions.put(profession.get(), new ScoresConfig.ProfessionPrice(skill.get(), percent));
        }
        return professions;
    }
}
