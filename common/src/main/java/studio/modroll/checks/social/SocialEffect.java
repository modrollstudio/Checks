package studio.modroll.checks.social;

import java.util.Optional;
import net.minecraft.world.entity.ai.gossip.GossipType;

/**
 * What a social outcome does to a trader: changes the player's prices by {@code pricePercent} (negative
 * for a discount) for {@code priceTicks}, spreads gossip about the player, refuses to trade with them for
 * {@code refuseTicks}, and may turn nearby iron golems on them. Zero ticks means no such effect.
 */
public record SocialEffect(
        double pricePercent, int priceTicks, Optional<Gossip> gossip, int refuseTicks, boolean angersGolems) {

    public static final SocialEffect NONE = new SocialEffect(0, 0, Optional.empty(), 0, false);

    /**
     * Vanilla villager gossip about the player: {@code amount} of {@code type}, told to the target, or with
     * {@code nearby} to every villager within the social gossip radius at once.
     */
    public record Gossip(GossipType type, int amount, boolean nearby) {}

    public boolean changesPrices() {
        return pricePercent != 0 && priceTicks > 0;
    }
}
