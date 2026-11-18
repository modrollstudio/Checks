package studio.modroll.checks.preset;

import net.minecraft.resources.ResourceLocation;

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

    public String id() {
        return id;
    }

    public ResourceLocation defaultIcon() {
        return defaultIcon;
    }

    public String directory() {
        return "checks/" + id;
    }
}
