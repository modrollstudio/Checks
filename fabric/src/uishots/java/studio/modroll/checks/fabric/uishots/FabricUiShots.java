package studio.modroll.checks.fabric.uishots;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import studio.modroll.checks.uishots.UiShots;

public final class FabricUiShots implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> UiShots.tick());
    }
}
