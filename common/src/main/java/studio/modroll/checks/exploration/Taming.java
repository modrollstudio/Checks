package studio.modroll.checks.exploration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import studio.modroll.checks.Checks;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * Animal Handling: each attempt by a player in survival or adventure to tame a wolf, cat or parrot (feeding
 * it) or a horse, donkey, mule or llama (riding it until it decides) rolls Animal Handling through the API. A
 * made check tames the animal on that attempt; a failed one leaves it to vanilla's own chance. Each loader
 * asks where vanilla decides an attempt.
 */
public final class Taming {

    private static final ResourceLocation ANIMAL_HANDLING = Checks.id("animal_handling");

    private Taming() {}

    /** Whether {@code tamer}'s attempt to tame an animal is made, rolling for it. */
    public static boolean made(Entity tamer) {
        ScoresConfig.TamingSettings settings =
                ScoresRuntime.config().exploration().taming();
        return settings.enabled()
                && tamer instanceof ServerPlayer player
                && ExplorationChecks.rollsFor(player)
                && ExplorationChecks.made(player, ANIMAL_HANDLING, settings.dc(), "taming")
                        .orElse(false);
    }
}
