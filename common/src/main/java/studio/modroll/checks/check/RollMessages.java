package studio.modroll.checks.check;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import studio.modroll.checks.ClientPayloads;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * Roll results above the hotbar. A client with Checks gets a {@link RollLinePayload} and draws the roll and
 * its detail on lines of their own for {@code roll_messages.duration_ticks}; any newer action bar message
 * hides them there, as in vanilla. A client without Checks, or any client while the setting is off, gets
 * only the roll as vanilla's action bar message, so it fits on one line, for vanilla's three seconds.
 */
public final class RollMessages {

    private RollMessages() {}

    public static void showResult(ServerPlayer player, Component roll) {
        showResult(player, roll, Optional.empty());
    }

    /** {@code detail} goes on a line below the roll, on a client with Checks only. */
    public static void showResult(ServerPlayer player, Component roll, Optional<Component> detail) {
        ScoresConfig.RollMessageSettings settings = ScoresRuntime.config().rollMessages();
        if (!settings.enabled()
                || !ClientPayloads.send(player, new RollLinePayload(roll, detail, settings.durationTicks()))) {
            player.displayClientMessage(roll, true);
        }
    }
}
