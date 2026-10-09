package studio.modroll.checks.creation;

import java.util.Optional;
import studio.modroll.checks.preset.PresetKind;

/** The pages of the creation screen, in order; the preset pages only while presets are on. */
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

    /** The lowercase id used in lang keys. */
    public String id() {
        return id;
    }

    /** The kind of preset this step chooses, if it is a preset step. */
    public Optional<PresetKind> presetKind() {
        return Optional.ofNullable(presetKind);
    }
}
