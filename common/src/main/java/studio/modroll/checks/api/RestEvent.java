package studio.modroll.checks.api;

import net.minecraft.server.level.ServerPlayer;

public record RestEvent(ServerPlayer player, RestKind kind) {}
