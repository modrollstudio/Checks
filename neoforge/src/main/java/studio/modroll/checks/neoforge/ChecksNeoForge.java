package studio.modroll.checks.neoforge;

import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import studio.modroll.checks.Checks;
import studio.modroll.checks.ClientPayloads;
import studio.modroll.checks.ServerHooks;
import studio.modroll.checks.body.BodyHooks;
import studio.modroll.checks.body.PlayerBodies;
import studio.modroll.checks.check.RollLinePayload;
import studio.modroll.checks.command.ChecksCommands;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.creation.CreationOfferPayload;
import studio.modroll.checks.creation.CreationOpenPayload;
import studio.modroll.checks.creation.CreationRollPayload;
import studio.modroll.checks.creation.CreationSubmitPayload;
import studio.modroll.checks.creation.client.CharacterCreationScreen;
import studio.modroll.checks.data.ReloadListeners;
import studio.modroll.checks.data.ScoresConfigLoader;
import studio.modroll.checks.exploration.ClimbingPayload;
import studio.modroll.checks.exploration.Landings;
import studio.modroll.checks.exploration.Leaps;
import studio.modroll.checks.exploration.Sneaking;
import studio.modroll.checks.exploration.client.ClientClimbing;
import studio.modroll.checks.level.ImprovementPayload;
import studio.modroll.checks.level.Levelling;
import studio.modroll.checks.neoforge.client.ChecksNeoForgeClient;
import studio.modroll.checks.save.VanillaSaves;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.sheet.StatSheetPayload;
import studio.modroll.checks.sheet.StatSheetRequestPayload;
import studio.modroll.checks.sheet.StatSheets;
import studio.modroll.checks.sheet.client.StatSheetScreen;
import studio.modroll.checks.social.HostilityPayload;
import studio.modroll.checks.social.ItemArcPayload;
import studio.modroll.checks.social.Performances;
import studio.modroll.checks.social.SocialActionPayload;
import studio.modroll.checks.social.SocialFeel;
import studio.modroll.checks.social.SocialMenuPayload;
import studio.modroll.checks.social.SocialMenuRequestPayload;
import studio.modroll.checks.social.SocialMobs;
import studio.modroll.checks.social.Socials;
import studio.modroll.checks.social.SpeechBubblePayload;
import studio.modroll.checks.social.client.ClientSocialFeel;
import studio.modroll.checks.social.client.SocialScreen;
import studio.modroll.checks.trait.DarkvisionPayload;
import studio.modroll.checks.trait.TraitHooks;
import studio.modroll.checks.trait.client.ClientDarkvision;
import studio.modroll.checks.trigger.CheckTriggers;

@Mod(Checks.MOD_ID)
public final class ChecksNeoForge {

    private final Path configFile =
            FMLPaths.CONFIGDIR.get().resolve(Checks.MOD_ID).resolve("scores.json");

