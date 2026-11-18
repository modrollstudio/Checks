package studio.modroll.checks.uishots;

import java.util.Optional;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.creation.CreationDraft;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.CreationStep;
import studio.modroll.checks.preset.PresetKind;

final class CreationFiller {

    private CreationFiller() {}

    static void fill(CreationDraft draft) {
        CreationStep step = draft.step();
        step.presetKind().ifPresent(kind -> chooseFirst(draft, kind));
        if (step == CreationStep.SCORES) {
            placeScores(draft);
        }
        if (step == CreationStep.SKILLS) {
            pickSkills(draft);
        }
    }

    private static void chooseFirst(CreationDraft draft, PresetKind kind) {
        draft.offer().presets().presets(kind).stream()
                .findFirst()
                .ifPresent(preset -> draft.choose(kind, Optional.of(preset.id())));
    }

    private static void placeScores(CreationDraft draft) {
        draft.select(CreationMethod.STANDARD_ARRAY);
        draft.useSuggested();
        for (Ability ability : Ability.values()) {
            while (draft.canRaiseBonus(ability)) {
                draft.raiseBonus(ability);
            }
        }
    }

    private static void pickSkills(CreationDraft draft) {
        for (Skill skill : draft.offer().skills()) {
            if (draft.stepProblem().isEmpty()) {
                return;
            }
            if (draft.canPickForSpecies(skill.id())) {
                draft.toggleSpeciesSkill(skill.id());
            } else {
                draft.toggleSkill(skill.id());
            }
        }
    }
}
