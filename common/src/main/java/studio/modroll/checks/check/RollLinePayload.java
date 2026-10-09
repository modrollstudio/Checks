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

/**
 * Server to client: a roll result to show above the hotbar for {@code durationTicks}: the {@code roll} and,
 * on a line of its own, the {@code detail} that goes with it, such as a social action's flavor line.
 */
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

    /** The roll, then the detail when there is one. */
    public List<Component> lines() {
        return Stream.concat(Stream.of(roll), detail.stream()).toList();
    }

    @Override
    public Type<RollLinePayload> type() {
        return TYPE;
    }
}
