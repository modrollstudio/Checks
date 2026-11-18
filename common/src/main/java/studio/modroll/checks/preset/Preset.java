package studio.modroll.checks.preset;

import net.minecraft.resources.ResourceLocation;

/** Its id is its datapack file's id. */
public interface Preset {

    ResourceLocation id();

    ResourceLocation icon();
}
