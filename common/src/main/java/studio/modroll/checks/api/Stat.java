package studio.modroll.checks.api;

/** What a check rolls on: one of the six {@link Ability abilities} or a loaded {@link Skill}. */
public sealed interface Stat permits Ability, Skill {}
