package studio.modroll.checks.creation;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

/** Client to server: reopen the creation screen; answered only while the player's character is unconfirmed. */
public record CreationOpenPayload() implements CustomPacketPayload {

    public static final CreationOpenPayload INSTANCE = new CreationOpenPayload();

    public static final CustomPacketPayload.Type<CreationOpenPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("creation_open"));

    public static final StreamCodec<ByteBuf, CreationOpenPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<CreationOpenPayload> type() {
        return TYPE;
    }
}
