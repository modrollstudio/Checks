package studio.modroll.checks.check;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import studio.modroll.checks.ClientPayloads;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * A client without Checks, or any client while roll messages are off, gets only the roll as vanilla's action bar
 * message, so it fits on one line.
 */
public final class RollMessages {

    private RollMessages() {}

    public static void showResult(ServerPlayer player, Component roll) {
        showResult(player, roll, Optional.empty());
    }

    public static void showResult(ServerPlayer player, Component roll, Optional<Component> detail) {
        ScoresConfig.RollMessageSettings settings = ScoresRuntime.config().rollMessages();
        if (!settings.enabled()
                || !ClientPayloads.send(player, new RollLinePayload(roll, detail, settings.durationTicks()))) {
            player.displayClientMessage(roll, true);
        }
    }
}
