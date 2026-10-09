package studio.modroll.checks.creation;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The six scores a player rolled, fixed for good once rolled; hardcore scores are in ability order. */
public record CharacterRolls(CreationMethod method, List<RolledScore> scores) {

    public static final StreamCodec<ByteBuf, CharacterRolls> STREAM_CODEC = StreamCodec.composite(
            CreationMethod.STREAM_CODEC,
            CharacterRolls::method,
            RolledScore.STREAM_CODEC.apply(ByteBufCodecs.list(CreationSubmission.ABILITY_COUNT)),
            CharacterRolls::scores,
            CharacterRolls::new);

    public CharacterRolls {
        scores = List.copyOf(scores);
    }

    public List<Integer> totals() {
        return scores.stream().map(RolledScore::total).toList();
    }
}
