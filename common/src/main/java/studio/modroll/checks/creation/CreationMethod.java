package studio.modroll.checks.creation;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** How a player generates their six ability scores at character creation. */
public enum CreationMethod {
    STANDARD_ARRAY("standard_array", false),
    POINT_BUY("point_buy", false),
    ROLL("roll", true),
    HARDCORE("hardcore", true);

    public static final StreamCodec<ByteBuf, CreationMethod> STREAM_CODEC =
            ByteBufCodecs.idMapper(index -> values()[index], CreationMethod::ordinal);

    private final String id;
    private final boolean rollsDice;

    CreationMethod(String id, boolean rollsDice) {
        this.id = id;
        this.rollsDice = rollsDice;
    }

    /** The lowercase id used in config, NBT and lang keys. */
    public String id() {
        return id;
    }

    public boolean rollsDice() {
        return rollsDice;
    }

    public static Optional<CreationMethod> byId(String id) {
        for (CreationMethod method : values()) {
            if (method.id.equals(id)) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }
}
