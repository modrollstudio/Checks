package studio.modroll.checks.level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;

/**
 * Only as many choices as the level and class earn apply, so a choice a class gave stops counting once the class or
 * level no longer earns it.
 */
public record PlayerLevel(
        int level,
        int xp,
        List<Map<Ability, Integer>> improvements,
        List<ResourceLocation> expertise,
        int hitDiceSpent) {

    public static final PlayerLevel START = new PlayerLevel(Levels.MIN_LEVEL, 0, List.of(), List.of(), 0);

    public PlayerLevel {
        improvements = improvements.stream().map(Map::copyOf).toList();
        expertise = List.copyOf(expertise);
        hitDiceSpent = Math.max(0, hitDiceSpent);
    }

    public int improvementBonus(Ability ability, int earned) {
        return improvements.stream()
                .limit(Math.max(0, earned))
                .mapToInt(improvement -> improvement.getOrDefault(ability, 0))
                .sum();
    }

    public Set<ResourceLocation> expertise(int earned) {
        return new LinkedHashSet<>(expertise.subList(0, Math.clamp(earned, 0, expertise.size())));
    }

    public PlayerLevel withLevelAndXp(int newLevel, int newXp) {
        return new PlayerLevel(newLevel, newXp, improvements, expertise, hitDiceSpent);
    }

    public PlayerLevel withImprovement(Map<Ability, Integer> improvement) {
        List<Map<Ability, Integer>> chosen = new ArrayList<>(improvements);
        chosen.add(improvement);
        return new PlayerLevel(level, xp, chosen, expertise, hitDiceSpent);
    }

    public PlayerLevel withExpertise(List<ResourceLocation> skills) {
        List<ResourceLocation> chosen = new ArrayList<>(expertise);
        chosen.addAll(skills);
        return new PlayerLevel(level, xp, improvements, chosen, hitDiceSpent);
    }

    public PlayerLevel withHitDiceSpent(int spent) {
        return new PlayerLevel(level, xp, improvements, expertise, Math.clamp(spent, 0, level));
    }
}
