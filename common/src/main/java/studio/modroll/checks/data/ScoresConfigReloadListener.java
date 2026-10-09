package studio.modroll.checks.data;

import java.nio.file.Path;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * Re-reads {@code config/checks/scores.json} whenever datapacks reload, making the config
 * hot-reloadable via {@code /reload} even though it is not itself a datapack resource.
 */
public record ScoresConfigReloadListener(Path configFile) implements ResourceManagerReloadListener {

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        ScoresRuntime.setConfig(ScoresConfigLoader.load(configFile));
    }
}
