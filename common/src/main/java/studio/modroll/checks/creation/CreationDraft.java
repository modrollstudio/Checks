package studio.modroll.checks.creation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.body.Size;
import studio.modroll.checks.body.SizeOption;
import studio.modroll.checks.preset.Background;
import studio.modroll.checks.preset.ClassPreset;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.checks.preset.Species;

public final class CreationDraft {

    private static final int EMPTY = -1;
    private static final List<CreationStep> STEPS_WITHOUT_PRESETS =
            List.of(CreationStep.SCORES, CreationStep.SKILLS, CreationStep.CONFIRM);

    private CreationOffer offer;
    private CreationStep step;
    private CreationMethod method;
    private List<Integer> pool = List.of();
    private final int[] chipOf = new int[CreationSubmission.ABILITY_COUNT];
    private final int[] bought = new int[CreationSubmission.ABILITY_COUNT];
    private final int[] bonuses = new int[CreationSubmission.ABILITY_COUNT];
    private OptionalInt selectedChip = OptionalInt.empty();
    private final List<ResourceLocation> skills = new ArrayList<>();
    private final List<ResourceLocation> speciesSkills = new ArrayList<>();
    private final Map<PresetKind, Optional<ResourceLocation>> chosen = new EnumMap<>(PresetKind.class);
    private Optional<Size> chosenSize = Optional.empty();

    public CreationDraft(CreationOffer offer) {
        this.offer = offer;
        this.step = steps().getFirst();
        select(offer.lockedMethod().orElse(offer.methods().getFirst()));
    }

    public CreationOffer offer() {
        return offer;
    }

    public List<CreationStep> steps() {
        return offer.presets().enabled() ? List.of(CreationStep.values()) : STEPS_WITHOUT_PRESETS;
    }

    public CreationStep step() {
        return step;
    }

    public boolean onFirstStep() {
        return steps().indexOf(step) == 0;
    }

    public boolean onLastStep() {
        return steps().indexOf(step) == steps().size() - 1;
    }

    public void back() {
        if (!onFirstStep()) {
            step = steps().get(steps().indexOf(step) - 1);
        }
    }

    public void next() {
        if (!onLastStep() && stepProblem().isEmpty()) {
            step = steps().get(steps().indexOf(step) + 1);
        }
    }

    public Optional<Component> stepProblem() {
        Optional<PresetKind> kind = step.presetKind();
        if (kind.isPresent()) {
            return isChosen(kind.get())
                    ? Optional.empty()
                    : Optional.of(Component.translatable(
                            "screen.checks.creation.choose." + kind.get().id()));
        }
        Optional<Rejection> problem =
                switch (step) {
                    case SCORES ->
                        awaitingRoll()
                                ? Optional.of(Rejection.NOT_ROLLED)
                                : CreationRules.scoreProblem(submission(), offer);
                    case SKILLS -> CreationRules.skillProblem(submission(), offer);
                    default -> problem();
                };
        return problem.map(rejection -> rejection.message(offer, choices()));
    }

    public boolean isChosen(PresetKind kind) {
        return chosen.containsKey(kind);
    }

    public Optional<ResourceLocation> choice(PresetKind kind) {
        return chosen.getOrDefault(kind, Optional.empty());
    }

    public void choose(PresetKind kind, Optional<ResourceLocation> preset) {
        if (!offer.presets().offers(kind, preset) || preset.equals(chosen.get(kind))) {
            return;
        }
        chosen.put(kind, preset);
        if (kind == PresetKind.SPECIES) {
            chosenSize = Optional.empty();
        }
        if (kind == PresetKind.BACKGROUND) {
            Arrays.fill(bonuses, 0);
        }
        if (kind != PresetKind.CLASS) {
            speciesSkills.clear();
        }
        if (kind != PresetKind.SPECIES) {
            skills.clear();
        }
    }

    public PresetChoices choices() {
        return new PresetChoices(
                choice(PresetKind.SPECIES), choice(PresetKind.BACKGROUND), choice(PresetKind.CLASS), chosenSize);
    }

    public Optional<Species> species() {
        return offer.presets().species(choice(PresetKind.SPECIES));
    }

    public Optional<SizeOption> size() {
        return species().flatMap(species -> species.size(chosenSize));
    }

    public void chooseSize(Size newSize) {
        if (species().filter(species -> species.offersSize(newSize)).isPresent()) {
            chosenSize = Optional.of(newSize);
        }
    }

    public Optional<Background> background() {
        return offer.presets().background(choice(PresetKind.BACKGROUND));
    }

    public Optional<ClassPreset> classPreset() {
        return offer.presets().classPreset(choice(PresetKind.CLASS));
    }

    public CreationMethod method() {
        return method;
    }

