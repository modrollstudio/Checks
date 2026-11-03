# Levelling

Every player has a **character level** from 1 to 20 and a running total of **character XP**, both
saved in the world (`data/checks_levels.dat`). Character XP is separate from the vanilla XP bar: it
only ever goes up, and enchanting, anvils or dying never take any of it away.

New players start at level 1 with 0 XP.

## Earning XP

| Source | Default | Config |
| --- | --- | --- |
| Vanilla XP points, counted as they are gained | 1 character XP per point | `levelling.vanilla_xp` |
| Earning an advancement | 25 character XP | `levelling.advancements` |

- **Vanilla XP**: an orb counts its full value when it is collected, including the points Mending
  spends repairing an item. Points added without an orb (`/xp add <player> <n>`, other mods) count
  when they reach the XP bar; negative points are ignored. Spending XP on enchanting or an anvil,
  and losing it on death, never takes character XP away.
- **Advancements** count once each, when they are completed. Only advancements shown in the
  advancement screen count, so recipe unlocks and hidden technical advancements give nothing. An
  advancement earned while dying gives nothing either (a first death to a mob earns Adventure).

Each source has its own `enabled` switch. With both off, levels only change through commands, for
milestone levelling.

## Levels

A player reaches a level once their total XP meets its threshold. The defaults are the 5e XP table
divided by 10. That puts level 2 a few mobs in, level 5 at roughly the XP of vanilla level 21, and
level 20 at 35,500 XP (about vanilla level 106 if all of it came from XP points). Advancements speed
this up.

| Level | XP | Proficiency | Level | XP | Proficiency |
| --- | --- | --- | --- | --- | --- |
| 1 | 0 | +2 | 11 | 8,500 | +4 |
| 2 | 30 | +2 | 12 | 10,000 | +4 |
| 3 | 90 | +2 | 13 | 12,000 | +5 |
| 4 | 270 | +2 | 14 | 14,000 | +5 |
| 5 | 650 | +3 | 15 | 16,500 | +5 |
| 6 | 1,400 | +3 | 16 | 19,500 | +5 |
| 7 | 2,300 | +3 | 17 | 22,500 | +6 |
| 8 | 3,400 | +3 | 18 | 26,500 | +6 |
| 9 | 4,800 | +4 | 19 | 30,500 | +6 |
| 10 | 6,400 | +4 | 20 | 35,500 | +6 |

XP never lowers a level. A level set by command stays put until the XP passes the next threshold.

On a level-up, everyone sees `Steve reached level 5!` in chat. If the new level brings an ability
score improvement or Expertise, the player is also told to open their character sheet.

