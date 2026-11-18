package studio.modroll.checks.api;

import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.critfall.api.dice.RollResult;

public record OpenRoll(RollResult result, RollMode mode, boolean canceled) {}
