package studio.modroll.checks.creation;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

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

    public String id() {
        return id;
    }

    public boolean rollsDice() {
        return rollsDice;
    }

    public static Optional<CreationMethod> byId(String id) {
        return Arrays.stream(values()).filter(method -> method.id.equals(id)).findFirst();
    }
}
