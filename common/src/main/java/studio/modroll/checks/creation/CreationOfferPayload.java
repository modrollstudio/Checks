package studio.modroll.checks.creation;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

/** Server to client: opens, or refreshes, the creation screen with what the player may choose. */
public record CreationOfferPayload(CreationOffer offer) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CreationOfferPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("creation_offer"));

    public static final StreamCodec<ByteBuf, CreationOfferPayload> STREAM_CODEC =
            CreationOffer.STREAM_CODEC.map(CreationOfferPayload::new, CreationOfferPayload::offer);

    @Override
    public Type<CreationOfferPayload> type() {
        return TYPE;
    }
}
