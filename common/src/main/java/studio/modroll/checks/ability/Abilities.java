package studio.modroll.checks.ability;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import studio.modroll.checks.api.Ability;

public final class Abilities {

    public static final StreamCodec<ByteBuf, Ability> STREAM_CODEC =
            ByteBufCodecs.idMapper(index -> Ability.values()[index], Ability::ordinal);

    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 30;

    private Abilities() {}

    public static int clamp(int score) {
        return Math.clamp(score, MIN_SCORE, MAX_SCORE);
    }

    public static boolean inRange(int score) {
        return score >= MIN_SCORE && score <= MAX_SCORE;
    }

    public static int modifier(int score) {
        return Math.floorDiv(score - 10, 2);
    }
}
