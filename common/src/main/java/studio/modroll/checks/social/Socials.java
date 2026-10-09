package studio.modroll.checks.social;

import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.text.FallbackText;
import studio.modroll.checks.text.Names;

/**
 * Social skill uses, called from each loader's payload and interaction hooks. The server checks everything
 * the client sends: the entity must be in reach, and an action available on it, when the request arrives.
 */
public final class Socials {

    /** As vanilla's own check on an entity interaction packet. */
    private static final double REACH_BUFFER = 1.0;

    private Socials() {}

    /** Social time is game time, which keeps counting across restarts. */
    public static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    /**
     * The menu for the entity; empty while social uses are off or when it is not in reach. When nothing
     * there can be talked to, the player is told so instead.
     */
    public static Optional<SocialMenu> menu(ServerPlayer player, int entityId) {
        if (!ScoresRuntime.config().social().enabled()) {
            return Optional.empty();
        }
        Optional<SocialMenu> menu = inReach(player, entityId).map(entity -> SocialOptions.menu(player, entity));
        if (menu.isPresent() && !menu.get().worthOpening()) {
            player.displayClientMessage(FallbackText.of("checks.social.nobody"), true);
            return Optional.empty();
        }
        return menu;
    }

    /** Tries {@code action} on the entity, or tells the player why it is not available. */
    public static void act(ServerPlayer player, int entityId, SocialAction action) {
        inReach(player, entityId).ifPresent(entity -> SocialOptions.option(player, entity, action)
                .ifPresent(option -> {
                    if (option.available()) {
                        SocialActions.perform(player, entity, option);
                    } else {
                        player.displayClientMessage(SocialText.unavailable(option), true);
                    }
                }));
    }

    private static Optional<LivingEntity> inReach(ServerPlayer player, int entityId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (player.isSpectator()
                || !(entity instanceof LivingEntity living)
                || !player.canInteractWithEntity(entity, REACH_BUFFER)) {
            return Optional.empty();
        }
        return Optional.of(living);
    }

    /** A trader that refuses the player shakes its head instead of trading; consumes the interaction then. */
    public static InteractionResult onUseEntity(ServerPlayer player, InteractionHand hand, Entity entity) {
        if (!(entity instanceof AbstractVillager trader) || !refuses(player, trader)) {
            return InteractionResult.PASS;
        }
        if (hand == InteractionHand.MAIN_HAND) {
            SocialReactions.shakeHead(trader);
            player.displayClientMessage(
                    FallbackText.oneOf("checks.social.refuses", trader.getRandom(), Names.of(trader)), true);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Whether the trader refuses the player over something a switched-on action did to it, or, while
     * witnesses are on, that it saw done to another villager.
     */
    public static boolean refuses(ServerPlayer player, AbstractVillager trader) {
        ScoresConfig.SocialSettings settings = ScoresRuntime.config().social();
        SocialMemory memory = SocialMemory.get(player.server);
        long now = now(player.server);
        return SocialTarget.of(trader).isPresent()
                && (memory.refuses(player.getUUID(), trader.getUUID(), now, settings::active)
                        || (settings.witnesses().enabled()
                                && memory.refusesAsWitness(player.getUUID(), trader.getUUID(), now, settings::active)));
    }

    /**
     * Whether piglins take the entity for a gold wearer: a player a made Deception check disguised, or a
     * made Performance charmed.
     */
    public static boolean disguised(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        ScoresConfig.SocialSettings settings = ScoresRuntime.config().social();
        SocialMemory memory = SocialMemory.get(player.server);
        long now = now(player.server);
        return (settings.active(SocialAction.DECEIVE) && memory.disguised(player.getUUID(), now))
                || (settings.enabled() && settings.performance().enabled() && memory.charmed(player.getUUID(), now));
    }
}
