package studio.modroll.checks.creation;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.preset.Background;
import studio.modroll.checks.preset.ClassPreset;
import studio.modroll.checks.preset.Preset;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.checks.preset.Species;

/** The presets a player may build from, plus how background bonuses may be split. Custom is always allowed. */
public record PresetOffer(
        boolean enabled,
        List<Species> species,
        List<Background> backgrounds,
        List<ClassPreset> classes,
        List<List<Integer>> bonusOptions,
        int bonusMaxScore) {

    public static final StreamCodec<ByteBuf, PresetOffer> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            PresetOffer::enabled,
            Species.STREAM_CODEC.apply(ByteBufCodecs.list()),
            PresetOffer::species,
            Background.STREAM_CODEC.apply(ByteBufCodecs.list()),
            PresetOffer::backgrounds,
            ClassPreset.STREAM_CODEC.apply(ByteBufCodecs.list()),
            PresetOffer::classes,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()),
            PresetOffer::bonusOptions,
            ByteBufCodecs.VAR_INT,
            PresetOffer::bonusMaxScore,
            PresetOffer::new);

    public PresetOffer {
        species = List.copyOf(species);
        backgrounds = List.copyOf(backgrounds);
        classes = List.copyOf(classes);
        bonusOptions = bonusOptions.stream().map(List::copyOf).toList();
    }

    public List<? extends Preset> presets(PresetKind kind) {
        return switch (kind) {
            case SPECIES -> species;
            case BACKGROUND -> backgrounds;
            case CLASS -> classes;
        };
    }

    /** Custom (an empty choice) is always offered; a preset only when this offer lists it. */
    public boolean offers(PresetKind kind, Optional<ResourceLocation> choice) {
        return choice.map(id -> find(presets(kind), id).isPresent()).orElse(true);
    }

    public Optional<Species> species(Optional<ResourceLocation> choice) {
        return choice.flatMap(id -> find(species, id));
    }

    public Optional<Background> background(Optional<ResourceLocation> choice) {
        return choice.flatMap(id -> find(backgrounds, id));
    }

    public Optional<ClassPreset> classPreset(Optional<ResourceLocation> choice) {
        return choice.flatMap(id -> find(classes, id));
    }

    /** The bonus splits that fit the background, i.e. need no more abilities than it lists. */
    public List<List<Integer>> bonusOptionsFor(Background background) {
        return bonusOptions.stream()
                .filter(option -> option.size() <= background.abilities().size())
                .toList();
    }

    private static <T extends Preset> Optional<T> find(List<T> presets, ResourceLocation id) {
        return presets.stream().filter(preset -> preset.id().equals(id)).findFirst();
    }
}
