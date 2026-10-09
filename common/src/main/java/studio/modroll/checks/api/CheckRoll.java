package studio.modroll.checks.api;

import studio.modroll.critfall.api.combat.SaveResult;

/**
 * A check or saving throw against a DC, from {@link ChecksApi}: Critfall's result, and whether a check event
 * listener canceled it. A canceled check rolled nothing: its {@code result} is a failure with natural 0.
 */
public record CheckRoll(SaveResult result, boolean canceled) {}
