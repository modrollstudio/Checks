package studio.modroll.checks.creation;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.body.Size;
import studio.modroll.checks.preset.PresetKind;

/**
 * The species, background and class a character was built from, where empty means Custom, and the
 * size picked from the species' sizes, where empty means the species' default.
 */
public record PresetChoices(
        Optional<ResourceLocation> species,
        Optional<ResourceLocation> background,
        Optional<ResourceLocation> classPreset,
        Optional<Size> size) {

    public static final PresetChoices CUSTOM =
            new PresetChoices(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    private static final StreamCodec<ByteBuf, Optional<ResourceLocation>> CHOICE =
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC);

    public static final StreamCodec<ByteBuf, PresetChoices> STREAM_CODEC = StreamCodec.composite(
            CHOICE,
            PresetChoices::species,
            CHOICE,
            PresetChoices::background,
            CHOICE,
            PresetChoices::classPreset,
            ByteBufCodecs.optional(Size.STREAM_CODEC),
            PresetChoices::size,
            PresetChoices::new);

    public Optional<ResourceLocation> get(PresetKind kind) {
        return switch (kind) {
            case SPECIES -> species;
            case BACKGROUND -> background;
            case CLASS -> classPreset;
        };
    }
}
