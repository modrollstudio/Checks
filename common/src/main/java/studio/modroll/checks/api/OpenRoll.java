package studio.modroll.checks.api;

import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.critfall.api.dice.RollResult;

/**
 * A check with no DC, from {@link ChecksApi#roll}: Critfall's result and the mode the d20 was rolled with
 * once bonus sources and check event listeners had their say, so {@code RollDetail.of(mode, result)} gives
 * its d20s. A canceled roll rolled nothing: its {@code result} has no dice and totals 0.
 */
public record OpenRoll(RollResult result, RollMode mode, boolean canceled) {}
