package studio.modroll.checks.social;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

/** Server to client: the mobs the player's Insight senses as a threat, replacing the last set; empty clears it. */
public record HostilityPayload(List<Integer> entityIds) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HostilityPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("hostility"));

    public static final StreamCodec<ByteBuf, HostilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), HostilityPayload::entityIds, HostilityPayload::new);

    public HostilityPayload {
        entityIds = List.copyOf(entityIds);
    }

    @Override
    public Type<HostilityPayload> type() {
        return TYPE;
    }
}
