package studio.modroll.checks.level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import studio.modroll.checks.api.Ability;

/**
 * One player's character level and total character XP, the ability score improvements they chose (each
 * an ability to increase map), and the hit die of their class at each level gained, kept for hit points.
 */
public record PlayerLevel(int level, int xp, List<Map<Ability, Integer>> improvements, Map<Integer, Integer> hitDice) {

    public static final PlayerLevel START = new PlayerLevel(Levels.MIN_LEVEL, 0, List.of(), Map.of());

    public PlayerLevel {
        improvements = improvements.stream().map(Map::copyOf).toList();
        hitDice = Map.copyOf(hitDice);
    }

    /** What the chosen improvements add to {@code ability}. */
    public int improvementBonus(Ability ability) {
        return improvements.stream()
                .mapToInt(improvement -> improvement.getOrDefault(ability, 0))
                .sum();
    }

    public PlayerLevel withLevelAndXp(int newLevel, int newXp) {
        return new PlayerLevel(newLevel, newXp, improvements, hitDice);
    }

    public PlayerLevel withImprovement(Map<Ability, Integer> improvement) {
        List<Map<Ability, Integer>> chosen = new ArrayList<>(improvements);
        chosen.add(improvement);
        return new PlayerLevel(level, xp, chosen, hitDice);
    }

    public PlayerLevel withHitDie(int atLevel, int sides) {
        Map<Integer, Integer> dice = new HashMap<>(hitDice);
        dice.put(atLevel, sides);
        return new PlayerLevel(level, xp, improvements, dice);
    }
}
