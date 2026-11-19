package studio.modroll.checks.data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.gossip.GossipType;
import studio.modroll.checks.Checks;
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

public record ScoresConfig(
        Map<Ability, Integer> playerDefaults,
        Derivation derivation,
        boolean profilesEnabled,
        boolean skillsEnabled,
        ProficiencySettings proficiency,
        CritfallSettings critfall,
        boolean statScreenEnabled,
        CreationSettings creation,
        LevellingSettings levelling,
        ExtensionSettings extensions,
        BodySettings body,
        SaveSettings saves,
        TraitSettings traits,
        SocialSettings social,
        RollMessageSettings rollMessages,
        DeathMessageSettings deathMessages,
        ExplorationSettings exploration,
        RestSettings rest,
        PlayerRollSettings playerRolls) {

    public static final int FORMAT_VERSION = 2;

    public record Derivation(
            boolean enabled,
            double strengthBase,
            double strengthPerAttackDamage,
            double dexterityBase,
            double dexterityPerSpeed,
            double constitutionBase,
            double constitutionPerHealth,
            int defaultMentalScore) {}

    public record ProficiencySettings(boolean enabled, int defaultBonus) {}

    public record CritfallSettings(boolean enabled, boolean unprofiledMobs, Map<String, Ability> saveAbilities) {}

    public record CreationSettings(
            boolean enabled,
            List<CreationMethod> methods,
            int skillChoices,
            List<Integer> standardArray,
            PointBuy pointBuy,
            DiceExpression rollDice,
            DiceExpression hardcoreDice,
            PresetSettings presets) {

        public DiceExpression dice(CreationMethod method) {
            return switch (method) {
                case ROLL -> rollDice;
                case HARDCORE -> hardcoreDice;
                case STANDARD_ARRAY, POINT_BUY -> throw new IllegalArgumentException(method.id() + " rolls no dice");
            };
        }
    }

    public record PresetSettings(
            boolean enabled, boolean includeShipped, List<List<Integer>> bonusOptions, int bonusMaxScore) {

        public PresetSettings {
            bonusOptions = bonusOptions.stream().map(List::copyOf).toList();
        }
    }

    public record LevellingSettings(
            boolean enabled,
            List<Integer> xpThresholds,
            List<Integer> proficiencyBonuses,
            XpSources xpSources,
            ImprovementSettings improvements,
            ExpertiseSettings expertise,
            ExtraAttackSettings extraAttack) {

        public LevellingSettings {
            xpThresholds = List.copyOf(xpThresholds);
            proficiencyBonuses = List.copyOf(proficiencyBonuses);
        }
    }

    public record XpSources(
            boolean vanillaXpEnabled, int xpPerPoint, boolean advancementsEnabled, int xpPerAdvancement) {}

    public record ImprovementSettings(
            List<Integer> levels,
            Map<ResourceLocation, List<Integer>> classLevels,
            List<List<Integer>> options,
            int maxScore) {

        public ImprovementSettings {
            levels = List.copyOf(levels);
            classLevels = copyOfLists(classLevels);
            options = options.stream().map(List::copyOf).toList();
        }

        public List<Integer> levelsFor(Optional<ResourceLocation> classId) {
            List<Integer> all = new ArrayList<>(levels);
            classId.map(id -> classLevels.getOrDefault(id, List.of())).ifPresent(all::addAll);
            return all;
        }
    }

    public record ExpertiseSettings(
            boolean enabled, int skillsPerLevel, Map<ResourceLocation, List<Integer>> classLevels) {

        public ExpertiseSettings {
            classLevels = copyOfLists(classLevels);
        }

        public List<Integer> levelsFor(Optional<ResourceLocation> classId) {
            return enabled
                    ? classId.map(id -> classLevels.getOrDefault(id, List.of())).orElse(List.of())
                    : List.of();
        }
    }

    public record ExtraAttackSettings(boolean enabled, Map<ResourceLocation, List<Integer>> classLevels) {

        public ExtraAttackSettings {
            classLevels = copyOfLists(classLevels);
        }

        public List<Integer> levelsFor(Optional<ResourceLocation> classId) {
            return enabled
                    ? classId.map(id -> classLevels.getOrDefault(id, List.of())).orElse(List.of())
                    : List.of();
        }
    }

    private static Map<ResourceLocation, List<Integer>> copyOfLists(Map<ResourceLocation, List<Integer>> lists) {
        Map<ResourceLocation, List<Integer>> copy = new HashMap<>();
        lists.forEach((id, levels) -> copy.put(id, List.copyOf(levels)));
        return Map.copyOf(copy);
    }

    public record ExtensionSettings(
            boolean events,
            BonusSourceSettings bonusSources,
            boolean triggers,
            boolean checkCommand,
            boolean sheetSections,
            boolean levelUpChoices) {}

    public record BonusSourceSettings(boolean enabled, int passiveAdvantage) {}

    public record BodySettings(
            HealthSettings health,
            ArmorClassSettings armorClass,
            boolean sizeEnabled,
            Map<Extra, ExtraSetting> extras) {

        public BodySettings {
            extras = Map.copyOf(extras);
        }

        public ExtraSetting extra(Extra extra) {
            return extras.get(extra);
        }
    }

    public record HealthSettings(
            boolean enabled,
            double perConPoint,
            boolean perLevelEnabled,
            double perLevelMultiplier,
            boolean conPerLevel,
            int minPerLevel,
            int customHitDie,
            boolean wholeHearts) {}

    public record ArmorClassSettings(boolean enabled, int mediumMaxDex) {}

    public record ExtraSetting(boolean enabled, double perPoint) {}

    public record SaveSettings(
            boolean profiledMobs, int cooldownTicks, double knockbackMinStrength, Map<VanillaSave, SaveSetting> saves) {

        public SaveSettings {
            saves = Map.copyOf(saves);
        }

        public SaveSetting save(VanillaSave save) {
            return saves.get(save);
        }
    }

    public record SaveSetting(boolean enabled, int dc, double successMultiplier) {}

    public record TraitSettings(
            boolean enabled, int rechargeTicks, double darkvisionStrength, Map<TraitEffectType, Boolean> effects) {

        public TraitSettings {
            effects = Map.copyOf(effects);
        }

        public boolean active(TraitEffectType type) {
            return enabled && effects.get(type) && (type != TraitEffectType.DARKVISION || darkvisionStrength > 0);
        }
    }

    public record RollMessageSettings(boolean enabled, int durationTicks) {}

    public record PlayerRollSettings(boolean enabled, double radius) {}

    public record DeathMessageSettings(boolean enabled, int windowTicks) {}

    public record SocialSettings(
            boolean enabled,
            double nearbyRadius,
            double gossipRadius,
            PersuadeSettings persuade,
            DeceiveSettings deceive,
            IntimidateSettings intimidate,
            PickpocketSettings pickpocket,
            PassivePriceSettings passivePrices,
            ReactionSettings reactions,
            WitnessSettings witnesses,
            GolemAlarmSettings golemAlarm,
            OnGuardSettings onGuard,
            WarySettings wary,
            FeelSettings feel,
            IntimidateMobSettings intimidateMob,
            CalmSettings calm,
            InsightSettings insight,
            PerformanceSettings performance) {

        public boolean active(SocialAction action) {
            return enabled
                    && switch (action) {
                        case PERSUADE -> persuade.enabled();
                        case DECEIVE -> deceive.enabled();
                        case PLEAD -> onGuard.enabled() && onGuard.plead().enabled();
                        case LIE -> onGuard.enabled() && onGuard.lie().enabled();
                        case INTIMIDATE -> intimidate.enabled();
                        case PICKPOCKET -> pickpocket.enabled();
                        case INTIMIDATE_MOB -> intimidateMob.enabled();
                        case CALM -> calm.enabled();
                    };
        }

        public int cooldownTicks(SocialAction action) {
            return switch (action) {
                case PERSUADE -> persuade.cooldownTicks();
                case DECEIVE -> deceive.cooldownTicks();
                case PLEAD, LIE -> 0;
                case INTIMIDATE -> intimidate.cooldownTicks();
                case PICKPOCKET -> pickpocket.cooldownTicks();
                case INTIMIDATE_MOB -> intimidateMob.cooldownTicks();
                case CALM -> calm.cooldownTicks();
            };
        }
    }

    public record Deal(int barelyMargin, Map<SocialOutcome, SocialEffect> outcomes) {

        public Deal {
            outcomes = Map.copyOf(outcomes);
        }

        public SocialEffect effect(SocialOutcome outcome) {
            return outcomes.get(outcome);
        }
    }

    public record PersuadeSettings(boolean enabled, int dc, int cooldownTicks, Deal deal) {}

    public record DeceiveSettings(
            boolean enabled, int cooldownTicks, Deal deal, PiglinDeceit piglin, TraderDeceit wanderingTrader) {}

    public record TraderDeceit(int refuseTicks, boolean llamasSpit) {}

    public record PiglinDeceit(int disguiseTicks, int criticalDisguiseTicks, int angerTicks) {}

    public record IntimidateSettings(
            boolean enabled,
            int dc,
            int cooldownTicks,
            SocialEffect success,
            SocialEffect failure,
            int piglinAngerTicks) {}

    public record PickpocketSettings(boolean enabled, int cooldownTicks, double behindDegrees, SocialEffect failure) {}

    public record PassivePriceSettings(
            boolean enabled, double charismaPercentPerPoint, Map<ResourceLocation, ProfessionPrice> professions) {

        public PassivePriceSettings {
            professions = Map.copyOf(professions);
        }
    }

    public record ProfessionPrice(ResourceLocation skill, double percentPerPoint) {}

    public record ReactionSettings(boolean enabled, double fleeDistance, int fleeTicks, double fleeSpeed) {}

    public record WitnessSettings(boolean enabled, double refuseFraction, double fleeDistance) {}

    public record GolemAlarmSettings(boolean enabled, double radius, int durationTicks) {}

    public record OnGuardSettings(boolean enabled, DeEscalation plead, DeEscalation lie) {}

    public record DeEscalation(boolean enabled, int dc) {}

    public record WarySettings(boolean enabled, int maxReputation) {}

    public record FeelSettings(
            double radius, boolean pickpocketAnimation, SpeechBubbleSettings speechBubbles, boolean voices) {}

    public record SpeechBubbleSettings(boolean enabled, int durationTicks) {}

    public record IntimidateMobSettings(
            boolean enabled,
            int dc,
            int cooldownTicks,
            double maxHealth,
            ResourceLocation excludeTag,
            int fleeTicks,
            double fleeDistance,
            double fleeSpeed,
            int speedBoostTicks,
            int speedBoostAmplifier) {}

    public record CalmSettings(boolean enabled, int dc, int cooldownTicks, ResourceLocation tag) {}

    public record InsightSettings(InsightSense senseHostility, InsightSense creeperWarning) {}

    public record InsightSense(boolean enabled, int dc, double range) {}

    public record PerformanceSettings(
            boolean enabled,
            int dc,
            int cooldownTicks,
            double range,
            PerformanceEffect success,
            PerformanceEffect criticalSuccess,
            PerformanceEffect criticalFailure) {}

    public record PerformanceEffect(Optional<SocialEffect.Gossip> gossip, int piglinTicks) {}

    public record ExplorationSettings(
            LeapSettings leap,
            LandingSettings landing,
            CobwebSettings cobwebs,
            ClimbingSettings climbing,
            SneakSettings sneak,
            SpotTripwireSettings spotTripwires,
            DisarmTripwireSettings disarmTripwires,
            SearchSettings searchChests,
            MonsterLoreSettings monsterLore,
            StructureLoreSettings structureLore,
            TamingSettings taming,
            ReductionSettings hunger,
            ReductionSettings ailments) {}

    public record LeapSettings(boolean enabled, int dc, double boost, int minDrop) {}

    public record LandingSettings(boolean enabled, int baseDc, double dcPerDamage, double successMultiplier) {

        public int dc(float damage) {
            return baseDc + (int) Math.floor(damage * dcPerDamage);
        }
    }

    public record CobwebSettings(boolean enabled, int dc, int retryTicks) {}

    public record ClimbingSettings(boolean enabled, double perPoint, double maxBonus) {}

    public record SneakSettings(boolean enabled, double perPoint, double minVisibility) {}

    public record SpotTripwireSettings(boolean enabled, int dc, int range, int intervalTicks) {}

    public record DisarmTripwireSettings(boolean enabled, int dc) {}

    public record SearchSettings(boolean enabled, int dc) {}

    public record MonsterLoreSettings(boolean enabled, int dc, double range, int retryTicks, int intervalTicks) {}

    public record StructureLoreSettings(boolean enabled, int dc, int retryTicks, int intervalTicks) {}

    public record TamingSettings(boolean enabled, int dc) {}

    public record ReductionSettings(boolean enabled, double perPoint, double maxReduction) {}

    public record RestSettings(HitDiceSettings hitDice, ShortRestSettings shortRest, LongRestSettings longRest) {}

    public record HitDiceSettings(boolean enabled, double healMultiplier, int minHeal) {}

    public record ShortRestSettings(
            boolean enabled, int ticks, double campfireRadius, double monsterRange, double monsterHeight) {}

    public record LongRestSettings(
            boolean enabled, double regainFraction, int minRegain, boolean healToFull, boolean rechargesTraits) {}

    public int playerDefault(Ability ability) {
        return playerDefaults.get(ability);
    }

    public static final ScoresConfig DEFAULTS = new ScoresConfig(
            defaultPlayerScores(),
            new Derivation(true, 8.0, 1.0, 6.0, 24.0, 8.0, 0.2, 10),
            true,
            true,
            new ProficiencySettings(true, 2),
            new CritfallSettings(
                    true,
                    false,
                    Map.of(
                            ModifierProvider.SPELL_SAVE, Ability.DEXTERITY,
                            ModifierProvider.STRENGTH_SAVE, Ability.STRENGTH,
                            ModifierProvider.DEXTERITY_SAVE, Ability.DEXTERITY,
                            ModifierProvider.CONSTITUTION_SAVE, Ability.CONSTITUTION,
                            ModifierProvider.INTELLIGENCE_SAVE, Ability.INTELLIGENCE,
                            ModifierProvider.WISDOM_SAVE, Ability.WISDOM,
                            ModifierProvider.CHARISMA_SAVE, Ability.CHARISMA)),
            true,
            new CreationSettings(
                    true,
                    List.of(CreationMethod.values()),
                    2,
                    List.of(15, 14, 13, 12, 10, 8),
                    new PointBuy(27, Map.of(8, 0, 9, 1, 10, 2, 11, 3, 12, 4, 13, 5, 14, 7, 15, 9)),
                    DiceExpression.parse("4d6kh3"),
                    DiceExpression.parse("3d6"),
                    new PresetSettings(true, true, List.of(List.of(2, 1), List.of(1, 1, 1)), 20)),
            new LevellingSettings(
                    true,
                    List.of(
                            0, 30, 90, 270, 650, 1400, 2300, 3400, 4800, 6400, 8500, 10000, 12000, 14000, 16500, 19500,
                            22500, 26500, 30500, 35500),
                    List.of(2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6),
                    new XpSources(true, 1, true, 25),
                    new ImprovementSettings(
                            List.of(4, 8, 12, 16, 19),
                            Map.of(Checks.id("fighter"), List.of(6, 14), Checks.id("rogue"), List.of(10)),
                            List.of(List.of(2), List.of(1, 1)),
                            20),
                    new ExpertiseSettings(
                            true, 2, Map.of(Checks.id("rogue"), List.of(1, 6), Checks.id("bard"), List.of(3, 10))),
                    new ExtraAttackSettings(
                            true,
                            Map.of(
                                    Checks.id("fighter"), List.of(5, 11, 20),
                                    Checks.id("barbarian"), List.of(5),
                                    Checks.id("monk"), List.of(5),
                                    Checks.id("paladin"), List.of(5),
                                    Checks.id("ranger"), List.of(5)))),
            new ExtensionSettings(true, new BonusSourceSettings(true, 5), true, true, true, true),
            new BodySettings(
                    new HealthSettings(true, 2.0, true, 0.2, true, 1, 8, true),
                    new ArmorClassSettings(true, 2),
                    true,
                    defaultExtras()),
            new SaveSettings(false, 20, 0.7, defaultSaves()),
            new TraitSettings(true, 24000, 0.4, allTraitEffects()),
            new SocialSettings(
                    true,
                    16.0,
                    32.0,
                    new PersuadeSettings(true, 12, 24000, defaultDeal()),
                    new DeceiveSettings(
                            true,
                            24000,
                            defaultDeal(),
                            new PiglinDeceit(2400, 6000, 600),
                            new TraderDeceit(12000, true)),
                    new IntimidateSettings(
                            true,
                            13,
                            72000,
                            new SocialEffect(15, 24000, gossip(GossipType.MINOR_NEGATIVE, 20, false), 0, false),
                            new SocialEffect(0, 0, gossip(GossipType.MAJOR_NEGATIVE, 19, true), 48000, true),
                            600),
                    new PickpocketSettings(
                            true,
                            72000,
                            60,
                            new SocialEffect(0, 0, gossip(GossipType.MAJOR_NEGATIVE, 19, true), 48000, true)),
                    new PassivePriceSettings(true, 2, defaultProfessionPrices()),
                    new ReactionSettings(true, 12, 100, 0.75),
                    new WitnessSettings(true, 0.5, 6),
                    new GolemAlarmSettings(true, 48, 2400),
                    new OnGuardSettings(true, new DeEscalation(true, 15), new DeEscalation(true, 15)),
                    new WarySettings(true, -95),
                    new FeelSettings(32, true, new SpeechBubbleSettings(true, 200), true),
                    new IntimidateMobSettings(
                            true, 12, 1200, 20, Checks.id("intimidation_immune"), 200, 16, 1.25, 100, 1),
                    new CalmSettings(true, 13, 1200, Checks.id("calmable")),
                    new InsightSettings(new InsightSense(true, 12, 16), new InsightSense(true, 12, 16)),
                    new PerformanceSettings(
                            true,
                            12,
                            12000,
                            16,
                            new PerformanceEffect(gossip(GossipType.MINOR_POSITIVE, 5, true), 1200),
                            new PerformanceEffect(gossip(GossipType.MINOR_POSITIVE, 10, true), 3600),
                            new PerformanceEffect(gossip(GossipType.MINOR_NEGATIVE, 5, true), 0))),
            new RollMessageSettings(true, 240),
            new DeathMessageSettings(true, 1200),
            new ExplorationSettings(
                    new LeapSettings(true, 12, 0.15, 3),
                    new LandingSettings(true, 10, 0.5, 0.5),
                    new CobwebSettings(true, 12, 60),
                    new ClimbingSettings(true, 0.1, 0.5),
                    new SneakSettings(true, 0.05, 0.25),
                    new SpotTripwireSettings(true, 13, 8, 20),
                    new DisarmTripwireSettings(true, 13),
                    new SearchSettings(true, 13),
                    new MonsterLoreSettings(true, 12, 16, 6000, 20),
                    new StructureLoreSettings(true, 12, 6000, 20),
                    new TamingSettings(true, 12),
                    new ReductionSettings(true, 0.05, 0.25),
                    new ReductionSettings(true, 0.05, 0.25)),
            new RestSettings(
                    new HitDiceSettings(true, 1.0, 1),
                    new ShortRestSettings(true, 1000, 3, 8, 5),
                    new LongRestSettings(true, 1.0, 1, true, true)),
            new PlayerRollSettings(true, 16.0));

    private static Deal defaultDeal() {
        return new Deal(
                2,
                Map.of(
                        SocialOutcome.CRITICAL_FAILURE,
                                new SocialEffect(30, 48000, gossip(GossipType.MINOR_NEGATIVE, 25, true), 0, false),
                        SocialOutcome.FAILURE, new SocialEffect(15, 12000, Optional.empty(), 0, false),
                        SocialOutcome.BARELY, new SocialEffect(-10, 12000, Optional.empty(), 0, false),
                        SocialOutcome.SUCCESS, new SocialEffect(-20, 12000, Optional.empty(), 0, false),
                        SocialOutcome.CRITICAL_SUCCESS,
                                new SocialEffect(-35, 24000, gossip(GossipType.MINOR_POSITIVE, 10, false), 0, false)));
    }

    private static Optional<SocialEffect.Gossip> gossip(GossipType type, int amount, boolean nearby) {
        return Optional.of(new SocialEffect.Gossip(type, amount, nearby));
    }

    private static Map<ResourceLocation, ProfessionPrice> defaultProfessionPrices() {
        ProfessionPrice religion = new ProfessionPrice(Checks.id("religion"), 2);
        ProfessionPrice history = new ProfessionPrice(Checks.id("history"), 2);
        return Map.of(
                ResourceLocation.withDefaultNamespace("cleric"), religion,
                ResourceLocation.withDefaultNamespace("librarian"), history,
                ResourceLocation.withDefaultNamespace("cartographer"), history);
    }

    private static Map<TraitEffectType, Boolean> allTraitEffects() {
        Map<TraitEffectType, Boolean> effects = new EnumMap<>(TraitEffectType.class);
        for (TraitEffectType type : TraitEffectType.values()) {
            effects.put(type, true);
        }
        return effects;
    }

    private static Map<VanillaSave, SaveSetting> defaultSaves() {
        return Map.of(
                VanillaSave.EXPLOSION, new SaveSetting(true, 13, 0.5),
                VanillaSave.POISON, new SaveSetting(true, 12, 0.5),
                VanillaSave.KNOCKBACK, new SaveSetting(true, 13, 0.5),
                VanillaSave.FIRE, new SaveSetting(true, 12, 0.0),
                VanillaSave.DARKNESS, new SaveSetting(true, 13, 0.5));
    }

    private static Map<Extra, ExtraSetting> defaultExtras() {
        return Map.of(
                Extra.KNOCKBACK, new ExtraSetting(true, 0.1),
                Extra.MINING_SPEED, new ExtraSetting(true, 0.05),
                Extra.BOW_DRAW, new ExtraSetting(true, 0.1),
                Extra.CROSSBOW_RELOAD, new ExtraSetting(true, 0.1),
                Extra.BREATH, new ExtraSetting(true, 0.2),
                Extra.EXHAUSTION, new ExtraSetting(true, 0.05));
    }

    private static Map<Ability, Integer> defaultPlayerScores() {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            scores.put(ability, 10);
        }
        return Map.copyOf(scores);
    }
}
