package studio.modroll.checks.social;

/** A natural 1 or 20 decides it outright. */
public enum SocialOutcome {
    CRITICAL_FAILURE("critical_failure"),
    FAILURE("failure"),
    BARELY("barely"),
    SUCCESS("success"),
    CRITICAL_SUCCESS("critical_success");

    private static final int NATURAL_ONE = 1;
    private static final int NATURAL_TWENTY = 20;

    private final String id;

    SocialOutcome(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** This outcome's key in {@code action}'s lang pools: Plead and Lie have none for barely, which counts as a success. */
    public String poolId(SocialAction action) {
        return action.deEscalates() && this == BARELY ? SUCCESS.id() : id;
    }

    public boolean succeeded() {
        return this == BARELY || this == SUCCESS || this == CRITICAL_SUCCESS;
    }

    public static SocialOutcome of(int natural, int total, int dc, int barelyMargin) {
        if (natural == NATURAL_ONE) {
            return CRITICAL_FAILURE;
        }
        if (natural == NATURAL_TWENTY) {
            return CRITICAL_SUCCESS;
        }
        if (total < dc) {
            return FAILURE;
        }
        return total - dc <= barelyMargin ? BARELY : SUCCESS;
    }
}
