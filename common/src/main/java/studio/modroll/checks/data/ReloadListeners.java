package studio.modroll.checks.data;

import java.nio.file.Path;
import java.util.List;
import net.minecraft.server.packs.resources.PreparableReloadListener;

/** Checks' reload listeners in the order they run, each under the name Fabric registers it by. */
public final class ReloadListeners {

    public static final String SKILLS = "skill";

    /** {@code afterSkills}: the listener resolves skill keys, so it runs after the skill listener. */
    public record Entry(String name, PreparableReloadListener listener, boolean afterSkills) {}

    private ReloadListeners() {}

    public static List<Entry> all(Path configFile) {
        return List.of(
                new Entry(SKILLS, DatapackListener.skills(), false),
                new Entry("entity_profile", DatapackListener.entityProfiles(), true),
                new Entry("bonus", DatapackListener.bonusSources(), true),
                new Entry("trigger", DatapackListener.triggers(), true),
                new Entry("trait", DatapackListener.traits(), true),
                new Entry("species", PresetReloadListener.species(), false),
                new Entry("background", PresetReloadListener.backgrounds(), true),
                new Entry("class", PresetReloadListener.classes(), true),
                new Entry("scores_config", new ScoresConfigReloadListener(configFile), false));
    }
}
