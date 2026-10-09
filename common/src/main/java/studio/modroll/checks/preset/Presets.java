package studio.modroll.checks.preset;

/** The loaded species, backgrounds and classes. */
public final class Presets {

    public static final PresetStore<Species> SPECIES = new PresetStore<>();
    public static final PresetStore<Background> BACKGROUNDS = new PresetStore<>();
    public static final PresetStore<ClassPreset> CLASSES = new PresetStore<>();

    private Presets() {}

    public static void clear() {
        SPECIES.clear();
        BACKGROUNDS.clear();
        CLASSES.clear();
    }
}
