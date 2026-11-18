package studio.modroll.checks;

import java.util.function.BiPredicate;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Until a loader sets the sender, no client gets a payload. */
public final class ClientPayloads {

    private static volatile BiPredicate<ServerPlayer, CustomPacketPayload> sender = (player, payload) -> false;

    private ClientPayloads() {}

    /** Set by each loader; the sender answers whether the player's client has the channel. */
    public static void setSender(BiPredicate<ServerPlayer, CustomPacketPayload> newSender) {
        sender = newSender;
    }

    public static BiPredicate<ServerPlayer, CustomPacketPayload> sender() {
        return sender;
    }

    /** Whether the player's client has Checks, and so was sent the payload. */
    public static boolean send(ServerPlayer player, CustomPacketPayload payload) {
        return sender.test(player, payload);
    }
}
