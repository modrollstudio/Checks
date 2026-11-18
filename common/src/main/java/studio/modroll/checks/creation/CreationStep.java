package studio.modroll.checks.creation;

import java.util.Optional;
import studio.modroll.checks.preset.PresetKind;

/** Declared in page order. */
public enum CreationStep {
    SPECIES("species", PresetKind.SPECIES),
    BACKGROUND("background", PresetKind.BACKGROUND),
    CLASS("class", PresetKind.CLASS),
    SCORES("scores", null),
    SKILLS("skills", null),
    CONFIRM("confirm", null);

    private final String id;
    private final PresetKind presetKind;

    CreationStep(String id, PresetKind presetKind) {
        this.id = id;
        this.presetKind = presetKind;
    }

    public String id() {
        return id;
    }

    public Optional<PresetKind> presetKind() {
        return Optional.ofNullable(presetKind);
    }
}
