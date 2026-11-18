package studio.modroll.checks.body;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import studio.modroll.checks.api.Ability;

/**
 * {@code percent} amounts are fractions shown as percentages; {@code reduces} ones shrink something, so a positive
 * amount shows as a minus.
 */
public enum Extra {
    KNOCKBACK("knockback", Ability.STRENGTH, false, false),
    MINING_SPEED("mining_speed", Ability.STRENGTH, true, false),
    BOW_DRAW("bow_draw", Ability.DEXTERITY, true, false),
    CROSSBOW_RELOAD("crossbow_reload", Ability.DEXTERITY, true, false),
    BREATH("breath", Ability.CONSTITUTION, true, false),
    EXHAUSTION("exhaustion", Ability.CONSTITUTION, true, true);

    public static final StreamCodec<ByteBuf, Extra> STREAM_CODEC =
            ByteBufCodecs.idMapper(index -> values()[index], Extra::ordinal);

    private final String id;
    private final Ability ability;
    private final boolean percent;
    private final boolean reduces;

    Extra(String id, Ability ability, boolean percent, boolean reduces) {
        this.id = id;
        this.ability = ability;
        this.percent = percent;
        this.reduces = reduces;
    }

    public String id() {
        return id;
    }

    public Ability ability() {
        return ability;
    }

    public boolean percent() {
        return percent;
    }

    public boolean reduces() {
        return reduces;
    }
}
