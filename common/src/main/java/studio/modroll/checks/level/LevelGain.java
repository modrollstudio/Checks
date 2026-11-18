package studio.modroll.checks.level;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What one level gave: the max health it added (for level 1, all of level 1's), the proficiency bonus when it rose
 * there, how many ability score improvements and Expertise skills it earned, and the attacks per action when Extra
 * Attack raised them there.
 */
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
