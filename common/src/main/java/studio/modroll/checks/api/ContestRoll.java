package studio.modroll.checks.api;

import studio.modroll.critfall.api.combat.ContestResult;

/**
 * Both sides of a contest, from {@link ChecksApi#contest}: Critfall's result, and whether a check event
 * listener canceled either side. A canceled contest rolled nothing: its {@code result} goes to the opponent
 * with every total 0.
 */
public record ContestRoll(ContestResult result, boolean canceled) {}
