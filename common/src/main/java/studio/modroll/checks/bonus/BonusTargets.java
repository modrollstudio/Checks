package studio.modroll.checks.bonus;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.CheckKind;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.api.Stat;
import studio.modroll.checks.data.LenientJson;

/**
 * Which rolls a bonus source affects. An ability target covers that ability's checks and, as in 5e, the
 * checks of every skill it governs; contest sides count as checks. Saves are only covered by
 * {@code saves} and {@code allSaves}.
 */
public record BonusTargets(
        Set<ResourceLocation> skills, Set<Ability> abilities, Set<Ability> saves, boolean allChecks, boolean allSaves) {

    public BonusTargets {
        skills = Set.copyOf(skills);
        abilities = Set.copyOf(abilities);
        saves = Set.copyOf(saves);
    }

    public boolean appliesTo(CheckKind kind, Stat stat) {
        if (kind == CheckKind.SAVE) {
            return allSaves || (stat instanceof Ability ability && saves.contains(ability));
        }
        return allChecks
                || switch (stat) {
                    case Ability ability -> abilities.contains(ability);
                    case Skill skill -> skills.contains(skill.id()) || abilities.contains(skill.ability());
                };
    }

    public static BonusTargets parse(LenientJson json, Function<ResourceLocation, Optional<Skill>> skillLookup) {
        BonusTargets targets = new BonusTargets(
                parseSkills(json, skillLookup),
                parseAbilities(json, "abilities"),
                parseAbilities(json, "saves"),
                json.getBool("all_checks", false),
                json.getBool("all_saves", false));
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("'applies_to' targets no skill, ability, save, all_checks or all_saves");
        }
        return targets;
    }

    private boolean isEmpty() {
        return skills.isEmpty() && abilities.isEmpty() && saves.isEmpty() && !allChecks && !allSaves;
    }

    private static Set<ResourceLocation> parseSkills(
            LenientJson json, Function<ResourceLocation, Optional<Skill>> skillLookup) {
        Set<ResourceLocation> skills = new HashSet<>();
        for (String key : json.stringList("skills")) {
            Optional<Skill> skill =
                    Optional.ofNullable(ResourceLocation.tryParse(key)).flatMap(skillLookup);
            skill.ifPresentOrElse(
                    found -> skills.add(found.id()), () -> json.warn("unknown skill '" + key + "' (skipped)"));
        }
        return skills;
    }

    private static Set<Ability> parseAbilities(LenientJson json, String key) {
        Set<Ability> abilities = EnumSet.noneOf(Ability.class);
        for (String id : json.stringList(key)) {
            abilities.add(Ability.byId(id)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "'" + key + "' entry '" + id + "' is not one of str, dex, con, int, wis, cha")));
        }
        return abilities;
    }
}
