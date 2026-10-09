package studio.modroll.checks.creation;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.preset.Background;
import studio.modroll.checks.preset.ClassPreset;

/**
 * The skill rules for one background and class. The background's skills are fixed. The class adds its
 * number of picks from its list; each background skill on that list would be a wasted pick, so it frees
 * one pick for any other skill instead (the 5e overlap rule). A Custom background gives the configured
 * number of free picks from every skill instead of fixed skills; a Custom class adds no picks and no list.
 * The species' trait skill choices are picked apart, from their own list, and never repeat another skill.
 * Pure, so client and server share it.
 *
 * @param classPicks the class's picks, or 0 for a Custom class
 * @param freePicks the Custom background's picks of any skill, or 0 for a real background
 * @param overlap the background skills that are also on the class list
 * @param speciesOptions the skills the species' choices may pick, the background's left out
 * @param speciesRequired how many the species' choices pick
 */
public record SkillPlan(
        Set<ResourceLocation> offered,
        Set<ResourceLocation> fixed,
        Set<ResourceLocation> classOptions,
        int classPicks,
        int freePicks,
        Set<ResourceLocation> overlap,
        int required,
        Set<ResourceLocation> speciesOptions,
        int speciesRequired) {

    public SkillPlan {
        offered = Set.copyOf(offered);
        fixed = Set.copyOf(fixed);
        classOptions = Set.copyOf(classOptions);
        overlap = Set.copyOf(overlap);
        speciesOptions = Set.copyOf(speciesOptions);
    }

    /** Never more picks than there are skills left to pick, so a datapack with few skills cannot block creation. */
    public static SkillPlan of(CreationOffer offer, PresetChoices choices) {
        Set<ResourceLocation> offered = offer.skills().stream().map(Skill::id).collect(Collectors.toSet());
        Optional<Background> background = offer.presets().background(choices.background());
        Optional<ClassPreset> classPreset = offer.presets().classPreset(choices.classPreset());
        Set<ResourceLocation> fixed =
                background.map(b -> within(b.skills(), offered)).orElse(Set.of());
        int freePicks = background.isPresent() ? 0 : offer.skillChoices();
        Set<ResourceLocation> options =
                classPreset.map(c -> within(c.skillOptions(), offered)).orElse(offered);
        int classPicks =
                classPreset.map(c -> Math.min(c.skillChoices(), options.size())).orElse(0);
        Set<ResourceLocation> overlap = classPreset.isPresent()
                ? fixed.stream().filter(options::contains).collect(Collectors.toSet())
                : Set.of();
        int required = Math.min(classPicks + freePicks, offered.size() - fixed.size());
        SkillChoice speciesChoice = offer.speciesSkillChoice(choices.species());
        Set<ResourceLocation> speciesOptions = (speciesChoice.from().isEmpty()
                        ? offered
                        : within(List.copyOf(speciesChoice.from()), offered))
                .stream().filter(skill -> !fixed.contains(skill)).collect(Collectors.toSet());
        int speciesRequired = Math.min(speciesChoice.count(), speciesOptions.size());
        return new SkillPlan(
                offered, fixed, options, classPicks, freePicks, overlap, required, speciesOptions, speciesRequired);
    }

    /** The class picks freed by overlapping background skills, one each, never more than the class gives. */
    public int overlapPicks() {
        return Math.min(overlap.size(), classPicks);
    }

    /** How many picks may be skills outside the class list. */
    public int offListAllowance() {
        return freePicks + overlapPicks();
    }

    public boolean onClassList(ResourceLocation skill) {
        return classOptions.contains(skill);
    }

    /** Whether picking {@code skill} next keeps {@code picks} within the rules. */
    public boolean canPick(ResourceLocation skill, Collection<ResourceLocation> picks) {
        boolean free = !picks.contains(skill) && offered.contains(skill) && !fixed.contains(skill);
        boolean allowed = onClassList(skill) || offListPicks(picks) < offListAllowance();
        return free && allowed && picks.size() < required;
    }

    public long offListPicks(Collection<ResourceLocation> picks) {
        return picks.stream().filter(skill -> !onClassList(skill)).count();
    }

    public Optional<Rejection> validate(List<ResourceLocation> picks) {
        if (new HashSet<>(picks).size() != picks.size()) {
            return Optional.of(Rejection.DUPLICATE_SKILL);
        }
        if (!offered.containsAll(picks)) {
            return Optional.of(Rejection.UNKNOWN_SKILL);
        }
        if (picks.stream().anyMatch(fixed::contains)) {
            return Optional.of(Rejection.SKILL_ALREADY_PROFICIENT);
        }
        if (offListPicks(picks) > offListAllowance()) {
            return Optional.of(Rejection.SKILL_NOT_ON_CLASS_LIST);
        }
        return picks.size() == required ? Optional.empty() : Optional.of(Rejection.SKILL_COUNT);
    }

    /** Whether picking {@code skill} next with the species' choices keeps both pick lists within the rules. */
    public boolean canPickForSpecies(
            ResourceLocation skill, Collection<ResourceLocation> speciesPicks, Collection<ResourceLocation> picks) {
        return speciesOptions.contains(skill)
                && !speciesPicks.contains(skill)
                && !picks.contains(skill)
                && speciesPicks.size() < speciesRequired;
    }

    /** The species' picks, checked after the class and background picks they must not repeat. */
    public Optional<Rejection> validateSpecies(List<ResourceLocation> speciesPicks, List<ResourceLocation> picks) {
        boolean repeats = new HashSet<>(speciesPicks).size() != speciesPicks.size()
                || speciesPicks.stream().anyMatch(picks::contains);
        if (repeats) {
            return Optional.of(Rejection.DUPLICATE_SKILL);
        }
        if (speciesPicks.stream().anyMatch(fixed::contains)) {
            return Optional.of(Rejection.SKILL_ALREADY_PROFICIENT);
        }
        if (!speciesOptions.containsAll(speciesPicks)) {
            return Optional.of(Rejection.SKILL_NOT_FROM_SPECIES);
        }
        return speciesPicks.size() == speciesRequired ? Optional.empty() : Optional.of(Rejection.SPECIES_SKILL_COUNT);
    }

    private static Set<ResourceLocation> within(List<ResourceLocation> skills, Set<ResourceLocation> offered) {
        return skills.stream().filter(offered::contains).collect(Collectors.toSet());
    }
}
