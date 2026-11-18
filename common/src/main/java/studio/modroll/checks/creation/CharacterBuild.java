package studio.modroll.checks.creation;

import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;

public record CharacterBuild(
        CreationMethod method,
        Map<Ability, Integer> scores,
        Set<ResourceLocation> skills,
        PresetChoices choices,
        PresetGrants grants) {

    public CharacterBuild {
        scores = Map.copyOf(scores);
        skills = Set.copyOf(skills);
    }

    public int score(Ability ability) {
        return scores.get(ability);
    }
}
