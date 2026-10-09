package studio.modroll.checks.level;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.Ability;

/** Client to server: one ability score improvement, the increase per ability in ability order. */
public record ImprovementPayload(List<Integer> increases) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ImprovementPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("improvement"));

    public static final StreamCodec<ByteBuf, ImprovementPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .apply(ByteBufCodecs.list(Ability.values().length))
            .map(ImprovementPayload::new, ImprovementPayload::increases);

    public ImprovementPayload {
        increases = List.copyOf(increases);
    }

    @Override
    public Type<ImprovementPayload> type() {
        return TYPE;
    }
}
