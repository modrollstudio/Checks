# Critfall modifiers

Checks registers a Critfall `ModifierProvider` at startup, so Critfall's attack, damage and save rolls
and AC use Checks scores. Critfall still rolls every die. Configured by the
`critfall` object in `config/checks/scores.json` (see [scores-config.md](scores-config.md)):

```json
"critfall": {
  "enabled": true,
  "unprofiled_mobs": false,
  "save_abilities": { "critfall:spell": "dex" }
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
`dex`. Listed keys override the defaults; a key with an unknown ability is skipped with a warning.
Save keys not in the map keep Critfall's own bonus.

## Toggle

`enabled: false` takes Checks out of Critfall's single provider slot, so Critfall rolls exactly as it
does without Checks and another mod can register its own provider. Turning it back on (for example
with `/reload`) registers Checks again. Checks never replaces or removes another mod's provider: if
one holds the slot, Checks logs a warning and supplies no Critfall modifiers. Critfall's own
`modifier_providers.enabled: false` in its `rules.json` ignores any provider from Critfall's side.
