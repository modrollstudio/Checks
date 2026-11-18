package studio.modroll.checks.check;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import studio.modroll.checks.Checks;

public record RollLinePayload(Component roll, Optional<Component> detail, int durationTicks)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RollLinePayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("roll_line"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RollLinePayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC,
            RollLinePayload::roll,
            ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC),
            RollLinePayload::detail,
            ByteBufCodecs.VAR_INT,
            RollLinePayload::durationTicks,
            RollLinePayload::new);

    public List<Component> lines() {
        return Stream.concat(Stream.of(roll), detail.stream()).toList();
    }

    @Override
    public Type<RollLinePayload> type() {
        return TYPE;
    }
}
