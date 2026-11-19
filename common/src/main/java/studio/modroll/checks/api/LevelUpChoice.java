package studio.modroll.checks.api;

import net.minecraft.network.chat.Component;

public record LevelUpChoice(Component label, int count, Runnable open) {}
