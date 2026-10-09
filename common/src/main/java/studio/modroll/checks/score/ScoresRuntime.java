package studio.modroll.checks.score;

import studio.modroll.checks.critfall.ChecksModifierProvider;
import studio.modroll.checks.data.ScoresConfig;

/** The live scores config, swapped on load and {@code /reload}; Critfall's provider slot follows it. */
public final class ScoresRuntime {

    private static volatile ScoresConfig config = ScoresConfig.DEFAULTS;

    private ScoresRuntime() {}

    public static ScoresConfig config() {
        return config;
    }

    public static void setConfig(ScoresConfig newConfig) {
        config = newConfig;
        ChecksModifierProvider.sync(newConfig.critfall());
    }
}
