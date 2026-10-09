package studio.modroll.checks.creation;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A point budget and the cost of each buyable score; every score starts at the cheapest one. */
public record PointBuy(int budget, Map<Integer, Integer> costs) {

    public static final StreamCodec<ByteBuf, PointBuy> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            PointBuy::budget,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.VAR_INT),
            PointBuy::costs,
            PointBuy::new);

    public PointBuy {
        costs = Map.copyOf(costs);
    }

    public int min() {
        return costs.keySet().stream().mapToInt(Integer::intValue).min().orElseThrow();
    }

    public int max() {
        return costs.keySet().stream().mapToInt(Integer::intValue).max().orElseThrow();
    }

    public boolean buyable(int score) {
        return costs.containsKey(score);
    }

    /** Empty when any score is not buyable. */
    public OptionalInt totalCost(List<Integer> scores) {
        if (!scores.stream().allMatch(this::buyable)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(scores.stream().mapToInt(costs::get).sum());
    }
}
