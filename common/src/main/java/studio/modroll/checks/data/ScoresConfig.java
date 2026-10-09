package studio.modroll.checks.data;

import java.util.EnumMap;
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
import studio.modroll.checks.level.Levels;
import studio.modroll.checks.save.VanillaSave;
import studio.modroll.checks.social.SocialAction;
import studio.modroll.checks.social.SocialEffect;
import studio.modroll.checks.social.SocialOutcome;
import studio.modroll.checks.trait.TraitEffectType;
import studio.modroll.critfall.api.ModifierProvider;
import studio.modroll.critfall.api.dice.DiceExpression;

/** The contents of {@code config/checks/scores.json}. */
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
        ExplorationSettings exploration) {

    public static final int FORMAT_VERSION = 1;

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

    /** {@code saveAbilities} maps a Critfall save key to the ability whose save modifier it uses. */
    public record CritfallSettings(boolean enabled, boolean unprofiledMobs, Map<String, Ability> saveAbilities) {}

    /** {@code rollDice} serves the roll method, {@code hardcoreDice} the hardcore one; one roll per ability. */
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

    /**
     * Species, backgrounds and classes at creation. {@code includeShipped} offers the presets Checks
     * ships; {@code bonusOptions} are the ways a background's ability bonuses may be split (e.g. +2/+1
     * or +1/+1/+1), and no bonus may raise a score above {@code bonusMaxScore}.
     */
    public record PresetSettings(
            boolean enabled, boolean includeShipped, List<List<Integer>> bonusOptions, int bonusMaxScore) {

        public PresetSettings {
            bonusOptions = bonusOptions.stream().map(List::copyOf).toList();
        }
    }

    /**
     * Character levels 1 to {@link Levels#MAX_LEVEL}. {@code xpThresholds} is the total XP each level
     * needs and {@code proficiencyBonuses} the proficiency bonus at each level, level 1 first.
     */
    public record LevellingSettings(
            boolean enabled,
            List<Integer> xpThresholds,
            List<Integer> proficiencyBonuses,
            XpSources xpSources,
            ImprovementSettings improvements) {

        public LevellingSettings {
            xpThresholds = List.copyOf(xpThresholds);
            proficiencyBonuses = List.copyOf(proficiencyBonuses);
        }
    }

    /** Character XP per vanilla XP point gained and per advancement earned, each with its own switch. */
    public record XpSources(
            boolean vanillaXpEnabled, int xpPerPoint, boolean advancementsEnabled, int xpPerAdvancement) {}

    /**
     * Ability score improvements: one is earned at each of {@code levels}; {@code options} are the
     * allowed splits (e.g. +2 or +1/+1), and no improvement may raise a score above {@code maxScore}.
     */
    public record ImprovementSettings(List<Integer> levels, List<List<Integer>> options, int maxScore) {

        public ImprovementSettings {
            levels = List.copyOf(levels);
            options = options.stream().map(List::copyOf).toList();
        }
    }

    /**
     * The switches for what other mods and packs build on Checks: check events, datapack bonus sources and
     * check triggers, {@code /checks check}, and stat screen sections added by other mods.
     */
    public record ExtensionSettings(
            boolean events,
            BonusSourceSettings bonusSources,
            boolean triggers,
            boolean checkCommand,
            boolean sheetSections) {}

    /** {@code passiveAdvantage} is what advantage adds to a passive score and disadvantage takes away. */
    public record BonusSourceSettings(boolean enabled, int passiveAdvantage) {}

    /** Health, armor class, size and the attribute extras, each with its own switch. */
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

    /**
     * Max health from the CON modifier, {@code perConPoint} hit points per point, and optionally from
     * the hit dice recorded at each level gained, scaled by {@code perLevelMultiplier}.
     */
    public record HealthSettings(
            boolean enabled, double perConPoint, boolean perLevelEnabled, double perLevelMultiplier) {}

    /** DEX added to AC, capped at {@code mediumMaxDex} in medium armor. */
    public record ArmorClassSettings(boolean enabled, int mediumMaxDex) {}

    /** One attribute extra: {@code perPoint} per point of its ability's modifier. */
    public record ExtraSetting(boolean enabled, double perPoint) {}

    /**
     * Saving throws against vanilla hazards. Players save, and mobs with an entity profile when
     * {@code profiledMobs} is on. Each kind of save rolls at most once per entity per {@code cooldownTicks};
     * a trigger in between reuses the last result. Knockback up to {@code knockbackMinStrength} never
     * calls for a save.
     */
    public record SaveSettings(
            boolean profiledMobs, int cooldownTicks, double knockbackMinStrength, Map<VanillaSave, SaveSetting> saves) {

        public SaveSettings {
            saves = Map.copyOf(saves);
        }

        public SaveSetting save(VanillaSave save) {
            return saves.get(save);
        }
    }

    /** One vanilla save: its DC, and what a made save multiplies the damage, duration or push by. */
    public record SaveSetting(boolean enabled, int dc, double successMultiplier) {}

    /**
     * Species traits. Once-a-day effects come back {@code rechargeTicks} after their last use; darkvision
     * shows darkness as dim light at {@code darkvisionStrength}, 0 for none.
     */
    public record TraitSettings(
            boolean enabled, int rechargeTicks, double darkvisionStrength, Map<TraitEffectType, Boolean> effects) {

        public TraitSettings {
            effects = Map.copyOf(effects);
        }

        /** Whether traits, and effects of this type, are on. */
        public boolean active(TraitEffectType type) {
            return enabled && effects.get(type) && (type != TraitEffectType.DARKVISION || darkvisionStrength > 0);
        }
    }

    /**
     * How long a client with Checks keeps a roll result on the action bar, in ticks; while off, or on a client
     * without Checks, it shows for vanilla's three seconds.
     */
    public record RollMessageSettings(boolean enabled, int durationTicks) {}

    /**
     * Story death messages: a player who dies within {@code windowTicks} of a related Checks event (caught
     * pickpocketing, a failed threat, a failed DEX save against an explosion, a spent last stand) gets one in
     * place of vanilla's.
     */
    public record DeathMessageSettings(boolean enabled, int windowTicks) {}

    /**
     * Social skill uses on villagers, wandering traders and piglins, from the social menu, plus the passive
     * price shifts. {@code nearbyRadius} is how far around the target iron golems and piglins join in;
     * {@code gossipRadius} how far gossip marked {@code nearby} reaches, and how far off a witness can be.
     */
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

        /** Whether social uses, and this action, are on. */
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

        /** Plead and Lie have none: each may be tried once per alarm instead. */
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

    /** A haggle: each outcome's effect on the trader; a check that makes the DC by at most {@code barelyMargin} barely succeeds. */
    public record Deal(int barelyMargin, Map<SocialOutcome, SocialEffect> outcomes) {

        public Deal {
            outcomes = Map.copyOf(outcomes);
        }

        public SocialEffect effect(SocialOutcome outcome) {
            return outcomes.get(outcome);
        }
    }

    public record PersuadeSettings(boolean enabled, int dc, int cooldownTicks, Deal deal) {}

    /** Against the target's passive Insight: a haggle with traders, a disguise among piglins. */
    public record DeceiveSettings(
            boolean enabled, int cooldownTicks, Deal deal, PiglinDeceit piglin, TraderDeceit wanderingTrader) {}

    /** A wandering trader who catches the lie refuses the player for {@code refuseTicks}; his llamas may spit. */
    public record TraderDeceit(int refuseTicks, boolean llamasSpit) {}

    /** Piglins take the player for a gold wearer for {@code disguiseTicks}, longer on a natural 20, or turn hostile. */
    public record PiglinDeceit(int disguiseTicks, int criticalDisguiseTicks, int angerTicks) {}

    public record IntimidateSettings(
            boolean enabled,
            int dc,
            int cooldownTicks,
            SocialEffect success,
            SocialEffect failure,
            int piglinAngerTicks) {}

    /** Against the target's passive Perception, with advantage within {@code behindDegrees} of straight behind it. */
    public record PickpocketSettings(boolean enabled, int cooldownTicks, double behindDegrees, SocialEffect failure) {}

    /**
     * Villager prices shift by {@code charismaPercentPerPoint} per point of the player's CHA modifier, and
     * for each listed profession by the listed skill's modifier; a positive modifier lowers prices.
     */
    public record PassivePriceSettings(
            boolean enabled, double charismaPercentPerPoint, Map<ResourceLocation, ProfessionPrice> professions) {

        public PassivePriceSettings {
            professions = Map.copyOf(professions);
        }
    }

    public record ProfessionPrice(ResourceLocation skill, double percentPerPoint) {}

    /**
     * How targets show what they think: particles, sounds, head shakes and running away. A fleeing target
     * runs {@code fleeDistance} blocks from the player at {@code fleeSpeed} times its walking speed, and
     * keeps away from them for up to {@code fleeTicks}.
     */
    public record ReactionSettings(boolean enabled, double fleeDistance, int fleeTicks, double fleeSpeed) {}

    /**
     * Villagers within the gossip radius of a caught pickpocket's or a failed threat's target that can see
     * the player or the target: each refuses the player for {@code refuseFraction} of the target's refusal
     * and runs {@code fleeDistance} blocks from them, as a fleeing target does.
     */
    public record WitnessSettings(boolean enabled, double refuseFraction, double fleeDistance) {}

    /**
     * Once a golem turns on a player over a Checks crime, every golem within {@code radius} blocks of a golem
     * after them joins in, for {@code durationTicks} or until the player dies.
     */
    public record GolemAlarmSettings(boolean enabled, double radius, int durationTicks) {}

    /**
     * While a golem alarm is on a player, villagers within its radius of the crime are on guard: Pickpocket
     * and Intimidate are off against them, and Persuade and Deceive give way to {@code plead} and
     * {@code lie}, each tried once per alarm.
     */
    public record OnGuardSettings(boolean enabled, DeEscalation plead, DeEscalation lie) {}

    /** A way to talk down an alarm, rolled against {@code dc}. */
    public record DeEscalation(boolean enabled, int dc) {}

    /**
     * A villager that refuses the player, or whose reputation of them is at most {@code maxReputation}, is
     * wary: Pickpocket and Intimidate against it roll with disadvantage.
     */
    public record WarySettings(boolean enabled, int maxReputation) {}

    /**
     * Cosmetics that make social actions feel alive, each with its own switch: the pickpocket animation,
     * speech bubbles above the target and mood voices. Players with Checks within {@code radius} blocks of
     * the target see the bubbles and the stolen item fly.
     */
    public record FeelSettings(
            double radius, boolean pickpocketAnimation, SpeechBubbleSettings speechBubbles, boolean voices) {}

    /** A speech bubble stays above the target's head for {@code durationTicks}. */
    public record SpeechBubbleSettings(boolean enabled, int durationTicks) {}

    /**
     * Intimidate on hostile mobs, against {@code dc}: only those with at most {@code maxHealth} max health,
     * never a boss or a mob in {@code excludeTag}. A made check sends the mob running for {@code fleeTicks},
     * keeping {@code fleeDistance} blocks from the player at {@code fleeSpeed} times its speed; a natural 1
     * gives it Speed of {@code speedBoostAmplifier} toward the player for {@code speedBoostTicks}.
     */
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

    /**
     * Calm, a Persuasion check against {@code dc}, on wolves, bees, endermen, zombified piglins, iron golems
     * and the mobs in {@code tag} that are angry at the player.
     */
    public record CalmSettings(boolean enabled, int dc, int cooldownTicks, ResourceLocation tag) {}

    /** Passive Insight, with no roll: sensing mobs after the player, and creepers about to blow. */
    public record InsightSettings(InsightSense senseHostility, InsightSense creeperWarning) {}

    /** Works on mobs within {@code range} blocks while the player's passive Insight is at least {@code dc}. */
    public record InsightSense(boolean enabled, int dc, double range) {}

    /**
     * Playing a note block or blowing a goat horn rolls Performance against {@code dc}, at most once per
     * {@code cooldownTicks} per player, heard by villagers and piglins within {@code range} blocks.
     */
    public record PerformanceSettings(
            boolean enabled,
            int dc,
            int cooldownTicks,
            double range,
            PerformanceEffect success,
            PerformanceEffect criticalSuccess,
            PerformanceEffect criticalFailure) {}

    /**
     * Gossip about the player told to every listening villager, and how long piglins take the player for a
     * gold wearer, as the Deceive disguise; {@code 0} ticks for no change.
     */
    public record PerformanceEffect(Optional<SocialEffect.Gossip> gossip, int piglinTicks) {}

    /** Exploration skill uses, each with its own switch. */
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

    /**
     * Athletics: a sprinting jump toward a drop of at least {@code minDrop} blocks rolls against {@code dc}; a
     * made check adds {@code boost} blocks per tick to the leap. Within {@code cooldownTicks} another leap
     * reuses the last result.
     */
    public record LeapSettings(boolean enabled, int dc, int cooldownTicks, double boost, int minDrop) {}

    /**
     * Acrobatics: fall damage rolls against {@code baseDc} plus the damage times {@code dcPerDamage}, rounded
     * down; a made check multiplies the damage by {@code successMultiplier}.
     */
    public record LandingSettings(boolean enabled, int baseDc, double dcPerDamage, double successMultiplier) {

        public int dc(float damage) {
            return baseDc + (int) Math.floor(damage * dcPerDamage);
        }
    }

    /**
     * Athletics: a player inside a cobweb rolls against {@code dc}; a made check breaks the web. After a failed
     * one, the next roll waits {@code retryTicks}.
     */
    public record CobwebSettings(boolean enabled, int dc, int retryTicks) {}

    /**
     * Athletics, passively: climbing speed rises by {@code perPoint} per point of a positive Athletics
     * modifier, by at most {@code maxBonus}.
     */
    public record ClimbingSettings(boolean enabled, double perPoint, double maxBonus) {}

    /**
     * Stealth: while a player sneaks, each point their passive Stealth beats a mob's passive Perception
     * shrinks how far off that mob notices them by {@code perPoint}, down to {@code minVisibility}.
     */
    public record SneakSettings(boolean enabled, double perPoint, double minVisibility) {}

    /**
     * Perception: while a player's passive Perception is at least {@code dc}, armed tripwires within
     * {@code range} blocks glint for them, checked every {@code intervalTicks}.
     */
    public record SpotTripwireSettings(boolean enabled, int dc, int range, int intervalTicks) {}

    /** Sleight of Hand: sneaking and using an armed tripwire with an empty hand rolls against {@code dc}. */
    public record DisarmTripwireSettings(boolean enabled, int dc) {}

    /** Investigation: sneaking and using an opened loot chest rolls against {@code dc}, once per chest. */
    public record SearchSettings(boolean enabled, int dc) {}

    /**
     * Nature, Arcana or Religion: the first time a player sees a mob type within {@code range} blocks they roll
     * against {@code dc}, looked for every {@code intervalTicks}. After a failed roll that mob type waits
     * {@code retryTicks}; a made one is never rolled for again.
     */
    public record MonsterLoreSettings(boolean enabled, int dc, double range, int retryTicks, int intervalTicks) {}

    /**
     * History: the first time a player enters a kind of structure they roll against {@code dc}, looked for
     * every {@code intervalTicks}. After a failed roll that kind waits {@code retryTicks}; a made one is never
     * rolled for again.
     */
    public record StructureLoreSettings(boolean enabled, int dc, int retryTicks, int intervalTicks) {}

    /** Animal Handling: each taming attempt rolls against {@code dc}; a made check tames on that attempt. */
    public record TamingSettings(boolean enabled, int dc) {}

    /**
     * A passive skill shortening something: {@code perPoint} of it per point of a positive skill modifier,
     * at most {@code maxReduction}.
     */
    public record ReductionSettings(boolean enabled, double perPoint, double maxReduction) {}

    public int playerDefault(Ability ability) {
        return playerDefaults.get(ability);
    }

    public static final ScoresConfig DEFAULTS = new ScoresConfig(
            defaultPlayerScores(),
            new Derivation(true, 8.0, 1.0, 6.0, 24.0, 8.0, 0.2, 10),
            true,
            true,
            new ProficiencySettings(true, 2),
            new CritfallSettings(true, false, Map.of(ModifierProvider.SPELL_SAVE, Ability.DEXTERITY)),
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
                    new ImprovementSettings(List.of(4, 8, 12, 16, 19), List.of(List.of(2), List.of(1, 1)), 20)),
            new ExtensionSettings(true, new BonusSourceSettings(true, 5), true, true, true),
            new BodySettings(
                    new HealthSettings(true, 2.0, false, 0.2), new ArmorClassSettings(true, 2), true, defaultExtras()),
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
                    new LeapSettings(true, 12, 40, 0.15, 3),
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
                    new ReductionSettings(true, 0.05, 0.25)));

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
