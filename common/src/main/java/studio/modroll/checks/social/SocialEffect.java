package studio.modroll.checks.social;

import java.util.Optional;
import net.minecraft.world.entity.ai.gossip.GossipType;

/** Zero ticks means no such effect. */
public record SocialEffect(
        double pricePercent, int priceTicks, Optional<Gossip> gossip, int refuseTicks, boolean angersGolems) {

    public static final SocialEffect NONE = new SocialEffect(0, 0, Optional.empty(), 0, false);

    /** With {@code nearby}, every villager within the gossip radius hears it at once. */
    public record Gossip(GossipType type, int amount, boolean nearby) {}

    public boolean changesPrices() {
        return pricePercent != 0 && priceTicks > 0;
    }
}
