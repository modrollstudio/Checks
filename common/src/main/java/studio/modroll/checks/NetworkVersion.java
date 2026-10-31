package studio.modroll.checks;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.text.FallbackText;

/**
 * Which payload formats this build of Checks speaks. Bump {@link #CURRENT} whenever a payload's fields change, so
 * a client and server that would misread each other are turned away when joining instead of failing on a payload
 * later. NeoForge compares it as the registrar version. Fabric has no such check, so its loader module trades a
 * {@link NetworkVersionPayload} while configuring. The messages fall back to English for a client whose Checks
 * lacks the keys.
 */
public final class NetworkVersion {

    public static final int CURRENT = 3;

    private NetworkVersion() {}

    /**
     * Whether the other side has Checks from before this check, read from the play channels it accepts: a Checks
     * one, but not {@link NetworkVersionPayload}'s.
     */
    public static boolean fromBeforeTheCheck(Collection<ResourceLocation> channels) {
        return channels.stream().anyMatch(channel -> channel.getNamespace().equals(Checks.MOD_ID))
                && !channels.contains(NetworkVersionPayload.TYPE.id());
    }

    /** Why the server turns away a client that answered {@code client}; empty when the versions match. */
    public static Optional<Component> refusal(NetworkVersionPayload server, NetworkVersionPayload client) {
        if (client.version() == server.version()) {
            return Optional.empty();
        }
        return Optional.of(FallbackText.of("checks.network.mismatch", server.modVersion(), client.modVersion()));
    }

    /** For a client whose Checks is from before the version check. */
    public static Component olderClient(String serverModVersion) {
        return FallbackText.of("checks.network.older_client", serverModVersion);
    }

    /** For a server whose Checks is from before the version check. */
    public static Component olderServer(String clientModVersion) {
        return FallbackText.of("checks.network.older_server", clientModVersion);
    }
}
