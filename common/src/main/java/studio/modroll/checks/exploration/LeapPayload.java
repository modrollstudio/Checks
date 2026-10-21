package studio.modroll.checks.exploration;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

/** Server to client: the horizontal push, in blocks per tick, a made Athletics check adds to the player's leap. */
public record LeapPayload(double x, double z) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LeapPayload> TYPE = new CustomPacketPayload.Type<>(Checks.id("leap"));

    public static final StreamCodec<ByteBuf, LeapPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, LeapPayload::x, ByteBufCodecs.DOUBLE, LeapPayload::z, LeapPayload::new);

    @Override
    public Type<LeapPayload> type() {
        return TYPE;
    }
}
