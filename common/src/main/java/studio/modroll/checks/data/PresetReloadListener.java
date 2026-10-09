package studio.modroll.checks.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import studio.modroll.checks.Checks;
import studio.modroll.checks.preset.Background;
import studio.modroll.checks.preset.ClassPreset;
import studio.modroll.checks.preset.Preset;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.checks.preset.PresetStore;
import studio.modroll.checks.preset.Presets;
import studio.modroll.checks.preset.Species;

/**
 * Loads one kind of preset. A file with {@code "removed": true} removes the entry a lower pack defined
 * at the same path. Backgrounds and classes resolve skills against the current {@link SkillStore}, so
 * they must run after the skill listener.
 */
public final class PresetReloadListener<T extends Preset> extends DatapackListener<T> {

    private static final String REMOVED_KEY = "removed";

    public PresetReloadListener(
            PresetKind kind, PresetStore<T> store, BiFunction<ResourceLocation, JsonObject, T> parser) {
        super(kind.directory(), kind.id(), parser, store::set);
    }

    public static PresetReloadListener<Species> species() {
        return new PresetReloadListener<>(
                PresetKind.SPECIES,
                Presets.SPECIES,
                (id, json) -> Species.parse(id, json, BuiltInRegistries.ITEM::containsKey, Checks.LOG::warn));
    }

    public static PresetReloadListener<Background> backgrounds() {
        return new PresetReloadListener<>(
                PresetKind.BACKGROUND,
                Presets.BACKGROUNDS,
                (id, json) -> Background.parse(
                        id, json, SkillStore::find, BuiltInRegistries.ITEM::containsKey, Checks.LOG::warn));
    }

    public static PresetReloadListener<ClassPreset> classes() {
        return new PresetReloadListener<>(
                PresetKind.CLASS,
                Presets.CLASSES,
                (id, json) -> ClassPreset.parse(
                        id,
                        json,
                        SkillStore::find,
                        SkillStore.skills().keySet(),
                        BuiltInRegistries.ITEM::containsKey,
                        Checks.LOG::warn));
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        super.apply(withoutRemoved(files), resourceManager, profiler);
    }

    private static Map<ResourceLocation, JsonElement> withoutRemoved(Map<ResourceLocation, JsonElement> files) {
        Map<ResourceLocation, JsonElement> kept = new HashMap<>(files);
        kept.values().removeIf(PresetReloadListener::removed);
        return kept;
    }

    private static boolean removed(JsonElement file) {
        if (!file.isJsonObject()) {
            return false;
        }
        JsonElement flag = file.getAsJsonObject().get(REMOVED_KEY);
        return flag != null
                && flag.isJsonPrimitive()
                && flag.getAsJsonPrimitive().isBoolean()
                && flag.getAsBoolean();
    }
}
