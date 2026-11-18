package studio.modroll.checks.preset;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.data.LenientJson;

/** Each reader throws IllegalArgumentException to reject the file. */
final class PresetJson {

    private PresetJson() {}

    static List<Ability> abilities(LenientJson json, String key) {
        List<Ability> abilities = new ArrayList<>();
        for (String id : json.stringList(key)) {
            Ability ability = Ability.byId(id)
                    .orElseThrow(
                            () -> new IllegalArgumentException("'" + key + "' names unknown ability '" + id + "'"));
            if (abilities.contains(ability)) {
                throw new IllegalArgumentException("'" + key + "' lists '" + id + "' twice");
            }
            abilities.add(ability);
        }
        return List.copyOf(abilities);
    }

    static Optional<Skill> skill(String text, Function<ResourceLocation, Optional<Skill>> skills) {
        return Optional.ofNullable(ResourceLocation.tryParse(text)).flatMap(skills);
    }

    static ResourceLocation id(String text, String namespace) {
        ResourceLocation id =
                text.contains(":") ? ResourceLocation.tryParse(text) : ResourceLocation.tryBuild(namespace, text);
        if (id == null) {
            throw new IllegalArgumentException("'" + text + "' is not a valid id");
        }
        return id;
    }

    static ResourceLocation icon(LenientJson json, PresetKind kind, Predicate<ResourceLocation> itemExists) {
        Optional<String> text = json.optionalString("icon");
        if (text.isEmpty()) {
            return kind.defaultIcon();
        }
        Optional<ResourceLocation> item = text.map(ResourceLocation::tryParse).filter(itemExists);
        if (item.isEmpty()) {
            json.warn("'icon' names no registered item, showing " + kind.defaultIcon() + " instead");
        }
        return item.orElse(kind.defaultIcon());
    }
}
