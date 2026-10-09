package studio.modroll.checks.body;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A character's size category, the SRD 5.2 sizes a playable species can have. */
public enum Size {
    SMALL("small"),
    MEDIUM("medium");

    public static final StreamCodec<ByteBuf, Size> STREAM_CODEC =
            ByteBufCodecs.idMapper(index -> values()[index], Size::ordinal);

    private final String id;

    Size(String id) {
        this.id = id;
    }

    /** The lowercase id used in datapack JSON and NBT. */
    public String id() {
        return id;
    }

    public static Optional<Size> byId(String id) {
        return Arrays.stream(values()).filter(size -> size.id.equals(id)).findFirst();
    }
}
