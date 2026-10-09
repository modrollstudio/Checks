package studio.modroll.checks.creation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.checks.sheet.SheetText;

/**
 * The confirm page's paragraph, in the second person: who the character is by species, background and
 * class, their two best abilities and their proficient skills, e.g. "You are Dev1, Dwarf by birth,
 * Soldier by upbringing and Fighter by calling. …". A Custom or unchosen preset is left out.
 */
public final class CharacterStory {

    static final int BEST_ABILITIES = 2;
    private static final String KEY = "screen.checks.creation.story";

    private CharacterStory() {}

    public static Component of(
            Component name,
            Map<PresetKind, Optional<ResourceLocation>> presets,
            Map<Ability, Integer> scores,
            List<ResourceLocation> skills) {
        return Component.translatable(KEY, identity(name, presets), best(scores), skillsSentence(skills));
    }

    private static Component identity(Component name, Map<PresetKind, Optional<ResourceLocation>> presets) {
        List<Component> clauses = List.of(PresetKind.values()).stream()
                .filter(kind -> presets.getOrDefault(kind, Optional.empty()).isPresent())
                .<Component>map(kind ->
                        Component.translatable(KEY + "." + kind.id(), SheetText.presetName(kind, presets.get(kind))))
                .toList();
        if (clauses.isEmpty()) {
            return Component.translatable(KEY + ".name", name);
        }
        return Component.translatable(KEY + ".name_and_identity", name, SheetText.andList(clauses));
    }

    private static Component best(Map<Ability, Integer> scores) {
        List<Component> best = bestAbilities(scores).stream()
                .<Component>map(ability ->
                        Component.translatable(KEY + ".ability", SheetText.abilityName(ability), scores.get(ability)))
                .toList();
        if (best.isEmpty()) {
            return Component.empty();
        }
        String key = best.size() == 1 ? KEY + ".best.one" : KEY + ".best";
        return Component.translatable(key, SheetText.andList(best));
    }

    /** The highest scores first, ties in STR to CHA order. */
    static List<Ability> bestAbilities(Map<Ability, Integer> scores) {
        return scores.keySet().stream()
                .sorted(Comparator.comparing((Ability ability) -> scores.get(ability))
                        .reversed()
                        .thenComparing(Ability::ordinal))
                .limit(BEST_ABILITIES)
                .toList();
    }

    private static Component skillsSentence(List<ResourceLocation> skills) {
        if (skills.isEmpty()) {
            return Component.translatable(KEY + ".no_skills");
        }
        return Component.translatable(
                KEY + ".skills",
                SheetText.andList(skills.stream().map(SheetText::skillName).toList()));
    }
}
