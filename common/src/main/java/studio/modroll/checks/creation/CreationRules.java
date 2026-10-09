package studio.modroll.checks.creation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.preset.Background;
import studio.modroll.checks.preset.PresetKind;

/** Validates a submission against the offer it was made from. Pure, so client and server share it. */
public final class CreationRules {

    private CreationRules() {}

    public static Optional<Rejection> validate(CreationSubmission submission, CreationOffer offer) {
        return validatePresets(submission.choices(), offer.presets())
                .or(() -> scoreProblem(submission, offer))
                .or(() -> skillProblem(submission, offer));
    }

    /** The method, base scores and background bonuses; the presets are assumed to be offered. */
    public static Optional<Rejection> scoreProblem(CreationSubmission submission, CreationOffer offer) {
        CreationMethod method = submission.method();
        if (!offer.methods().contains(method)) {
            return Optional.of(Rejection.METHOD_NOT_ALLOWED);
        }
        if (!offer.allows(method)) {
            return Optional.of(Rejection.METHOD_LOCKED);
        }
        if (submission.scores().size() != CreationSubmission.ABILITY_COUNT) {
            return Optional.of(Rejection.SCORE_COUNT);
        }
        return validateScores(submission.scores(), method, offer).or(() -> validateBonuses(submission, offer));
    }

    public static Optional<Rejection> skillProblem(CreationSubmission submission, CreationOffer offer) {
        SkillPlan plan = SkillPlan.of(offer, submission.choices());
        return plan.validate(submission.skills())
                .or(() -> plan.validateSpecies(submission.speciesSkills(), submission.skills()));
    }

    private static Optional<Rejection> validatePresets(PresetChoices choices, PresetOffer presets) {
        if (!presets.offers(PresetKind.SPECIES, choices.species())) {
            return Optional.of(Rejection.UNKNOWN_SPECIES);
        }
        if (!sizeOffered(choices, presets)) {
            return Optional.of(Rejection.SIZE_NOT_OFFERED);
        }
        if (!presets.offers(PresetKind.BACKGROUND, choices.background())) {
            return Optional.of(Rejection.UNKNOWN_BACKGROUND);
        }
        if (!presets.offers(PresetKind.CLASS, choices.classPreset())) {
            return Optional.of(Rejection.UNKNOWN_CLASS);
        }
        return Optional.empty();
    }

    /** No size picks the species' default; a picked size must be one the species offers. */
    private static boolean sizeOffered(PresetChoices choices, PresetOffer presets) {
        return choices.size()
                .map(size -> presets.species(choices.species())
                        .map(species -> species.offersSize(size))
                        .orElse(false))
                .orElse(true);
    }

    private static Optional<Rejection> validateScores(
            List<Integer> scores, CreationMethod method, CreationOffer offer) {
        return switch (method) {
            case STANDARD_ARRAY ->
                sameValues(scores, offer.standardArray()) ? Optional.empty() : Optional.of(Rejection.WRONG_ARRAY);
            case POINT_BUY -> validatePointBuy(scores, offer.pointBuy());
            case ROLL ->
                offer.rolledTotals(method)
                        .map(totals -> validateRoll(scores, totals))
                        .orElse(Optional.of(Rejection.NOT_ROLLED));
            case HARDCORE ->
                offer.rolledTotals(method)
                        .map(totals -> validateHardcore(scores, totals))
                        .orElse(Optional.of(Rejection.NOT_ROLLED));
        };
    }

    private static Optional<Rejection> validatePointBuy(List<Integer> scores, PointBuy pointBuy) {
        OptionalInt cost = pointBuy.totalCost(scores);
        if (cost.isEmpty()) {
            return Optional.of(Rejection.POINT_BUY_RANGE);
        }
        return cost.getAsInt() > pointBuy.budget() ? Optional.of(Rejection.POINTS_OVERSPENT) : Optional.empty();
    }

    private static Optional<Rejection> validateRoll(List<Integer> scores, List<Integer> totals) {
        return sameValues(scores, totals) ? Optional.empty() : Optional.of(Rejection.ROLLS_CHANGED);
    }

    private static Optional<Rejection> validateHardcore(List<Integer> scores, List<Integer> totals) {
        if (scores.equals(totals)) {
            return Optional.empty();
        }
        return Optional.of(sameValues(scores, totals) ? Rejection.HARDCORE_ORDER : Rejection.ROLLS_CHANGED);
    }

    /**
     * Bonuses go only to the background's abilities, split as one of the options that fit it, and never
     * past the cap. A Custom background takes no bonuses.
     */
    private static Optional<Rejection> validateBonuses(CreationSubmission submission, CreationOffer offer) {
        List<Integer> bonuses = submission.bonuses();
        if (bonuses.size() != CreationSubmission.ABILITY_COUNT) {
            return Optional.of(Rejection.BONUS_PATTERN);
        }
        Optional<Background> background =
                offer.presets().background(submission.choices().background());
        List<Ability> listed = background.map(Background::abilities).orElse(List.of());
        if (abilitiesWith(bonuses).anyMatch(ability -> !listed.contains(ability))) {
            return Optional.of(Rejection.BONUS_NOT_LISTED);
        }
        List<List<Integer>> options =
                background.map(offer.presets()::bonusOptionsFor).orElse(List.of());
        if (!matchesAnOption(bonuses, options)) {
            return Optional.of(Rejection.BONUS_PATTERN);
        }
        int max = offer.presets().bonusMaxScore();
        boolean overMax = abilitiesWith(bonuses)
                .anyMatch(ability -> submission.scores().get(ability.ordinal()) + bonuses.get(ability.ordinal()) > max);
        return overMax ? Optional.of(Rejection.BONUS_OVER_MAX) : Optional.empty();
    }

    /** With no option that fits, the only valid split is no bonus at all. */
    private static boolean matchesAnOption(List<Integer> bonuses, List<List<Integer>> options) {
        List<Integer> given =
                descending(bonuses.stream().filter(bonus -> bonus != 0).toList());
        if (options.isEmpty()) {
            return given.isEmpty();
        }
        return options.stream().map(CreationRules::descending).anyMatch(given::equals);
    }

    private static Stream<Ability> abilitiesWith(List<Integer> bonuses) {
        return IntStream.range(0, bonuses.size())
                .filter(index -> bonuses.get(index) != 0)
                .mapToObj(index -> Ability.values()[index]);
    }

    private static List<Integer> descending(List<Integer> values) {
        return values.stream().sorted(Comparator.reverseOrder()).toList();
    }

    private static boolean sameValues(List<Integer> a, List<Integer> b) {
        return a.size() == b.size() && counts(a).equals(counts(b));
    }

    private static Map<Integer, Long> counts(List<Integer> values) {
        return values.stream().collect(Collectors.groupingBy(value -> value, Collectors.counting()));
    }
}
