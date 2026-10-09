# Proficiency

Every living entity has a **proficiency bonus**, and a proficiency level in each skill and each
saving throw. The bonus is a flat number per entity, set by the config, datapack profiles and
commands; while levelling is on, a player's bonus follows their character level instead of the
profile and config default (see [levelling.md](levelling.md#proficiency-bonus)).

## Levels

| Level | Skill | Saving throw |
| --- | --- | --- |
| `none` | adds nothing | adds nothing |
| `proficient` | adds the proficiency bonus | adds the proficiency bonus |
| `expertise` | adds the proficiency bonus twice | not allowed |

## Rules

- **skill modifier** = governing ability modifier + proficiency bonus × (0, 1 or 2) + flat skill bonus
- **save modifier** = ability modifier + proficiency bonus if proficient in that save
- **passive score** = 10 + skill modifier, plus [bonus sources](bonus-sources.md#passive-scores)

An ability check (`/checks roll <target> dex`, `ChecksApi.check(entity, Ability.DEXTERITY, dc)`)
adds the plain ability modifier. Only a saving throw (`ChecksApi.savingThrow`) adds save
proficiency.

With DEX 16 (+3), a flat stealth bonus of 1 and proficiency bonus +2, stealth is +4 with no
proficiency, +6 proficient and +8 with expertise (passive 14, 16, 18).

## Where values come from

The proficiency bonus is an integer `0..10`. Highest first:

1. players: a value set with `/checks set <player> proficiency <n>` (persisted per player)
2. players, while levelling is on: their level's bonus (`+2` at levels 1–4 up to `+6` at 17–20)
3. a datapack profile's `proficiency.bonus` (see
   [entity-score-profiles.md](entity-score-profiles.md#proficiency))
4. `proficiency.default_bonus` in `config/checks/scores.json` (default `2`, see
   [scores-config.md](scores-config.md))

A skill or save proficiency, highest first:

1. players: a level set with `/checks prof` (persisted per player; `none` here overrides a profile)
2. players: `proficient` from their confirmed character: the skills they picked, their background's
   skills, skills picked with species traits, and their class's saves (see
   [character-creation.md](character-creation.md#skills))
3. a datapack profile's `proficiency.skills` / `proficiency.saves`
4. `none`

## Commands

Both require op (permission level 2).

- `/checks set <player> proficiency <n>` sets and persists a player's proficiency bonus (`0..10`).
- `/checks prof <player> <skill/ability> <none/proficient/expertise>` sets and persists a player's
  skill proficiency or, for an ability id, saving-throw proficiency. `expertise` on an ability is
  rejected.

`/checks get <target>` shows the proficiency bonus and every save modifier on its second line, and
marks proficient skills and saves:

```
Steve: str 10 (+0), dex 16 (+3), con 12 (+1), int 10 (+0), wis 10 (+0), cha 10 (+0)
Proficiency +3, saves: str +0, dex +3, con +4 (proficient), int +0, wis +0, cha +0
Skills: acrobatics +3 (dex, passive 13), ..., stealth +9 (dex, expertise, passive 19), ...
```

`/checks get <target> <ability>` adds the save (`Steve: con 12 (+1), save +4 (proficient)`).

## Toggle

`proficiency.enabled: false` in `config/checks/scores.json` makes every proficiency bonus `0` and
every level `none`: a skill modifier is the ability modifier + flat skill bonus, a save modifier the
ability modifier. Stored and profiled values
are kept and apply again when it is turned back on.

The `skills.enabled` toggle only zeroes the flat skill bonus; skill proficiency follows
`proficiency.enabled`.
