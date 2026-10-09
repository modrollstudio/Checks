package studio.modroll.checks.sheet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

/** Client to server: carries nothing, so a player can only ever ask for their own sheet. */
public record StatSheetRequestPayload() implements CustomPacketPayload {

    public static final StatSheetRequestPayload INSTANCE = new StatSheetRequestPayload();

    public static final CustomPacketPayload.Type<StatSheetRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("stat_sheet_request"));

    public static final StreamCodec<ByteBuf, StatSheetRequestPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<StatSheetRequestPayload> type() {
        return TYPE;
    }
}
