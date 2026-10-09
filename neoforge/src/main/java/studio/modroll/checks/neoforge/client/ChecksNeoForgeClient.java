package studio.modroll.checks.neoforge.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.modroll.checks.Checks;
import studio.modroll.checks.check.RollLinePayload;
import studio.modroll.checks.check.client.ChatBox;
import studio.modroll.checks.check.client.ClientRollLines;
import studio.modroll.checks.creation.client.CharacterCreationScreen;
import studio.modroll.checks.exploration.client.ClientClimbing;
import studio.modroll.checks.neoforge.mixin.client.ChatComponentAccessor;
import studio.modroll.checks.neoforge.mixin.client.GuiAccessor;
import studio.modroll.checks.sheet.StatSheetRequestPayload;
import studio.modroll.checks.sheet.client.StatSheetScreen;
import studio.modroll.checks.social.SocialMenuRequestPayload;
import studio.modroll.checks.social.client.ClientSocialFeel;
import studio.modroll.checks.social.client.SocialScreen;
import studio.modroll.checks.trait.client.ClientDarkvision;

@Mod(value = Checks.MOD_ID, dist = Dist.CLIENT)
public final class ChecksNeoForgeClient {

    /** Where vanilla draws the action bar above the bottom of the screen. */
    private static final int ACTION_BAR_HEIGHT = 68;
    /** NeoForge raises the action bar this far above the status bars, as from the selected item name. */
    private static final int ABOVE_STATUS_BARS = 68 - 59;

    public ChecksNeoForgeClient(IEventBus modBus) {
        modBus.addListener(ChecksNeoForgeClient::onRegisterKeyMappings);
        modBus.addListener(ChecksNeoForgeClient::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(ChecksNeoForgeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            ClientDarkvision.reset();
            ClientClimbing.reset();
            ClientSocialFeel.clear();
        });
        NeoForge.EVENT_BUS.addListener(ChecksNeoForgeClient::onRenderLevelStage);
    }

    // Vanilla flushes the buffers itself later in the frame.
    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            ClientSocialFeel.render(
                    event.getPoseStack(),
                    Minecraft.getInstance().renderBuffers().bufferSource(),
                    event.getCamera(),
                    event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
    }

    public static void showRollLine(RollLinePayload payload) {
        ClientRollLines.show(
                payload, ticks -> ((GuiAccessor) Minecraft.getInstance().gui).checks$setOverlayMessageTime(ticks));
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.OVERLAY_MESSAGE, Checks.id("roll_lines"), ChecksNeoForgeClient::renderRollLines);
    }

    /** On the action bar, which NeoForge raises over status bars taller than vanilla's. */
    private static void renderRollLines(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Gui gui = Minecraft.getInstance().gui;
        GuiAccessor overlay = (GuiAccessor) gui;
        int height = Math.max(Math.max(gui.leftHeight, gui.rightHeight) + ABOVE_STATUS_BARS, ACTION_BAR_HEIGHT);
        ClientRollLines.render(
                graphics,
                deltaTracker.getGameTimeDeltaPartialTick(false),
                overlay.checks$overlayMessage(),
                overlay.checks$overlayMessageTime(),
                graphics.guiHeight() - height,
                ChatBox.of(
                        Minecraft.getInstance(),
                        ((ChatComponentAccessor) gui.getChat()).checks$trimmedMessages(),
                        graphics.guiHeight()));
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(StatSheetScreen.KEY);
        event.register(SocialScreen.KEY);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        CharacterCreationScreen.openPending();
        while (StatSheetScreen.KEY.consumeClick()) {
            PacketDistributor.sendToServer(StatSheetRequestPayload.INSTANCE);
        }
        while (SocialScreen.KEY.consumeClick()) {
            SocialScreen.target()
                    .ifPresent(entityId -> PacketDistributor.sendToServer(new SocialMenuRequestPayload(entityId)));
        }
    }
}
