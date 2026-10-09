# Scores config

`config/checks/scores.json` is created on first run and reloads on `/reload`. These are the defaults
([examples/scores.json](examples/scores.json)); a key left out takes its default.

```json
{
  "format_version": 1,
  "player_defaults": { "str": 10, "dex": 10, "con": 10, "int": 10, "wis": 10, "cha": 10 },
  "derivation": {
    "enabled": true,
    "strength_base": 8,
    "strength_per_attack_damage": 1.0,
    "dexterity_base": 6,
    "dexterity_per_speed": 24.0,
    "constitution_base": 8,
    "constitution_per_health": 0.2,
    "default_mental_score": 10
  },
  "profiles": { "enabled": true },
  "skills": { "enabled": true },
  "proficiency": { "enabled": true, "default_bonus": 2 },
  "critfall": {
    "enabled": true,
    "unprofiled_mobs": false,
    "save_abilities": { "critfall:spell": "dex" }
  },
  "stat_screen": { "enabled": true },
  "creation": {
    "enabled": true,
    "methods": ["standard_array", "point_buy", "roll", "hardcore"],
    "skill_choices": 2,
    "standard_array": [15, 14, 13, 12, 10, 8],
    "point_buy": {
      "budget": 27,
      "costs": { "8": 0, "9": 1, "10": 2, "11": 3, "12": 4, "13": 5, "14": 7, "15": 9 }
    },
    "roll_dice": "4d6kh3",
    "hardcore_dice": "3d6",
    "presets": {
      "enabled": true,
      "include_shipped": true,
      "bonus_options": [[2, 1], [1, 1, 1]],
      "bonus_max_score": 20
    }
  },
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
      "options": [[2], [1, 1]],
      "max_score": 20
    }
  },
  "events": { "enabled": true },
  "bonus_sources": { "enabled": true, "passive_advantage": 5 },
  "triggers": { "enabled": true },
  "check_command": { "enabled": true },
  "stat_screen_sections": { "enabled": true },
  "health": {
    "enabled": true,
    "per_con_point": 2,
    "per_level": { "enabled": false, "multiplier": 0.2 }
  },
  "armor_class": { "enabled": true, "medium_max_dex": 2 },
  "size": { "enabled": true },
  "attribute_extras": {
    "knockback": { "enabled": true, "per_point": 0.1 },
    "mining_speed": { "enabled": true, "per_point": 0.05 },
    "bow_draw": { "enabled": true, "per_point": 0.1 },
    "crossbow_reload": { "enabled": true, "per_point": 0.1 },
    "breath": { "enabled": true, "per_point": 0.2 },
    "exhaustion": { "enabled": true, "per_point": 0.05 }
  },
  "vanilla_saves": {
    "profiled_mobs": false,
    "cooldown_ticks": 20,
    "explosion": { "enabled": true, "dc": 13, "success_multiplier": 0.5 },
    "poison": { "enabled": true, "dc": 12, "success_multiplier": 0.5 },
    "knockback": { "enabled": true, "dc": 13, "success_multiplier": 0.5, "min_strength": 0.7 },
    "fire": { "enabled": true, "dc": 12, "success_multiplier": 0.0 },
    "darkness": { "enabled": true, "dc": 13, "success_multiplier": 0.5 }
  },
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
  },
  "social": {
    "enabled": true,
    "nearby_radius": 16,
    "gossip_radius": 32,
    "persuade": {
      "enabled": true,
      "dc": 12,
      "cooldown_ticks": 24000,
      "barely_margin": 2,
      "critical_failure": {
        "price_percent": 30,
        "price_ticks": 48000,
        "gossip": { "type": "minor_negative", "amount": 25, "nearby": true }
      },
      "failure": { "price_percent": 15, "price_ticks": 12000 },
      "barely": { "price_percent": -10, "price_ticks": 12000 },
      "success": { "price_percent": -20, "price_ticks": 12000 },
      "critical_success": {
        "price_percent": -35,
        "price_ticks": 24000,
        "gossip": { "type": "minor_positive", "amount": 10 }
      }
    },
    "deceive": {
      "enabled": true,
      "cooldown_ticks": 24000,
      "barely_margin": 2,
      "critical_failure": {
        "price_percent": 30,
        "price_ticks": 48000,
        "gossip": { "type": "minor_negative", "amount": 25, "nearby": true }
      },
      "failure": { "price_percent": 15, "price_ticks": 12000 },
      "barely": { "price_percent": -10, "price_ticks": 12000 },
      "success": { "price_percent": -20, "price_ticks": 12000 },
      "critical_success": {
        "price_percent": -35,
        "price_ticks": 24000,
        "gossip": { "type": "minor_positive", "amount": 10 }
      },
      "piglin": { "disguise_ticks": 2400, "critical_disguise_ticks": 6000, "anger_ticks": 600 },
      "wandering_trader": { "refuse_ticks": 12000, "llamas_spit": true }
    },
    "intimidate": {
      "enabled": true,
      "dc": 13,
      "cooldown_ticks": 72000,
      "success": {
        "price_percent": 15,
        "price_ticks": 24000,
        "gossip": { "type": "minor_negative", "amount": 20 }
      },
      "failure": {
        "refuse_ticks": 48000,
        "angers_golems": true,
        "gossip": { "type": "major_negative", "amount": 19, "nearby": true }
      },
      "piglin_anger_ticks": 600
    },
    "pickpocket": {
      "enabled": true,
      "cooldown_ticks": 72000,
      "behind_degrees": 60,
      "failure": {
        "refuse_ticks": 48000,
        "angers_golems": true,
        "gossip": { "type": "major_negative", "amount": 19, "nearby": true }
      }
    },
    "passive_prices": {
      "enabled": true,
      "charisma_percent_per_point": 2,
      "professions": {
        "minecraft:cleric": { "skill": "checks:religion", "percent_per_point": 2 },
        "minecraft:librarian": { "skill": "checks:history", "percent_per_point": 2 },
        "minecraft:cartographer": { "skill": "checks:history", "percent_per_point": 2 }
      }
    },
    "reactions": {
      "enabled": true,
      "flee_distance": 12,
      "flee_ticks": 100,
      "flee_speed": 0.75
    },
    "witnesses": { "enabled": true, "refuse_fraction": 0.5, "flee_distance": 6 },
    "golem_alarm": { "enabled": true, "radius": 48, "duration_ticks": 2400 },
    "on_guard": {
      "enabled": true,
      "plead": { "enabled": true, "dc": 15 },
      "lie": { "enabled": true, "dc": 15 }
    },
    "wary": { "enabled": true, "max_reputation": -95 },
    "feel": {
      "radius": 32,
      "pickpocket_animation": { "enabled": true },
      "speech_bubbles": { "enabled": true, "duration_ticks": 200 },
      "voices": { "enabled": true }
    },
    "intimidate_mob": {
      "enabled": true,
      "dc": 12,
      "cooldown_ticks": 1200,
      "max_health": 20,
      "exclude_tag": "checks:intimidation_immune",
      "flee_ticks": 200,
      "flee_distance": 16,
      "flee_speed": 1.25,
      "speed_boost_ticks": 100,
      "speed_boost_amplifier": 1
    },
    "calm": { "enabled": true, "dc": 13, "cooldown_ticks": 1200, "tag": "checks:calmable" },
    "insight": {
      "sense_hostility": { "enabled": true, "dc": 12, "range": 16 },
      "creeper_warning": { "enabled": true, "dc": 12, "range": 16 }
    },
    "performance": {
      "enabled": true,
      "dc": 12,
      "cooldown_ticks": 12000,
      "range": 16,
      "success": { "gossip": { "type": "minor_positive", "amount": 5 }, "piglin_ticks": 1200 },
      "critical_success": { "gossip": { "type": "minor_positive", "amount": 10 }, "piglin_ticks": 3600 },
      "critical_failure": { "gossip": { "type": "minor_negative", "amount": 5 } }
    }
  },
  "roll_messages": { "enabled": true, "duration_ticks": 240 },
  "death_messages": { "enabled": true, "window_ticks": 1200 },
  "exploration": {
    "leap": { "enabled": true, "dc": 12, "cooldown_ticks": 40, "boost": 0.15, "min_drop": 3 },
    "landing": { "enabled": true, "base_dc": 10, "dc_per_damage": 0.5, "success_multiplier": 0.5 },
    "cobwebs": { "enabled": true, "dc": 12, "retry_ticks": 60 },
    "climbing": { "enabled": true, "per_point": 0.1, "max_bonus": 0.5 },
    "sneak": { "enabled": true, "per_point": 0.05, "min_visibility": 0.25 },
    "spot_tripwires": { "enabled": true, "dc": 13, "range": 8, "interval_ticks": 20 },
    "disarm_tripwires": { "enabled": true, "dc": 13 },
    "search_chests": { "enabled": true, "dc": 13 },
    "monster_lore": { "enabled": true, "dc": 12, "range": 16, "retry_ticks": 6000, "interval_ticks": 20 },
    "structure_lore": { "enabled": true, "dc": 12, "retry_ticks": 6000, "interval_ticks": 20 },
    "taming": { "enabled": true, "dc": 12 },
    "hunger": { "enabled": true, "per_point": 0.05, "max_reduction": 0.25 },
    "ailments": { "enabled": true, "per_point": 0.05, "max_reduction": 0.25 }
  }
}
```

