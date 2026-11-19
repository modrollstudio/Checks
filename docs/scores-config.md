# Scores config

`config/checks/scores.json` is created on first run and reloads on `/reload`. The defaults are in
[examples/scores.json](examples/scores.json); a key left out takes its default.

`format_version` is `2` since Checks 0.2.0. An older file is moved to it once, on load, with a line in
the log; the move turns `health.per_level.enabled` on, since `false` was only the old shipped default,
and changes nothing else (see [body.md](body.md#config)).

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

`levelling` sets up character levels: the XP table, the proficiency bonus per level, the XP sources,
ability score improvements (shared and per class), Expertise and Extra Attack. See
[levelling.md](levelling.md#config).

## Rests

`rest` sets up Hit Dice, short rests at a campfire and long rests from sleeping through the night, each
with its own `enabled` switch. See [rests.md](rests.md#config).

## Critfall

`critfall` controls the modifiers Checks supplies to Critfall's attack, damage and save rolls:
`enabled`, `unprofiled_mobs` (default `false`) and `save_abilities` (default `critfall:spell` to
`dex`, and each of the six `critfall:<ability>` keys to its ability). See [critfall.md](critfall.md).

## Body

`health`, `armor_class`, `size` and `attribute_extras` set up what scores do to a player's body:
max health from CON and levels, DEX in AC by armor weight, species size, and the STR,
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

## Player rolls

`player_rolls` lets players without op use `/checks roll` for their own character:
`/checks roll stealth` or `/checks roll stealth 15` (naming themselves works too). The result shows
to them and to every player within `radius` blocks (default 16, not negative), with their name.
Rolling for anyone else stays op only.
`enabled: false` makes `/checks roll` op only again.

## Death messages

`death_messages` tells a story instead of vanilla's death message when a player dies within
`window_ticks` (default 1200, a minute; not negative) of a related Checks event, such as being caught
pickpocketing before a golem gets them. See [death-messages.md](death-messages.md).

## Extensions

`events`, `bonus_sources`, `triggers`, `check_command`, `stat_screen_sections` and `level_up_choices` each hold one
`enabled` switch, all `true` by default, for what other mods and packs build on Checks. `bonus_sources` also holds
`passive_advantage` (default `5`, not negative): what advantage adds to a passive score and
disadvantage takes away (see [bonus-sources.md](bonus-sources.md#passive-scores)).

## Toggles

Every feature has its own `enabled` switch; the Toggles section on each feature's page says what turning
it off does. `derivation.enabled: false` stops derivation, so unprofiled abilities on non-player entities
all fall back to `default_mental_score`.

## Commands

Every command except `help`, `get` and a player's own `roll` requires op (permission level 2).

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
- `/checks roll <ability/skill> [dc]` rolls a check for the player who runs it, optionally against a
  DC, and players nearby see the result (see [Player rolls](#player-rolls)). Players without op can
  use it. `/checks roll <target> <ability/skill> [dc]` rolls for a target and prints the roll to the
  op who ran it (see [api.md](api.md#checks-roll)). The result is the total, for
  `execute store result`.
- `/checks check <target> <ability/skill> <dc>` rolls a check against a DC for datapacks: a success
  returns 1, a failure fails the command, so `execute store success` and `execute store result`
  record 1 or 0 (see [api.md](api.md#checks-check)).
- `/checks reset <player>` clears a player's character, rolls included, and their level, XP and
  ability score improvements, so character creation opens again on their next join (see
  [character-creation.md](character-creation.md)).
- `/checks level <player> set <1..20>` and `/checks xp <player> add <n>` set a player's level or add
  character XP (see [levelling.md](levelling.md#commands)).
