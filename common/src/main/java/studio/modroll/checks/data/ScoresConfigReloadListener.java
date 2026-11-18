package studio.modroll.checks.data;

import java.nio.file.Path;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import studio.modroll.checks.score.ScoresRuntime;

/** Makes {@code scores.json} reload with {@code /reload}, though it is no datapack resource. */
public record ScoresConfigReloadListener(Path configFile) implements ResourceManagerReloadListener {

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        ScoresRuntime.setConfig(ScoresConfigLoader.load(configFile));
    }
}
