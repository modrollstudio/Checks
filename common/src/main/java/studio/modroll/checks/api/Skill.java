package studio.modroll.checks.api;

import net.minecraft.resources.ResourceLocation;

/** A skill and its governing ability, loaded from {@code data/<ns>/checks/skill/<id>.json}. */
public record Skill(ResourceLocation id, Ability ability) implements Stat {}
