package studio.modroll.checks.creation;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

/** Client to server: the player's choices, which the server validates before anything is saved. */
public record CreationSubmitPayload(CreationSubmission submission) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CreationSubmitPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("creation_submit"));

    public static final StreamCodec<ByteBuf, CreationSubmitPayload> STREAM_CODEC =
            CreationSubmission.STREAM_CODEC.map(CreationSubmitPayload::new, CreationSubmitPayload::submission);

    @Override
    public Type<CreationSubmitPayload> type() {
        return TYPE;
    }
}
