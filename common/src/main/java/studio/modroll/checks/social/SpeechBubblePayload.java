package studio.modroll.checks.social;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

/** Server to client: {@code line} floats above the entity's head for {@code durationTicks}. */
public record SpeechBubblePayload(int entityId, Component line, int durationTicks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SpeechBubblePayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("speech_bubble"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpeechBubblePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SpeechBubblePayload::entityId,
            ComponentSerialization.STREAM_CODEC,
            SpeechBubblePayload::line,
            ByteBufCodecs.VAR_INT,
            SpeechBubblePayload::durationTicks,
            SpeechBubblePayload::new);

    @Override
    public Type<SpeechBubblePayload> type() {
        return TYPE;
    }
}
