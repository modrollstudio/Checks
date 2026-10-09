# Vanilla saves

Vanilla hazards call for a saving throw. Every save is rolled through `ChecksApi.savingThrow`, so
Critfall rolls the d20, the [check events](api.md#check-events) fire, and
[bonus sources](bonus-sources.md) on that save (a flat bonus, advantage or disadvantage) apply.
The result shows above the hotbar, for `roll_messages.duration_ticks` on a client with Checks (twelve seconds; see
[scores-config.md](scores-config.md#roll-messages)), like a [check trigger](check-triggers.md):

```
Dexterity save vs explosion, DC 13: d20 15 (+2) = 17, success
```

| Save | Hazard | A made save |
| --- | --- | --- |
| DEX | explosion damage (creepers, TNT, beds, respawn anchors, …) | takes the damage × `success_multiplier` (half) |
| CON | the poison, wither and hunger effects | gets the effect for its duration × `success_multiplier` (half) |
| STR | an explosion's or wind charge's push, and a hit's knockback stronger than `min_strength` | is pushed × `success_multiplier` (half as far) |
| DEX | catching fire: fire blocks, fire aspect, burning arrows, fireballs, lightning | burns for the time × `success_multiplier` (`0`: not set alight) |
| WIS | the darkness effect (the Warden and sculk shriekers) | gets the effect for its duration × `success_multiplier` (half) |

A failed save changes nothing. Effects a command gives (`/effect give`, typed or in a function) never
call for a save; only effects from mobs, potions, food and the world do. Lava never calls for a save: standing in lava, or in a lava cauldron,
sets you alight as in vanilla. An effect with an infinite duration never calls for one either.

The default `knockback.min_strength` of `0.7` leaves out plain and sprinting hits and Knockback I
(0.5 or less); a sprinting hit with Knockback I, Knockback II and a ravager (0.75) call for a save.

Species [traits](traits.md) can give advantage on a vanilla save (`vanilla_save_advantage`): Dwarves
against poison, Elves and Halflings against darkness, Goliaths against knockback.

## Who saves

Players in survival and adventure. Mobs only with `profiled_mobs: true`, and then only mobs with an
[entity profile](entity-score-profiles.md).

## Cooldown

Each kind of save rolls at most once per entity per `cooldown_ticks` (default 20, one second).
Anything that calls for the same save during the cooldown reuses the last result without rolling,
so standing in fire or a cave spider's repeated bites do not flood the action bar. A canceled roll
(see [check events](api.md#check-events)) counts as failed and starts no cooldown. Cooldowns are not
saved; a restart clears them.

## Config

In `config/checks/scores.json`, under `vanilla_saves` (see [scores-config.md](scores-config.md)):

```json
"vanilla_saves": {
  "profiled_mobs": false,
  "cooldown_ticks": 20,
  "explosion": { "enabled": true, "dc": 13, "success_multiplier": 0.5 },
  "poison": { "enabled": true, "dc": 12, "success_multiplier": 0.5 },
  "knockback": { "enabled": true, "dc": 13, "success_multiplier": 0.5, "min_strength": 0.7 },
  "fire": { "enabled": true, "dc": 12, "success_multiplier": 0.0 },
  "darkness": { "enabled": true, "dc": 13, "success_multiplier": 0.5 }
}
```

| Key | Value |
| --- | --- |
| `profiled_mobs` | whether mobs with an entity profile save too; default `false` |
| `cooldown_ticks` | ticks a save's result is reused, not negative; `0` rolls every time |
| `<save>.enabled` | the switch for that save alone |
| `<save>.dc` | the save's DC |
| `<save>.success_multiplier` | what a made save multiplies the damage, duration, push or burn time by, not negative |
| `knockback.min_strength` | only knockback stronger than this calls for a STR save, not negative |

## Toggles

`<save>.enabled: false` turns that save off alone: the hazard behaves as in vanilla and nothing is
rolled.
