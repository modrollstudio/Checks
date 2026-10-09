package studio.modroll.checks.bonus;

import java.util.Collection;
import java.util.List;
import studio.modroll.checks.api.CheckKind;
import studio.modroll.checks.api.Stat;
import studio.modroll.critfall.api.dice.RollMode;

/** Every bonus source that applies to one check, in source id order. */
public record SituationalBonus(List<BonusPart> parts) {

    public static final SituationalBonus NONE = new SituationalBonus(List.of());

    public SituationalBonus {
        parts = List.copyOf(parts);
    }

    /** {@code sources} must already be in id order. */
    public static SituationalBonus of(
            Collection<BonusSource> sources, BonusSource.Bearer bearer, CheckKind kind, Stat stat) {
        return new SituationalBonus(sources.stream()
                .filter(source -> source.appliesTo(bearer, kind, stat))
                .map(BonusSource::part)
                .toList());
    }

    public int bonus() {
        return parts.stream().mapToInt(BonusPart::bonus).sum();
    }

    /**
     * What these sources add to a passive score, the 5e way: the flat bonuses, plus {@code
     * passiveAdvantage} for advantage or minus it for disadvantage; having both adds neither.
     */
    public int passiveBonus(int passiveAdvantage) {
        if (advantage() == disadvantage()) {
            return bonus();
        }
        return bonus() + (advantage() ? passiveAdvantage : -passiveAdvantage);
    }

    public boolean advantage() {
        return has(RollMode.ADVANTAGE);
    }

    public boolean disadvantage() {
        return has(RollMode.DISADVANTAGE);
    }

    private boolean has(RollMode mode) {
        return parts.stream().anyMatch(part -> part.mode() == mode);
    }
}
