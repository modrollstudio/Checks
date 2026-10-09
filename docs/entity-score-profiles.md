# Entity score profiles

Datapack files at `data/<namespace>/checks/entity_profile/*.json` assign ability scores, skill
bonuses and proficiencies to entity types. Checks ships none: every entity works out of the box
through attribute derivation (see [scores-config.md](scores-config.md)), and profiles pin specific
values. The conventions match Critfall's entity profiles.

## Format

Example: [examples/entity_profile.json](examples/entity_profile.json)

```json
{
  "format_version": 1,
  "matches": ["minecraft:zombie", "#minecraft:skeletons", "alexsmobs:*"],
  "priority": 0,
  "abilities": { "str": 13, "dex": 8, "con": 14 },
  "skills": { "athletics": 2, "checks:perception": 1 },
  "proficiency": {
    "bonus": 3,
    "skills": { "athletics": "expertise", "perception": "proficient" },
    "saves": { "str": "proficient", "con": "proficient" }
  }
}
```

- `format_version` (int): currently `1`. A newer version warns and parses anyway.
- `matches` (required, string or list): which entity types this profile applies to. Each entry is an
  exact id (`minecraft:zombie`), a tag (`#minecraft:skeletons`), or a whole namespace
  (`alexsmobs:*`), so modded entities can be profiled too.
- `priority` (int, default `0`): higher wins when several profiles match. Ties break by match
  specificity (exact > tag > namespace), then by the smaller file id.
- `abilities` (object): any of `str`, `dex`, `con`, `int`, `wis`, `cha`, each an integer `1..30`. A
  listed ability overrides derivation for that ability only; an omitted one is still derived.
- `skills` (object): skill id to bonus, each an integer `-30..30`. A listed skill gets that bonus; an
  omitted one has bonus `0`. Keys are skill ids as described in [skills.md](skills.md#skill-ids), so
  `"stealth"` means `checks:stealth`. A key naming no loaded skill is skipped with a warning; the
  rest of the profile still loads.
- `proficiency` (object, optional): see [Proficiency](#proficiency).

`minecraft:player` can be profiled like any other entity type. For players, a profile overrides the
config `player_defaults`, the default skill bonus of `0` and the config proficiency bonus, and
`/checks set` / `/checks prof` override the profile.

## Proficiency

The `proficiency` object sets the entity's proficiency bonus and its skill and save proficiencies
(rules in [proficiency.md](proficiency.md)):

- `bonus` (int `0..10`): the proficiency bonus. Omitted, it is the config
  `proficiency.default_bonus`.
- `skills` (object): skill id to `"none"`, `"proficient"` or `"expertise"`. Keys work like the
  `skills` bonus keys: a key naming no loaded skill is skipped with a warning. An omitted skill is
  `none`.
- `saves` (object): any of `str`, `dex`, `con`, `int`, `wis`, `cha` to `"none"` or `"proficient"`.
  An omitted save is `none`.

## Validation

A file is rejected (logged as an error and skipped, while the rest of the pack loads normally) when:

- `matches` is missing, empty, or has an invalid entry
- an ability score is outside `1..30` or not an integer
- a skill bonus is outside `-30..30` or not an integer
- `proficiency.bonus` is outside `0..10` or not an integer
- a proficiency level is not one of `"none"`, `"proficient"`, `"expertise"`
- a save is `"expertise"`
- the file root is not a JSON object

Unknown keys, including unknown ability and save keys, warn and are ignored.

## Reloading

Profiles load on server start and hot-reload on `/reload`, after skills, so skill keys resolve
against the skills of the same reload. Set `profiles.enabled` to `false` in
`config/checks/scores.json` to ignore all profiles, including their skill bonuses and proficiencies.
