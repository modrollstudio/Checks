package studio.modroll.checks.social;

import java.util.EnumSet;
import java.util.Set;

/** How a target visibly reacts to a social action. Cosmetic: no reaction changes an outcome. */
public enum SocialReaction {
    /** Head shake and a "no"; only villagers and wandering traders shake their heads. */
    SHAKE_HEAD,
    /** Angry particles; a piglin snarls. */
    ANGRY,
    /** Happy particles and a "yes"; a piglin admires; a mob only shows the particles. */
    PLEASED,
    /** Runs from the player; only villagers do. */
    FLEE,
    /** Sweats; a piglin backs off. */
    COWER,
    /** The wandering trader's llamas spit at the player. */
    LLAMAS_SPIT;

    /** What this outcome of {@code action} on {@code target} calls for. */
    public static Set<SocialReaction> of(SocialAction action, SocialTarget target, SocialOutcome outcome) {
        return switch (target) {
            case PIGLIN -> piglin(action, outcome.succeeded());
            case HOSTILE, NEUTRAL -> mob(action, outcome);
            case VILLAGER, WANDERING_TRADER -> trader(action, target, outcome);
        };
    }

    /** How villagers and piglins take a Performance: no reaction to a dull one. */
    public static Set<SocialReaction> ofPerformance(SocialOutcome outcome) {
        if (outcome.succeeded()) {
            return EnumSet.of(PLEASED);
        }
        return outcome == SocialOutcome.CRITICAL_FAILURE ? EnumSet.of(ANGRY) : EnumSet.noneOf(SocialReaction.class);
    }

    private static Set<SocialReaction> piglin(SocialAction action, boolean succeeded) {
        return switch (action) {
            case DECEIVE -> EnumSet.of(succeeded ? PLEASED : ANGRY);
            case INTIMIDATE -> EnumSet.of(succeeded ? COWER : ANGRY);
            case PERSUADE, PICKPOCKET, PLEAD, LIE, INTIMIDATE_MOB, CALM -> EnumSet.noneOf(SocialReaction.class);
        };
    }

    /** A scared mob sweats and one that rushes the player bristles; a calmed one settles, an unmoved one bristles. */
    private static Set<SocialReaction> mob(SocialAction action, SocialOutcome outcome) {
        boolean succeeded = outcome.succeeded();
        return switch (action) {
            case INTIMIDATE_MOB -> {
                if (succeeded) {
                    yield EnumSet.of(COWER);
                }
                yield outcome == SocialOutcome.CRITICAL_FAILURE
                        ? EnumSet.of(ANGRY)
                        : EnumSet.noneOf(SocialReaction.class);
            }
            case CALM -> EnumSet.of(succeeded ? PLEASED : ANGRY);
            case PERSUADE, DECEIVE, PLEAD, LIE, INTIMIDATE, PICKPOCKET -> EnumSet.noneOf(SocialReaction.class);
        };
    }

    private static Set<SocialReaction> trader(SocialAction action, SocialTarget target, SocialOutcome outcome) {
        boolean succeeded = outcome.succeeded();
        return switch (action) {
            case PERSUADE, PLEAD, LIE -> haggle(outcome);
            case DECEIVE -> {
                Set<SocialReaction> reactions = haggle(outcome);
                if (!succeeded && target == SocialTarget.WANDERING_TRADER) {
                    reactions.add(LLAMAS_SPIT);
                }
                yield reactions;
            }
            case INTIMIDATE -> succeeded ? EnumSet.of(FLEE, COWER) : EnumSet.of(SHAKE_HEAD, ANGRY);
            case PICKPOCKET -> succeeded ? EnumSet.noneOf(SocialReaction.class) : EnumSet.of(SHAKE_HEAD, ANGRY, FLEE);
            case INTIMIDATE_MOB, CALM -> EnumSet.noneOf(SocialReaction.class);
        };
    }

    private static Set<SocialReaction> haggle(SocialOutcome outcome) {
        if (outcome.succeeded()) {
            return EnumSet.of(PLEASED);
        }
        return outcome == SocialOutcome.CRITICAL_FAILURE ? EnumSet.of(SHAKE_HEAD, ANGRY) : EnumSet.of(SHAKE_HEAD);
    }
}
