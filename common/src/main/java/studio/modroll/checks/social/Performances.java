package studio.modroll.checks.social;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.gameevent.GameEvent;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.CheckRoll;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.text.FallbackText;
import studio.modroll.critfall.api.combat.SaveResult;

/**
 * Performance: a player playing a note block or blowing a goat horn rolls Performance through
 * {@link ChecksApi}, at most once per cooldown, while a villager or piglin is in range to hear it; with no
 * audience, nothing is rolled. A note block played by redstone has no player behind it and rolls nothing.
 * Villagers on guard against the player are no audience. A made check is told as gossip to every listening
 * villager and charms piglins as the Deceive disguise does; a natural 1 spreads gossip the other way.
 * Every listener shows how it took it, and the nearest villager and the nearest piglin say so. The
 * cooldown lives in memory only, so a restart ends it.
 */
public final class Performances {

    private static final ResourceLocation PERFORMANCE = Checks.id("performance");
    private static final int NO_MARGIN = 0;

    private static final Map<UUID, Long> READY_AT = new HashMap<>();

    private Performances() {}

    private record Audience(List<Villager> villagers, List<Piglin> piglins) {

        boolean empty() {
            return villagers.isEmpty() && piglins.isEmpty();
        }
    }

    /** Called for every game event on the server; only a player's note block or goat horn counts. */
    public static void onGameEvent(Holder<GameEvent> event, GameEvent.Context context) {
        if (context.sourceEntity() instanceof ServerPlayer player
                && (event.is(GameEvent.NOTE_BLOCK_PLAY) || event.is(GameEvent.INSTRUMENT_PLAY))) {
            perform(player);
        }
    }

    public static void clear() {
        READY_AT.clear();
    }

    private static void perform(ServerPlayer player) {
        ScoresConfig.SocialSettings social = ScoresRuntime.config().social();
        ScoresConfig.PerformanceSettings settings = social.performance();
        long now = Socials.now(player.server);
        if (!social.enabled() || !settings.enabled() || now < READY_AT.getOrDefault(player.getUUID(), now)) {
            return;
        }
        Audience audience = audience(player, settings.range());
        if (audience.empty()) {
            return;
        }
        ChecksApi.skill(PERFORMANCE).ifPresent(skill -> {
            CheckRoll roll = ChecksApi.check(player, skill, settings.dc());
            if (roll.canceled()) {
                return;
            }
            SaveResult result = roll.result();
            READY_AT.put(player.getUUID(), now + settings.cooldownTicks());
            SocialOutcome outcome = SocialOutcome.of(result.natural(), result.saveTotal(), result.dc(), NO_MARGIN);
            effect(outcome, settings).ifPresent(effect -> apply(player, audience, effect, now));
            react(player, audience, outcome);
            SocialActions.announce(
                    player,
                    PERFORMANCE,
                    result,
                    outcome,
                    FallbackText.oneOf(
                            "checks.social.performance." + heard(outcome).id(), player.getRandom()));
        });
    }

    private static Audience audience(ServerPlayer player, double range) {
        return new Audience(
                around(player, Villager.class, range, villager -> !GolemAlarms.onGuard(player, villager)),
                around(player, Piglin.class, range, piglin -> true));
    }

    /** Nearest first. */
    private static <T extends LivingEntity> List<T> around(
            ServerPlayer player, Class<T> type, double range, Predicate<T> filter) {
        return player.level().getEntitiesOfClass(type, player.getBoundingBox().inflate(range), filter).stream()
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .toList();
    }

    /** A barely made check counts as a made one. */
    private static SocialOutcome heard(SocialOutcome outcome) {
        return outcome == SocialOutcome.BARELY ? SocialOutcome.SUCCESS : outcome;
    }

    private static Optional<ScoresConfig.PerformanceEffect> effect(
            SocialOutcome outcome, ScoresConfig.PerformanceSettings settings) {
        return switch (heard(outcome)) {
            case CRITICAL_SUCCESS -> Optional.of(settings.criticalSuccess());
            case SUCCESS -> Optional.of(settings.success());
            case CRITICAL_FAILURE -> Optional.of(settings.criticalFailure());
            case FAILURE, BARELY -> Optional.empty();
        };
    }

    private static void apply(ServerPlayer player, Audience audience, ScoresConfig.PerformanceEffect effect, long now) {
        effect.gossip().ifPresent(gossip -> audience.villagers()
                .forEach(villager -> villager.getGossips().add(player.getUUID(), gossip.type(), gossip.amount())));
        if (!audience.piglins().isEmpty()) {
            SocialMemory.get(player.server).charm(player.getUUID(), now, effect.piglinTicks());
        }
    }

    private static void react(ServerPlayer player, Audience audience, SocialOutcome outcome) {
        SocialReaction.ofPerformance(outcome).forEach(reaction -> {
            audience.villagers().forEach(villager -> SocialReactions.show(villager, reaction));
            audience.piglins().forEach(piglin -> SocialReactions.show(piglin, reaction));
        });
        speakFirst(player, audience.villagers(), outcome);
        speakFirst(player, audience.piglins(), outcome);
    }

    /** The nearest grown-up listener speaks for the rest. */
    private static void speakFirst(ServerPlayer player, List<? extends LivingEntity> listeners, SocialOutcome outcome) {
        for (LivingEntity listener : listeners) {
            Optional<SocialTarget> kind = SocialTarget.of(listener);
            if (kind.isPresent()) {
                SocialFeel.voiceAndSay(
                        player,
                        listener,
                        kind.get(),
                        Optional.of(SocialMood.ofPerformance(outcome)),
                        Optional.of(SpeechPools.performance(kind.get(), outcome)));
                return;
            }
        }
    }
}
