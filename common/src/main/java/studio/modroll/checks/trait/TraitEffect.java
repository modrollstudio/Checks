package studio.modroll.checks.trait;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.bonus.BonusTargets;
import studio.modroll.checks.check.RollModes;
import studio.modroll.checks.data.LenientJson;
import studio.modroll.checks.data.MatchEntry;
import studio.modroll.checks.save.VanillaSave;
import studio.modroll.critfall.api.dice.RollMode;

/** One effect of a trait, as written in its {@code effects} list. */
public sealed interface TraitEffect {

    TraitEffectType type();

    /** Advantage on these vanilla saves. */
    record VanillaSaveAdvantage(Set<VanillaSave> against) implements TraitEffect {
        public VanillaSaveAdvantage {
            against = Set.copyOf(against);
        }

        @Override
        public TraitEffectType type() {
            return TraitEffectType.VANILLA_SAVE_ADVANTAGE;
        }
    }

    /** An always-on bonus source: a flat bonus and/or a roll mode on the rolls it targets. */
    record RollBonus(BonusTargets targets, int bonus, RollMode mode) implements TraitEffect {
        @Override
        public TraitEffectType type() {
            return TraitEffectType.ROLL_BONUS;
        }
    }

    /** Matching incoming damage is multiplied by {@code multiplier}. */
    record DamageResistance(List<MatchEntry> damageTypes, double multiplier) implements TraitEffect {
        static final double DEFAULT_MULTIPLIER = 0.5;

        public DamageResistance {
            damageTypes = List.copyOf(damageTypes);
        }

        @Override
        public TraitEffectType type() {
            return TraitEffectType.DAMAGE_RESISTANCE;
        }
    }

    /** Max health plus {@code perLevel} per character level, rounded down. */
    record ExtraHealth(double perLevel) implements TraitEffect {
        @Override
        public TraitEffectType type() {
            return TraitEffectType.EXTRA_HEALTH;
        }
    }

    /** Darkness looks like dim light; how dim is {@code traits.darkvision.strength}. */
    record Darkvision() implements TraitEffect {
        @Override
        public TraitEffectType type() {
            return TraitEffectType.DARKVISION;
        }
    }

    /** Rerolls a Checks d20 roll that comes up 1, or that fails; {@code daily} limits it to once per recharge. */
    record Reroll(When on, boolean daily) implements TraitEffect {
        @Override
        public TraitEffectType type() {
            return TraitEffectType.REROLL;
        }
    }

    enum When {
        NATURAL_1,
        FAILURE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Once per recharge, damage that would kill leaves the player at 1 HP instead. */
    record LastStand() implements TraitEffect {
        @Override
        public TraitEffectType type() {
            return TraitEffectType.LAST_STAND;
        }
    }

    /** These mobs never pick the player as a target. */
    record IgnoredBy(List<MatchEntry> entities) implements TraitEffect {
        public IgnoredBy {
            entities = List.copyOf(entities);
        }

        @Override
        public TraitEffectType type() {
            return TraitEffectType.IGNORED_BY;
        }
    }

    /** {@code count} more skill picks at creation, from {@code from} or, when empty, from any skill. */
    record SkillChoices(int count, Set<ResourceLocation> from) implements TraitEffect {
        public SkillChoices {
            from = Set.copyOf(from);
        }

        @Override
        public TraitEffectType type() {
            return TraitEffectType.SKILL_CHOICES;
        }
    }

    /** A transient attribute modifier. */
    record Attribute(ResourceLocation attribute, double amount, AttributeModifier.Operation operation)
            implements TraitEffect {
        @Override
        public TraitEffectType type() {
            return TraitEffectType.ATTRIBUTE;
        }
    }

    /** What an effect's parser may look up. */
    record Lookups(
            Function<ResourceLocation, Optional<Skill>> skills,
            Predicate<ResourceLocation> entityExists,
            Predicate<ResourceLocation> attributeExists) {}

    /** An unknown type, a missing or bad required key rejects the trait file. */
    static TraitEffect parse(LenientJson json, Lookups lookups) {
        String typeId = json.optionalString("type").orElse("");
        TraitEffectType type = TraitEffectType.byId(typeId)
                .orElseThrow(() -> new IllegalArgumentException("unknown effect type '" + typeId + "'"));
        return switch (type) {
            case VANILLA_SAVE_ADVANTAGE -> new VanillaSaveAdvantage(parseSaves(json));
            case ROLL_BONUS -> parseRollBonus(json, lookups);
            case DAMAGE_RESISTANCE ->
                new DamageResistance(
                        parseMatches(json, "damage_types", id -> true),
                        nonNegative(json, "multiplier", DamageResistance.DEFAULT_MULTIPLIER));
            case EXTRA_HEALTH -> new ExtraHealth(nonNegative(json, "per_level", Double.NaN));
            case DARKVISION -> new Darkvision();
            case REROLL -> new Reroll(parseWhen(json), json.getBool("daily", false));
            case LAST_STAND -> new LastStand();
            case IGNORED_BY -> new IgnoredBy(parseMatches(json, "entities", lookups.entityExists()));
            case SKILL_CHOICES -> parseSkillChoices(json, lookups);
            case ATTRIBUTE -> parseAttribute(json, lookups);
        };
    }

