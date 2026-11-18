package studio.modroll.checks.api;

import studio.modroll.critfall.api.combat.ContestResult;

public record ContestRoll(ContestResult result, boolean canceled) {}
