package studio.modroll.checks.sheet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

/** Server to client: the requesting player's own stat sheet. */
public record StatSheetPayload(StatSheet sheet) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StatSheetPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("stat_sheet"));

    public static final StreamCodec<ByteBuf, StatSheetPayload> STREAM_CODEC =
            StatSheet.STREAM_CODEC.map(StatSheetPayload::new, StatSheetPayload::sheet);

    @Override
    public Type<StatSheetPayload> type() {
        return TYPE;
    }
}
