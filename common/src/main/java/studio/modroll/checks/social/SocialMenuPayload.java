package studio.modroll.checks.social;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

/** Server to client: opens the social menu. */
public record SocialMenuPayload(SocialMenu menu) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SocialMenuPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("social_menu"));

    public static final StreamCodec<ByteBuf, SocialMenuPayload> STREAM_CODEC =
            SocialMenu.STREAM_CODEC.map(SocialMenuPayload::new, SocialMenuPayload::menu);

    @Override
    public Type<SocialMenuPayload> type() {
        return TYPE;
    }
}
