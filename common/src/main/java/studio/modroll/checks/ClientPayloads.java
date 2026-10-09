package studio.modroll.checks;

import java.util.function.BiPredicate;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Server-to-client payloads, sent through the running loader. Until a loader sets the sender, no client gets one. */
public final class ClientPayloads {

    private static volatile BiPredicate<ServerPlayer, CustomPacketPayload> sender = (player, payload) -> false;

    private ClientPayloads() {}

    /** Each loader sets how to send a payload; it answers whether the player's client has the channel. */
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
