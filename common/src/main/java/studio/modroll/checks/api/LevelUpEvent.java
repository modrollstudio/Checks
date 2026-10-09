package studio.modroll.checks.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * A player gained one character level: {@code newLevel} is always {@code oldLevel + 1}, so a jump of
 * several levels arrives as one event per level, lowest first.
 */
public record LevelUpEvent(ServerPlayer player, int oldLevel, int newLevel) {}
