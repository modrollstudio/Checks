package studio.modroll.checks.preset;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** One stack of a background's starting kit. */
public record KitItem(ResourceLocation item, int count) {

    public static final StreamCodec<ByteBuf, KitItem> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, KitItem::item, ByteBufCodecs.VAR_INT, KitItem::count, KitItem::new);
}
