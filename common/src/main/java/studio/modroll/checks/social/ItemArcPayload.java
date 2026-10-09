package studio.modroll.checks.social;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import studio.modroll.checks.Checks;

/**
 * Server to client: {@code item} flies from the target to the thief, or with {@code caught} drops halfway
 * and is snatched back. See {@link ItemArc} for the path.
 */
public record ItemArcPayload(int fromId, int toId, ItemStack item, boolean caught) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ItemArcPayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("item_arc"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemArcPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ItemArcPayload::fromId,
            ByteBufCodecs.VAR_INT,
            ItemArcPayload::toId,
            ItemStack.STREAM_CODEC,
            ItemArcPayload::item,
            ByteBufCodecs.BOOL,
            ItemArcPayload::caught,
            ItemArcPayload::new);

    @Override
    public Type<ItemArcPayload> type() {
        return TYPE;
    }
}
