package studio.modroll.checks;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Both ways while configuring: the sender's {@link NetworkVersion} and its Checks version, which only names it in
 * a refusal. Its fields must never change, or two versions could not tell each other they differ.
 */
public record NetworkVersionPayload(int version, String modVersion) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NetworkVersionPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("network_version"));

    public static final StreamCodec<ByteBuf, NetworkVersionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            NetworkVersionPayload::version,
            ByteBufCodecs.STRING_UTF8,
            NetworkVersionPayload::modVersion,
            NetworkVersionPayload::new);

    @Override
    public Type<NetworkVersionPayload> type() {
        return TYPE;
    }
}
