package studio.modroll.checks.preset;

import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.body.Size;
import studio.modroll.checks.body.SizeOption;
import studio.modroll.checks.data.LenientJson;

/**
 * A species, its trait ids and the sizes it offers, the first being the default. What a trait does lives
 * in its own trait file. A species with no sizes leaves a player's size alone.
 */
public record Species(ResourceLocation id, List<ResourceLocation> traits, List<SizeOption> sizes, ResourceLocation icon)
        implements Preset {

    public static final int FORMAT_VERSION = 1;

    public static final StreamCodec<ByteBuf, Species> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            Species::id,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
            Species::traits,
            SizeOption.STREAM_CODEC.apply(ByteBufCodecs.list()),
            Species::sizes,
            ResourceLocation.STREAM_CODEC,
            Species::icon,
            Species::new);

    public Species {
        traits = List.copyOf(traits);
        sizes = List.copyOf(sizes);
    }

    /** The chosen size if this species offers it, else the default; empty when it offers none. */
    public Optional<SizeOption> size(Optional<Size> chosen) {
        return sizes.stream()
                .filter(option -> chosen.equals(Optional.of(option.size())))
                .findFirst()
                .or(() -> sizes.stream().findFirst());
    }

    public boolean offersSize(Size size) {
        return sizes.stream().anyMatch(option -> option.size() == size);
    }

    /** A trait id without a namespace takes the species file's namespace. */
    public static Species parse(
            ResourceLocation id, JsonObject json, Predicate<ResourceLocation> itemExists, Consumer<String> warn) {
        LenientJson j = new LenientJson(json, "species " + id, warn);
        j.checkFormatVersion(FORMAT_VERSION);
        List<ResourceLocation> traits = j.stringList("traits").stream()
                .map(trait -> PresetJson.id(trait, id.getNamespace()))
                .distinct()
                .toList();
        List<SizeOption> sizes = parseSizes(j);
        ResourceLocation icon = PresetJson.icon(j, PresetKind.SPECIES, itemExists);
        j.finish();
        return new Species(id, traits, sizes, icon);
    }

    /** Rejects the file on an unknown or repeated size, or a scale outside Minecraft's bounds. */
    private static List<SizeOption> parseSizes(LenientJson json) {
        List<SizeOption> sizes = new ArrayList<>();
        for (LenientJson entry : json.objectList("sizes")) {
            String text = entry.optionalString("size").orElse("");
            Size size = Size.byId(text)
                    .orElseThrow(() -> new IllegalArgumentException("'sizes' names unknown size '" + text + "'"));
            double scale = entry.getDouble("scale", Double.NaN);
            if (!SizeOption.validScale(scale)) {
                throw new IllegalArgumentException(
                        "size '" + text + "' needs a 'scale' in " + SizeOption.MIN_SCALE + ".." + SizeOption.MAX_SCALE);
            }
            if (sizes.stream().anyMatch(option -> option.size() == size)) {
                throw new IllegalArgumentException("'sizes' lists '" + text + "' twice");
            }
            sizes.add(new SizeOption(size, scale));
        }
        return sizes;
    }
}
