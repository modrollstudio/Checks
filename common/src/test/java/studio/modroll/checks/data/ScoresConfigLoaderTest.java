package studio.modroll.checks.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.gossip.GossipType;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.body.Extra;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.PointBuy;
import studio.modroll.checks.save.VanillaSave;
import studio.modroll.checks.social.SocialAction;
import studio.modroll.checks.social.SocialEffect;
import studio.modroll.checks.social.SocialOutcome;
import studio.modroll.checks.trait.TraitEffectType;
import studio.modroll.critfall.api.ModifierProvider;
import studio.modroll.critfall.api.dice.DiceExpression;

class ScoresConfigLoaderTest {

    private static final int VANILLA_GOLEM_REPUTATION = -100;

    private final List<String> warnings = new ArrayList<>();

    private ScoresConfig parse(String json) {
        return ScoresConfigLoader.parse(JsonParser.parseString(json).getAsJsonObject(), warnings::add);
    }

    @Test
    void defaultFileRoundTripsToDefaults() {
        assertEquals(ScoresConfig.DEFAULTS, parse(ScoresConfigLoader.DEFAULT_FILE));
        assertEquals(List.of(), warnings);
    }

    @Test
    void docsExampleParsesCleanlyToTheDefaults() throws IOException {
        assertEquals(ScoresConfig.DEFAULTS, parse(Files.readString(Path.of("../docs/examples/scores.json"))));
        assertEquals(List.of(), warnings);
    }

    /** The file written on first run lists every key, exactly as the documented example does. */
    @Test
    void defaultFileMatchesTheDocsExample() throws IOException {
        assertEquals(
                JsonParser.parseString(Files.readString(Path.of("../docs/examples/scores.json"))),
                JsonParser.parseString(ScoresConfigLoader.DEFAULT_FILE));
    }

    @Test
    void readsOverriddenValues() {
        ScoresConfig config = parse("{\"player_defaults\": {\"str\": 15}, \"derivation\": {\"enabled\": false},"
                + " \"profiles\": {\"enabled\": false}, \"skills\": {\"enabled\": false},"
                + " \"proficiency\": {\"enabled\": false, \"default_bonus\": 4}}");
        assertEquals(15, config.playerDefault(Ability.STRENGTH));
        assertEquals(10, config.playerDefault(Ability.DEXTERITY));
        assertFalse(config.derivation().enabled());
        assertFalse(config.profilesEnabled());
        assertFalse(config.skillsEnabled());
        assertFalse(config.proficiency().enabled());
        assertEquals(4, config.proficiency().defaultBonus());
    }

    @Test
    void readsTheExtensionSwitches() {
        ScoresConfig config = parse("{\"events\": {\"enabled\": false}, \"bonus_sources\": {\"enabled\": false},"
                + " \"triggers\": {\"enabled\": false}, \"check_command\": {\"enabled\": false},"
                + " \"stat_screen_sections\": {\"enabled\": false}}");

        assertEquals(List.of(), warnings);
        assertEquals(
                new ScoresConfig.ExtensionSettings(
                        false, new ScoresConfig.BonusSourceSettings(false, 5), false, false, false),
                config.extensions());
    }

