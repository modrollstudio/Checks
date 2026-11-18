package studio.modroll.checks.api;

import net.minecraft.resources.ResourceLocation;

public record Skill(ResourceLocation id, Ability ability) implements Stat {}
