package studio.modroll.checks.preset;

import net.minecraft.resources.ResourceLocation;

/** A species, background or class preset; its id is its datapack file's id. */
public interface Preset {

    ResourceLocation id();

    /** The item drawn for this preset on the creation screen. */
    ResourceLocation icon();
}
