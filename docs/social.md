# Social skills

Players can talk to villagers, wandering traders and piglins, and stare down mobs. Press **G**
(rebindable, under *Critfall: Checks* in the controls) while looking at one within reach to open the
social menu, a panel under a banner with the target's spawn egg. It lists every action with its skill and modifier, e.g. `Persuade: Persuasion +3`; one
that can't be tried right now is greyed out with the reason below it (wrong target, no trades, or how
long until it can be tried again). A mob is offered only the two actions for mobs, Intimidate and
Calm; villagers, wandering traders and piglins only the others. The menu is titled *Talk to the Farmer* for
villagers, wandering traders and piglins, *Face the Zombie* for a hostile mob and *Calm the Wolf* for a
neutral one (`screen.checks.social.title.<talk|face|calm>`), naming the target as the flavor lines do. Looking at anything else shows
*There's no one here to talk to.*

Two social skills work without the menu: passive [Insight](#insight) senses danger, and
[Performance](#performance) rolls when a player plays a note block or blows a goat horn.

Every action is rolled through `ChecksApi`, so Critfall rolls the d20, the
[check events](api.md#check-events) fire and [bonus sources](bonus-sources.md) on the skill apply.
The server checks the target, its reach and the cooldown again before rolling. The result shows just
above the hotbar: the roll with its outcome, and below it one line picked from that outcome's pool of
flavor lines. It stays up for `roll_messages.duration_ticks` on a client with Checks (see
[scores-config.md](scores-config.md#roll-messages)); a client without Checks shows only the roll, in
the action bar:

```
Persuasion vs DC 12: d20 15 (+3) = 18, success
"For a friend? Of course." Prices drop.
```

The flavor line is cosmetic: it is picked with Minecraft's own randomness, never a Critfall roll, and
changes nothing. Each outcome has four to six lines in the lang file
(`checks.social.<action>.<outcome>.<n>`; `%1$s` is the target's name, `%2$s` what changed hands), so a
resource pack can rewrite them or add more. A wandering trader has persuade and deceive lines of his own,
`checks.social.<persuade|deceive>.wandering_trader.<outcome>.<n>`; where the lang file has none for an
outcome, he uses the generic ones. A target without a custom name is named by its type through
`checks.name.unnamed` ("the Farmer", "the Piglin"); one with a custom name by that name ("Bob"). Lines
never start a sentence with the target's name, so the lowercase "the" always fits.

A trader who refuses the player says so with a line picked from `checks.social.refuses.<n>`.

Social actions never work on players, and nothing on this page applies to or between players.

## Actions

| Action | Skill | Works on | DC |
| --- | --- | --- | --- |
| Persuade | Persuasion | villagers, wandering traders | `persuade.dc` (12) |
| Deceive | Deception | villagers, wandering traders, piglins | the target's passive Insight |
| Intimidate | Intimidation | villagers, piglins | `intimidate.dc` (13) |
| Pickpocket | Sleight of Hand | villagers | the target's passive Perception; advantage from behind |
| Plead | Persuasion | villagers on guard, in place of Persuade | `on_guard.plead.dc` (15) |
| Lie | Deception | villagers on guard, in place of Deceive | `on_guard.lie.dc` (15) |
| Intimidate (mobs) | Intimidation | hostile mobs | `intimidate_mob.dc` (12) |
| Calm | Persuasion | wolves, bees, endermen, zombified piglins, iron golems, the `calm.tag` mobs | `calm.dc` (13) |

Baby villagers and piglins are never targets. A villager or wandering trader with no trades (an
unemployed villager or a nitwit) has nothing to haggle over or take.

### Persuade and deceive a trader

A haggle. A natural 1 or 20 decides the outcome outright; otherwise a check below the DC fails, and a
check that makes it by at most `barely_margin` (2) only barely succeeds.

| Outcome | Default effect | Reaction |
| --- | --- | --- |
| natural 1 | prices +30% for two days; minor negative gossip to every villager within 32 blocks (a wandering trader tells no one) | head shake, "no", angry particles |
| failure | prices +15% for half a day | head shake, "no" |
| barely | prices −10% for half a day | happy particles, "yes" |
| success | prices −20% for half a day | happy particles, "yes" |
| natural 20 | prices −35% for a day; minor positive gossip to the target | happy particles, "yes" |

Deceive works the same against the target's passive Insight, with its own outcome table. A wandering
trader who catches the lie (failure or natural 1) also refuses the player for
`wandering_trader.refuse_ticks` (half a day), and the llamas leashed to him spit at the player
(`wandering_trader.llamas_spit`).

Persuade and Deceive each cool down for one Minecraft day per player per target by default.

### Deceive a piglin

A made check makes every piglin treat the player as if they wore gold for `piglin.disguise_ticks`
(2 minutes), `piglin.critical_disguise_ticks` (5 minutes) on a natural 20; the piglin admires the
player (happy particles and its admiring sound). A failed one turns that piglin hostile for
`piglin.anger_ticks`, with angry particles and a snarl.

### Intimidate

- **Villager, success:** it hands over one of its trade results (one item, picked with a die rolled
  through Critfall), runs from the player and sweats; fear spreads: minor negative gossip and its
  prices +15% for a day.
- **Villager, failure:** it shakes its head with angry particles, refuses to trade with the player for
  two days, strong negative gossip reaches every villager within 32 blocks, and the iron golems around
  it turn on the player and call for backup. Villagers who saw it are scared off (see
  [Witnesses](#witnesses)).
- **Piglin, success:** it backs off and throws the player one barter from the piglin bartering loot
  table.
- **Piglin, failure:** every piglin around it turns on the player for `piglin_anger_ticks`, snarling.

Intimidate has a long cooldown per target: three Minecraft days by default.

### Pickpocket

Rolled with advantage when the player stands within `behind_degrees` (60) of straight behind the
villager's gaze. A success quietly takes one of its trade results. A failure means the player is
caught: the villager shakes its head with angry particles and runs from the player, it refuses to
trade for two days, strong negative gossip reaches every villager within 32 blocks, nearby iron golems
turn on the player and call for backup, and villagers who saw it turn on the player too (see
[Witnesses](#witnesses)). Three Minecraft days of cooldown per target by default.

### Intimidate a hostile mob

Works on hostile mobs that walk with goals (zombies, skeletons, spiders, creepers, witches and the
like) with at most `intimidate_mob.max_health` (20) max health. Bosses (the `c:bosses` tag: the
ender dragon and the wither) never, whatever the limit, and nor do the mobs in
`intimidate_mob.exclude_tag` (`checks:intimidation_immune`: the warden and the elder guardian by
default). A mob too strong is greyed out: "Too tough to scare off." Slimes, ghasts and phantoms have
no such goals and are no targets.

| Outcome | Effect |
| --- | --- |
| Success (or barely, or natural 20) | The mob drops the player as its target and runs from them for `flee_ticks` (ten seconds), keeping `flee_distance` (16) blocks away at `flee_speed` (1.25) times its speed. Meanwhile it neither attacks nor closes in. Then it goes back to what it was doing. |
| Failure | Nothing. |
| Natural 1 | The mob turns on the player with Speed `speed_boost_amplifier` + 1 (Speed II) for `speed_boost_ticks` (five seconds). |

Each mob cools down for a minute per player (`cooldown_ticks`). The flavor lines are
`checks.social.intimidate_mob.<success|failure|critical_failure>`. A scared mob sweats, one that
rushes the player shows angry particles; mobs never speak. Hoglins and zoglins have too much health
by default; with a higher limit their own brain may override the running.

### Calm

Persuasion against `calm.dc` (13) on a wolf, bee, enderman, zombified piglin or iron golem that is
angry at the player, or after them, plus any mob in `calm.tag` (`checks:calmable`, empty by default).
One that isn't angry at the player is greyed out: "Isn't angry at you."

| Outcome | Effect |
| --- | --- |
| Success (or barely, or natural 20) | It stops being angry at the player, with happy particles. |
| Failure (or natural 1) | It stays angry, with angry particles. |

A calmed iron golem also sits out the rest of the current [golem alarm](#golems-call-for-backup): the
golems still after the player can't call it back in. Each mob cools down for a minute per player
(`cooldown_ticks`). The flavor lines are `checks.social.calm.<success|failure>`.

## Insight

Passive Insight, with no roll: the player's passive Insight score against a DC, checked every tick.

- **Sense hostility.** While it meets `insight.sense_hostility.dc` (12), every mob within
  `sense_hostility.range` (16) blocks that is after the player gets a small red **!** above its head.
- **Creeper warning.** While it meets `insight.creeper_warning.dc` (12), a creeper within
  `creeper_warning.range` (16) blocks that lights its fuse gets the marker at once, and the player hears
  a warning from where it is, once per fuse: `checks:insight.warning` (a low bell by default, subtitle
  "Something feels wrong"), which a resource pack can replace in `assets/checks/sounds.json` as the
  voices can.

The server decides which mobs to mark and tells the player's client only when that changes; the
client draws the markers with the speech bubbles' rules: only while the mob's head is in view, never
behind a wall, always whole and at full brightness. Players without Checks see and hear nothing.

## Performance

A player who plays a note block (hitting or tuning it) or blows a goat horn rolls Performance against
`performance.dc` (12), at most once per `performance.cooldown_ticks` (half a day) per player, kept in
memory, so a restart ends it. It only rolls with an audience: a villager or piglin within
`performance.range` (16) blocks. A note block played by redstone has no player behind it and rolls
nothing. Villagers on guard against the player (during a golem alarm) are no audience: they gain
nothing and lose nothing.

| Outcome | Default effect |
| --- | --- |
| Success (or barely) | 5 `minor_positive` gossip to every listening villager; if a piglin is listening, piglins take the player for a gold wearer for a minute, as the Deceive disguise does |
| Natural 20 | 10 `minor_positive` gossip; piglins charmed for three minutes |
| Failure | nothing |
| Natural 1 | 5 `minor_negative` gossip to every listening villager |

Every listener shows how it took it (happy particles for a made check, angry ones for a natural 1);
the nearest villager and the nearest piglin also voice their mood and say a line from
`checks.speech.<villager|piglin>.performance.<outcome>`. The roll's flavor line comes from
`checks.social.performance.<outcome>`. Switching Performance off lifts the piglin charm it gave.

## Prices, gossip and refusals

Price changes work like vanilla reputation: Checks adds them to each trade's price when the player
opens the trading screen and takes them back when they close it, so they only ever apply to that
player at that trader. They change the first cost of each trade by the percentage, rounded to the
nearest item, and never take a price below one item. A newer price change from the same action at
the same trader replaces the older one; changes from different actions add up.

Gossip is vanilla villager gossip about the player (`minor_negative`, `major_negative`,
`minor_positive`, `major_positive` or `trading`), so it changes villagers' own reputation prices and
fades the vanilla way. Gossip marked `nearby` is told at once to every villager within `gossip_radius`
of the target, so the whole village reacts; otherwise only the target hears it, and it spreads as
villagers talk. At strong enough negative gossip, a reputation of -100 or lower with a villager
within 10 blocks of them, vanilla's iron golems also go after the player on their own. A single caught
pickpocket or failed threat leaves -95 (19 `major_negative`, weighted ×5), just above that: golems turn
on the player for the crime only through Checks' own `angers_golems` and alarm, which a plea can end. A
second crime, or other negative gossip on top, crosses the line, and vanilla's golems join in until the
gossip fades.

What happens with a wandering trader stays between him and the player: his price changes and refusal,
and his own llamas, but no gossip (he keeps none, and tells no villager around him, even with
`nearby`) and no iron golems, even for an outcome set to `angers_golems`.

A trader that refuses the player shakes its head when they try to trade. Iron golems a player built
never turn on players, as in vanilla, and no golem attacks on Peaceful.

Cooldowns, price changes, refusals (witnesses' too), piglin disguises and Performance's piglin charm
are kept in the world save, in game time, so they survive restarts. Golem alarms, scared mobs and the
Performance cooldown are not: a restart ends them.

## Witnesses

A caught pickpocket or a failed threat on a villager has witnesses: every other villager within
`gossip_radius` of the target that can see the player or the target, with no block in the way. Each
witness:

- refuses to trade with the player for `witnesses.refuse_fraction` of the target's own refusal (half,
  so one day by default), with a line from the refusal pool when the player tries;
- runs `witnesses.flee_distance` blocks from the player, as a fleeing target does (see
  [Reactions](#reactions));
- shows angry particles at a thief, sweats at a bully, and says a line from the target's pool for that
  failure: `checks.speech.villager.caught` for a pickpocket, `checks.speech.villager.failure` for a
  threat (when speech bubbles are on).

Wandering traders are never witnesses, and a crime on one has none. Switching `witnesses` off lifts
the refusals witnesses gave and stops new ones; the target's own refusal stays.

## Golems call for backup

When an iron golem turns on a player over a Checks crime (any outcome set to `angers_golems`: a caught
pickpocket or a failed threat by default), the player is wanted for `golem_alarm.duration_ticks` (two
minutes) or until they die. While the alarm lasts, every iron golem within `golem_alarm.radius` blocks
(48) of a golem after the player turns on them too, checked every tick: a golem that joins later, or
walks into range of one, calls the golems around it in turn. Golems that may not attack the player
keep to vanilla's rules: those a player built never join, and none do on Peaceful. A golem the player
[calms](#calm) sits out the rest of the alarm.

## On guard and wary

While a golem alarm is on a player, every villager within `golem_alarm.radius` blocks of the crime is
on guard against them (`on_guard`):

- **Pickpocket** and **Intimidate** are greyed out: "Everyone's watching you."
- **Persuade** and **Deceive** give way to two ways to talk the alarm down, each tried once per alarm
  (a restarted alarm is the same alarm):
  - **Plead** (Persuasion) against `on_guard.plead.dc` (15);
  - **Lie** (Deception) against `on_guard.lie.dc` (15), a fixed DC rather than the villager's passive
    Insight.

| Outcome | Effect |
| --- | --- |
| Success (or barely) | The alarm ends: the golems it called stop going after the player, and villagers running from them around the crime calm down. Refusals and price changes stay. |
| Natural 20 | As a success, and the time left on every refusal the player's thefts and threats earned, the witnesses' too, is halved. |
| Failure | Nothing changes. |
| Natural 1 | The alarm starts over at full length, and the golems within `nearby_radius` of the villager are called again. |

Each has flavor lines (`checks.social.<plead|lie>.<outcome>`) and speech bubbles
(`checks.speech.villager.<plead|lie>.<outcome>`), four to six per outcome; a barely uses the success
pools.

A successful plea only stands down the golems the alarm called. If the player's reputation with a
villager near a golem is at -100 or lower, after a second crime for instance, vanilla's golems can still
go after them on their own (see [Prices, gossip and refusals](#prices-gossip-and-refusals)).

After the alarm, a villager is wary of the player while it still refuses them, or while its reputation
of them is at most `wary.max_reputation` (-95, what a single crime's gossip leaves): Pickpocket and Intimidate against
it roll with disadvantage, shown as "with disadvantage (wary)" in the menu. Advantage from behind and
wariness cancel out.

## Reactions

Targets show what they think with vanilla's own particles, sounds and head shake. A villager that
runs panics the way vanilla villagers do when hurt: it heads for a spot `flee_distance` blocks further
away from the player at `flee_speed` times its walking speed and keeps away from them until it is
clear or `flee_ticks` (five seconds) pass, then goes back to its day. Reactions are cosmetic, except
that llama spit hurts a little, as in vanilla.

## Feel

Cosmetics that make each action feel alive, under `social.feel`, each with its own switch. None of
them changes an outcome; any line or item they pick is picked with Minecraft's own randomness, never
a Critfall roll. Players with Checks within `feel.radius` blocks of the target see the bubbles and
the flying item; players without Checks see nothing extra.

- **Pickpocket animation.** The thief's arm swings and the lifted item arcs from the villager to
  them with a soft pop. On a pickpocket that only barely went unnoticed, the villager turns its head
  to the thief, then looks around. A caught one drops the item it reached for halfway, and the
  villager snatches it back with a sound. (The villager model has no arm swing, so the snatch shows as
  the item jumping back to it.)
- Mobs, hostile or neutral, have no voice and no speech bubbles.
- **Speech bubbles.** A short line floats above the target's head for `speech_bubbles.duration_ticks`
  (default 200, ten seconds), fading in over its first fifth of a second and out over its last
  third. It is dark text on a soft light background with rounded corners and a small tail pointing
  down at the speaker, wrapped at a fixed width of about three blocks, just above the head. Closer than four blocks it
  shrinks, so it never looks bigger than it does from four blocks away. It shows only while you can see the target's head:
  a wall or other solid block between you and the head hides it (glass does not), and a visible bubble is
  drawn whole and at full brightness, never cut off or dimmed by what is in front of it. Lines come from a pool of four to seven per target
  kind and outcome, `checks.speech.<villager|wandering_trader|piglin>.<outcome>.<n>` in the lang file, plus
  `checks.speech.villager.caught` for a caught pickpocket; a theft that goes unnoticed says nothing.
  `%1$s` in a line is the player.
- **Voices.** The target voices its mood after the action: `checks:<target>.<mood>`, with target
  `villager`, `wandering_trader` or `piglin` and mood `angry`, `happy`, `scared` or `suspicious`. By
  default they are vanilla villager, wandering trader and piglin sounds at a different pitch and
  timing (a snap of anger, a slow suspicious beat), with subtitles. While voices are on, players with
  Checks hear the voice in place of the vanilla sound the reaction would play; players without Checks
  hear only that vanilla sound, as with voices off. A resource pack can replace each one with real voice
  lines by overriding its entry in `assets/checks/sounds.json`, without code changes:

```json
{
  "villager.angry": {
    "replace": true,
    "subtitle": "subtitles.checks.villager.angry",
    "sounds": ["mypack:voice/villager_angry_1", "mypack:voice/villager_angry_2"]
  }
}
```

  The voices are sound events no registry holds, so a client without Checks can still join; it is
  never sent one.

## Passive prices

No roll: a villager's prices shift by `charisma_percent_per_point` (2%) per point of the player's CHA
modifier, down for a positive modifier and up for a negative one. Some professions also shift by a
skill's modifier: by default Religion for clerics, History for librarians and cartographers, each 2%
per point. Wandering traders have no profession and no passive shift. A pack can list any profession,
modded ones too, with any skill.

## Stat screen

Hovering a skill the social menu, passive prices, Insight or Performance use (Persuasion, Deception,
Intimidation, Sleight of Hand, Religion, History, Insight and Performance) ends its tooltip with an
*In this mod:* line saying what it does.

## Config

In `config/checks/scores.json`, under `social` (see [scores-config.md](scores-config.md)); the full
default is in [examples/scores.json](examples/scores.json):

```json
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
}
```

| Key | Value |
| --- | --- |
| `enabled` | the master switch for everything on this page |
| `nearby_radius` | how far around the target, in blocks, iron golems, piglins and leashed llamas join in; not negative |
| `gossip_radius` | how far around the target, in blocks, gossip marked `nearby` reaches and witnesses can be; not negative |
| `<action>.enabled` | the switch for that action alone |
| `<action>.cooldown_ticks` | ticks before a player may try that action on the same target again; not negative |
| `persuade.dc`, `intimidate.dc` | the DC; deceive and pickpocket use the target's passive skill instead |
| `persuade.barely_margin`, `deceive.barely_margin` | how far above the DC still only barely succeeds; not negative |
| `deceive.piglin.*` | disguise lengths and the deceived piglin's anger, in ticks; not negative |
| `deceive.wandering_trader.refuse_ticks` | how long a wandering trader who caught the lie refuses the player; not negative |
| `deceive.wandering_trader.llamas_spit` | whether his llamas spit at the player |
| `intimidate.piglin_anger_ticks` | how long piglins stay angry after a failed threat; not negative |
| `pickpocket.behind_degrees` | how far from straight behind still counts as behind, `0..180` |
| `passive_prices.charisma_percent_per_point` | price change per point of CHA modifier at every villager |
| `passive_prices.professions` | profession id → the `skill` whose modifier shifts that profession's prices and its `percent_per_point`; listing it replaces the defaults |
| `reactions.enabled` | the switch for particles, sounds, head shakes, fleeing and llama spit |
| `reactions.flee_distance` | how far a target runs from the player, in blocks; not negative |
| `reactions.flee_ticks` | how long a fleeing villager keeps away from the player at most; not negative |
| `reactions.flee_speed` | running speed, times the villager's walking speed, for targets and witnesses; not negative |
| `witnesses.enabled` | the switch for witnesses: their refusals, fleeing, particles and lines |
| `witnesses.refuse_fraction` | a witness's refusal, as a share of the target's `refuse_ticks`; not negative |
| `witnesses.flee_distance` | how far a witness runs from the player, in blocks; not negative |
| `golem_alarm.enabled` | the switch for golems calling for backup |
| `golem_alarm.radius` | how far from a golem after the player, in blocks, other golems join in; not negative |
| `golem_alarm.duration_ticks` | how long the alarm lasts; `0` for none; not negative |
| `on_guard.enabled` | the switch for villagers on guard during an alarm; off keeps the usual menu |
| `on_guard.plead.enabled`, `on_guard.lie.enabled` | the switch for each; while on guard, Persuade and Deceive stay replaced either way |
| `on_guard.plead.dc`, `on_guard.lie.dc` | the DC to talk the alarm down |
| `wary.enabled` | the switch for disadvantage against wary villagers |
| `wary.max_reputation` | a villager whose vanilla reputation of the player is at most this is wary |
| `feel.radius` | how far around the target, in blocks, players see speech bubbles and the flying item; not negative |
| `feel.pickpocket_animation.enabled` | the switch for the arm swing, the flying item and the villager's look around |
| `feel.speech_bubbles.enabled` | the switch for speech bubbles |
| `feel.speech_bubbles.duration_ticks` | how long a bubble stays up; at least 1 |
| `feel.voices.enabled` | the switch for mood voices; off plays the reactions' vanilla sounds again |
| `intimidate_mob.enabled` | the switch for Intimidate on hostile mobs; off also stops every mob running |
| `intimidate_mob.dc`, `calm.dc` | the DC |
| `intimidate_mob.cooldown_ticks`, `calm.cooldown_ticks` | ticks before a player may try it on the same mob again; not negative |
| `intimidate_mob.max_health` | the most max health a mob may have and still be scared; not negative |
| `intimidate_mob.exclude_tag` | an entity type tag of mobs that are never scared, without `#` |
| `intimidate_mob.flee_ticks` | how long a scared mob runs; not negative |
| `intimidate_mob.flee_distance` | how far from the player it keeps, in blocks; not negative |
| `intimidate_mob.flee_speed` | its running speed, times its own; not negative |
| `intimidate_mob.speed_boost_ticks`, `intimidate_mob.speed_boost_amplifier` | the Speed a natural 1 gives the mob: its length and amplifier (0 is Speed I); not negative |
| `calm.enabled` | the switch for Calm |
| `calm.tag` | an entity type tag of extra mobs Calm works on, without `#` |
| `insight.sense_hostility.enabled`, `insight.creeper_warning.enabled` | the switch for each sense |
| `insight.<sense>.dc` | the passive Insight the player needs |
| `insight.<sense>.range` | how far around the player, in blocks, it reaches; not negative |
| `performance.enabled` | the switch for Performance; off also lifts its piglin charm |
| `performance.dc` | the DC |
| `performance.cooldown_ticks` | ticks before a player's next Performance rolls; not negative |
| `performance.range` | how far around the player, in blocks, villagers and piglins hear it; not negative |
| `performance.<success\|critical_success\|critical_failure>.gossip` | `type` and `amount` of gossip told to every listening villager; `amount` 0 for none |
| `performance.<outcome>.piglin_ticks` | how long piglins take the player for a gold wearer when one listened; `0` for none; not negative |

Each outcome (`critical_failure`, `failure`, `barely`, `success`, `critical_success` for persuade and
deceive; `success` and `failure` for intimidate; `failure` for pickpocket) is an effect on the
trader. A missing key keeps its default:

| Effect key | Value |
| --- | --- |
| `price_percent` | the player's price change at this trader, negative for a discount |
| `price_ticks` | how long the price change lasts; `0` for none |
| `gossip.type` | the vanilla gossip type spread about the player |
| `gossip.amount` | how much of it; `0` for none |
| `gossip.nearby` | `true` tells every villager within `gossip_radius` of a villager target at once; `false` only the target. Wandering traders never pass gossip on |
| `refuse_ticks` | how long the trader refuses to trade with the player; `0` for never |
| `angers_golems` | whether iron golems within `nearby_radius` of a villager target turn on the player, raising the golem alarm |

Durations are in game ticks: 1200 is a minute, 24000 a Minecraft day.

## Toggles

- `social.enabled: false` turns everything here off: the key opens nothing, and earlier price
  changes, refusals and disguises stop applying.
- `<action>.enabled: false` turns one action off alone: it leaves the menu, and the price changes,
  refusals and (for deceive) piglin disguises it left behind stop applying.
- `passive_prices.enabled: false` turns the CHA and profession price shifts off.
- `reactions.enabled: false` turns every reaction off; outcomes, gossip and refusals still happen.
- `feel.pickpocket_animation.enabled`, `feel.speech_bubbles.enabled` and `feel.voices.enabled` each
  turn one cosmetic off alone; with voices off the reactions play their vanilla sounds.
- `gossip.nearby: false` on an outcome keeps its gossip with the target.
- `intimidate_mob.enabled`, `calm.enabled`, `insight.sense_hostility.enabled`,
  `insight.creeper_warning.enabled` and `performance.enabled` each turn one off alone: the mob actions
  leave the menu (a running mob stops), the markers clear and the warning stops, and Performance stops
  rolling and lifts its piglin charm.

Turning a switch back on brings back whatever has not run out yet.
