package studio.modroll.checks.creation;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;

/** Kept apart from the rest of the build, so turning presets or trait skill choices off drops exactly these. */
public record PresetGrants(
        Map<Ability, Integer> bonuses,
        Set<ResourceLocation> skills,
        Set<Ability> saves,
        Set<ResourceLocation> traitSkills) {

    public static final PresetGrants NONE = new PresetGrants(Map.of(), Set.of(), Set.of(), Set.of());

    public PresetGrants {
        bonuses = Map.copyOf(bonuses);
        skills = Set.copyOf(skills);
        saves = Set.copyOf(saves);
        traitSkills = Set.copyOf(traitSkills);
    }

    public int bonus(Ability ability) {
        return bonuses.getOrDefault(ability, 0);
    }

    public static PresetGrants of(CreationSubmission submission, PresetOffer offer) {
        Map<Ability, Integer> bonuses = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            int bonus = submission.bonuses().get(ability.ordinal());
            if (bonus != 0) {
                bonuses.put(ability, bonus);
            }
        }
        PresetChoices choices = submission.choices();
        return new PresetGrants(
                bonuses,
                offer.background(choices.background())
                        .map(background -> Set.copyOf(background.skills()))
                        .orElse(Set.of()),
                offer.classPreset(choices.classPreset())
                        .map(classPreset -> Set.copyOf(classPreset.saves()))
                        .orElse(Set.of()),
                Set.copyOf(submission.speciesSkills()));
    }
}