    private static Set<VanillaSave> parseSaves(LenientJson json) {
        Set<VanillaSave> saves = new HashSet<>();
        for (String id : json.stringList("against")) {
            saves.add(VanillaSave.byId(id)
                    .orElseThrow(
                            () -> new IllegalArgumentException("'against' names unknown vanilla save '" + id + "'")));
        }
        if (saves.isEmpty()) {
            throw new IllegalArgumentException("'against' names no vanilla save");
        }
        return saves;
    }

    private static RollBonus parseRollBonus(LenientJson json, Lookups lookups) {
        BonusTargets targets = BonusTargets.parse(json.object("applies_to"), lookups.skills());
        int bonus = json.getInt("bonus", 0);
        RollMode mode = json.optionalString("mode")
                .map(text -> RollModes.byId(text)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "'mode' must be normal, advantage or disadvantage, was '" + text + "'")))
                .orElse(RollMode.NORMAL);
        if (bonus == 0 && mode == RollMode.NORMAL) {
            throw new IllegalArgumentException("a roll_bonus needs a non-zero 'bonus' or a 'mode'");
        }
        return new RollBonus(targets, bonus, mode);
    }

    private static List<MatchEntry> parseMatches(LenientJson json, String key, Predicate<ResourceLocation> exists) {
        List<MatchEntry> matches =
                json.stringList(key).stream().map(MatchEntry::parse).toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("'" + key + "' lists nothing");
        }
        for (MatchEntry match : matches) {
            if (match instanceof MatchEntry.Exact exact && !exists.test(exact.id())) {
                throw new IllegalArgumentException("'" + key + "' names unknown '" + exact.id() + "'");
            }
        }
        return matches;
    }

    private static When parseWhen(LenientJson json) {
        String text = json.optionalString("on").orElse("");
        return Arrays.stream(When.values())
                .filter(when -> when.id().equals(text))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("'on' must be natural_1 or failure, was '" + text + "'"));
    }

    /** An unknown skill in {@code from} is skipped with a warning; a list that names none left rejects the file. */
    private static SkillChoices parseSkillChoices(LenientJson json, Lookups lookups) {
        int count = json.getInt("count", 0);
        if (count < 1) {
            throw new IllegalArgumentException("'count' must be at least 1");
        }
        Set<ResourceLocation> from = new HashSet<>();
        List<String> listed = json.stringList("from");
        for (String key : listed) {
            Optional.ofNullable(ResourceLocation.tryParse(key))
                    .flatMap(lookups.skills())
                    .ifPresentOrElse(
                            skill -> from.add(skill.id()), () -> json.warn("unknown skill '" + key + "' (skipped)"));
        }
        if (!listed.isEmpty() && from.isEmpty()) {
            throw new IllegalArgumentException("'from' names no known skill");
        }
        return new SkillChoices(count, from);
    }

    private static Attribute parseAttribute(LenientJson json, Lookups lookups) {
        String text = json.optionalString("attribute").orElse("");
        ResourceLocation attribute = ResourceLocation.tryParse(text);
        if (attribute == null || !lookups.attributeExists().test(attribute)) {
            throw new IllegalArgumentException("unknown attribute '" + text + "'");
        }
        double amount = json.getDouble("amount", Double.NaN);
        if (Double.isNaN(amount)) {
            throw new IllegalArgumentException("'amount' must be a number");
        }
        String operationText = json.optionalString("operation").orElse("add_value");
        AttributeModifier.Operation operation = Arrays.stream(AttributeModifier.Operation.values())
                .filter(op -> op.getSerializedName().equals(operationText))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("'operation' must be add_value, add_multiplied_base"
                        + " or add_multiplied_total, was '" + operationText + "'"));
        return new Attribute(attribute, amount, operation);
    }

    /** A required value when {@code fallback} is NaN. */
    private static double nonNegative(LenientJson json, String key, double fallback) {
        double value = json.getDouble(key, fallback);
        if (Double.isNaN(value) || value < 0) {
            throw new IllegalArgumentException("'" + key + "' must be a number of at least 0");
        }
        return value;
    }
}
