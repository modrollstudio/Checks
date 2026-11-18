package studio.modroll.checks;

import java.util.function.BiPredicate;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class ClientPayloads {

    private static volatile BiPredicate<ServerPlayer, CustomPacketPayload> sender = (player, payload) -> false;

    private ClientPayloads() {}

    public static void setSender(BiPredicate<ServerPlayer, CustomPacketPayload> newSender) {
        sender = newSender;
    }

    public static BiPredicate<ServerPlayer, CustomPacketPayload> sender() {
        return sender;
    }

    public static boolean send(ServerPlayer player, CustomPacketPayload payload) {
        return sender.test(player, payload);
    }
}
