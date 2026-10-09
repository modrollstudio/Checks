package studio.modroll.checks.trait;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.data.LenientJson;

/**
 * One {@code data/<ns>/checks/trait/*.json} file: what a species trait does. A trait a species lists with
 * no file is flavor only: a name and a description.
 */
public record Trait(ResourceLocation id, List<TraitEffect> effects) {

    public static final int FORMAT_VERSION = 1;

    public Trait {
        effects = List.copyOf(effects);
    }

    public static Trait parse(
            ResourceLocation id, JsonObject json, TraitEffect.Lookups lookups, Consumer<String> warn) {
        LenientJson j = new LenientJson(json, "trait " + id, warn);
        j.checkFormatVersion(FORMAT_VERSION);
        List<TraitEffect> effects = j.objectList("effects").stream()
                .map(effect -> TraitEffect.parse(effect, lookups))
                .toList();
        j.finish();
        return new Trait(id, effects);
    }
}
