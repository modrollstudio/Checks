package studio.modroll.checks.social;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

/** Client to server: the entity under the crosshair; the server checks it is in reach. */
public record SocialMenuRequestPayload(int entityId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SocialMenuRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("social_menu_request"));

    public static final StreamCodec<ByteBuf, SocialMenuRequestPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(SocialMenuRequestPayload::new, SocialMenuRequestPayload::entityId);

    @Override
    public Type<SocialMenuRequestPayload> type() {
        return TYPE;
    }
}
