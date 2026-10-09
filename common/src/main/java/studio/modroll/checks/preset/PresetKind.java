package studio.modroll.checks.preset;

import net.minecraft.resources.ResourceLocation;

/** The three kinds of character preset, each loaded from {@code data/<ns>/checks/<id>/*.json}. */
public enum PresetKind {
    SPECIES("species", "armor_stand"),
    BACKGROUND("background", "book"),
    CLASS("class", "wooden_sword");

    private final String id;
    private final ResourceLocation defaultIcon;

    PresetKind(String id, String defaultIcon) {
        this.id = id;
        this.defaultIcon = ResourceLocation.withDefaultNamespace(defaultIcon);
    }

    /** The lowercase id used in datapack paths, NBT and lang keys. */
    public String id() {
        return id;
    }

    /** The item shown for a preset of this kind whose file names no icon. */
    public ResourceLocation defaultIcon() {
        return defaultIcon;
    }

    public String directory() {
        return "checks/" + id;
    }
}
