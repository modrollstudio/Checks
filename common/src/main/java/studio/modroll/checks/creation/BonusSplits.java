package studio.modroll.checks.creation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * How far a player's background bonuses are from an allowed split. Bonuses are placed one point at a
 * time, so a partial split is still reachable while some option can hold each placed bonus at least as
 * large. Pure, for the creation screen.
 */
public final class BonusSplits {

    private BonusSplits() {}

    /** Whether {@code given} (the non-zero bonuses) can still grow into one of {@code options}. */
    public static boolean reachable(List<Integer> given, List<List<Integer>> options) {
        List<Integer> placed = descending(given);
        return options.stream().map(BonusSplits::descending).anyMatch(option -> dominates(option, placed));
    }

    /**
     * The bonuses each option still needs on top of {@code given}, for the options that contain every
     * given bonus exactly; an empty remainder means the split is complete.
     */
    public static List<List<Integer>> remaining(List<Integer> given, List<List<Integer>> options) {
        List<List<Integer>> remainders = new ArrayList<>();
        for (List<Integer> option : options) {
            List<Integer> left = new ArrayList<>(option);
            boolean containsAll = given.stream().allMatch(bonus -> left.remove(bonus));
            List<Integer> remainder = descending(left);
            if (containsAll && !remainders.contains(remainder)) {
                remainders.add(remainder);
            }
        }
        return remainders;
    }

    private static boolean dominates(List<Integer> option, List<Integer> placed) {
        if (placed.size() > option.size()) {
            return false;
        }
        for (int i = 0; i < placed.size(); i++) {
            if (placed.get(i) > option.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static List<Integer> descending(List<Integer> values) {
        return values.stream().sorted(Comparator.reverseOrder()).toList();
    }
}
