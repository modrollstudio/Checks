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

/**
 * One player's server-resolved values for the stat screen; the client only displays them.
 * {@code creationAvailable} shows the button that reopens character creation; {@code identity} is the
 * confirmed character's species, background and class while presets are on; {@code level} is present
 * while levelling is on; {@code modSections} shows the sections other mods add; {@code body} is what the
 * player's scores do to their health, armor class, size and the attribute extras.
 */
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

    /** {@code saveModifier} includes the flat save bonuses in {@code bonuses}. */
    public record AbilityLine(
            Ability ability,
            int score,
            int modifier,
            int saveModifier,
            Proficiency saveProficiency,
            AbilityBonuses bonuses) {}

    /** The bonus sources on this ability's checks and on its saving throw. */
    public record AbilityBonuses(List<BonusPart> checks, List<BonusPart> saves) {

        public static final AbilityBonuses NONE = new AbilityBonuses(List.of(), List.of());

        public AbilityBonuses {
            checks = List.copyOf(checks);
            saves = List.copyOf(saves);
        }
    }

    /** {@code modifier} includes the flat bonuses in {@code situational}, the skill's bonus sources. */
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

    /** The species' traits are listed so the client can name them in a tooltip. */
    public record Identity(PresetChoices choices, List<ResourceLocation> traits) {}

    /**
     * {@code levelXp} is the total XP the current level needed and {@code nextLevelXp} the total the next
     * one needs, empty at the top level.
     */
    public record LevelLine(int level, int xp, int levelXp, Optional<Integer> nextLevelXp, Improvements improvements) {

        /** How far the XP is from this level's threshold to the next, {@code 0..1}; full at the top level. */
        public float progress() {
            return nextLevelXp
                    .filter(next -> next > levelXp)
                    .map(next -> Math.clamp((float) (xp - levelXp) / (next - levelXp), 0f, 1f))
                    .orElse(1f);
        }
    }

    /** How many ability score improvements wait to be chosen, and the rules a choice must follow. */
    public record Improvements(int pending, List<List<Integer>> options, int maxScore) {

        public Improvements {
            options = options.stream().map(List::copyOf).toList();
        }
    }

    /** Each part is empty, and each extra left out, while its switch is off. */
    public record Body(
            Optional<HealthLine> health, Optional<ArmorLine> armor, Optional<SizeOption> size, List<ExtraLine> extras) {

        public static final Body NONE = new Body(Optional.empty(), Optional.empty(), Optional.empty(), List.of());

        public Body {
            extras = List.copyOf(extras);
        }
    }

    /** {@code maxHealth} is the player's whole max health; the rest is what CON and levels add to it. */
    public record HealthLine(float maxHealth, int conModifier, double conHealth, double levelHealth) {}

    /**
     * {@code armorClass} is what attacks roll against: Critfall's {@code baseArmorClass} plus {@code dexBonus},
     * the part of {@code dexModifier} the heaviest worn {@code category} allows.
     */
    public record ArmorLine(
            int armorClass,
            int baseArmorClass,
            int dexModifier,
            ArmorCategory category,
            int dexBonus,
            int mediumMaxDex) {}

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

    private static final StreamCodec<ByteBuf, LevelLine> LEVEL_LINE = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            LevelLine::level,
            ByteBufCodecs.VAR_INT,
            LevelLine::xp,
            ByteBufCodecs.VAR_INT,
            LevelLine::levelXp,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT),
            LevelLine::nextLevelXp,
            IMPROVEMENTS,
            LevelLine::improvements,
            LevelLine::new);

    private static final StreamCodec<ByteBuf, HealthLine> HEALTH_LINE = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            HealthLine::maxHealth,
            ByteBufCodecs.VAR_INT,
            HealthLine::conModifier,
            ByteBufCodecs.DOUBLE,
            HealthLine::conHealth,
            ByteBufCodecs.DOUBLE,
            HealthLine::levelHealth,
            HealthLine::new);

    private static final StreamCodec<ByteBuf, ArmorLine> ARMOR_LINE = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ArmorLine::armorClass,
            ByteBufCodecs.VAR_INT,
            ArmorLine::baseArmorClass,
            ByteBufCodecs.VAR_INT,
            ArmorLine::dexModifier,
            ArmorCategory.STREAM_CODEC,
            ArmorLine::category,
            ByteBufCodecs.VAR_INT,
            ArmorLine::dexBonus,
            ByteBufCodecs.VAR_INT,
            ArmorLine::mediumMaxDex,
            ArmorLine::new);

    private static final StreamCodec<ByteBuf, ExtraLine> EXTRA_LINE = StreamCodec.composite(
            Extra.STREAM_CODEC, ExtraLine::extra, ByteBufCodecs.DOUBLE, ExtraLine::amount, ExtraLine::new);

    private static final StreamCodec<ByteBuf, Body> BODY = StreamCodec.composite(
            ByteBufCodecs.optional(HEALTH_LINE),
            Body::health,
            ByteBufCodecs.optional(ARMOR_LINE),
            Body::armor,
            ByteBufCodecs.optional(SizeOption.STREAM_CODEC),
            Body::size,
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
