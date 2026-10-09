package studio.modroll.checks.creation;

import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** The extra skill picks a species gives at creation: {@code count} skills from {@code from}, any skill when empty. */
public record SkillChoice(int count, Set<ResourceLocation> from) {

    public static final SkillChoice NONE = new SkillChoice(0, Set.of());

    public static final StreamCodec<ByteBuf, SkillChoice> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SkillChoice::count,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)),
            SkillChoice::from,
            SkillChoice::new);

    public SkillChoice {
        from = Set.copyOf(from);
    }

    /** All of a species' choices as one: the counts add up and the lists join; one open to any skill opens all. */
    public static SkillChoice pooled(List<SkillChoice> choices) {
        int count = choices.stream().mapToInt(SkillChoice::count).sum();
        boolean anySkill = choices.stream().anyMatch(choice -> choice.from().isEmpty());
        Set<ResourceLocation> from = new HashSet<>();
        if (!anySkill) {
            choices.forEach(choice -> from.addAll(choice.from()));
        }
        return new SkillChoice(count, from);
    }
}