A wrong-typed value falls back to its default with a warning. A score outside `1..30` or a
`proficiency.default_bonus` outside `0..10` is clamped with a warning. A file that is not valid JSON
is logged as an error and the defaults are used. A bad config never crashes the server.

## Scores and modifiers

Every entity has the six abilities STR, DEX, CON, INT, WIS and CHA. Scores are clamped to `1..30`.
The modifier is `floor((score - 10) / 2)`, so `1 → -5`, `10 → 0`, `11 → 0`, `30 → +10`.

## Player scores

A player's effective score, highest first:

1. a value set with `/checks set` (persisted per player in the world save)
2. the player's confirmed character (see [character-creation.md](character-creation.md))
3. a datapack profile matching `minecraft:player`
4. `player_defaults`

Ability score improvements chosen while levelling add on top of 2–4 (see
[levelling.md](levelling.md#ability-score-improvements)).

## Proficiency

`proficiency.default_bonus` is the proficiency bonus of every entity with no profile or command
value for it (`0..10`, default `2`). While levelling is on, players use their level's bonus instead
of a profile or this default (see [levelling.md](levelling.md#proficiency-bonus)). See
[proficiency.md](proficiency.md).

## Derivation

An unprofiled entity's scores are derived from vanilla attributes, rounded and clamped to `1..30`:

- `str = strength_base + strength_per_attack_damage * attack_damage`
- `dex = dexterity_base + dexterity_per_speed * movement_speed`
- `con = constitution_base + constitution_per_health * max_health`
- `int`, `wis`, `cha = default_mental_score`

Attack damage stands in for physical power, movement speed for agility and max health for toughness,
the same attributes Critfall's derivation reads. A missing attribute counts as `0`. Minecraft has no
mental attributes, so INT, WIS and CHA take the flat `default_mental_score` instead of an invented
proxy.

With the defaults, a zombie (attack 3, speed 0.23, health 20) derives STR 11, DEX 12, CON 12.

## Character creation

`creation` sets up the first-join character creation screen: which methods are offered, the
standard array, point-buy budget and costs, the dice for the rolling methods and how many skills
players pick. See [character-creation.md](character-creation.md). `creation.presets` sets up
species, backgrounds and classes; see [presets.md](presets.md#config).

## Levelling

`levelling` sets up character levels: the XP table, the proficiency bonus per level, the XP sources
and ability score improvements. See [levelling.md](levelling.md#config).

## Critfall

`critfall` controls the modifiers Checks supplies to Critfall's attack, damage and save rolls:
`enabled`, `unprofiled_mobs` (default `false`) and `save_abilities` (default
`{ "critfall:spell": "dex" }`). See [critfall.md](critfall.md).

## Body

`health`, `armor_class`, `size` and `attribute_extras` set up what scores do to a player's body:
max health from CON (and optionally levels), DEX in AC by armor weight, species size, and the STR,
DEX and CON extras. Each part and each extra has its own `enabled` switch. See [body.md](body.md#config).

## Vanilla saves

`vanilla_saves` sets up the saving throws against vanilla hazards: DEX against explosions and
catching fire, CON against poison, wither and hunger, STR against big knockback and WIS against
darkness, each with its own `enabled` switch, `dc` and `success_multiplier`, plus who saves and the
cooldown. See [vanilla-saves.md](vanilla-saves.md#config).

## Traits

`traits` switches species trait effects: a master `enabled`, one `enabled` per effect type, the
`recharge_ticks` of once-a-day effects (default 24000) and `darkvision.strength` (`0..1`, default
`0.4`, `0` for none). See [traits.md](traits.md#config).

## Social skills

`social` sets up the social menu (persuade, deceive, intimidate and pickpocket on villagers,
wandering traders and piglins; intimidate and calm on mobs), the passive price shifts from CHA,
Religion and History, passive Insight and Performance: a master `enabled`, `nearby_radius`, then one
object per action with its own `enabled`, DC or passive DC, cooldown and outcome effects,
`passive_prices`, `reactions`, the cosmetic `feel` (pickpocket animation, speech bubbles, voices),
`intimidate_mob`, `calm`, `insight` and `performance`. See [social.md](social.md#config).

## Exploration skills

`exploration` sets up the exploration skill uses: the Athletics leap, cobwebs and climbing, the
Acrobatics landing, sneaking past mobs on passive Stealth, spotting tripwires on passive Perception,
disarming them with Sleight of Hand, searching looted chests with Investigation, monster lore (Nature,
Arcana, Religion), structure lore (History), taming with Animal Handling, and the passive Survival
(hunger) and Medicine (ailments), each with its own `enabled` switch. See
[exploration.md](exploration.md#config).

## Roll messages

`roll_messages` shows Checks' roll results (social actions, [exploration skills](exploration.md),
[vanilla saves](vanilla-saves.md) and [check triggers](check-triggers.md)) on lines of their own, centered just above the hotbar where the
action bar sits, and longer than vanilla's three seconds. On a client with Checks the roll is the
first line and what goes with it (a social action's flavor line) the second; a line wider than the
screen wraps onto another instead of being cut off, and while chat lines are showing under them the
lines move up above the chat instead of covering it. They stay up for `duration_ticks` (default 240,
twelve seconds; at least 1), fading over the last second as vanilla's action bar does. Any newer action
bar message, from vanilla, Critfall, another mod or Checks, hides them at once, never the other way
round. A client without Checks gets only the roll, as vanilla's action bar message for vanilla's three
seconds, so it fits on one line. `enabled: false` sends every client that roll-only action bar
message.

## Death messages

`death_messages` tells a story instead of vanilla's death message when a player dies within
`window_ticks` (default 1200, a minute; not negative) of a related Checks event, such as being caught
pickpocketing before a golem gets them. See [death-messages.md](death-messages.md).

## Extensions

`events`, `bonus_sources`, `triggers`, `check_command` and `stat_screen_sections` each hold one
`enabled` switch, all `true` by default, for what other mods and packs build on Checks. `bonus_sources` also holds
`passive_advantage` (default `5`, not negative): what advantage adds to a passive score and
disadvantage takes away (see [bonus-sources.md](bonus-sources.md#passive-scores)).

## Toggles

- `derivation.enabled: false` stops derivation. Unprofiled abilities on non-player entities all
  fall back to `default_mental_score`.
- `profiles.enabled: false` ignores datapack profiles entirely. Entities resolve by derivation, and
  players by `/checks set` and `player_defaults`. Profile skill bonuses are ignored too.
- `skills.enabled: false` treats every flat skill bonus as `0`. See [skills.md](skills.md).
- `proficiency.enabled: false` makes every proficiency bonus `0` and every skill and save
  proficiency `none`. See [proficiency.md](proficiency.md#toggle).
- `critfall.enabled: false` frees Critfall's modifier provider slot, so Critfall rolls exactly as
  without Checks. See [critfall.md](critfall.md#toggle).
- `stat_screen.enabled: false` stops the server from answering the stat screen key, so no sheet
  opens. See [stat-screen.md](stat-screen.md#toggle).
- `creation.enabled: false` stops character creation and ignores confirmed characters until it is
  back on. See [character-creation.md](character-creation.md#toggle).
- `creation.presets.enabled: false` offers no species, backgrounds or classes and ignores what
  confirmed characters' presets granted. See [presets.md](presets.md#toggle).
- `levelling.enabled: false` stops XP and level changes, gives players the flat proficiency bonus
  and ignores chosen ability score improvements, keeping stored levels. See
  [levelling.md](levelling.md#toggle).
- `events.enabled: false` stops calling check event listeners, so other mods and scripts can no
  longer change or cancel rolls. See [api.md](api.md#check-events).
- `bonus_sources.enabled: false` ignores every datapack bonus source. See
  [bonus-sources.md](bonus-sources.md).
- `triggers.enabled: false` stops every datapack check trigger. See
  [check-triggers.md](check-triggers.md).
- `check_command.enabled: false` hides `/checks check`.
- `stat_screen_sections.enabled: false` hides the stat screen sections other mods add. See
  [api.md](api.md#stat-screen-sections).
- `health.enabled`, `armor_class.enabled`, `size.enabled` and each `attribute_extras.<extra>.enabled`
  turn off that part of the body alone; its attribute modifier is removed on the next tick. See
  [body.md](body.md#toggles).
- `traits.enabled: false` makes every species trait flavor only; each `traits.<type>.enabled` does that
  for one effect type. See [traits.md](traits.md#toggles).
- Each `vanilla_saves.<save>.enabled` turns off that save alone, so the hazard behaves as in
  vanilla. See [vanilla-saves.md](vanilla-saves.md#toggles).
- `social.enabled: false` turns every social skill use off; each `social.<action>.enabled` turns off one
  action and what it left behind, and `social.passive_prices.enabled` the passive price shifts. See
  [social.md](social.md#toggles).
- `social.feel.pickpocket_animation.enabled`, `social.feel.speech_bubbles.enabled` and
  `social.feel.voices.enabled` each turn one social cosmetic off. See [social.md](social.md#feel).
- `social.intimidate_mob.enabled`, `social.calm.enabled`, `social.insight.sense_hostility.enabled`,
  `social.insight.creeper_warning.enabled` and `social.performance.enabled` each turn one off alone. See
  [social.md](social.md#toggles).
- `exploration.leap.enabled`, `exploration.landing.enabled`, `exploration.cobwebs.enabled`,
  `exploration.climbing.enabled`, `exploration.sneak.enabled`, `exploration.spot_tripwires.enabled`,
  `exploration.disarm_tripwires.enabled`, `exploration.search_chests.enabled`,
  `exploration.monster_lore.enabled`, `exploration.structure_lore.enabled`, `exploration.taming.enabled`,
  `exploration.hunger.enabled` and `exploration.ailments.enabled` each turn one exploration skill use off alone, leaving that moment as in vanilla. See
  [exploration.md](exploration.md#toggles).
- `death_messages.enabled: false` gives every death vanilla's message. See
  [death-messages.md](death-messages.md).
- `roll_messages.enabled: false` shows only the roll, in the action bar for vanilla's three seconds,
  on every client.

## Commands

Every command except `help` and `get` requires op (permission level 2).

- `/checks help [command]` lists every `/checks` command you have permission for, each with its
  syntax and a one-line description below it; clicking one puts that command in the chat box.
  Naming a command shows a longer description and an example (`/checks help roll`).

- `/checks get <target> [ability/skill]` shows an entity's effective scores and modifiers, its
  proficiency bonus and save modifiers, plus its skills, with the same values as the stat screen:
  skill and save modifiers and passive scores include [bonus sources](bonus-sources.md) (see [skills.md](skills.md#commands) and
  [proficiency.md](proficiency.md#commands)).
- `/checks set <player> <ability/skill/proficiency> <value>` sets and persists a player's ability
  score (`1..30`), skill bonus (`-30..30`) or proficiency bonus (`0..10`).
- `/checks prof <player> <skill/ability> <none/proficient/expertise>` sets and persists a player's
  skill or save proficiency (see [proficiency.md](proficiency.md#commands)).
- `/checks roll <target> <ability/skill> [dc]` rolls a check, optionally against a DC, and prints
  the roll (see [api.md](api.md#checks-roll)). Its result is the total, for `execute store result`.
- `/checks check <target> <ability/skill> <dc>` rolls a check against a DC for datapacks: a success
  returns 1, a failure fails the command, so `execute store success` and `execute store result`
  record 1 or 0 (see [api.md](api.md#checks-check)).
- `/checks reset <player>` clears a player's character, rolls included, and their level, XP and
  ability score improvements, so character creation opens again on their next join (see
  [character-creation.md](character-creation.md)).
- `/checks level <player> set <1..20>` and `/checks xp <player> add <n>` set a player's level or add
  character XP (see [levelling.md](levelling.md#commands)).
