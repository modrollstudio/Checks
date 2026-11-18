package studio.modroll.checks.preset;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.Checks;

public final class PresetStore<T extends Preset> {

    private volatile Map<ResourceLocation, T> presets = Map.of();

    public void set(Map<ResourceLocation, T> loaded) {
        presets = Map.copyOf(loaded);
    }

    public void clear() {
        set(Map.of());
    }

    public Map<ResourceLocation, T> all() {
        return presets;
    }

    public Optional<T> find(ResourceLocation id) {
        return Optional.ofNullable(presets.get(id));
    }

    public List<T> offered(boolean includeShipped) {
        return presets.values().stream()
                .filter(preset -> includeShipped || !preset.id().getNamespace().equals(Checks.MOD_ID))
                .sorted(Comparator.comparing(Preset::id))
                .toList();
    }
}