    public ChecksNeoForge(IEventBus modBus) {
        Checks.init();
        ScoresRuntime.setConfig(ScoresConfigLoader.load(configFile));
        ClientPayloads.setSender(ChecksNeoForge::sendToClient);
        SocialFeel.setChecksClient(player -> player.connection.hasChannel(SpeechBubblePayload.TYPE));
        // NeoForge's access transformer makes the goal selector public.
        SocialMobs.setGoals(mob -> mob.goalSelector);
        modBus.addListener(ChecksNeoForge::onRegisterPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        // Lowest priority, and skipped when cancelled, so character XP matches what another mod lets through.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ChecksNeoForge::onXpChange);
        NeoForge.EVENT_BUS.addListener(ChecksNeoForge::onAdvancementEarned);
        NeoForge.EVENT_BUS.addListener(ChecksNeoForge::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ChecksNeoForge::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(ChecksNeoForge::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(ChecksNeoForge::onServerTick);
        NeoForge.EVENT_BUS.addListener(ChecksNeoForge::onGameEvent);
        NeoForge.EVENT_BUS.addListener(ChecksNeoForge::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(ChecksNeoForge::onPlayerSave);
        NeoForge.EVENT_BUS.addListener(ChecksNeoForge::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(ChecksNeoForge::onJump);
        // Lowest priority, so Stealth shrinks the visibility other mods settled on.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ChecksNeoForge::onVisibility);
        // Lowest priority, and skipped when cancelled, so DEX speeds up the draw another mod settled on.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ChecksNeoForge::onArrowLoose);
        // Lowest priority, and skipped when cancelled, so a save scales what other mods settled on.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ChecksNeoForge::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ChecksNeoForge::onKnockBack);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ChecksNeoForge::onExplosionKnockback);
        // Lowest priority, and skipped when cancelled, so a last stand is spent only on a death nothing else stopped.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ChecksNeoForge::onDeath);
        // NeoForge asks about the totem before LivingDeathEvent; Fabric's ALLOW_DEATH comes before the totem.
        // Keeping the totem while a last stand is ready lets the death event spend the last stand first.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ChecksNeoForge::onUseTotem);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        float resisted = TraitHooks.incomingDamage(event.getEntity(), event.getSource(), event.getAmount());
        float saved = VanillaSaves.explosionDamage(event.getEntity(), event.getSource(), resisted);
        event.setAmount(Landings.fallDamage(event.getEntity(), event.getSource(), saved));
    }

    private static void onDeath(LivingDeathEvent event) {
        if (TraitHooks.survivesDeath(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    private static void onUseTotem(LivingUseTotemEvent event) {
        if (TraitHooks.lastStandReady(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    private static void onJump(LivingEvent.LivingJumpEvent event) {
        Leaps.onJump(event.getEntity());
    }

    private static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        event.modifyVisibility(Sneaking.visibility(event.getEntity(), event.getLookingEntity()));
    }

    private static void onKnockBack(LivingKnockBackEvent event) {
        event.setStrength((float) VanillaSaves.knockback(event.getEntity(), event.getStrength()));
    }

    private static void onExplosionKnockback(ExplosionKnockbackEvent event) {
        event.setKnockbackVelocity(
                VanillaSaves.explosionKnockback(event.getAffectedEntity(), event.getKnockbackVelocity()));
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        ServerHooks.onServerTick(event.getServer());
    }

    private static void onGameEvent(VanillaGameEvent event) {
        Performances.onGameEvent(event.getVanillaEvent(), event.getContext());
    }

    private static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer newPlayer && event.getOriginal() instanceof ServerPlayer old) {
            PlayerBodies.onRespawn(old, newPlayer, !event.isWasDeath());
        }
    }

    private static void onPlayerSave(PlayerEvent.SaveToFile event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerBodies.onSave(player);
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerBodies.sync(player);
        }
    }

    private static void onArrowLoose(ArrowLooseEvent event) {
        event.setCharge(BodyHooks.bowCharge(event.getEntity(), event.getCharge()));
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            consumeIfFired(
                    event,
                    ServerHooks.onUseBlock(player, event.getHand(), event.getPos()),
                    event::setCancellationResult);
        }
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            consumeIfFired(
                    event,
                    ServerHooks.onUseEntity(player, event.getHand(), event.getTarget()),
                    event::setCancellationResult);
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            consumeIfFired(event, CheckTriggers.onUseItem(player, event.getHand()), event::setCancellationResult);
        }
    }

    private static void consumeIfFired(
            ICancellableEvent event, InteractionResult result, Consumer<InteractionResult> cancellationResult) {
        if (result.consumesAction()) {
            event.setCanceled(true);
            cancellationResult.accept(result);
        }
    }

    private static void onXpChange(PlayerXpEvent.XpChange event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Levelling.onVanillaXp(player, event.getAmount());
        }
    }

    private static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Levelling.onAdvancement(player, event.getAdvancement());
        }
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        ReloadListeners.all(configFile).forEach(entry -> event.addListener(entry.listener()));
    }

    // The client handler is a lambda so a dedicated server never resolves the client-only screen class.
    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(
                        StatSheetRequestPayload.TYPE,
                        StatSheetRequestPayload.STREAM_CODEC,
                        ChecksNeoForge::onStatSheetRequest)
                .playToClient(
                        StatSheetPayload.TYPE,
                        StatSheetPayload.STREAM_CODEC,
                        (payload, context) -> StatSheetScreen.open(payload.sheet()))
                .playToServer(
                        CreationOpenPayload.TYPE,
                        CreationOpenPayload.STREAM_CODEC,
                        (payload, context) -> asServerPlayer(context, CharacterCreation::requestOpen))
                .playToServer(
                        CreationRollPayload.TYPE,
                        CreationRollPayload.STREAM_CODEC,
                        (payload, context) ->
                                asServerPlayer(context, player -> CharacterCreation.roll(player, payload.method())))
                .playToServer(
                        CreationSubmitPayload.TYPE,
                        CreationSubmitPayload.STREAM_CODEC,
                        (payload, context) -> asServerPlayer(
                                context, player -> CharacterCreation.submit(player, payload.submission())))
                .playToClient(
                        CreationOfferPayload.TYPE,
                        CreationOfferPayload.STREAM_CODEC,
                        (payload, context) -> CharacterCreationScreen.open(payload.offer()))
                .playToClient(
                        DarkvisionPayload.TYPE,
                        DarkvisionPayload.STREAM_CODEC,
                        (payload, context) -> ClientDarkvision.set(payload.strength()))
                .playToClient(
                        ClimbingPayload.TYPE,
                        ClimbingPayload.STREAM_CODEC,
                        (payload, context) -> ClientClimbing.set(payload.speed()))
                .playToServer(
                        ImprovementPayload.TYPE,
                        ImprovementPayload.STREAM_CODEC,
                        (payload, context) ->
                                asServerPlayer(context, player -> Levelling.improve(player, payload.increases())))
                .playToClient(
                        RollLinePayload.TYPE,
                        RollLinePayload.STREAM_CODEC,
                        (payload, context) -> ChecksNeoForgeClient.showRollLine(payload))
                .playToServer(
                        SocialMenuRequestPayload.TYPE,
                        SocialMenuRequestPayload.STREAM_CODEC,
                        (payload, context) -> asServerPlayer(context, player -> Socials.menu(player, payload.entityId())
                                .ifPresent(menu -> context.reply(new SocialMenuPayload(menu)))))
                .playToClient(
                        SpeechBubblePayload.TYPE,
                        SpeechBubblePayload.STREAM_CODEC,
                        (payload, context) -> ClientSocialFeel.say(payload))
                .playToClient(
                        HostilityPayload.TYPE,
                        HostilityPayload.STREAM_CODEC,
                        (payload, context) -> ClientSocialFeel.sense(payload))
                .playToClient(
                        ItemArcPayload.TYPE,
                        ItemArcPayload.STREAM_CODEC,
                        (payload, context) -> ClientSocialFeel.fly(payload))
                .playToClient(
                        SocialMenuPayload.TYPE,
                        SocialMenuPayload.STREAM_CODEC,
                        (payload, context) -> SocialScreen.open(payload.menu()))
                .playToServer(
                        SocialActionPayload.TYPE,
                        SocialActionPayload.STREAM_CODEC,
                        (payload, context) -> asServerPlayer(
                                context, player -> Socials.act(player, payload.entityId(), payload.action())));
    }

    private static void onStatSheetRequest(StatSheetRequestPayload payload, IPayloadContext context) {
        asServerPlayer(context, player -> StatSheets.forPlayer(player)
                .ifPresent(sheet -> context.reply(new StatSheetPayload(sheet))));
    }

    private static void asServerPlayer(IPayloadContext context, Consumer<ServerPlayer> handler) {
        if (context.player() instanceof ServerPlayer player) {
            handler.accept(player);
        }
    }

    /** Whether the player's client has the channel, and so was sent the payload. */
    private static boolean sendToClient(ServerPlayer player, CustomPacketPayload payload) {
        if (!player.connection.hasChannel(payload)) {
            return false;
        }
        PacketDistributor.sendToPlayer(player, payload);
        return true;
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerHooks.onPlayerJoin(player);
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ChecksCommands.register(event.getDispatcher());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        ServerHooks.onServerStopping();
    }
}
