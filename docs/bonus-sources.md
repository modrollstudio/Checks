# Bonus sources

A bonus source gives a flat bonus, advantage or disadvantage to some checks or saves while its
condition holds: an item held or worn, or a mob effect. Boots of elvenkind, a lucky charm, a curse.
They apply to every living entity, players and mobs alike, and to every roll made through `ChecksApi`:
checks, saves, contests, checks against a passive score, `/checks roll`, `/checks check` and
[check triggers](check-triggers.md).

## Files

One source per file at `data/<namespace>/checks/bonus/<path>.json`, reloaded with `/reload`. A file
that fails to parse is logged and skipped. [`examples/bonus_source.json`](examples/bonus_source.json)
gives advantage on Stealth while leather boots are worn:

```json
{
  "format_version": 1,
  "source": { "item": "minecraft:leather_boots", "slot": "armor" },
  "applies_to": { "skills": ["stealth"] },
  "mode": "advantage"
}
```

| Key | Value |
| --- | --- |
| `format_version` | `1` |
| `source` | the condition: an `item` with a `slot`, or an `effect` (exactly one) |
| `applies_to` | which rolls it affects (at least one target) |
| `bonus` | a flat bonus added to the roll, may be negative; default `0` |
| `mode` | `advantage` or `disadvantage`; default none |

A source needs a non-zero `bonus`, a `mode`, or both.

### `source`

- `{ "item": "<id or #tag>", "slot": "mainhand" | "offhand" | "armor" }`: the item is in that slot;
  `armor` means any of head, chest, legs or feet. An item tag (`#minecraft:foot_armor`) matches any
  item in it.
- `{ "effect": "<mob effect id>" }`: the entity has that effect, e.g. `minecraft:invisibility`.

An unknown item or effect id rejects the file.

### `applies_to`

| Key | Covers |
| --- | --- |
| `skills` | checks with these skills (`stealth`, `mypack:lockpicking`) |
| `abilities` | checks with these abilities (`str`, `dex`, …), and, as in 5e, checks with every skill they govern |
| `saves` | saving throws with these abilities |
| `all_checks` | every ability and skill check |
| `all_saves` | every saving throw |

Contest sides count as checks. Saves are only covered by `saves` and `all_saves`. An unknown skill is
skipped with a warning. Bonus files load after skills, so they can target datapack skills.

## Stacking

Every source whose condition holds applies; flat bonuses add up. Advantage and disadvantage combine
the 5e way: any advantage together with any disadvantage rolls normally, however many of each. A
source counts once, even when several slots hold matching items.

## Passive scores

Passive scores count the bonus sources on that skill's checks the 5e way: flat bonuses add,
advantage adds 5 and disadvantage takes 5 away; having both changes nothing. So `ChecksApi.passiveScore`,
checks against a passive score (`checkAgainstPassive`), the stat screen and `/checks get` all
include them. A +2 Perception item raises passive Perception by 2; advantage on Perception raises it
by 5.

The 5 is `bonus_sources.passive_advantage` in `config/checks/scores.json` (default `5`, not
negative).

## What leaves them out

The plain modifier reads (`ChecksApi.abilityModifier`, `skillModifier`, `saveModifier`, `modifier`)
leave bonus sources out, since a roll adds them itself. Critfall's attack and damage modifiers, and
its spell-save modifier, never include them.

## Stat screen

The [stat screen](stat-screen.md) and `/checks get` add an entity's current flat bonuses to the skill
and save modifiers they show and count bonus sources in passive scores. The stat screen also lists
each source in the tooltip breakdown and names the sources granting advantage or disadvantage:

```
DEX +2, Elven Boots +1 = +3
Advantage from Elven Boots.
```

A source is named by the lang key `checks.bonus.<namespace>.<path>`; without one, the last path
segment in title case (`mypack:gear/elven_boots` → `Elven Boots`).

## Toggle

`bonus_sources.enabled: false` in `config/checks/scores.json` ignores every bonus source, rolls and
passive scores alike. See
[scores-config.md](scores-config.md#toggles).
