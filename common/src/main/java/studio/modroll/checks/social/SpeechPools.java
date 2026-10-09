package studio.modroll.checks.social;

import java.util.Optional;

/**
 * Which pool of speech bubble lines a social outcome draws from: {@code checks.speech.<target>.<outcome>}
 * in the lang file. A caught pickpocket has its own pool; one that goes unnoticed says nothing. Plead and
 * Lie have their own, {@code checks.speech.<target>.<plead|lie>.<outcome>}, as has a Performance,
 * {@code checks.speech.<target>.performance.<outcome>}. Mobs never speak.
 */
public final class SpeechPools {

    private static final String PREFIX = "checks.speech.";
    private static final String CAUGHT = "caught";
    private static final String PERFORMANCE = "performance.";

    private SpeechPools() {}

    public static Optional<String> of(SocialTarget target, SocialAction action, SocialOutcome outcome) {
        if (target.mob()) {
            return Optional.empty();
        }
        if (action.deEscalates()) {
            return Optional.of(key(target, action.id() + "." + outcome.poolId(action)));
        }
        if (action != SocialAction.PICKPOCKET) {
            return Optional.of(key(target, outcome.id()));
        }
        return switch (outcome) {
            case CRITICAL_FAILURE, FAILURE -> Optional.of(key(target, CAUGHT));
            case BARELY -> Optional.of(key(target, outcome.id()));
            case SUCCESS, CRITICAL_SUCCESS -> Optional.empty();
        };
    }

    /** A barely made Performance counts as a success. */
    public static String performance(SocialTarget target, SocialOutcome outcome) {
        SocialOutcome heard = outcome == SocialOutcome.BARELY ? SocialOutcome.SUCCESS : outcome;
        return key(target, PERFORMANCE + heard.id());
    }

    private static String key(SocialTarget target, String outcome) {
        return PREFIX + target.id() + "." + outcome;
    }
}
