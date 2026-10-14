# Critfall modifiers

Checks registers a Critfall `ModifierProvider` at startup, so Critfall's attack, damage and save rolls
and AC use Checks scores. Critfall still rolls every die. Configured by the
`critfall` object in `config/checks/scores.json` (see [scores-config.md](scores-config.md)):

```json
"critfall": {
  "enabled": true,
  "unprofiled_mobs": false,
  "save_abilities": {
    "critfall:spell": "dex",
    "critfall:strength": "str",
    "critfall:dexterity": "dex",
    "critfall:constitution": "con",
    "critfall:intelligence": "int",
    "critfall:wisdom": "wis",
    "critfall:charisma": "cha"
  }
}
```

## Who gets Checks values

- **Players**, always.
- **Mobs with a Checks profile** (a datapack profile in `data/<namespace>/checks/entity_profile/`, see
  [entity-score-profiles.md](entity-score-profiles.md)). With `profiles.enabled: false` no mob has
  one.
- **Every other mob** keeps Critfall's own tuned or derived bonuses, unless `unprofiled_mobs` is
  `true`; then it gets its derived Checks values too.

## What they get

| Roll | Melee | Projectile, thrown | Spell |
| --- | --- | --- | --- |
| Attack | STR modifier + proficiency bonus | DEX modifier + proficiency bonus | Critfall's own |
| Damage | STR modifier | DEX modifier | Critfall's own |
| Defender's AC | + DEX by armor weight | + DEX by armor weight | + DEX by armor weight |

The damage modifier is added to Critfall's own damage dice: `1d8+2` with STR +3 rolls `1d8+5`.
Everything Critfall already counts stays, such as weapon material, Sharpness and the other damage
enchantments, bow draw and Power, and the Strength and Weakness effects. Critfall leaves a dice-less
amount (its derived flat `1`) as is.

**AC**: Checks adds the defender's DEX to Critfall's AC by the 5e armor rules: all of it unarmored
or in light armor, up to +2 in medium, none in heavy (see [body.md](body.md#armor-class)).
`armor_class.enabled: false` keeps Critfall's own AC.

**Saves**: `save_abilities` maps a Critfall save key to the ability whose save modifier (with save
proficiency) Checks supplies. `critfall:spell`, the save against a Critfall save spell, defaults to
`dex`, and the six ability keys (`critfall:strength`, `critfall:dexterity`, `critfall:constitution`,
`critfall:intelligence`, `critfall:wisdom`, `critfall:charisma`) default to their own ability. Listed
keys override the defaults, and keys a file leaves out keep theirs, so a config written before 0.1.1
still gets the six. A key with an unknown ability is skipped with a warning. Save keys not in the map
keep Critfall's own bonus.

## Hazards

Critfall (0.2.13 and newer) rolls saves and attacks for hazards with no living attacker, such as Dungeon Crawl's
traps. A hazard save asks for its save key like any other Critfall save, so a DEX trap saves with the
DEX modifier, plus the proficiency bonus when proficient in DEX saves. A hazard attack rolls against the
AC Checks adds DEX to.

[Species traits](traits.md) and [bonus sources](bonus-sources.md) count on hazard saves:

- **Poison**: a hazard whose damage type is in `#c:is_poison` counts as a save against poison, so
  `vanilla_save_advantage` against `poison` (Dwarven Resilience) applies, whatever the save's ability.
  The tag is the loaders' conventional one, so a mod's poison counts once it tags its damage type.
- **Roll bonuses and bonus sources**: a trait `roll_bonus` or a datapack bonus source on saves of an
  ability (Gnomish Cunning on INT, WIS and CHA) adds its flat `bonus` and its `mode` to hazard saves
  that `save_abilities` maps to that ability. Advantage and disadvantage combine the 5e way.
- **Luck**: an unlimited `natural_1` reroll rerolls a natural 1 on the save once. Critfall rerolls it,
  so there is no chat line for it.
- Damage resistance and last stand apply to a hazard's damage as to any other.

A hazard save gets no `failure` or `daily` reroll (Resourceful): Critfall rerolls only natural 1s, and
only when told before the roll. `BeforeCheckEvent` and `AfterCheckEvent` do not fire for a hazard
save. The readout is Critfall's, such as `Spike trap: DEX save d20 8+2=10 vs DC 13 — FAILED 2d6 = 7`,
with both dice under advantage. A hazard attack is the hazard's roll, so no trait or bonus source
changes it. `enabled: false` also stops traits and bonus sources from changing hazard rolls.

## Attack rolls

An unlimited `natural_1` reroll (Luck) rerolls a natural 1 on the player's Critfall attack rolls once,
melee, ranged and spell. Bonus sources and other traits don't change attack rolls. `enabled: false`
turns this off too.

## Toggle

`enabled: false` takes Checks out of Critfall's single provider slot, so Critfall rolls exactly as it
does without Checks and another mod can register its own provider. Turning it back on (for example
with `/reload`) registers Checks again. Checks never replaces or removes another mod's provider: if
one holds the slot, Checks logs a warning and supplies no Critfall modifiers. Critfall's own
`modifier_providers.enabled: false` in its `rules.json` ignores any provider from Critfall's side.
