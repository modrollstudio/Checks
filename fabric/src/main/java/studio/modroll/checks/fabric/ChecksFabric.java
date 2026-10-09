package studio.modroll.checks.fabric;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import studio.modroll.checks.Checks;
import studio.modroll.checks.ClientPayloads;
import studio.modroll.checks.ServerHooks;
import studio.modroll.checks.body.PlayerBodies;
import studio.modroll.checks.check.RollLinePayload;
import studio.modroll.checks.command.ChecksCommands;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.creation.CreationOfferPayload;
import studio.modroll.checks.creation.CreationOpenPayload;
import studio.modroll.checks.creation.CreationRollPayload;
import studio.modroll.checks.creation.CreationSubmitPayload;
import studio.modroll.checks.data.ReloadListeners;
import studio.modroll.checks.data.ScoresConfigLoader;
import studio.modroll.checks.exploration.ClimbingPayload;
import studio.modroll.checks.fabric.mixin.MobAccessor;
import studio.modroll.checks.level.ImprovementPayload;
import studio.modroll.checks.level.Levelling;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.sheet.StatSheetPayload;
import studio.modroll.checks.sheet.StatSheetRequestPayload;
import studio.modroll.checks.sheet.StatSheets;
import studio.modroll.checks.social.HostilityPayload;
import studio.modroll.checks.social.ItemArcPayload;
import studio.modroll.checks.social.SocialActionPayload;
import studio.modroll.checks.social.SocialFeel;
import studio.modroll.checks.social.SocialMenuPayload;
import studio.modroll.checks.social.SocialMenuRequestPayload;
import studio.modroll.checks.social.SocialMobs;
import studio.modroll.checks.social.Socials;
import studio.modroll.checks.social.SpeechBubblePayload;
import studio.modroll.checks.trait.DarkvisionPayload;
import studio.modroll.checks.trait.TraitHooks;
import studio.modroll.checks.trigger.CheckTriggers;

public final class ChecksFabric implements ModInitializer {

    private final Path configFile =
            FabricLoader.getInstance().getConfigDir().resolve(Checks.MOD_ID).resolve("scores.json");

