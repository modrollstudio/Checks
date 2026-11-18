package studio.modroll.checks.api;

import studio.modroll.critfall.api.combat.SaveResult;

public record CheckRoll(SaveResult result, boolean canceled) {}
