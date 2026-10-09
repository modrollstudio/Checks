package studio.modroll.checks.api;

/**
 * What a roll through {@link ChecksApi} is: an ability or skill check (with or without a DC, or against a
 * passive score), a saving throw, or one side of a contest.
 */
public enum CheckKind {
    CHECK,
    SAVE,
    CONTEST
}
