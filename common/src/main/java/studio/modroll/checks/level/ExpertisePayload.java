package studio.modroll.checks.level;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.Checks;

/** Client to server: the skills chosen for Expertise. */
public record ExpertisePayload(List<ResourceLocation> skills) implements CustomPacketPayload {

    /** Far more than any class gives; only bounds what a client can send. */
    private static final int MAX_SKILLS = 64;

    public static final CustomPacketPayload.Type<ExpertisePayload> TYPE =
            new CustomPacketPayload.Type<>(Checks.id("expertise"));

    public static final StreamCodec<ByteBuf, ExpertisePayload> STREAM_CODEC = ResourceLocation.STREAM_CODEC
            .apply(ByteBufCodecs.list(MAX_SKILLS))
            .map(ExpertisePayload::new, ExpertisePayload::skills);

    public ExpertisePayload {
        skills = List.copyOf(skills);
    }

    @Override
    public Type<ExpertisePayload> type() {
        return TYPE;
    }
}