    public void select(CreationMethod newMethod) {
        if (!offer.allows(newMethod)) {
            return;
        }
        method = newMethod;
        restart();
    }

    public void update(CreationOffer newOffer) {
        boolean wasAwaitingRoll = awaitingRoll();
        offer = newOffer;
        chosen.entrySet().removeIf(choice -> !newOffer.presets().offers(choice.getKey(), choice.getValue()));
        chosenSize = chosenSize.filter(
                kept -> species().filter(species -> species.offersSize(kept)).isPresent());
        Arrays.stream(Ability.values())
                .filter(ability -> !takesBonus(ability))
                .forEach(ability -> bonuses[ability.ordinal()] = 0);
        skills.removeIf(id -> !newOffer.offersSkill(id));
        speciesSkills.removeIf(id -> !newOffer.offersSkill(id));
        if (!steps().contains(step)) {
            step = steps().getFirst();
        }
        if (!newOffer.allows(method)) {
            select(newOffer.lockedMethod().orElse(newOffer.methods().getFirst()));
        } else if (wasAwaitingRoll) {
            restart();
        }
    }

    public boolean awaitingRoll() {
        return method.rollsDice() && offer.rolledTotals(method).isEmpty();
    }

    public boolean usesPool() {
        return (method == CreationMethod.STANDARD_ARRAY || method == CreationMethod.ROLL) && !awaitingRoll();
    }

    public OptionalInt score(Ability ability) {
        if (method == CreationMethod.POINT_BUY) {
            return OptionalInt.of(bought[ability.ordinal()]);
        }
        return chip(ability).stream().map(pool::get).findFirst();
    }

    public OptionalInt chip(Ability ability) {
        int chip = chipOf[ability.ordinal()];
        return chip == EMPTY ? OptionalInt.empty() : OptionalInt.of(chip);
    }

    public List<Integer> pool() {
        return pool;
    }

    public boolean inPool(int chip) {
        return Arrays.stream(chipOf).noneMatch(placed -> placed == chip);
    }

    public OptionalInt selectedChip() {
        return selectedChip;
    }

    public void selectChip(int chip) {
        if (!usesPool() || !inPool(chip)) {
            return;
        }
        boolean alreadySelected = selectedChip.equals(OptionalInt.of(chip));
        selectedChip = alreadySelected ? OptionalInt.empty() : OptionalInt.of(chip);
    }

    public boolean clickAbility(Ability ability) {
        if (!usesPool()) {
            return false;
        }
        if (selectedChip.isPresent()) {
            chipOf[ability.ordinal()] = selectedChip.getAsInt();
            selectedChip = OptionalInt.empty();
            return true;
        }
        boolean placed = chipOf[ability.ordinal()] != EMPTY;
        chipOf[ability.ordinal()] = EMPTY;
        return placed;
    }

    public boolean canUseSuggested() {
        return usesPool() && classPreset().isPresent();
    }

    public void useSuggested() {
        if (!canUseSuggested()) {
            return;
        }
        List<Integer> highestFirst = IntStream.range(0, pool.size())
                .boxed()
                .sorted(Comparator.comparing(pool::get, Comparator.reverseOrder()))
                .toList();
        List<Ability> order = classPreset().orElseThrow().suggestedOrder();
        for (int i = 0; i < order.size(); i++) {
            chipOf[order.get(i).ordinal()] = highestFirst.get(i);
        }
        selectedChip = OptionalInt.empty();
    }

    public boolean canRaise(Ability ability) {
        return method == CreationMethod.POINT_BUY
                && nextBuyable(bought[ability.ordinal()], 1)
                        .filter(next -> costOfChange(ability, next) <= pointsLeft())
                        .isPresent();
    }

    public boolean canLower(Ability ability) {
        return method == CreationMethod.POINT_BUY
                && nextBuyable(bought[ability.ordinal()], -1).isPresent();
    }

    public void raise(Ability ability) {
        if (canRaise(ability)) {
            bought[ability.ordinal()] =
                    nextBuyable(bought[ability.ordinal()], 1).orElseThrow();
        }
    }

    public void lower(Ability ability) {
        if (canLower(ability)) {
            bought[ability.ordinal()] =
                    nextBuyable(bought[ability.ordinal()], -1).orElseThrow();
        }
    }

    public int pointsLeft() {
        List<Integer> scores = Arrays.stream(bought).boxed().toList();
        return offer.pointBuy().budget() - offer.pointBuy().totalCost(scores).orElse(0);
    }

    public boolean takesBonus(Ability ability) {
        return background().map(b -> b.abilities().contains(ability)).orElse(false);
    }

    public int bonus(Ability ability) {
        return bonuses[ability.ordinal()];
    }

    public List<List<Integer>> bonusOptions() {
        return background().map(offer.presets()::bonusOptionsFor).orElse(List.of());
    }

