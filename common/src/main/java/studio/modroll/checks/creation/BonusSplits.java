package studio.modroll.checks.creation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BonusSplits {

    private BonusSplits() {}

    public static boolean reachable(List<Integer> given, List<List<Integer>> options) {
        List<Integer> placed = descending(given);
        return options.stream().map(BonusSplits::descending).anyMatch(option -> dominates(option, placed));
    }

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
