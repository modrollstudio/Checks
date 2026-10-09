package studio.modroll.checks.api;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.critfall.api.dice.RollMode;

/**
 * Fired on the server before a check, save or contest side made through {@link ChecksApi} is rolled.
 * Listeners may change the DC, add to the bonus, grant advantage, impose disadvantage, or cancel.
 * Advantage and disadvantage combine the 5e way: having both rolls normally, however many sources
 * grant each. The bonus starts with the entity's bonus sources; the roll mode starts with the caller's
 * mode and the bonus sources' modes.
 */
public final class BeforeCheckEvent {

    private final LivingEntity entity;
    private final Stat stat;
    private final CheckKind kind;
    private final Optional<LivingEntity> opponent;
    private final int modifier;
    private OptionalInt dc;
    private int bonus;
    private boolean advantage;
    private boolean disadvantage;
    private boolean canceled;

    public BeforeCheckEvent(
            LivingEntity entity,
            Stat stat,
            CheckKind kind,
            Optional<LivingEntity> opponent,
            OptionalInt dc,
            int modifier,
            int bonus,
            boolean advantage,
            boolean disadvantage) {
        this.entity = entity;
        this.stat = stat;
        this.kind = kind;
        this.opponent = opponent;
        this.dc = dc;
        this.modifier = modifier;
        this.bonus = bonus;
        this.advantage = advantage;
        this.disadvantage = disadvantage;
    }

    /** The entity rolling. */
    public LivingEntity entity() {
        return entity;
    }

    /** For a save, always an {@link Ability}. */
    public Stat stat() {
        return stat;
    }

    public CheckKind kind() {
        return kind;
    }

    /** The other side of a contest, or the entity whose passive score is the DC. */
    public Optional<LivingEntity> opponent() {
        return opponent;
    }

    /** Empty for a contest side or an open roll. */
    public OptionalInt dc() {
        return dc;
    }

    /** @throws IllegalStateException for a contest side or an open roll, which have no DC */
    public void dc(int value) {
        if (dc.isEmpty()) {
            throw new IllegalStateException("a " + kind + " without a DC cannot be given one");
        }
        dc = OptionalInt.of(value);
    }

    /** The stat's own modifier: ability, skill or save modifier. */
    public int modifier() {
        return modifier;
    }

    /** What the bonus sources and listeners add on top of {@link #modifier()}. */
    public int bonus() {
        return bonus;
    }

    public void addBonus(int value) {
        bonus += value;
    }

    public void grantAdvantage() {
        advantage = true;
    }

    public void imposeDisadvantage() {
        disadvantage = true;
    }

    public boolean hasAdvantage() {
        return advantage;
    }

    public boolean hasDisadvantage() {
        return disadvantage;
    }

    /** The mode the d20 is rolled with once advantage and disadvantage are combined. */
    public RollMode mode() {
        if (advantage == disadvantage) {
            return RollMode.NORMAL;
        }
        return advantage ? RollMode.ADVANTAGE : RollMode.DISADVANTAGE;
    }

    /** Nothing is rolled; see {@link ChecksApi} for what a canceled roll returns. */
    public void cancel() {
        canceled = true;
    }

    public boolean isCanceled() {
        return canceled;
    }
}
