package studio.modroll.checks.death;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.text.FallbackText;
import studio.modroll.checks.text.Names;

public final class DeathStories {

    private static final StoryLog LOG = new StoryLog();

    private DeathStories() {}

    public static void record(ServerPlayer player, DeathStory story) {
        record(player, story, Component.empty());
    }

    public static void record(ServerPlayer player, DeathStory story, Component item) {
        LOG.record(player.getUUID(), story, item, player.server.getTickCount());
    }

    public static Component deathMessage(ServerPlayer player, DamageSource source, Component vanilla) {
        ScoresConfig.DeathMessageSettings settings = ScoresRuntime.config().deathMessages();
        Optional<StoryLog.Told> told = LOG.take(
                player.getUUID(), player.server.getTickCount(), settings.windowTicks(), story -> story.fits(source));
        if (!settings.enabled() || told.isEmpty()) {
            return vanilla;
        }
        return FallbackText.oneOf(
                "checks.death." + told.get().story().id(),
                player.getRandom(),
                player.getDisplayName(),
                killer(source),
                told.get().item());
    }

    public static void clear() {
        LOG.clear();
    }

    private static Component killer(DamageSource source) {
        Entity killer = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        return killer != null ? Names.of(killer) : FallbackText.of("checks.death.killer.unknown");
    }
}
