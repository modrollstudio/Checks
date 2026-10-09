package studio.modroll.checks.trigger;

import java.util.Locale;
import java.util.Set;

/** The branch a trigger's roll takes. */
public enum TriggerOutcome {
    SUCCESS,
    FAILURE,
    NATURAL_1,
    NATURAL_20;

    private static final int ONE = 1;
    private static final int TWENTY = 20;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * A natural 20 or natural 1 takes its own branch when the trigger has one; otherwise meeting the DC
     * succeeds and anything else fails, as for any check.
     */
    public static TriggerOutcome of(int natural, boolean metDc, Set<TriggerOutcome> branches) {
        if (natural == TWENTY && branches.contains(NATURAL_20)) {
            return NATURAL_20;
        }
        if (natural == ONE && branches.contains(NATURAL_1)) {
            return NATURAL_1;
        }
        return metDc ? SUCCESS : FAILURE;
    }
}
