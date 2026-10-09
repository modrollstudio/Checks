# Skills

Every living entity has every loaded skill. A skill is governed by one of the six abilities, and an
entity's value for it is:

- **skill modifier** = governing ability modifier + proficiency bonus × (0, 1 or 2) + skill bonus
- **passive score** = 10 + skill modifier, plus [bonus sources](bonus-sources.md#passive-scores)

The proficiency multiplier is 0 for no proficiency, 1 when proficient and 2 with expertise (see
[proficiency.md](proficiency.md)). The flat skill bonus defaults to `0`. Entities get it from
datapack profiles (see [entity-score-profiles.md](entity-score-profiles.md)); players can also have
it set with `/checks set`.

## Standard skills

Checks ships the 18 standard skills in its built-in datapack under the `checks` namespace:

| Skill | Ability | Skill | Ability |
| --- | --- | --- | --- |
| `acrobatics` | dex | `medicine` | wis |
| `animal_handling` | wis | `nature` | int |
| `arcana` | int | `perception` | wis |
| `athletics` | str | `performance` | cha |
| `deception` | cha | `persuasion` | cha |
| `history` | int | `religion` | int |
| `insight` | wis | `sleight_of_hand` | dex |
| `intimidation` | cha | `stealth` | dex |
| `investigation` | int | `survival` | wis |

## Adding skills

A datapack adds a skill with a file at `data/<namespace>/checks/skill/<name>.json`. The skill id is
`<namespace>:<name>`. Example: [examples/skill.json](examples/skill.json)

```json
{
  "format_version": 1,
  "ability": "wis"
}
```

- `format_version` (int): currently `1`. A newer version warns and parses anyway.
- `ability` (required): one of `str`, `dex`, `con`, `int`, `wis`, `cha`.

A file at the same path as a standard skill (`data/checks/checks/skill/stealth.json`) replaces it,
for example to change its governing ability. The six abilities themselves are fixed.

A file is rejected (logged as an error and skipped, while the rest load normally) when `ability` is
missing, not a string, or not one of the six ids, or when the file root is not a JSON object.
Unknown keys warn and are ignored.

The stat screen names a skill through the lang key `checks.skill.<namespace>.<path>`, falling back
to its path in title case. See [stat-screen.md](stat-screen.md#names).

## Skill ids

Wherever a skill is named (profile `skills` keys, commands), a full id (`mypack:sailing`) always
works. A name with no namespace (`stealth`) means the `checks:` skill of that name, so the standard
skills can be written short.

## Players

A player's skill bonus, highest first:

1. a value set with `/checks set` (persisted per player in the world save)
2. a datapack profile matching `minecraft:player`
3. `0`

A bonus set for a skill that a datapack later removes stays in the save and comes back if the skill
does.

## Reloading

Skills load on server start and hot-reload on `/reload`, before profiles, so a profile can give a
bonus to a skill added in the same reload.

## Toggle

`skills.enabled: false` in `config/checks/scores.json` (see [scores-config.md](scores-config.md))
treats every flat skill bonus as `0`, from profiles and `/checks set` alike: each skill modifier is
then its governing ability modifier plus any skill proficiency (which has its own
[toggle](proficiency.md#toggle)). Stored bonuses are kept and apply again when it is turned back on.

## Commands

`set` and `prof` require op (permission level 2).

- `/checks get <target>` shows the six abilities, the proficiency bonus and saves, then every skill
  as `stealth +3 (dex, passive 13)`, with its proficiency when it has one:
  `stealth +7 (dex, expertise, passive 17)`.
- `/checks get <target> <skill>` shows one skill. Its command result is the skill modifier.
- `/checks set <player> <skill> <bonus>` sets and persists a player's skill bonus (`-30..30`).
- `/checks prof <player> <skill> <none/proficient/expertise>` sets and persists a player's skill
  proficiency.
