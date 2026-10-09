package studio.modroll.checks.social;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

/** Client to server: try {@code action} on the entity; the server checks everything again. */
public record SocialActionPayload(int entityId, SocialAction action) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SocialActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("social_action"));

    public static final StreamCodec<ByteBuf, SocialActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SocialActionPayload::entityId,
            SocialMenu.Option.ACTION_CODEC,
            SocialActionPayload::action,
            SocialActionPayload::new);

    @Override
    public Type<SocialActionPayload> type() {
        return TYPE;
    }
}