Each level after the first also adds max health from the class's hit die and CON (see
[body.md](body.md#max-health)) and one Hit Die to spend on a short rest (see [rests.md](rests.md)).

## Proficiency bonus

While levelling is on, a player's proficiency bonus comes from their level
(`levelling.proficiency_bonus`, one entry per level) instead of a `minecraft:player` profile or
`proficiency.default_bonus`. In order:

1. `/checks set <player> proficiency <n>`
2. the player's level
3. a datapack profile matching `minecraft:player`
4. `proficiency.default_bonus`

With levelling off, step 2 drops out.

Mobs are not affected: they use their profile or `proficiency.default_bonus`. See
[proficiency.md](proficiency.md).

## Ability score improvements

At levels 4, 8, 12, 16 and 19 (`levelling.ability_improvements.levels`) a player earns an ability
score improvement: **+2 to one ability or +1 to two** (`options`). No improvement can raise a score
above 20 (`max_score`). Some classes earn more, at their own levels (`class_levels`, keyed by class
id): a **Fighter** at 6 and 14, a **Rogue** at 10, as in 5e. A class level that repeats a shared level
gives a second improvement there.

Improvements wait until chosen and stack: a player who jumps from level 3 to 12 has three to choose.
They are chosen from the **Level up!** panel (see [below](#the-level-up-panel)): − and + per ability
that only allow the configured splits and stop at the max. **Confirm** sends the choice to the
server, which checks it again: an improvement must be pending, match an allowed split and stay within
the max score. A refused choice is explained in red in chat. Either way the panel reopens with the
current values.

A chosen improvement adds to the score on top of the character's base score (or the player profile
or `player_defaults`), below `/checks set`: a score set by command replaces it. Improvements
are earned per level reached, so lowering a level and raising it again never grants one twice. Only
as many improvements apply as the level and class earn now, in the order chosen: a level 6 Fighter who
chose two and then becomes a Custom class (or turns presets off) keeps the first and loses the
second until they are a Fighter again.

## Expertise

A **Rogue** picks two skills for Expertise at levels 1 and 6, a **Bard** two at 3 and 10
(`levelling.expertise`). Expertise doubles the proficiency bonus on a skill: a level 1 Rogue's Stealth
goes from +2 to +4 proficiency. Only a skill the player is proficient in, without Expertise yet, can be
picked; the stat screen shows two dots for it.

Like improvements, Expertise waits until chosen from the Level up! panel, which lists the skills that
can be picked; the server checks the choice again. Only as much Expertise applies as the level and
class earn now, so a Rogue who becomes a Fighter loses it until they are a Rogue again, and a skill
whose proficiency goes away loses its Expertise with it. A proficiency set with `/checks prof` counts:
Expertise raises it the same way, and a command that sets `expertise` or `none` wins.

## The Level up! panel

While levelling is on, the stat screen has a button that opens the panel: **Level up!** while an
improvement or Expertise waits, **Levels** otherwise. The panel lists what each level up to the
current one gave, newest first: the max health it added (all of level 1's for level 1, so the list
adds up to the max health; in whole hearts, so a level can add nothing and the next a heart), a Hit
Die, the proficiency bonus where it rose, ability score improvements and Expertise. Under the list are a button for each choice waiting, **Improvement (1)**
and **Expertise (2)**, and **Back**.

## Commands

All require op (permission level 2) and are refused while levelling is off.

- `/checks level <player> set <1..20>` sets the level. Raising it counts as a level-up, one event
  per level gained, with the chat message. The player's XP moves to that level's threshold unless it
  already falls within the level. Its command result is the level.
- `/checks xp <player> add <n>` adds `n` (at least 1) character XP and levels up as needed. Its
  command result is the new total.
- `/checks reset <player>` clears the level, XP and chosen improvements along with the character.

## For other mods

`ChecksApi.level(player)` and `ChecksApi.experience(player)` read a player's level and XP.
`ChecksApi.onLevelUp(listener)` is called once per level gained. See [api.md](api.md#levels).

## Config

`levelling` in `config/checks/scores.json` (see [scores-config.md](scores-config.md)):

```json
"levelling": {
  "enabled": true,
  "xp_thresholds": [
    0, 30, 90, 270, 650, 1400, 2300, 3400, 4800, 6400,
    8500, 10000, 12000, 14000, 16500, 19500, 22500, 26500, 30500, 35500
  ],
  "proficiency_bonus": [2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6],
  "vanilla_xp": { "enabled": true, "xp_per_point": 1 },
  "advancements": { "enabled": true, "xp": 25 },
  "ability_improvements": {
    "levels": [4, 8, 12, 16, 19],
    "class_levels": { "checks:fighter": [6, 14], "checks:rogue": [10] },
    "options": [[2], [1, 1]],
    "max_score": 20
  },
  "expertise": {
    "enabled": true,
    "skills_per_level": 2,
    "class_levels": { "checks:rogue": [1, 6], "checks:bard": [3, 10] }
  }
}
```

- `xp_thresholds`: the total XP for levels 1 to 20, starting at `0` and never decreasing.
- `proficiency_bonus`: the bonus at levels 1 to 20, each `0..10`.
- `vanilla_xp.xp_per_point`, `advancements.xp`: character XP per point and per advancement, `0` or
  more.
- `ability_improvements.levels`: the levels that grant one, each `1..20`.
- `ability_improvements.class_levels`: more levels that grant one, per class id. Listing it replaces
  the whole default map, so a pack's own class needs its entry here; `{}` turns class improvements off.
- `ability_improvements.options`: the allowed splits, each a list of positive increases on
  different abilities.
- `ability_improvements.max_score`: `1..30`.
- `expertise.skills_per_level`: how many skills each Expertise level lets the player pick, `0` or more.
- `expertise.class_levels`: the levels each class gets Expertise at, per class id; replaces the default
  map when listed.

A list with the wrong length or a value out of range falls back to its default with a warning; a bad
improvement level, class id or option is skipped with a warning.

## Toggle

`levelling.enabled: false` turns levelling off. Players gain no XP and change no level; their
proficiency bonus is the flat one (profile or `proficiency.default_bonus`),
chosen improvements and Expertise stop applying, levels add no max health, the Hit Dice pool is 1,
the stat screen shows no level, and the level commands, improvement and Expertise choices are
refused. `ChecksApi.level` reads 1 and `ChecksApi.experience`
0. Stored levels, XP, improvements and Expertise are kept and apply again when it is turned back on.

`expertise.enabled: false` turns Expertise off alone: none is earned and chosen Expertise stops
applying.
