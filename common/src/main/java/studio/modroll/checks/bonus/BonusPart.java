package studio.modroll.checks.bonus;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.critfall.api.dice.RollMode;

/** What one bonus source adds to a check: a flat bonus and a roll mode, {@code NORMAL} for none. */
public record BonusPart(ResourceLocation source, int bonus, RollMode mode) {

    private static final StreamCodec<ByteBuf, RollMode> MODE =
            ByteBufCodecs.idMapper(index -> RollMode.values()[index], RollMode::ordinal);

    public static final StreamCodec<ByteBuf, BonusPart> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            BonusPart::source,
            ByteBufCodecs.VAR_INT,
            BonusPart::bonus,
            MODE,
            BonusPart::mode,
            BonusPart::new);
}
