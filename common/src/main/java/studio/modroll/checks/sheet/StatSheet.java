package studio.modroll.checks.sheet;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.body.ArmorCategory;
import studio.modroll.checks.body.Extra;
import studio.modroll.checks.body.SizeOption;
import studio.modroll.checks.bonus.BonusPart;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.level.LevelGain;

public record StatSheet(
        int proficiencyBonus,
        List<AbilityLine> abilities,
        List<SkillLine> skills,
        List<PassiveLine> passives,
        boolean creationAvailable,
        Optional<Identity> identity,
        Optional<LevelLine> level,
        boolean modSections,
        Body body) {

    public record AbilityLine(
            Ability ability,
            int score,
            int modifier,
            int saveModifier,
            Proficiency saveProficiency,
            AbilityBonuses bonuses) {}

    public record AbilityBonuses(List<BonusPart> checks, List<BonusPart> saves) {

        public static final AbilityBonuses NONE = new AbilityBonuses(List.of(), List.of());

        public AbilityBonuses {
            checks = List.copyOf(checks);
            saves = List.copyOf(saves);
        }
    }

    public record SkillLine(
            ResourceLocation id,
            Ability ability,
            int modifier,
            Proficiency proficiency,
            int bonus,
            List<BonusPart> situational) {

        public SkillLine {
            situational = List.copyOf(situational);
        }
    }

    public record PassiveLine(ResourceLocation skill, int score) {}

    public record Identity(PresetChoices choices, List<ResourceLocation> traits) {}

    public record LevelLine(
            int level,
            int xp,
            int levelXp,
            Optional<Integer> nextLevelXp,
            Improvements improvements,
            ExpertiseChoice expertise,
            List<LevelGain> gains) {

        public LevelLine {
            gains = List.copyOf(gains);
        }

        public boolean choicesPending() {
            return improvements.pending() > 0 || expertise.pending() > 0;
        }

        public float progress() {
            return nextLevelXp
                    .filter(next -> next > levelXp)
                    .map(next -> Math.clamp((float) (xp - levelXp) / (next - levelXp), 0f, 1f))
                    .orElse(1f);
        }
    }

    public record Improvements(int pending, List<List<Integer>> options, int maxScore) {

        public Improvements {
            options = options.stream().map(List::copyOf).toList();
        }
    }

    public record ExpertiseChoice(int pending, List<ResourceLocation> options) {

        public static final ExpertiseChoice NONE = new ExpertiseChoice(0, List.of());

        public ExpertiseChoice {
            options = List.copyOf(options);
        }
    }

    public record Body(
            Optional<HealthLine> health,
            Optional<ArmorLine> armor,
            Optional<SizeOption> size,
            Optional<HitDiceLine> hitDice,
            Optional<AttacksLine> attacks,
            List<ExtraLine> extras) {

        public static final Body NONE = new Body(
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of());

        public Body {
            extras = List.copyOf(extras);
        }
    }

    public record HealthLine(
            float maxHealth, int conModifier, double conHealth, int hitDie, double levelHealth, double rounding) {}

    public record HitDiceLine(int sides, int left, int total, double regainFraction, boolean healsFully) {}

    public record AttacksLine(int attacks, Optional<Integer> nextLevel) {}

    public record ArmorLine(
            int baseArmorClass,
            Optional<ArmorPoints> worn,
            int dexModifier,
            ArmorCategory category,
            int dexBonus,
            int floorBonus,
            int mediumMaxDex) {

        public int armorClass() {
            return baseArmorClass + dexBonus + floorBonus;
        }
    }

    public record ArmorPoints(int base, double armor, int armorPart, double toughness, int toughnessPart) {}

    public record ExtraLine(Extra extra, double amount) {}

    private static final StreamCodec<ByteBuf, Ability> ABILITY = Abilities.STREAM_CODEC;
    private static final StreamCodec<ByteBuf, Proficiency> PROFICIENCY =
            ByteBufCodecs.idMapper(index -> Proficiency.values()[index], Proficiency::ordinal);

    private static final StreamCodec<ByteBuf, List<BonusPart>> BONUS_PARTS =
            BonusPart.STREAM_CODEC.apply(ByteBufCodecs.list());

    private static final StreamCodec<ByteBuf, AbilityBonuses> ABILITY_BONUSES = StreamCodec.composite(
            BONUS_PARTS, AbilityBonuses::checks, BONUS_PARTS, AbilityBonuses::saves, AbilityBonuses::new);

    private static final StreamCodec<ByteBuf, AbilityLine> ABILITY_LINE = StreamCodec.composite(
            ABILITY,
            AbilityLine::ability,
            ByteBufCodecs.VAR_INT,
            AbilityLine::score,
            ByteBufCodecs.VAR_INT,
            AbilityLine::modifier,
            ByteBufCodecs.VAR_INT,
            AbilityLine::saveModifier,
            PROFICIENCY,
            AbilityLine::saveProficiency,
            ABILITY_BONUSES,
            AbilityLine::bonuses,
            AbilityLine::new);

    private static final StreamCodec<ByteBuf, SkillLine> SKILL_LINE = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            SkillLine::id,
            ABILITY,
            SkillLine::ability,
            ByteBufCodecs.VAR_INT,
            SkillLine::modifier,
            PROFICIENCY,
            SkillLine::proficiency,
            ByteBufCodecs.VAR_INT,
            SkillLine::bonus,
            BONUS_PARTS,
            SkillLine::situational,
            SkillLine::new);

    private static final StreamCodec<ByteBuf, PassiveLine> PASSIVE_LINE = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            PassiveLine::skill,
            ByteBufCodecs.VAR_INT,
            PassiveLine::score,
            PassiveLine::new);

    private static final StreamCodec<ByteBuf, Identity> IDENTITY = StreamCodec.composite(
            PresetChoices.STREAM_CODEC,
            Identity::choices,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
            Identity::traits,
            Identity::new);

    private static final StreamCodec<ByteBuf, Improvements> IMPROVEMENTS = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Improvements::pending,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()),
            Improvements::options,
            ByteBufCodecs.VAR_INT,
            Improvements::maxScore,
            Improvements::new);

    private static final StreamCodec<ByteBuf, ExpertiseChoice> EXPERTISE = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ExpertiseChoice::pending,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ExpertiseChoice::options,
            ExpertiseChoice::new);

    private static final StreamCodec<ByteBuf, List<LevelGain>> GAINS =
            LevelGain.STREAM_CODEC.apply(ByteBufCodecs.list());

    // Written by hand: StreamCodec.composite stops at six fields.
    private static final StreamCodec<ByteBuf, LevelLine> LEVEL_LINE =
            StreamCodec.of(StatSheet::writeLevel, StatSheet::readLevel);

    private static void writeLevel(ByteBuf buffer, LevelLine line) {
        ByteBufCodecs.VAR_INT.encode(buffer, line.level());
        ByteBufCodecs.VAR_INT.encode(buffer, line.xp());
        ByteBufCodecs.VAR_INT.encode(buffer, line.levelXp());
        ByteBufCodecs.optional(ByteBufCodecs.VAR_INT).encode(buffer, line.nextLevelXp());
        IMPROVEMENTS.encode(buffer, line.improvements());
        EXPERTISE.encode(buffer, line.expertise());
        GAINS.encode(buffer, line.gains());
    }

    private static LevelLine readLevel(ByteBuf buffer) {
        return new LevelLine(
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.optional(ByteBufCodecs.VAR_INT).decode(buffer),
                IMPROVEMENTS.decode(buffer),
                EXPERTISE.decode(buffer),
                GAINS.decode(buffer));
    }

    private static final StreamCodec<ByteBuf, HealthLine> HEALTH_LINE = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            HealthLine::maxHealth,
            ByteBufCodecs.VAR_INT,
            HealthLine::conModifier,
            ByteBufCodecs.DOUBLE,
            HealthLine::conHealth,
            ByteBufCodecs.VAR_INT,
            HealthLine::hitDie,
            ByteBufCodecs.DOUBLE,
            HealthLine::levelHealth,
            ByteBufCodecs.DOUBLE,
            HealthLine::rounding,
            HealthLine::new);

    private static final StreamCodec<ByteBuf, HitDiceLine> HIT_DICE_LINE = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            HitDiceLine::sides,
            ByteBufCodecs.VAR_INT,
            HitDiceLine::left,
            ByteBufCodecs.VAR_INT,
            HitDiceLine::total,
            ByteBufCodecs.DOUBLE,
            HitDiceLine::regainFraction,
            ByteBufCodecs.BOOL,
            HitDiceLine::healsFully,
            HitDiceLine::new);

    private static final StreamCodec<ByteBuf, AttacksLine> ATTACKS_LINE = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            AttacksLine::attacks,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT),
            AttacksLine::nextLevel,
            AttacksLine::new);

    private static final StreamCodec<ByteBuf, ArmorPoints> ARMOR_POINTS = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ArmorPoints::base,
            ByteBufCodecs.DOUBLE,
            ArmorPoints::armor,
            ByteBufCodecs.VAR_INT,
            ArmorPoints::armorPart,
            ByteBufCodecs.DOUBLE,
            ArmorPoints::toughness,
            ByteBufCodecs.VAR_INT,
            ArmorPoints::toughnessPart,
            ArmorPoints::new);

    private static final StreamCodec<ByteBuf, Optional<ArmorPoints>> OPTIONAL_ARMOR_POINTS =
            ByteBufCodecs.optional(ARMOR_POINTS);

    // Written by hand: StreamCodec.composite stops at six fields.
    private static final StreamCodec<ByteBuf, ArmorLine> ARMOR_LINE =
            StreamCodec.of(StatSheet::writeArmor, StatSheet::readArmor);

    private static void writeArmor(ByteBuf buffer, ArmorLine armor) {
        ByteBufCodecs.VAR_INT.encode(buffer, armor.baseArmorClass());
        OPTIONAL_ARMOR_POINTS.encode(buffer, armor.worn());
        ByteBufCodecs.VAR_INT.encode(buffer, armor.dexModifier());
        ArmorCategory.STREAM_CODEC.encode(buffer, armor.category());
        ByteBufCodecs.VAR_INT.encode(buffer, armor.dexBonus());
        ByteBufCodecs.VAR_INT.encode(buffer, armor.floorBonus());
        ByteBufCodecs.VAR_INT.encode(buffer, armor.mediumMaxDex());
    }

    private static ArmorLine readArmor(ByteBuf buffer) {
        return new ArmorLine(
                ByteBufCodecs.VAR_INT.decode(buffer),
                OPTIONAL_ARMOR_POINTS.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ArmorCategory.STREAM_CODEC.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer));
    }

    private static final StreamCodec<ByteBuf, ExtraLine> EXTRA_LINE = StreamCodec.composite(
            Extra.STREAM_CODEC, ExtraLine::extra, ByteBufCodecs.DOUBLE, ExtraLine::amount, ExtraLine::new);

    private static final StreamCodec<ByteBuf, Body> BODY = StreamCodec.composite(
            ByteBufCodecs.optional(HEALTH_LINE),
            Body::health,
            ByteBufCodecs.optional(ARMOR_LINE),
            Body::armor,
            ByteBufCodecs.optional(SizeOption.STREAM_CODEC),
            Body::size,
            ByteBufCodecs.optional(HIT_DICE_LINE),
            Body::hitDice,
            ByteBufCodecs.optional(ATTACKS_LINE),
            Body::attacks,
            EXTRA_LINE.apply(ByteBufCodecs.list()),
            Body::extras,
            Body::new);

    private static final StreamCodec<ByteBuf, List<AbilityLine>> ABILITY_LINES =
            ABILITY_LINE.apply(ByteBufCodecs.list());
    private static final StreamCodec<ByteBuf, List<SkillLine>> SKILL_LINES = SKILL_LINE.apply(ByteBufCodecs.list());
    private static final StreamCodec<ByteBuf, List<PassiveLine>> PASSIVE_LINES =
            PASSIVE_LINE.apply(ByteBufCodecs.list());
    private static final StreamCodec<ByteBuf, Optional<Identity>> OPTIONAL_IDENTITY = ByteBufCodecs.optional(IDENTITY);
    private static final StreamCodec<ByteBuf, Optional<LevelLine>> OPTIONAL_LEVEL = ByteBufCodecs.optional(LEVEL_LINE);

    // Written by hand: StreamCodec.composite stops at six fields.
    public static final StreamCodec<ByteBuf, StatSheet> STREAM_CODEC =
            StreamCodec.of(StatSheet::write, StatSheet::read);

    private static void write(ByteBuf buffer, StatSheet sheet) {
        ByteBufCodecs.VAR_INT.encode(buffer, sheet.proficiencyBonus());
        ABILITY_LINES.encode(buffer, sheet.abilities());
        SKILL_LINES.encode(buffer, sheet.skills());
        PASSIVE_LINES.encode(buffer, sheet.passives());
        ByteBufCodecs.BOOL.encode(buffer, sheet.creationAvailable());
        OPTIONAL_IDENTITY.encode(buffer, sheet.identity());
        OPTIONAL_LEVEL.encode(buffer, sheet.level());
        ByteBufCodecs.BOOL.encode(buffer, sheet.modSections());
        BODY.encode(buffer, sheet.body());
    }

    private static StatSheet read(ByteBuf buffer) {
        return new StatSheet(
                ByteBufCodecs.VAR_INT.decode(buffer),
                ABILITY_LINES.decode(buffer),
                SKILL_LINES.decode(buffer),
                PASSIVE_LINES.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                OPTIONAL_IDENTITY.decode(buffer),
                OPTIONAL_LEVEL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                BODY.decode(buffer));
    }

    public AbilityLine ability(Ability ability) {
        return abilities.stream()
                .filter(line -> line.ability() == ability)
                .findFirst()
                .orElseThrow();
    }
}
