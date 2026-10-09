package studio.modroll.checks.body;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A size a species offers and the scale it gives a player, as a multiple of normal player size. */
public record SizeOption(Size size, double scale) {

    /** Minecraft's own bounds for the scale attribute. */
    public static final double MIN_SCALE = 0.0625;

    public static final double MAX_SCALE = 16.0;

    public static final StreamCodec<ByteBuf, SizeOption> STREAM_CODEC = StreamCodec.composite(
            Size.STREAM_CODEC, SizeOption::size, ByteBufCodecs.DOUBLE, SizeOption::scale, SizeOption::new);

    public static boolean validScale(double scale) {
        return scale >= MIN_SCALE && scale <= MAX_SCALE;
    }
}
