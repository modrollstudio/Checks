package studio.modroll.checks.social;

import java.util.Optional;

/**
 * The mood a target voices after a social action. A pickpocket that goes unnoticed leaves the target with
 * nothing to say.
 */
public enum SocialMood {
    ANGRY("angry", 0),
    HAPPY("happy", 2),
    SCARED("scared", 0),
    SUSPICIOUS("suspicious", 12);

    private final String id;
    private final int delayTicks;

    SocialMood(String id, int delayTicks) {
        this.id = id;
        this.delayTicks = delayTicks;
    }

    /** The second half of the voice sound id, as in {@code checks:villager.angry}. */
    public String id() {
        return id;
    }

    /** How long the target pauses before speaking: a snap of anger, a slow suspicious beat. */
    public int delayTicks() {
        return delayTicks;
    }

    public static Optional<SocialMood> of(SocialAction action, SocialOutcome outcome) {
        return switch (outcome) {
            case CRITICAL_FAILURE -> Optional.of(ANGRY);
            case FAILURE ->
                Optional.of(action == SocialAction.DECEIVE || action == SocialAction.LIE ? SUSPICIOUS : ANGRY);
            case BARELY -> Optional.of(SUSPICIOUS);
            case SUCCESS, CRITICAL_SUCCESS ->
                switch (action) {
                    case INTIMIDATE, INTIMIDATE_MOB -> Optional.of(SCARED);
                    case PICKPOCKET -> Optional.empty();
                    case PERSUADE, DECEIVE, PLEAD, LIE, CALM -> Optional.of(HAPPY);
                };
        };
    }

    /** How a listener takes a Performance: delighted, unimpressed or annoyed. */
    public static SocialMood ofPerformance(SocialOutcome outcome) {
        if (outcome.succeeded()) {
            return HAPPY;
        }
        return outcome == SocialOutcome.CRITICAL_FAILURE ? ANGRY : SUSPICIOUS;
    }
}
