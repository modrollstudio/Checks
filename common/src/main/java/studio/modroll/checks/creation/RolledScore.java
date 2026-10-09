package studio.modroll.checks.creation;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import studio.modroll.critfall.api.dice.DieRoll;
import studio.modroll.critfall.api.dice.RollResult;

/** One rolled score and every die behind it, dropped dice included, so the screen can show the roll. */
public record RolledScore(int total, List<DieRoll> dice) {

    private static final int MAX_DICE_ON_WIRE = 64;

    private static final StreamCodec<ByteBuf, DieRoll> DIE = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            DieRoll::sides,
            ByteBufCodecs.VAR_INT,
            DieRoll::value,
            ByteBufCodecs.BOOL,
            DieRoll::kept,
            DieRoll::new);

    public static final StreamCodec<ByteBuf, RolledScore> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            RolledScore::total,
            DIE.apply(ByteBufCodecs.list(MAX_DICE_ON_WIRE)),
            RolledScore::dice,
            RolledScore::new);

    public RolledScore {
        dice = List.copyOf(dice);
    }

    public static RolledScore of(RollResult result) {
        return new RolledScore(result.total(), result.dice());
    }
}
