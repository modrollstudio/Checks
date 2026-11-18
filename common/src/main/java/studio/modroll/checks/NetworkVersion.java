package studio.modroll.checks;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.text.FallbackText;

/**
 * Bump {@link #CURRENT} whenever a payload's fields change. NeoForge compares it as the registrar version; Fabric,
 * which has no such check, trades a {@link NetworkVersionPayload} while configuring.
 */
public final class NetworkVersion {

    public static final int CURRENT = 4;

    private NetworkVersion() {}

    /** A Checks from before the version check accepts Checks' play channels but not {@link NetworkVersionPayload}'s. */
    public static boolean fromBeforeTheCheck(Collection<ResourceLocation> channels) {
        return channels.stream().anyMatch(channel -> channel.getNamespace().equals(Checks.MOD_ID))
                && !channels.contains(NetworkVersionPayload.TYPE.id());
    }

    public static Optional<Component> refusal(NetworkVersionPayload server, NetworkVersionPayload client) {
        if (client.version() == server.version()) {
            return Optional.empty();
        }
        return Optional.of(FallbackText.of("checks.network.mismatch", server.modVersion(), client.modVersion()));
    }

    public static Component olderClient(String serverModVersion) {
        return FallbackText.of("checks.network.older_client", serverModVersion);
    }

    public static Component olderServer(String clientModVersion) {
        return FallbackText.of("checks.network.older_server", clientModVersion);
    }
}