    @Override
    public void onInitialize() {
        Checks.init();
        ClientPayloads.setSender(ChecksFabric::sendToClient);
        ScoresRuntime.setConfig(ScoresConfigLoader.load(configFile));

        PayloadTypeRegistry.playC2S().register(StatSheetRequestPayload.TYPE, StatSheetRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(StatSheetPayload.TYPE, StatSheetPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
                StatSheetRequestPayload.TYPE, (payload, context) -> StatSheets.forPlayer(context.player())
                        .ifPresent(sheet -> context.responseSender().sendPacket(new StatSheetPayload(sheet))));
        registerCreation();
        registerSocial();
        PayloadTypeRegistry.playS2C().register(RollLinePayload.TYPE, RollLinePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ImprovementPayload.TYPE, ImprovementPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
                ImprovementPayload.TYPE,
                (payload, context) -> Levelling.improve(context.player(), payload.increases()));

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> ChecksCommands.register(dispatcher));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ServerHooks.onServerStopping());
        registerTriggers();
        ServerTickEvents.END_SERVER_TICK.register(ServerHooks::onServerTick);
        ServerLivingEntityEvents.ALLOW_DEATH.register(
                (entity, source, amount) -> !TraitHooks.survivesDeath(entity, source));
        PayloadTypeRegistry.playS2C().register(DarkvisionPayload.TYPE, DarkvisionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ClimbingPayload.TYPE, ClimbingPayload.STREAM_CODEC);
        ServerPlayerEvents.COPY_FROM.register(PlayerBodies::onRespawn);
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
                (player, origin, destination) -> PlayerBodies.sync(player));

        ResourceManagerHelper data = ResourceManagerHelper.get(PackType.SERVER_DATA);
        List<ResourceLocation> afterSkills = List.of(Checks.id(ReloadListeners.SKILLS));
        for (ReloadListeners.Entry entry : ReloadListeners.all(configFile)) {
            register(data, entry.name(), entry.listener(), entry.afterSkills() ? afterSkills : List.of());
        }
    }

    // Fabric calls UseEntityCallback for both entity packets; answering only the position-less one fires
    // once per click, like NeoForge's EntityInteract.
    private static void registerTriggers() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> player instanceof ServerPlayer serverPlayer
                ? ServerHooks.onUseBlock(serverPlayer, hand, hit.getBlockPos())
                : InteractionResult.PASS);
        UseEntityCallback.EVENT.register(
                (player, level, hand, entity, hit) -> player instanceof ServerPlayer serverPlayer && hit == null
                        ? ServerHooks.onUseEntity(serverPlayer, hand, entity)
                        : InteractionResult.PASS);
        UseItemCallback.EVENT.register((player, level, hand) -> {
            boolean fired = player instanceof ServerPlayer serverPlayer
                    && CheckTriggers.onUseItem(serverPlayer, hand).consumesAction();
            return fired
                    ? InteractionResultHolder.success(player.getItemInHand(hand))
                    : InteractionResultHolder.pass(player.getItemInHand(hand));
        });
    }

    /** Whether the player's client has the channel, and so was sent the payload. */
    private static boolean sendToClient(ServerPlayer player, CustomPacketPayload payload) {
        if (!ServerPlayNetworking.canSend(player, payload.type())) {
            return false;
        }
        ServerPlayNetworking.send(player, payload);
        return true;
    }

    private static void registerSocial() {
        PayloadTypeRegistry.playC2S().register(SocialMenuRequestPayload.TYPE, SocialMenuRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SocialMenuPayload.TYPE, SocialMenuPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SocialActionPayload.TYPE, SocialActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SpeechBubblePayload.TYPE, SpeechBubblePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ItemArcPayload.TYPE, ItemArcPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(HostilityPayload.TYPE, HostilityPayload.STREAM_CODEC);
        SocialMobs.setGoals(mob -> ((MobAccessor) mob).checks$goalSelector());
        SocialFeel.setChecksClient(player -> ServerPlayNetworking.canSend(player, SpeechBubblePayload.TYPE));
        ServerPlayNetworking.registerGlobalReceiver(
                SocialMenuRequestPayload.TYPE, (payload, context) -> Socials.menu(context.player(), payload.entityId())
                        .ifPresent(menu -> context.responseSender().sendPacket(new SocialMenuPayload(menu))));
        ServerPlayNetworking.registerGlobalReceiver(
                SocialActionPayload.TYPE,
                (payload, context) -> Socials.act(context.player(), payload.entityId(), payload.action()));
    }

    private static void registerCreation() {
        PayloadTypeRegistry.playC2S().register(CreationOpenPayload.TYPE, CreationOpenPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(CreationRollPayload.TYPE, CreationRollPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(CreationSubmitPayload.TYPE, CreationSubmitPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(CreationOfferPayload.TYPE, CreationOfferPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
                CreationOpenPayload.TYPE, (payload, context) -> CharacterCreation.requestOpen(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(
                CreationRollPayload.TYPE,
                (payload, context) -> CharacterCreation.roll(context.player(), payload.method()));
        ServerPlayNetworking.registerGlobalReceiver(
                CreationSubmitPayload.TYPE,
                (payload, context) -> CharacterCreation.submit(context.player(), payload.submission()));
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> ServerHooks.onPlayerJoin(handler.getPlayer()));
    }

    private static void register(
            ResourceManagerHelper helper,
            String path,
            PreparableReloadListener delegate,
            Collection<ResourceLocation> dependencies) {
        ResourceLocation id = Checks.id(path);
        helper.registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return id;
            }

            @Override
            public Collection<ResourceLocation> getFabricDependencies() {
                return dependencies;
            }

            @Override
            public CompletableFuture<Void> reload(
                    PreparationBarrier barrier,
                    ResourceManager manager,
                    ProfilerFiller prepareProfiler,
                    ProfilerFiller applyProfiler,
                    Executor prepareExecutor,
                    Executor applyExecutor) {
                return delegate.reload(
                        barrier, manager, prepareProfiler, applyProfiler, prepareExecutor, applyExecutor);
            }
        });
    }
}
