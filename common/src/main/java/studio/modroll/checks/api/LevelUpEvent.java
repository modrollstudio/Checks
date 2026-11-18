package studio.modroll.checks.api;

import net.minecraft.server.level.ServerPlayer;

public record LevelUpEvent(ServerPlayer player, int oldLevel, int newLevel) {}
