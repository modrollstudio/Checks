# Check triggers

A check trigger makes a player roll when they use a block, an entity or an item, then runs a
function and/or hands out a loot table for the outcome. Force a cracked wall, persuade a villager,
read a strange map, all from a datapack.

## Files

One trigger per file at `data/<namespace>/checks/trigger/<path>.json`, reloaded with `/reload`. A
file that fails to parse is logged and skipped. [`examples/trigger.json`](examples/trigger.json):
right-clicking cracked stone bricks rolls Athletics against DC 15.

```json
{
  "format_version": 1,
  "on": "use_block",
  "block": "minecraft:cracked_stone_bricks",
  "stat": "athletics",
  "dc": 15,
  "cooldown": { "player": 20, "target": 100 },
  "outcomes": {
    "success": { "function": "mypack:cracked_wall/break" },
    "failure": { "function": "mypack:cracked_wall/strain" },
    "natural_1": { "function": "mypack:cracked_wall/collapse" }
  }
}
```

With these functions the wall breaks on a success and hurts on a failure, and a natural 1 hurts more:

```mcfunction
# data/mypack/function/cracked_wall/break.mcfunction
setblock ~ ~ ~ minecraft:air destroy

# data/mypack/function/cracked_wall/strain.mcfunction
damage @s 2 minecraft:generic

# data/mypack/function/cracked_wall/collapse.mcfunction
damage @s 6 minecraft:falling_block
```

| Key | Value |
| --- | --- |
| `format_version` | `1` |
| `on` | `use_block`, `use_entity` or `use_item` |
| `block` / `entity` / `item` | what is used, by `on`: an id or a `#tag` |
| `stat` | an ability (`str`) or skill (`athletics`, `mypack:lockpicking`) |
| `dc` | the DC to meet |
| `mode` | `normal`, `advantage` or `disadvantage`; default `normal` |
| `cooldown` | `player` and `target` in ticks; both default `0` |
| `outcomes` | `success`, `failure`, `natural_1`, `natural_20`: each a `function` and/or a `loot_table` |

An unknown exact block, entity type or item id, ability or skill rejects the file, as does a trigger
without any outcome or an outcome with neither a function nor a loot table. Trigger files load
after skills, so they can roll datapack skills.

## The roll

The trigger rolls `ChecksApi.check(player, stat, dc, mode)`, so the player's
[bonus sources](bonus-sources.md) apply and [check events](api.md#check-events) fire. The player
sees the roll above the hotbar, for `roll_messages.duration_ticks` on a client with Checks (see
[scores-config.md](scores-config.md#roll-messages)), e.g. `Athletics vs DC 15: d20 13 (+5) = 18, success`. Under
advantage or disadvantage it names the mode and shows both d20s:
`Athletics vs DC 15: advantage, d20 17 and 4, keeps 17 (+5) = 22, success`.

The outcome:

- a natural 20 runs `natural_20` when the trigger has one;
- a natural 1 runs `natural_1` when the trigger has one;
- otherwise meeting the DC runs `success`, and anything else `failure`.

A missing branch runs nothing. Checks have no automatic success or failure, so without a
`natural_20` branch a natural 20 that misses the DC fails.

## Outcomes

- `function` runs as the player, positioned at the target (the block's center, the entity, or the
  player for an item), at function permission level and without chat feedback, as vanilla runs an
  advancement reward. `~ ~ ~` is the block that was used; `@s` is the player.
- `loot_table` is rolled with the player as `this_entity` and the target position as `origin`, and
  the items go into the player's inventory, or drop at their feet when it is full.

## When it fires

- Block and entity triggers answer the main hand only, so one click rolls once. Item triggers
  answer the hand holding the item.
- Every matching trigger fires, in id order.
- A trigger that fires consumes the click: no block is placed, no menu opens, no item is used.
- Spectators never trigger anything.
- If a check event listener cancels the roll, nothing runs, no cooldown starts and the click goes
  to vanilla.

## Cooldowns

- `player`: after firing, the same player cannot fire this trigger again, anywhere, for that many
  ticks.
- `target`: after firing, nobody can fire this trigger on the same block or entity for that many
  ticks. Item use has no target, so `target` is ignored there with a warning.

While a cooldown runs the click goes to vanilla and nothing is rolled. Cooldowns live in memory: a
server restart clears them.

## Toggle

`triggers.enabled: false` in `config/checks/scores.json` stops every trigger; clicks behave as
without Checks. See [scores-config.md](scores-config.md#toggles).
