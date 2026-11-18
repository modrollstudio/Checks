package studio.modroll.checks.api;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.critfall.api.dice.RollMode;

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

    public LivingEntity entity() {
        return entity;
    }

    public Stat stat() {
        return stat;
    }

    public CheckKind kind() {
        return kind;
    }

    public Optional<LivingEntity> opponent() {
        return opponent;
    }

    public OptionalInt dc() {
        return dc;
    }

    public void dc(int value) {
        if (dc.isEmpty()) {
            throw new IllegalStateException("a " + kind + " without a DC cannot be given one");
        }
        dc = OptionalInt.of(value);
    }

    public int modifier() {
        return modifier;
    }

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

    public RollMode mode() {
        if (advantage == disadvantage) {
            return RollMode.NORMAL;
        }
        return advantage ? RollMode.ADVANTAGE : RollMode.DISADVANTAGE;
    }

    public void cancel() {
        canceled = true;
    }

    public boolean isCanceled() {
        return canceled;
    }
}
