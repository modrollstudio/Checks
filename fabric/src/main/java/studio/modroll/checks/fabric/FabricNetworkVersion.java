package studio.modroll.checks.fabric;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ConfigurationTask;
import studio.modroll.checks.Checks;
import studio.modroll.checks.NetworkVersion;
import studio.modroll.checks.NetworkVersionPayload;

/** Fabric's stand-in for NeoForge's registrar version. */
public final class FabricNetworkVersion {

    private static final ConfigurationTask.Type TASK =
            new ConfigurationTask.Type(NetworkVersionPayload.TYPE.id().toString());

    private FabricNetworkVersion() {}

    public static NetworkVersionPayload ours() {
        String modVersion = FabricLoader.getInstance()
                .getModContainer(Checks.MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
        return new NetworkVersionPayload(NetworkVersion.CURRENT, modVersion);
    }

    /** Call before any other join listener. */
    public static void register() {
        NetworkVersionPayload ours = ours();
        PayloadTypeRegistry.configurationS2C().register(NetworkVersionPayload.TYPE, NetworkVersionPayload.STREAM_CODEC);
        PayloadTypeRegistry.configurationC2S().register(NetworkVersionPayload.TYPE, NetworkVersionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkVersionPayload.TYPE, NetworkVersionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(NetworkVersionPayload.TYPE, NetworkVersionPayload.STREAM_CODEC);

        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (ServerConfigurationNetworking.canSend(handler, NetworkVersionPayload.TYPE)) {
                handler.addTask(new ConfigurationTask() {
                    @Override
                    public void start(Consumer<Packet<?>> sender) {
                        sender.accept(ServerConfigurationNetworking.createS2CPacket(ours));
                    }

                    @Override
                    public Type type() {
                        return TASK;
                    }
                });
            }
        });
        ServerConfigurationNetworking.registerGlobalReceiver(
                NetworkVersionPayload.TYPE,
                (payload, context) -> context.server().execute(() -> NetworkVersion.refusal(ours, payload)
                        .ifPresentOrElse(context.networkHandler()::disconnect, () -> context.networkHandler()
                                .completeTask(TASK))));

        // Only there so a client sees the channel and knows this server has the check.
        ServerPlayNetworking.registerGlobalReceiver(NetworkVersionPayload.TYPE, (payload, context) -> {});
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (NetworkVersion.fromBeforeTheCheck(ServerPlayNetworking.getSendable(handler))) {
                handler.disconnect(NetworkVersion.olderClient(ours.modVersion()));
            }
        });
    }
}
