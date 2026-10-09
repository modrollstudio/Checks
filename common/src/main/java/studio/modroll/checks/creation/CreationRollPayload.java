package studio.modroll.checks.creation;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

/** Client to server: roll the dice for a rolling method. The server ignores repeats, so rolls never change. */
public record CreationRollPayload(CreationMethod method) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CreationRollPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("creation_roll"));

    public static final StreamCodec<ByteBuf, CreationRollPayload> STREAM_CODEC =
            CreationMethod.STREAM_CODEC.map(CreationRollPayload::new, CreationRollPayload::method);

    @Override
    public Type<CreationRollPayload> type() {
        return TYPE;
    }
}
