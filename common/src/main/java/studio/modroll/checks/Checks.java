package studio.modroll.checks;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loader-agnostic entrypoint. Each loader module calls {@link #init} once at mod construction. */
public final class Checks {

    public static final String MOD_ID = "checks";
    public static final String MOD_NAME = "Critfall: Checks";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    private Checks() {}

    public static void init() {
        LOG.info("{} initialized", MOD_NAME);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
