package studio.modroll.checks.creation;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Skill;

/**
 * Everything a player may choose from, built by the server. The client draws its screen from it and
 * the server validates the submission against the same offer, so both apply identical rules.
 */
public record CreationOffer(
        List<CreationMethod> methods,
        List<Integer> standardArray,
        PointBuy pointBuy,
        int skillChoices,
        List<Skill> skills,
        Optional<CharacterRolls> rolls,
        PresetOffer presets,
        Map<ResourceLocation, SkillChoice> speciesSkillChoices) {

    private static final StreamCodec<ByteBuf, Skill> SKILL = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Skill::id, Abilities.STREAM_CODEC, Skill::ability, Skill::new);
    private static final StreamCodec<ByteBuf, List<CreationMethod>> METHODS =
            CreationMethod.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<ByteBuf, List<Integer>> INTS = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list());
    private static final StreamCodec<ByteBuf, List<Skill>> SKILLS = SKILL.apply(ByteBufCodecs.list());
    private static final StreamCodec<ByteBuf, Optional<CharacterRolls>> ROLLS =
            ByteBufCodecs.optional(CharacterRolls.STREAM_CODEC);
    private static final StreamCodec<ByteBuf, Map<ResourceLocation, SkillChoice>> SPECIES_SKILL_CHOICES =
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, SkillChoice.STREAM_CODEC);

    // Written out by hand: StreamCodec.composite takes at most six fields.
    public static final StreamCodec<ByteBuf, CreationOffer> STREAM_CODEC = StreamCodec.of(
            (buffer, offer) -> {
                METHODS.encode(buffer, offer.methods());
                INTS.encode(buffer, offer.standardArray());
                PointBuy.STREAM_CODEC.encode(buffer, offer.pointBuy());
                ByteBufCodecs.VAR_INT.encode(buffer, offer.skillChoices());
                SKILLS.encode(buffer, offer.skills());
                ROLLS.encode(buffer, offer.rolls());
                PresetOffer.STREAM_CODEC.encode(buffer, offer.presets());
                SPECIES_SKILL_CHOICES.encode(buffer, offer.speciesSkillChoices());
            },
            buffer -> new CreationOffer(
                    METHODS.decode(buffer),
                    INTS.decode(buffer),
                    PointBuy.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    SKILLS.decode(buffer),
                    ROLLS.decode(buffer),
                    PresetOffer.STREAM_CODEC.decode(buffer),
                    SPECIES_SKILL_CHOICES.decode(buffer)));

    public CreationOffer {
        methods = List.copyOf(methods);
        standardArray = List.copyOf(standardArray);
        skills = List.copyOf(skills);
        speciesSkillChoices = Map.copyOf(speciesSkillChoices);
    }

    /** The skill picks a species gives; none for Custom or a species without any. */
    public SkillChoice speciesSkillChoice(Optional<ResourceLocation> species) {
        return species.map(id -> speciesSkillChoices.getOrDefault(id, SkillChoice.NONE))
                .orElse(SkillChoice.NONE);
    }

    /** Once dice are rolled the character must be built from them. */
    public Optional<CreationMethod> lockedMethod() {
        return rolls.map(CharacterRolls::method);
    }

    public boolean allows(CreationMethod method) {
        return methods.contains(method) && lockedMethod().map(method::equals).orElse(true);
    }

    /** Empty until the player has rolled for {@code method}. */
    public Optional<List<Integer>> rolledTotals(CreationMethod method) {
        return rolls.filter(r -> r.method() == method).map(CharacterRolls::totals);
    }

    /** Empty until the player has rolled for {@code method}. */
    public Optional<List<RolledScore>> rolledScores(CreationMethod method) {
        return rolls.filter(r -> r.method() == method).map(CharacterRolls::scores);
    }

    public boolean offersSkill(ResourceLocation id) {
        return skills.stream().anyMatch(skill -> skill.id().equals(id));
    }
}
