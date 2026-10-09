package studio.modroll.checks.trait;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

/** Server to client: how strongly the player's darkvision lights up darkness, 0 for none. */
public record DarkvisionPayload(float strength) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DarkvisionPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("darkvision"));

    public static final StreamCodec<ByteBuf, DarkvisionPayload> STREAM_CODEC =
            ByteBufCodecs.FLOAT.map(DarkvisionPayload::new, DarkvisionPayload::strength);

    @Override
    public Type<DarkvisionPayload> type() {
        return TYPE;
    }
}
