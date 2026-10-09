# Species traits

A species lists trait ids (see [presets.md](presets.md#species)). A trait with a file under
`data/<namespace>/checks/trait/<path>.json` does something; one without is flavor only: a name and a
description on the creation and stat screens. Traits apply to players with a confirmed character of
that species, while character creation and presets are on.

Packs compose species from traits, and traits from a fixed set of effect types. Darkvision is defined
once and listed by six species.

## Files

One trait per file, reloaded with `/reload`. A file that fails to parse is logged and skipped, which
leaves the trait flavor only. [`examples/trait.json`](examples/trait.json):

```json
{
  "format_version": 1,
  "effects": [
    { "type": "vanilla_save_advantage", "against": ["poison"] },
    { "type": "roll_bonus", "applies_to": { "skills": ["perception"] }, "bonus": 2 }
  ]
}
```

`effects` is a list; a trait may combine any number. An unknown type, or a missing or bad key, rejects
the file. Trait files load after skills, so they can name datapack skills. Name and describe a trait
in the lang file as `checks.trait.<namespace>.<path>` and `.desc`.

## Effect types

| `type` | Keys | Effect |
| --- | --- | --- |
| `vanilla_save_advantage` | `against`: [vanilla save](vanilla-saves.md) ids (`explosion`, `poison`, `knockback`, `fire`, `darkness`) | advantage on those saves |
| `roll_bonus` | `applies_to`, `bonus`, `mode`, as in a [bonus source](bonus-sources.md) | an always-on bonus source on checks and saves rolled through Checks; counted in passive scores and shown on the stat screen under the trait's name |
| `damage_resistance` | `damage_types`: damage type ids or `#tags`; `multiplier` (default `0.5`) | matching damage is multiplied; with several matching resistances, the lowest multiplier applies |
| `extra_health` | `per_level` | max health + `per_level` × character level, rounded down |
| `darkvision` | — | darkness looks like dim light, at `traits.darkvision.strength` (see below) |
| `reroll` | `on`: `natural_1` or `failure`; `daily` (default `false`) | the d20 of a check or save rolled through Checks is rolled again through Critfall and the new roll stands; `daily` allows it once per recharge |
| `last_stand` | — | once per recharge, damage that would kill leaves the player at 1 HP; the void and `/kill` still kill. It comes before a Totem of Undying, which is only used while the last stand is recharging or off |
| `ignored_by` | `entities`: entity type ids or `#tags` | those mobs never target the player, and drop them as a target |
| `skill_choices` | `count`; `from`: skills (optional) | `count` more skill picks at character creation, from `from` or from any skill |
| `attribute` | `attribute` id, `amount`, `operation` (`add_value`, `add_multiplied_base`, `add_multiplied_total`; default `add_value`) | a transient attribute modifier, never saved with the player |

Notes:

- **Rerolls** only cover checks and saves rolled through `ChecksApi` (`/checks roll`, triggers,
  vanilla saves, other mods' checks). Contests are never rerolled, since Critfall rolls both sides at
  once, and neither are Critfall's own attack rolls and spell saves. An unlimited reroll is used
  before a daily one; a roll is rerolled at most once. The player is told in chat.
- **Darkvision** is decided on the server and sent to the client, which only draws it: the lightmap
  brightens darkness as night vision does, at the configured strength, so dark places look dim, not
  lit, and torches still matter. Night vision, when stronger, wins.
- **Skill choices** of one species pool together: the counts add up and the lists join; a choice open
  to any skill opens all of them. The picks sit in their own group on the skills page, never repeat a
  background or class skill, and are validated by the server.
- **Once a day** means `traits.recharge_ticks` (default 24000, one Minecraft day) of game time after the
  last use. Uses are kept per player in the world save (`checks_trait_uses`).

## The shipped traits

| Species | Trait | Effect |
| --- | --- | --- |
| Dragonborn, Dwarf, Elf, Gnome, Orc, Tiefling | Darkvision | darkvision |
| Dragonborn | Damage Resistance | fire and lava damage halved |
| Dragonborn | Draconic Flight | fall damage halved |
| Dwarf | Dwarven Resilience | advantage on the CON save against poison, wither and hunger |
| Dwarf | Dwarven Toughness | +0.5 max health per level |
| Elf | Fey Ancestry | advantage on the WIS save against darkness |
| Elf | Keen Senses | one skill pick from Insight, Perception or Survival |
| Elf | Trance | phantoms ignore you |
| Gnome | Gnomish Cunning | advantage on INT, WIS and CHA saves |
| Gnome | Gnomish Lineage | bees, wolves, polar bears, llamas and pandas (`#checks:gnome_friends`) ignore you |
| Goliath | Giant Ancestry | +0.5 attack knockback |
| Goliath | Large Form | +0.5 block and entity reach |
| Goliath | Powerful Build | advantage on the STR save against big knockback |
| Halfling | Brave | advantage on the WIS save against darkness |
| Halfling | Luck | natural 1s rerolled |
| Halfling | Naturally Stealthy | advantage on Stealth checks |
| Human | Resourceful | one failed check or save rerolled a day |
| Human | Skillful, Versatile | one skill pick of any skill each |
| Orc | Adrenaline Rush | +5% movement speed |
| Orc | Relentless Endurance | once a day, a killing blow leaves you on 1 HP |
| Tiefling | Fiendish Legacy | fire and lava damage halved |
| Tiefling | Otherworldly Presence | piglins ignore you |

Flavor only: Draconic Ancestry, Breath Weapon, Stonecunning, Elven Lineage and Halfling Nimbleness.

## Config

Under `traits` in `config/checks/scores.json` (see [scores-config.md](scores-config.md)):

```json
"traits": {
  "enabled": true,
  "recharge_ticks": 24000,
  "vanilla_save_advantage": { "enabled": true },
  "roll_bonus": { "enabled": true },
  "damage_resistance": { "enabled": true },
  "extra_health": { "enabled": true },
  "darkvision": { "enabled": true, "strength": 0.4 },
  "reroll": { "enabled": true },
  "last_stand": { "enabled": true },
  "ignored_by": { "enabled": true },
  "skill_choices": { "enabled": true },
  "attribute": { "enabled": true }
}
```

| Key | Value |
| --- | --- |
| `enabled` | the master switch for every trait effect |
| `recharge_ticks` | game ticks before a once-a-day effect can be used again, not negative |
| `<type>.enabled` | the switch for that effect type alone |
| `darkvision.strength` | `0..1`: how bright darkness looks; `0` turns darkvision off |

## Toggles

`traits.enabled: false` turns every trait into flavor only; `<type>.enabled: false` does that for one
effect type. Either way the effect stops at once: damage, saves, targeting and rolls are vanilla again,
modifiers are removed on the next tick, the client's darkvision goes, and skills picked with
`skill_choices` stop being proficient (they stay stored and count again when switched back on).
Daily uses already spent stay spent.
