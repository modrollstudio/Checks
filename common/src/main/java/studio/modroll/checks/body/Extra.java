package studio.modroll.checks.body;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import studio.modroll.checks.api.Ability;

/**
 * A small side effect of an ability modifier: each point of modifier adds its configured
 * {@code per_point} to the amount. {@code percent} amounts are fractions shown as percentages;
 * {@code reduces} amounts make something smaller, so a positive amount is shown as a minus.
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

    /** The key under {@code attribute_extras} in {@code scores.json} and in the lang file. */
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
