package studio.modroll.checks.exploration;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

/** Server to client: what the player's climbing speed is multiplied by, 1 for vanilla's. */
public record ClimbingPayload(float speed) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClimbingPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("climbing"));

    public static final StreamCodec<ByteBuf, ClimbingPayload> STREAM_CODEC =
            ByteBufCodecs.FLOAT.map(ClimbingPayload::new, ClimbingPayload::speed);

    @Override
    public Type<ClimbingPayload> type() {
        return TYPE;
    }
}
