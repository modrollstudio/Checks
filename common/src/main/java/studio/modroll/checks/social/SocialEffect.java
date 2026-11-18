package studio.modroll.checks.social;

import java.util.Optional;
import net.minecraft.world.entity.ai.gossip.GossipType;

public record SocialEffect(
        double pricePercent, int priceTicks, Optional<Gossip> gossip, int refuseTicks, boolean angersGolems) {

    public static final SocialEffect NONE = new SocialEffect(0, 0, Optional.empty(), 0, false);

    public record Gossip(GossipType type, int amount, boolean nearby) {}

    public boolean changesPrices() {
        return pricePercent != 0 && priceTicks > 0;
    }
}
