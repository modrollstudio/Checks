package studio.modroll.checks.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import studio.modroll.checks.check.RollLinePayload;
import studio.modroll.checks.check.client.ChatBox;
import studio.modroll.checks.check.client.ClientRollLines;
import studio.modroll.checks.creation.CreationOfferPayload;
import studio.modroll.checks.creation.client.CharacterCreationScreen;
import studio.modroll.checks.exploration.ClimbingPayload;
import studio.modroll.checks.exploration.client.ClientClimbing;
import studio.modroll.checks.fabric.mixin.client.ChatComponentAccessor;
import studio.modroll.checks.fabric.mixin.client.GuiAccessor;
import studio.modroll.checks.sheet.StatSheetPayload;
import studio.modroll.checks.sheet.StatSheetRequestPayload;
import studio.modroll.checks.sheet.client.StatSheetScreen;
import studio.modroll.checks.social.HostilityPayload;
import studio.modroll.checks.social.ItemArcPayload;
import studio.modroll.checks.social.SocialMenuPayload;
import studio.modroll.checks.social.SocialMenuRequestPayload;
import studio.modroll.checks.social.SpeechBubblePayload;
import studio.modroll.checks.social.client.ClientSocialFeel;
import studio.modroll.checks.social.client.SocialScreen;
import studio.modroll.checks.trait.DarkvisionPayload;
import studio.modroll.checks.trait.client.ClientDarkvision;

public final class ChecksFabricClient implements ClientModInitializer {

    /** Where vanilla draws the action bar above the bottom of the screen. */
    private static final int ACTION_BAR_HEIGHT = 68;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(StatSheetScreen.KEY);
        KeyBindingHelper.registerKeyBinding(SocialScreen.KEY);
        ClientPlayNetworking.registerGlobalReceiver(
                RollLinePayload.TYPE,
                (payload, context) -> ClientRollLines.show(
                        payload, ticks -> ((GuiAccessor) context.client().gui).checks$setOverlayMessageTime(ticks)));
        HudRenderCallback.EVENT.register(ChecksFabricClient::renderRollLines);
        ClientPlayNetworking.registerGlobalReceiver(
                SocialMenuPayload.TYPE, (payload, context) -> SocialScreen.open(payload.menu()));
        ClientPlayNetworking.registerGlobalReceiver(
                StatSheetPayload.TYPE, (payload, context) -> StatSheetScreen.open(payload.sheet()));
        ClientPlayNetworking.registerGlobalReceiver(
                CreationOfferPayload.TYPE, (payload, context) -> CharacterCreationScreen.open(payload.offer()));
        ClientPlayNetworking.registerGlobalReceiver(
                DarkvisionPayload.TYPE, (payload, context) -> ClientDarkvision.set(payload.strength()));
        ClientPlayNetworking.registerGlobalReceiver(
                ClimbingPayload.TYPE, (payload, context) -> ClientClimbing.set(payload.speed()));
        ClientPlayNetworking.registerGlobalReceiver(
                SpeechBubblePayload.TYPE, (payload, context) -> ClientSocialFeel.say(payload));
        ClientPlayNetworking.registerGlobalReceiver(
                ItemArcPayload.TYPE, (payload, context) -> ClientSocialFeel.fly(payload));
        ClientPlayNetworking.registerGlobalReceiver(
                HostilityPayload.TYPE, (payload, context) -> ClientSocialFeel.sense(payload));
        WorldRenderEvents.AFTER_ENTITIES.register(context -> ClientSocialFeel.render(
                context.matrixStack(),
                context.consumers(),
                context.camera(),
                context.tickCounter().getGameTimeDeltaPartialTick(false)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientDarkvision.reset();
            ClientClimbing.reset();
            ClientSocialFeel.clear();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CharacterCreationScreen.openPending();
            while (StatSheetScreen.KEY.consumeClick()) {
                requestStatSheet();
            }
            while (SocialScreen.KEY.consumeClick()) {
                SocialScreen.target().ifPresent(ChecksFabricClient::requestSocialMenu);
            }
        });
    }

    private static void renderRollLines(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        GuiAccessor overlay = (GuiAccessor) minecraft.gui;
        ClientRollLines.render(
                graphics,
                deltaTracker.getGameTimeDeltaPartialTick(false),
                overlay.checks$overlayMessage(),
                overlay.checks$overlayMessageTime(),
                graphics.guiHeight() - ACTION_BAR_HEIGHT,
                ChatBox.of(
                        minecraft,
                        ((ChatComponentAccessor) minecraft.gui.getChat()).checks$trimmedMessages(),
                        graphics.guiHeight()));
    }

    // Fabric lets a client join a server without Checks, which would reject the payload.
    private static void requestStatSheet() {
        if (ClientPlayNetworking.canSend(StatSheetRequestPayload.TYPE)) {
            ClientPlayNetworking.send(StatSheetRequestPayload.INSTANCE);
        }
    }

    private static void requestSocialMenu(int entityId) {
        if (ClientPlayNetworking.canSend(SocialMenuRequestPayload.TYPE)) {
            ClientPlayNetworking.send(new SocialMenuRequestPayload(entityId));
        }
    }
}
