package studio.modroll.checks.data;

import java.nio.file.Path;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import studio.modroll.checks.score.ScoresRuntime;

public record ScoresConfigReloadListener(Path configFile) implements ResourceManagerReloadListener {

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        ScoresRuntime.setConfig(ScoresConfigLoader.load(configFile));
    }
}
