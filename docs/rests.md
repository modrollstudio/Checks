# Hit Dice and rests

Every player has a pool of **Hit Dice**: one per character level, each the hit die of their class (a
Fighter's d10, a Wizard's d6; a Custom class or none uses `health.per_level.custom_hit_die`, a d8). A
short rest spends them to heal on its own; a long rest gives some back. Every Hit Die rolls through
Critfall.

## Short rest

Use a **lit campfire with an empty hand** to sit down by it for a short rest: 1000 ticks, about 50
seconds, one hour of Minecraft time. Vanilla does nothing with that click, so nothing else is lost.
Stay within 3 blocks of the fire the whole time. The rest breaks, and has to be started again, if:

- you take damage,
- you attack,
- you move away from the fire,
- the fire goes out,
- a monster that would stop you sleeping comes within 8 blocks across and 5 up or down (the same
  monsters as a bed's rule), or
- another mod says you are in a fight (through `ChecksApi.addRestBlocker`, for Initiative's encounters).

A rest can't start while any of those hold; you are told why in chat.

When the rest is done, Hit Dice are spent for you, one at a time, until you are at full health or out
of dice. Each rolls the die plus your CON modifier and heals that many hit points, at least 1 as in the
2024 rules (`rest.hit_dice.min_heal`), times `rest.hit_dice.heal_multiplier`. A die that would heal past full is still spent, as in 5e, where you
spend a die before you see its roll. One chat line says what the rest did:

- `Short rest: spent 2 Hit Dice, healed 11.`
- `Short rest: you are already at full health.` when no die was needed, or
- `Short rest: you have no Hit Dice left. Sleep through a night to get some back.`

With a full food bar vanilla's own regeneration heals about as much in those 50 seconds. Hit Dice are
healing that costs no food, and the only way to heal in a pack that turns `naturalRegeneration` off.

## Long rest

**Sleeping through the night** is a long rest for every player asleep when the night is skipped. As in
the 2024 rules, which character creation follows too, it heals you to full health, gives back every
spent Hit Die and readies your once-a-day traits, such as Relentless Endurance, as
`traits.recharge_ticks` passing does. Lying in bed without the night being skipped is no long rest.

For the 2014 rule set `regain_fraction` to `0.5`: half your level's Hit Dice come back, rounded down,
at least `min_regain` (1). The 2014 rule heals fully too; `heal_to_full: false` keeps sleeping from
healing at all.

## Stat screen

The **Body** section shows `Hit Dice 3/5 d8`: the dice left, the pool and the die. Its tooltip is
three short lines: the pool, that a rest at a lit campfire heals with them, and what sleeping does by
the server's settings: `Sleep to heal fully and get your Hit Dice back.` with the defaults,
`Sleep to heal fully and get half your Hit Dice back.` at `regain_fraction: 0.5`. See
[stat-screen.md](stat-screen.md).

## For other mods

`ChecksApi.addRestBlocker(predicate)` stops a player resting while it answers true: they can't start a
short rest, and a rest under way breaks before it spends anything. `ChecksApi.hitDiceLeft(player)` reads the
pool. See [api.md](api.md#rests).

## Config

`rest` in `config/checks/scores.json` (see [scores-config.md](scores-config.md)):

```json
"rest": {
  "hit_dice": { "enabled": true, "heal_multiplier": 1.0, "min_heal": 1 },
  "short_rest": { "enabled": true, "ticks": 1000, "campfire_radius": 3, "monster_range": 8, "monster_height": 5 },
  "long_rest": {
    "enabled": true,
    "regain_fraction": 1.0,
    "min_regain": 1,
    "heal_to_full": true,
    "recharges_traits": true
  }
}
```

- `hit_dice.heal_multiplier`: what a Hit Die's roll plus CON is multiplied by, `0` or more.
- `hit_dice.min_heal`: the least a Hit Die's roll plus CON counts as, before the multiplier, `0` or more.
- `short_rest.ticks`: how long a short rest lasts, at least 1.
- `short_rest.campfire_radius`: how far from the fire's center you may be, in blocks.
- `short_rest.monster_range`, `monster_height`: how far across and up or down a monster stops a rest.
- `long_rest.regain_fraction`: the part of your level's Hit Dice given back, `0..1`, rounded down;
  `1.0` (all of them) by the 2024 rules, `0.5` by the 2014 rules.
- `long_rest.min_regain`: the fewest given back, `0` or more.
- `long_rest.heal_to_full`: heal to full health on waking.
- `long_rest.recharges_traits`: ready every once-a-day trait on waking.

A bad value falls back to its default with a warning. Spent Hit Dice are saved with the level
(`data/checks_levels.dat`).

## Toggles

- `hit_dice.enabled: false` turns Hit Dice off: no pool on the sheet, no short rests, and campfires
  are left to vanilla. Long rests still ready traits.
- `short_rest.enabled: false` leaves campfires to vanilla; Hit Dice are only regained.
- `long_rest.enabled: false` makes sleeping give nothing back and ready nothing.
