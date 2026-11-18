package studio.modroll.checks.level;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record LevelGain(
        int level,
        double health,
        Optional<Integer> proficiencyBonus,
        int improvements,
        int expertise,
        Optional<Integer> attacks) {

    public static final StreamCodec<ByteBuf, LevelGain> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            LevelGain::level,
            ByteBufCodecs.DOUBLE,
            LevelGain::health,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT),
            LevelGain::proficiencyBonus,
            ByteBufCodecs.VAR_INT,
            LevelGain::improvements,
            ByteBufCodecs.VAR_INT,
            LevelGain::expertise,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT),
            LevelGain::attacks,
            LevelGain::new);
}