    public enum BonusLimit {
        ALL_USED,
        NO_SPLIT,
        OVER_MAX
    }

    public boolean canRaiseBonus(Ability ability) {
        return takesBonus(ability) && raiseLimit(ability).isEmpty();
    }

    public Optional<BonusLimit> raiseLimit(Ability ability) {
        int[] raised = bonuses.clone();
        raised[ability.ordinal()]++;
        if (!BonusSplits.reachable(placed(raised), bonusOptions())) {
            boolean complete = bonusesLeft().contains(List.of());
            return Optional.of(complete ? BonusLimit.ALL_USED : BonusLimit.NO_SPLIT);
        }
        boolean overMax = finalScore(ability).stream()
                .anyMatch(score -> score + 1 > offer.presets().bonusMaxScore());
        return overMax ? Optional.of(BonusLimit.OVER_MAX) : Optional.empty();
    }

    public List<List<Integer>> bonusesLeft() {
        return BonusSplits.remaining(placed(bonuses), bonusOptions());
    }

    public boolean canLowerBonus(Ability ability) {
        return bonus(ability) > 0;
    }

    public void raiseBonus(Ability ability) {
        if (canRaiseBonus(ability)) {
            bonuses[ability.ordinal()]++;
        }
    }

    public void lowerBonus(Ability ability) {
        if (canLowerBonus(ability)) {
            bonuses[ability.ordinal()]--;
        }
    }

    public OptionalInt finalScore(Ability ability) {
        OptionalInt base = score(ability);
        return base.isPresent() ? OptionalInt.of(base.getAsInt() + bonus(ability)) : base;
    }

    public SkillPlan skillPlan() {
        return SkillPlan.of(offer, choices());
    }

    public boolean picked(ResourceLocation skill) {
        return skills.contains(skill);
    }

    public int pickedCount() {
        return skills.size() + speciesSkills.size();
    }

    public int requiredCount() {
        SkillPlan plan = skillPlan();
        return plan.required() + plan.speciesRequired();
    }

    public boolean speciesPicked(ResourceLocation skill) {
        return speciesSkills.contains(skill);
    }

    public List<ResourceLocation> speciesPicks() {
        return List.copyOf(speciesSkills);
    }

    public boolean canPick(ResourceLocation skill) {
        return !speciesSkills.contains(skill) && skillPlan().canPick(skill, skills);
    }

    public boolean canPickForSpecies(ResourceLocation skill) {
        return skillPlan().canPickForSpecies(skill, speciesSkills, skills);
    }

    public void toggleSpeciesSkill(ResourceLocation skill) {
        if (!speciesSkills.remove(skill) && canPickForSpecies(skill)) {
            speciesSkills.add(skill);
        }
    }

    public List<ResourceLocation> picks() {
        return List.copyOf(skills);
    }

    public void toggleSkill(ResourceLocation skill) {
        if (skills.remove(skill)) {
            return;
        }
        if (canPick(skill)) {
            skills.add(skill);
        }
    }

    public Optional<Rejection> problem() {
        if (awaitingRoll()) {
            return Optional.of(Rejection.NOT_ROLLED);
        }
        return CreationRules.validate(submission(), offer);
    }

    public CreationSubmission submission() {
        List<Integer> scores = Arrays.stream(Ability.values())
                .map(this::score)
                .filter(OptionalInt::isPresent)
                .map(OptionalInt::getAsInt)
                .toList();
        return new CreationSubmission(
                method, scores, Arrays.stream(bonuses).boxed().toList(), skills, choices(), speciesSkills);
    }

    private static List<Integer> placed(int[] bonuses) {
        return Arrays.stream(bonuses).filter(bonus -> bonus != 0).boxed().toList();
    }

    private void restart() {
        selectedChip = OptionalInt.empty();
        pool = switch (method) {
            case STANDARD_ARRAY -> offer.standardArray();
            case POINT_BUY -> List.of();
            case ROLL, HARDCORE -> offer.rolledTotals(method).orElse(List.of());
        };
        boolean hardcoreRolled = method == CreationMethod.HARDCORE && !pool.isEmpty();
        IntStream.range(0, chipOf.length).forEach(i -> chipOf[i] = hardcoreRolled ? i : EMPTY);
        Arrays.fill(bought, offer.pointBuy().min());
    }

    private Optional<Integer> nextBuyable(int score, int direction) {
        return offer.pointBuy().costs().keySet().stream()
                .filter(candidate -> direction > 0 ? candidate > score : candidate < score)
                .min((a, b) -> direction > 0 ? Integer.compare(a, b) : Integer.compare(b, a));
    }

    private int costOfChange(Ability ability, int newScore) {
        return offer.pointBuy().costs().get(newScore) - offer.pointBuy().costs().get(bought[ability.ordinal()]);
    }
}
