package studio.modroll.checks.level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;

/**
 * One player's character level and total character XP, the ability score improvements they chose (each an ability
 * to increase map) and the skills they chose for Expertise, both in the order chosen, and how many of their Hit Dice
 * are spent. Only as many choices as the level and class earn apply, so a choice a class gave stops counting once the
 * class or the level no longer earns it.
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

    /** What the first {@code earned} chosen improvements add to {@code ability}. */
    public int improvementBonus(Ability ability, int earned) {
        return improvements.stream()
                .limit(Math.max(0, earned))
                .mapToInt(improvement -> improvement.getOrDefault(ability, 0))
                .sum();
    }

    /** The first {@code earned} skills chosen for Expertise. */
    public Set<ResourceLocation> expertise(int earned) {
        return new LinkedHashSet<>(expertise.subList(0, Math.clamp(earned, 0, expertise.size())));
    }

    /** One Hit Die per level, less those spent. */
    public int hitDiceLeft() {
        return Math.max(0, level - hitDiceSpent);
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

    /** Never more spent than the level has dice. */
    public PlayerLevel withHitDiceSpent(int spent) {
        return new PlayerLevel(level, xp, improvements, expertise, Math.clamp(spent, 0, level));
    }
}
