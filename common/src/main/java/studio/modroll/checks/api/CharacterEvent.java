package studio.modroll.checks.api;

import net.minecraft.server.level.ServerPlayer;

public record CharacterEvent(ServerPlayer player, CharacterChange change) {}
