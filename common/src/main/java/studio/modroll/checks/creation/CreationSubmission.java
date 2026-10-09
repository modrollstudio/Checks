package studio.modroll.checks.creation;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;

/**
 * A player's choices: one base score and one background bonus per ability in {@link Ability} order,
 * the picked skills (not the background's fixed ones), the chosen presets and the skills picked with the
 * species' skill choices.
 */
public record CreationSubmission(
        CreationMethod method,
        List<Integer> scores,
        List<Integer> bonuses,
        List<ResourceLocation> skills,
        PresetChoices choices,
        List<ResourceLocation> speciesSkills) {

    public static final int ABILITY_COUNT = Ability.values().length;
    private static final int MAX_SKILLS_ON_WIRE = 256;

    public static final StreamCodec<ByteBuf, CreationSubmission> STREAM_CODEC = StreamCodec.composite(
            CreationMethod.STREAM_CODEC,
            CreationSubmission::method,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(ABILITY_COUNT)),
            CreationSubmission::scores,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(ABILITY_COUNT)),
            CreationSubmission::bonuses,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SKILLS_ON_WIRE)),
            CreationSubmission::skills,
            PresetChoices.STREAM_CODEC,
            CreationSubmission::choices,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SKILLS_ON_WIRE)),
            CreationSubmission::speciesSkills,
            CreationSubmission::new);

    public CreationSubmission {
        scores = List.copyOf(scores);
        bonuses = List.copyOf(bonuses);
        skills = List.copyOf(skills);
        speciesSkills = List.copyOf(speciesSkills);
    }
}