    @Test
    void readsThePassiveAdvantageAndRejectsANegativeOne() {
        assertEquals(
                new ScoresConfig.BonusSourceSettings(true, 4),
                parse("{\"bonus_sources\": {\"passive_advantage\": 4}}")
                        .extensions()
                        .bonusSources());
        assertEquals(List.of(), warnings);

        assertEquals(
                5,
                parse("{\"bonus_sources\": {\"passive_advantage\": -1}}")
                        .extensions()
                        .bonusSources()
                        .passiveAdvantage());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("passive_advantage")));
    }

    @Test
    void preExtensionConfigLoadsWithEverythingOn() {
        assertEquals(
                ScoresConfig.DEFAULTS.extensions(),
                parse("{\"format_version\": 1}").extensions());
        assertEquals(List.of(), warnings);
    }

    @Test
    void outOfRangeProficiencyBonusIsClampedWithWarning() {
        assertEquals(
                10,
                parse("{\"proficiency\": {\"default_bonus\": 11}}")
                        .proficiency()
                        .defaultBonus());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("default_bonus")));
    }

    @Test
    void preProficiencyConfigLoadsWithProficiencyDefaults() {
        ScoresConfig config = parse("{\"format_version\": 1, \"skills\": {\"enabled\": true}}");
        assertEquals(List.of(), warnings);
        assertEquals(ScoresConfig.DEFAULTS.proficiency(), config.proficiency());
    }

    @Test
    void readsCritfallSettings() {
        ScoresConfig.CritfallSettings critfall = parse("{\"critfall\": {\"enabled\": false, \"unprofiled_mobs\": true,"
                        + " \"save_abilities\": {\"critfall:spell\": \"wis\", \"mymod:poison\": \"con\"}}}")
                .critfall();
        assertFalse(critfall.enabled());
        assertTrue(critfall.unprofiledMobs());
        assertEquals(
                Map.of(ModifierProvider.SPELL_SAVE, Ability.WISDOM, "mymod:poison", Ability.CONSTITUTION),
                critfall.saveAbilities());
        assertEquals(List.of(), warnings);
    }

    @Test
    void spellSaveDefaultsToDexterity() {
        assertEquals(
                Map.of(ModifierProvider.SPELL_SAVE, Ability.DEXTERITY),
                parse("{\"critfall\": {\"save_abilities\": {}}}").critfall().saveAbilities());
    }

    @Test
    void unknownSaveAbilityIsSkippedWithWarning() {
        ScoresConfig.CritfallSettings critfall = parse(
                        "{\"critfall\": {\"save_abilities\": {\"critfall:spell\": \"luck\"}}}")
                .critfall();
        assertEquals(Map.of(ModifierProvider.SPELL_SAVE, Ability.DEXTERITY), critfall.saveAbilities());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("luck")));
    }

    @Test
    void preCritfallConfigLoadsWithCritfallDefaults() {
        ScoresConfig config = parse("{\"format_version\": 1, \"proficiency\": {\"enabled\": true}}");
        assertEquals(List.of(), warnings);
        assertEquals(ScoresConfig.DEFAULTS.critfall(), config.critfall());
    }

    @Test
    void readsStatScreenToggle() {
        assertFalse(parse("{\"stat_screen\": {\"enabled\": false}}").statScreenEnabled());
    }

    @Test
    void preStatScreenConfigLoadsWithTheScreenEnabled() {
        ScoresConfig config = parse("{\"format_version\": 1, \"critfall\": {\"enabled\": true}}");
        assertEquals(List.of(), warnings);
        assertTrue(config.statScreenEnabled());
    }

    @Test
    void outOfRangePlayerDefaultIsClampedWithWarning() {
        assertEquals(30, parse("{\"player_defaults\": {\"str\": 99}}").playerDefault(Ability.STRENGTH));
        assertFalse(warnings.isEmpty());
    }

    @Test
    void derivationConstantsAreRead() {
        ScoresConfig config = parse("{\"derivation\": {\"strength_base\": 3, \"strength_per_attack_damage\": 2.5}}");
        assertEquals(3.0, config.derivation().strengthBase());
        assertEquals(2.5, config.derivation().strengthPerAttackDamage());
    }

    @Test
    void readsCreationSettings() {
        ScoresConfig.CreationSettings creation = parse("{\"creation\": {\"enabled\": false,"
                        + " \"methods\": [\"point_buy\", \"hardcore\"], \"skill_choices\": 4,"
                        + " \"standard_array\": [16, 14, 13, 12, 10, 8],"
                        + " \"point_buy\": {\"budget\": 30, \"costs\": {\"7\": 0, \"8\": 1}},"
                        + " \"roll_dice\": \"5d6kh3\", \"hardcore_dice\": \"4d6kh3\"}}")
                .creation();
        assertFalse(creation.enabled());
        assertEquals(List.of(CreationMethod.POINT_BUY, CreationMethod.HARDCORE), creation.methods());
        assertEquals(4, creation.skillChoices());
        assertEquals(List.of(16, 14, 13, 12, 10, 8), creation.standardArray());
        assertEquals(new PointBuy(30, Map.of(7, 0, 8, 1)), creation.pointBuy());
        assertEquals(DiceExpression.parse("5d6kh3"), creation.dice(CreationMethod.ROLL));
        assertEquals(DiceExpression.parse("4d6kh3"), creation.dice(CreationMethod.HARDCORE));
        assertEquals(List.of(), warnings);
    }

    @Test
    void badCreationValuesFallBackToDefaultsWithWarnings() {
        ScoresConfig.CreationSettings creation = parse("{\"creation\": {\"methods\": [\"lottery\"],"
                        + " \"skill_choices\": -1, \"standard_array\": [15, 14],"
                        + " \"point_buy\": {\"budget\": -5, \"costs\": {\"eight\": 0, \"40\": 1}},"
                        + " \"roll_dice\": \"4x6\", \"hardcore_dice\": \"3\"}}")
                .creation();
        assertEquals(ScoresConfig.DEFAULTS.creation(), creation);
        assertEquals(10, warnings.size(), warnings.toString());
    }

    @Test
    void preCreationConfigLoadsWithCreationDefaults() {
        ScoresConfig config = parse("{\"format_version\": 1, \"stat_screen\": {\"enabled\": true}}");
        assertEquals(List.of(), warnings);
        assertEquals(ScoresConfig.DEFAULTS.creation(), config.creation());
    }

    @Test
    void readsPresetSettings() {
        ScoresConfig.PresetSettings presets = parse("{\"creation\": {\"presets\": {\"enabled\": false,"
                        + " \"include_shipped\": false, \"bonus_options\": [[3], [1, 1]], \"bonus_max_score\": 18}}}")
                .creation()
                .presets();
        assertEquals(new ScoresConfig.PresetSettings(false, false, List.of(List.of(3), List.of(1, 1)), 18), presets);
        assertEquals(List.of(), warnings);
    }

    @Test
    void badBonusOptionsAreSkippedAndABadCapFallsBack() {
        ScoresConfig.PresetSettings presets = parse("{\"creation\": {\"presets\": {"
                        + " \"bonus_options\": [[2, 1], [], [0, 1]], \"bonus_max_score\": 31}}}")
                .creation()
                .presets();
        assertEquals(List.of(List.of(2, 1)), presets.bonusOptions());
        assertEquals(ScoresConfig.DEFAULTS.creation().presets().bonusMaxScore(), presets.bonusMaxScore());
        assertEquals(3, warnings.size(), warnings.toString());
    }

    @Test
    void readsLevellingSettings() {
        ScoresConfig.LevellingSettings levelling = parse("{\"levelling\": {\"enabled\": false,"
                        + " \"vanilla_xp\": {\"enabled\": false, \"xp_per_point\": 3},"
                        + " \"advancements\": {\"xp\": 100},"
                        + " \"ability_improvements\": {\"levels\": [3, 6], \"options\": [[1]], \"max_score\": 18}}}")
                .levelling();
        assertFalse(levelling.enabled());
        assertEquals(new ScoresConfig.XpSources(false, 3, true, 100), levelling.xpSources());
        assertEquals(
                new ScoresConfig.ImprovementSettings(List.of(3, 6), List.of(List.of(1)), 18), levelling.improvements());
        assertEquals(ScoresConfig.DEFAULTS.levelling().xpThresholds(), levelling.xpThresholds());
        assertEquals(List.of(), warnings);
    }

    @Test
    void preLevellingConfigLoadsWithLevellingDefaults() {
        assertEquals(
                ScoresConfig.DEFAULTS.levelling(),
                parse("{\"format_version\": 1}").levelling());
        assertEquals(List.of(), warnings);
    }

    @Test
    void badThresholdsFallBackToTheDefaultWithWarning() {
        for (String thresholds : List.of("[0, 10, 20]", "[5" + ", 10".repeat(19) + "]", decreasingThresholds())) {
            warnings.clear();
            assertEquals(
                    ScoresConfig.DEFAULTS.levelling().xpThresholds(),
                    parse("{\"levelling\": {\"xp_thresholds\": " + thresholds + "}}")
                            .levelling()
                            .xpThresholds());
            assertTrue(warnings.stream().anyMatch(w -> w.contains("xp_thresholds")), thresholds);
        }
    }

    @Test
    void badProficiencyBonusesFallBackToTheDefaultWithWarning() {
        assertEquals(
                ScoresConfig.DEFAULTS.levelling().proficiencyBonuses(),
                parse("{\"levelling\": {\"proficiency_bonus\": [2, 3]}}")
                        .levelling()
                        .proficiencyBonuses());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("proficiency_bonus")));
    }

    @Test
    void negativeXpAmountsFallBackWithWarning() {
        assertEquals(
                ScoresConfig.DEFAULTS.levelling().xpSources(),
                parse("{\"levelling\": {\"vanilla_xp\": {\"xp_per_point\": -1}, \"advancements\": {\"xp\": -5}}}")
                        .levelling()
                        .xpSources());
        assertEquals(2, warnings.size());
    }

    @Test
    void improvementLevelsOutsideTheLevelRangeAreSkipped() {
        assertEquals(
                List.of(4, 8),
                parse("{\"levelling\": {\"ability_improvements\": {\"levels\": [0, 4, 21, 8, 4]}}}")
                        .levelling()
                        .improvements()
                        .levels());
        assertEquals(2, warnings.size());
    }

    @Test
    void readsTheBodySettings() {
        ScoresConfig.BodySettings body = parse("{\"health\": {\"enabled\": false, \"per_con_point\": 1.5,"
                        + " \"per_level\": {\"enabled\": true, \"multiplier\": 0.5}},"
                        + " \"armor_class\": {\"enabled\": false, \"medium_max_dex\": 3},"
                        + " \"size\": {\"enabled\": false},"
                        + " \"attribute_extras\": {\"breath\": {\"enabled\": false, \"per_point\": 0.5}}}")
                .body();

        assertEquals(new ScoresConfig.HealthSettings(false, 1.5, true, 0.5), body.health());
        assertEquals(new ScoresConfig.ArmorClassSettings(false, 3), body.armorClass());
        assertFalse(body.sizeEnabled());
        assertEquals(new ScoresConfig.ExtraSetting(false, 0.5), body.extra(Extra.BREATH));
        assertEquals(ScoresConfig.DEFAULTS.body().extra(Extra.KNOCKBACK), body.extra(Extra.KNOCKBACK));
        assertEquals(List.of(), warnings);
    }

    @Test
    void negativeBodyValuesFallBackWithWarning() {
        ScoresConfig.BodySettings body = parse("{\"health\": {\"per_con_point\": -2,"
                        + " \"per_level\": {\"multiplier\": -1}},"
                        + " \"armor_class\": {\"medium_max_dex\": -1},"
                        + " \"attribute_extras\": {\"bow_draw\": {\"per_point\": -0.1}}}")
                .body();

        assertEquals(ScoresConfig.DEFAULTS.body(), body);
        assertEquals(4, warnings.size());
    }

    @Test
    void preBodyConfigLoadsWithTheBodyDefaults() {
        assertEquals(
                ScoresConfig.DEFAULTS.body(), parse("{\"format_version\": 1}").body());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void readsTheVanillaSaveSettings() {
        ScoresConfig.SaveSettings saves = parse("{\"vanilla_saves\": {\"profiled_mobs\": true, \"cooldown_ticks\": 40,"
                        + " \"fire\": {\"enabled\": false, \"dc\": 15, \"success_multiplier\": 0.25},"
                        + " \"knockback\": {\"min_strength\": 1.0}}}")
                .saves();

        assertTrue(saves.profiledMobs());
        assertEquals(40, saves.cooldownTicks());
        assertEquals(1.0, saves.knockbackMinStrength());
        assertEquals(new ScoresConfig.SaveSetting(false, 15, 0.25), saves.save(VanillaSave.FIRE));
        assertEquals(ScoresConfig.DEFAULTS.saves().save(VanillaSave.KNOCKBACK), saves.save(VanillaSave.KNOCKBACK));
        assertEquals(List.of(), warnings);
    }

    @Test
    void negativeSaveValuesFallBackWithWarning() {
        ScoresConfig.SaveSettings saves = parse("{\"vanilla_saves\": {\"cooldown_ticks\": -1,"
                        + " \"poison\": {\"success_multiplier\": -0.5},"
                        + " \"knockback\": {\"min_strength\": -1}}}")
                .saves();

        assertEquals(ScoresConfig.DEFAULTS.saves(), saves);
        assertEquals(3, warnings.size());
    }

    @Test
    void readsTheTraitSettings() {
        ScoresConfig.TraitSettings traits = parse("{\"traits\": {\"recharge_ticks\": 12000,"
                        + " \"darkvision\": {\"strength\": 0.25}, \"reroll\": {\"enabled\": false}}}")
                .traits();

        assertEquals(12000, traits.rechargeTicks());
        assertEquals(0.25, traits.darkvisionStrength());
        assertFalse(traits.active(TraitEffectType.REROLL));
        assertTrue(traits.active(TraitEffectType.LAST_STAND));
        assertEquals(List.of(), warnings);
    }

    @Test
    void zeroDarkvisionStrengthTurnsDarkvisionOff() {
        ScoresConfig.TraitSettings traits =
                parse("{\"traits\": {\"darkvision\": {\"strength\": 0}}}").traits();
        assertFalse(traits.active(TraitEffectType.DARKVISION));
    }

    @Test
    void badTraitValuesFallBackWithWarning() {
        ScoresConfig.TraitSettings traits = parse(
                        "{\"traits\": {\"recharge_ticks\": -1," + " \"darkvision\": {\"strength\": 2}}}")
                .traits();
        assertEquals(ScoresConfig.DEFAULTS.traits(), traits);
        assertEquals(2, warnings.size());
    }

    @Test
    void theMasterSwitchTurnsEveryTraitEffectOff() {
        ScoresConfig.TraitSettings traits =
                parse("{\"traits\": {\"enabled\": false}}").traits();
        for (TraitEffectType type : TraitEffectType.values()) {
            assertFalse(traits.active(type), type.id());
        }
    }

    @Test
    void preSaveConfigLoadsWithTheSaveDefaults() {
        assertEquals(
                ScoresConfig.DEFAULTS.saves(), parse("{\"format_version\": 1}").saves());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void readsTheSocialSettings() {
        ScoresConfig.SocialSettings social = parse("{\"social\": {\"nearby_radius\": 8, \"gossip_radius\": 20,"
                        + " \"persuade\": {\"dc\": 15, \"barely_margin\": 3, \"cooldown_ticks\": 100,"
                        + " \"success\": {\"price_percent\": -25, \"price_ticks\": 500,"
                        + " \"gossip\": {\"type\": \"trading\", \"amount\": 4, \"nearby\": true}}},"
                        + " \"deceive\": {\"enabled\": false, \"piglin\": {\"disguise_ticks\": 20},"
                        + " \"wandering_trader\": {\"refuse_ticks\": 50, \"llamas_spit\": false}},"
                        + " \"reactions\": {\"enabled\": false, \"flee_distance\": 9,"
                        + " \"flee_ticks\": 40, \"flee_speed\": 1.2},"
                        + " \"witnesses\": {\"enabled\": false, \"refuse_fraction\": 0.25, \"flee_distance\": 4},"
                        + " \"golem_alarm\": {\"enabled\": false, \"radius\": 30, \"duration_ticks\": 600},"
                        + " \"on_guard\": {\"plead\": {\"dc\": 17}, \"lie\": {\"enabled\": false}},"
                        + " \"wary\": {\"enabled\": false, \"max_reputation\": -50},"
                        + " \"intimidate\": {\"failure\": {\"angers_golems\": false}},"
                        + " \"pickpocket\": {\"behind_degrees\": 90},"
                        + " \"passive_prices\": {\"charisma_percent_per_point\": 1.5,"
                        + " \"professions\": {\"minecraft:armorer\": {\"skill\": \"athletics\","
                        + " \"percent_per_point\": 3}}}}}")
                .social();

        assertEquals(List.of(), warnings);
        assertEquals(8.0, social.nearbyRadius());
        assertEquals(20.0, social.gossipRadius());
        assertEquals(new ScoresConfig.TraderDeceit(50, false), social.deceive().wanderingTrader());
        assertEquals(new ScoresConfig.ReactionSettings(false, 9, 40, 1.2), social.reactions());
        assertEquals(new ScoresConfig.WitnessSettings(false, 0.25, 4), social.witnesses());
        assertEquals(new ScoresConfig.GolemAlarmSettings(false, 30, 600), social.golemAlarm());
        assertEquals(
                new ScoresConfig.OnGuardSettings(
                        true, new ScoresConfig.DeEscalation(true, 17), new ScoresConfig.DeEscalation(false, 15)),
                social.onGuard());
        assertEquals(new ScoresConfig.WarySettings(false, -50), social.wary());
        assertFalse(social.active(SocialAction.LIE));
        assertEquals(15, social.persuade().dc());
        assertEquals(100, social.persuade().cooldownTicks());
        assertEquals(3, social.persuade().deal().barelyMargin());
        assertEquals(
                new SocialEffect(-25, 500, Optional.of(new SocialEffect.Gossip(GossipType.TRADING, 4, true)), 0, false),
                social.persuade().deal().effect(SocialOutcome.SUCCESS));
        assertEquals(
                ScoresConfig.DEFAULTS.social().persuade().deal().effect(SocialOutcome.FAILURE),
                social.persuade().deal().effect(SocialOutcome.FAILURE));
        assertFalse(social.active(SocialAction.DECEIVE));
        assertTrue(social.active(SocialAction.PERSUADE));
        assertEquals(20, social.deceive().piglin().disguiseTicks());
        assertFalse(social.intimidate().failure().angersGolems());
        assertEquals(48000, social.intimidate().failure().refuseTicks());
        assertEquals(90.0, social.pickpocket().behindDegrees());
        assertEquals(1.5, social.passivePrices().charismaPercentPerPoint());
        assertEquals(
                Map.of(
                        ResourceLocation.parse("minecraft:armorer"),
                        new ScoresConfig.ProfessionPrice(ResourceLocation.parse("athletics"), 3)),
                social.passivePrices().professions());
    }

    @Test
    void persuadeAndDeceiveCoolDownForOneDayByDefault() {
        ScoresConfig.SocialSettings social = parse("{}").social();
        assertEquals(24000, social.cooldownTicks(SocialAction.PERSUADE));
        assertEquals(24000, social.cooldownTicks(SocialAction.DECEIVE));
    }

    @Test
    void negativeOutcomesTellTheWholeVillageByDefault() {
        ScoresConfig.SocialSettings social = ScoresConfig.DEFAULTS.social();
        assertTrue(nearby(social.persuade().deal().effect(SocialOutcome.CRITICAL_FAILURE)));
        assertTrue(nearby(social.deceive().deal().effect(SocialOutcome.CRITICAL_FAILURE)));
        assertTrue(nearby(social.intimidate().failure()));
        assertTrue(nearby(social.pickpocket().failure()));
        assertFalse(nearby(social.persuade().deal().effect(SocialOutcome.CRITICAL_SUCCESS)));
        assertFalse(nearby(social.intimidate().success()));
        assertEquals(32.0, social.gossipRadius());
    }

    /**
     * Vanilla's golems attack a player a nearby villager rates at -100 or lower (DefendVillageTargetGoal):
     * one crime's gossip must stay above that, so only Checks' alarm sends golems, yet still make wary.
     */
    @Test
    void aSingleCrimesGossipStopsShortOfVanillasGolemsButMakesWary() {
        ScoresConfig.SocialSettings social = ScoresConfig.DEFAULTS.social();
        for (SocialEffect crime :
                List.of(social.pickpocket().failure(), social.intimidate().failure())) {
            SocialEffect.Gossip gossip = crime.gossip().orElseThrow();
            int reputation = gossip.amount() * gossip.type().weight;
            assertTrue(reputation > VANILLA_GOLEM_REPUTATION, "a single crime leaves " + reputation);
            assertTrue(reputation <= social.wary().maxReputation(), "a single crime leaves " + reputation);
        }
    }

    @Test
    void gossipStaysWithTheTargetUnlessMarkedNearby() {
        ScoresConfig.SocialSettings social = parse(
                        "{\"social\": {\"pickpocket\": {\"failure\":" + " {\"gossip\": {\"nearby\": false}}}}}")
                .social();
        assertEquals(
                Optional.of(new SocialEffect.Gossip(GossipType.MAJOR_NEGATIVE, 19, false)),
                social.pickpocket().failure().gossip());
        assertEquals(List.of(), warnings);
    }

    @Test
    void readsTheRollMessageSettings() {
        assertEquals(
                new ScoresConfig.RollMessageSettings(false, 100),
                parse("{\"roll_messages\": {\"enabled\": false, \"duration_ticks\": 100}}")
                        .rollMessages());
        assertEquals(List.of(), warnings);
        assertEquals(
                ScoresConfig.DEFAULTS.rollMessages(),
                parse("{\"roll_messages\": {\"duration_ticks\": -1}}").rollMessages());
        assertEquals(
                ScoresConfig.DEFAULTS.rollMessages(),
                parse("{\"roll_messages\": {\"duration_ticks\": 0}}").rollMessages());
        assertEquals(2, warnings.size());
    }

    @Test
    void preRollMessageConfigKeepsResultsUpForEightSeconds() {
        assertEquals(
                new ScoresConfig.RollMessageSettings(true, 240),
                parse("{\"format_version\": 1}").rollMessages());
    }

    @Test
    void readsTheFeelSettings() {
        assertEquals(
                new ScoresConfig.FeelSettings(10, false, new ScoresConfig.SpeechBubbleSettings(false, 40), false),
                parse("{\"social\": {\"feel\": {\"radius\": 10,"
                                + " \"pickpocket_animation\": {\"enabled\": false},"
                                + " \"speech_bubbles\": {\"enabled\": false, \"duration_ticks\": 40},"
                                + " \"voices\": {\"enabled\": false}}}}")
                        .social()
                        .feel());
        assertEquals(List.of(), warnings);
        assertEquals(
                ScoresConfig.DEFAULTS.social().feel(),
                parse("{\"social\": {\"feel\": {\"radius\": -1, \"speech_bubbles\": {\"duration_ticks\": 0}}}}")
                        .social()
                        .feel());
        assertEquals(2, warnings.size());
    }

    @Test
    void preFeelConfigLoadsWithEveryCosmeticOn() {
        assertEquals(
                new ScoresConfig.FeelSettings(32, true, new ScoresConfig.SpeechBubbleSettings(true, 200), true),
                parse("{\"format_version\": 1}").social().feel());
    }

    @Test
    void readsIntimidateAndCalmOnMobs() {
        ScoresConfig.SocialSettings social = parse("{\"social\": {\"intimidate_mob\": {\"enabled\": false,"
                        + " \"dc\": 14, \"cooldown_ticks\": 40, \"max_health\": 30, \"exclude_tag\": \"pack:brave\","
                        + " \"flee_ticks\": 60, \"flee_distance\": 8, \"flee_speed\": 1.5,"
                        + " \"speed_boost_ticks\": 20, \"speed_boost_amplifier\": 2},"
                        + " \"calm\": {\"dc\": 9, \"cooldown_ticks\": 0, \"tag\": \"pack:calm\"}}}")
                .social();
        assertEquals(
                new ScoresConfig.IntimidateMobSettings(
                        false, 14, 40, 30, ResourceLocation.parse("pack:brave"), 60, 8, 1.5, 20, 2),
                social.intimidateMob());
        assertEquals(new ScoresConfig.CalmSettings(true, 9, 0, ResourceLocation.parse("pack:calm")), social.calm());
        assertFalse(social.active(SocialAction.INTIMIDATE_MOB));
        assertTrue(social.active(SocialAction.CALM));
        assertEquals(0, social.cooldownTicks(SocialAction.CALM));
        assertEquals(List.of(), warnings);
    }

    @Test
    void aBadTagIdOrNegativeValueKeepsItsDefault() {
        ScoresConfig.SocialSettings social = parse("{\"social\": {\"intimidate_mob\": {\"exclude_tag\": \"Not An Id\","
                        + " \"max_health\": -1}, \"calm\": {\"tag\": \"!!\"}}}")
                .social();
        assertEquals(ScoresConfig.DEFAULTS.social().intimidateMob(), social.intimidateMob());
        assertEquals(ScoresConfig.DEFAULTS.social().calm(), social.calm());
        assertEquals(3, warnings.size());
    }

    @Test
    void readsInsightAndPerformance() {
        ScoresConfig.SocialSettings social = parse("{\"social\": {\"insight\": {"
                        + " \"sense_hostility\": {\"enabled\": false, \"dc\": 15, \"range\": 8},"
                        + " \"creeper_warning\": {\"dc\": 10}},"
                        + " \"performance\": {\"dc\": 11, \"cooldown_ticks\": 100, \"range\": 6,"
                        + " \"success\": {\"gossip\": {\"type\": \"trading\", \"amount\": 3}, \"piglin_ticks\": 20},"
                        + " \"critical_failure\": {\"gossip\": {\"amount\": 0}}}}}")
                .social();
        assertEquals(
                new ScoresConfig.InsightSettings(
                        new ScoresConfig.InsightSense(false, 15, 8), new ScoresConfig.InsightSense(true, 10, 16)),
                social.insight());
        ScoresConfig.PerformanceSettings performance = social.performance();
        assertEquals(11, performance.dc());
        assertEquals(100, performance.cooldownTicks());
        assertEquals(6.0, performance.range());
        assertEquals(
                new ScoresConfig.PerformanceEffect(
                        Optional.of(new SocialEffect.Gossip(GossipType.TRADING, 3, true)), 20),
                performance.success());
        assertEquals(ScoresConfig.DEFAULTS.social().performance().criticalSuccess(), performance.criticalSuccess());
        assertEquals(Optional.empty(), performance.criticalFailure().gossip());
        assertEquals(List.of(), warnings);
    }

    @Test
    void preM13bConfigLoadsWithMobsInsightAndPerformanceOn() {
        ScoresConfig.SocialSettings social =
                parse("{\"format_version\": 1, \"social\": {}}").social();
        assertTrue(social.active(SocialAction.INTIMIDATE_MOB));
        assertTrue(social.active(SocialAction.CALM));
        assertTrue(social.insight().senseHostility().enabled());
        assertTrue(social.insight().creeperWarning().enabled());
        assertTrue(social.performance().enabled());
        assertEquals(20.0, social.intimidateMob().maxHealth());
    }

    @Test
    void readsTheDeathMessageSettings() {
        assertEquals(
                new ScoresConfig.DeathMessageSettings(false, 200),
                parse("{\"death_messages\": {\"enabled\": false, \"window_ticks\": 200}}")
                        .deathMessages());
        assertEquals(List.of(), warnings);
        assertEquals(
                ScoresConfig.DEFAULTS.deathMessages(),
                parse("{\"death_messages\": {\"window_ticks\": -1}}").deathMessages());
        assertEquals(1, warnings.size());
    }

    @Test
    void readsTheExplorationSettings() {
        ScoresConfig.ExplorationSettings exploration = parse("{\"exploration\": {"
                        + " \"leap\": {\"enabled\": false, \"dc\": 14, \"cooldown_ticks\": 0, \"boost\": 0.3,"
                        + " \"min_drop\": 5},"
                        + " \"landing\": {\"base_dc\": 8, \"dc_per_damage\": 1, \"success_multiplier\": 0.25},"
                        + " \"cobwebs\": {\"enabled\": false, \"dc\": 15, \"retry_ticks\": 20},"
                        + " \"climbing\": {\"per_point\": 0.2, \"max_bonus\": 1},"
                        + " \"sneak\": {\"per_point\": 0.1, \"min_visibility\": 0.5},"
                        + " \"spot_tripwires\": {\"dc\": 15, \"range\": 4, \"interval_ticks\": 5},"
                        + " \"disarm_tripwires\": {\"enabled\": false, \"dc\": 18},"
                        + " \"search_chests\": {\"enabled\": false, \"dc\": 15},"
                        + " \"monster_lore\": {\"dc\": 14, \"range\": 8, \"retry_ticks\": 100, \"interval_ticks\": 40},"
                        + " \"structure_lore\": {\"enabled\": false, \"dc\": 11, \"retry_ticks\": 0, \"interval_ticks\": 5},"
                        + " \"taming\": {\"enabled\": false, \"dc\": 16},"
                        + " \"hunger\": {\"per_point\": 0.1, \"max_reduction\": 0.5},"
                        + " \"ailments\": {\"enabled\": false, \"per_point\": 0.02, \"max_reduction\": 1}}}")
                .exploration();
        assertEquals(List.of(), warnings);
        assertEquals(new ScoresConfig.LeapSettings(false, 14, 0, 0.3, 5), exploration.leap());
        assertEquals(new ScoresConfig.LandingSettings(true, 8, 1, 0.25), exploration.landing());
        assertEquals(new ScoresConfig.CobwebSettings(false, 15, 20), exploration.cobwebs());
        assertEquals(new ScoresConfig.ClimbingSettings(true, 0.2, 1), exploration.climbing());
        assertEquals(new ScoresConfig.SneakSettings(true, 0.1, 0.5), exploration.sneak());
        assertEquals(new ScoresConfig.SpotTripwireSettings(true, 15, 4, 5), exploration.spotTripwires());
        assertEquals(new ScoresConfig.DisarmTripwireSettings(false, 18), exploration.disarmTripwires());
        assertEquals(new ScoresConfig.SearchSettings(false, 15), exploration.searchChests());
        assertEquals(new ScoresConfig.MonsterLoreSettings(true, 14, 8, 100, 40), exploration.monsterLore());
        assertEquals(new ScoresConfig.StructureLoreSettings(false, 11, 0, 5), exploration.structureLore());
        assertEquals(new ScoresConfig.TamingSettings(false, 16), exploration.taming());
        assertEquals(new ScoresConfig.ReductionSettings(true, 0.1, 0.5), exploration.hunger());
        assertEquals(new ScoresConfig.ReductionSettings(false, 0.02, 1), exploration.ailments());
    }

    @Test
    void badExplorationValuesFallBackWithWarnings() {
        ScoresConfig.ExplorationSettings exploration = parse("{\"exploration\": {"
                        + " \"leap\": {\"boost\": -1, \"min_drop\": 0}, \"sneak\": {\"min_visibility\": 2},"
                        + " \"landing\": {\"dc_per_damage\": -0.5}, \"cobwebs\": {\"retry_ticks\": -1},"
                        + " \"climbing\": {\"max_bonus\": -1}, \"spot_tripwires\": {\"interval_ticks\": 0},"
                        + " \"monster_lore\": {\"range\": -1, \"retry_ticks\": -1, \"interval_ticks\": 0},"
                        + " \"structure_lore\": {\"retry_ticks\": -5, \"interval_ticks\": -1},"
                        + " \"hunger\": {\"per_point\": -0.1}, \"ailments\": {\"max_reduction\": 1.5}}}")
                .exploration();
        assertEquals(ScoresConfig.DEFAULTS.exploration(), exploration);
        assertEquals(14, warnings.size());
    }

    @Test
    void theLandingDcIsTenPlusHalfTheFallDamageRoundedDown() {
        ScoresConfig.LandingSettings landing =
                ScoresConfig.DEFAULTS.exploration().landing();
        assertEquals(10, landing.dc(1));
        assertEquals(13, landing.dc(6));
        assertEquals(13, landing.dc(7));
        assertEquals(20, landing.dc(20));
    }

    @Test
    void preExplorationConfigLoadsWithTheExplorationDefaults() {
        assertEquals(
                ScoresConfig.DEFAULTS.exploration(),
                parse("{\"format_version\": 1}").exploration());
        assertEquals(List.of(), warnings);
    }

    @Test
    void preDeathMessageConfigTellsStoriesForAMinute() {
        assertEquals(
                new ScoresConfig.DeathMessageSettings(true, 1200),
                parse("{\"format_version\": 1}").deathMessages());
    }

    private static boolean nearby(SocialEffect effect) {
        return effect.gossip().map(SocialEffect.Gossip::nearby).orElse(false);
    }

    @Test
    void zeroGossipSpreadsNone() {
        ScoresConfig.SocialSettings social = parse(
                        "{\"social\": {\"persuade\": {\"critical_success\": {\"gossip\": {\"amount\": 0}}}}}")
                .social();
        assertEquals(
                Optional.empty(),
                social.persuade().deal().effect(SocialOutcome.CRITICAL_SUCCESS).gossip());
        assertEquals(List.of(), warnings);
    }

    @Test
    void badSocialValuesFallBackWithWarning() {
        ScoresConfig.SocialSettings social = parse("{\"social\": {\"nearby_radius\": -1,"
                        + " \"persuade\": {\"cooldown_ticks\": -5,"
                        + " \"critical_failure\": {\"gossip\": {\"type\": \"rumour\"}}},"
                        + " \"pickpocket\": {\"behind_degrees\": 200},"
                        + " \"witnesses\": {\"refuse_fraction\": -0.5},"
                        + " \"golem_alarm\": {\"radius\": -1, \"duration_ticks\": -20},"
                        + " \"passive_prices\": {\"professions\": {\"minecraft:cleric\": {}}}}}")
                .social();

        ScoresConfig.SocialSettings defaults = ScoresConfig.DEFAULTS.social();
        assertEquals(defaults.nearbyRadius(), social.nearbyRadius());
        assertEquals(defaults.persuade(), social.persuade());
        assertEquals(defaults.pickpocket(), social.pickpocket());
        assertEquals(defaults.witnesses(), social.witnesses());
        assertEquals(defaults.golemAlarm(), social.golemAlarm());
        assertEquals(Map.of(), social.passivePrices().professions());
        assertEquals(8, warnings.size());
    }

    @Test
    void theMasterSwitchTurnsEverySocialActionOff() {
        ScoresConfig.SocialSettings social =
                parse("{\"social\": {\"enabled\": false}}").social();
        for (SocialAction action : SocialAction.values()) {
            assertFalse(social.active(action), action.id());
        }
    }

    @Test
    void preSocialConfigLoadsWithTheSocialDefaults() {
        assertEquals(
                ScoresConfig.DEFAULTS.social(), parse("{\"format_version\": 1}").social());
        assertTrue(warnings.isEmpty());
    }

    private static String decreasingThresholds() {
        List<String> values = new ArrayList<>();
        for (int level = 1; level <= 20; level++) {
            values.add(Integer.toString(level == 20 ? 5 : (level - 1) * 100));
        }
        return "[" + String.join(", ", values) + "]";
    }
}
