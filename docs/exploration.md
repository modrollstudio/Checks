# Exploration skills

Thirteen skills have uses out in the world: Athletics, Acrobatics, Stealth, Perception, Sleight of Hand,
Investigation, Nature, Arcana, Religion, History, Animal Handling, Survival and Medicine. Things that
happen all the time use a passive score or modifier and never roll: climbing speed (Athletics), how
close mobs get before they notice a sneaking player (Stealth), noticing tripwires (Perception), how fast
hunger drains (Survival) and how long ailments last (Medicine). Single moments roll a check: a leap,
tearing free of a cobweb, a landing, a disarm, searching a looted chest, recalling what you know of a
mob or a place, a taming attempt. A moment whose outcome could not change anything rolls nothing.

Every check is rolled through `ChecksApi`, so Critfall rolls the d20, the
[check events](api.md#check-events) fire and [bonus sources](bonus-sources.md) on the skill apply.
Passive scores include bonus sources too (see
[bonus-sources.md](bonus-sources.md#passive-scores)). A roll shows just above the hotbar, as the
[social skills](social.md) do: the roll, and below it a flavor line on a client with Checks (see
[scores-config.md](scores-config.md#roll-messages)):

```
Athletics vs DC 12: d20 15 (+2) = 17, success
You push off hard and sail across.
```

Flavor lines are cosmetic: picked with Minecraft's own randomness, never a Critfall roll. Each
outcome has four to six lines in the lang file,
`checks.exploration.<leap|cobweb|landing|disarm|search|taming>.<success|failure>.<n>` and
`checks.exploration.<monster_lore|structure_lore>.failure.<n>`, so a resource pack can rewrite them or
add more. A made lore check shows its hint instead (see below). A listener that cancels the roll (see
[check events](api.md#check-events)) leaves the moment as in vanilla.

Only players use these. Players in creative or spectator never roll, and spectators see no glints.

## Athletics: leap

Only a jump that matters rolls: a sprinting jump where the ground one block ahead, where a plain jump
would land, drops `leap.min_drop` (3) or more blocks, a gap or a pit edge. That rolls Athletics against
`leap.dc` (12). A made check adds `leap.boost` (0.15) blocks per tick to the jump along the way the
player faces: about one block further. A failed check is a plain jump. Every other jump is a plain jump
and rolls nothing: onto flat ground, down a step or a drop of one or two blocks, a standing jump, and a
jump in water, while gliding, flying or riding.

Within `leap.cooldown_ticks` (40, two seconds) of a roll, another leap reuses its result without
rolling, so a run across several gaps rolls once.

## Athletics: cobwebs

A player caught in a cobweb rolls Athletics against `cobwebs.dc` (12). A made check tears through every
web they are in, with nothing dropped. A failed check leaves them stuck as in vanilla, and they roll
again after `cobwebs.retry_ticks` (60, three seconds) if they are still caught. A web the player may not
break (adventure mode, spawn protection) rolls nothing and holds as in vanilla.

## Athletics: climbing

Passive, with no roll. Climbing a ladder, vine, scaffolding or other climbable block is
`climbing.per_point` (0.1, 10%) faster per point of a positive Athletics modifier, at most
`climbing.max_bonus` (0.5, 50%) faster: +3 climbs 30% faster, +5 or more 50%. A modifier of 0 or less
climbs as in vanilla. The modifier is the skill modifier (ability, proficiency, flat skill bonus); bonus
sources do not count, as they only apply to rolls and passive scores.

This speeds up climbing up; climbing down is as in vanilla. The client moves its own player, so the
server works out each player's climbing speed and tells their client whenever it changes; a client
without Checks climbs as in vanilla.

## Acrobatics: landing

Fall damage rolls Acrobatics against `landing.base_dc` (10) plus the damage times
`landing.dc_per_damage` (0.5), rounded down: 10 plus half the fall damage, so DC 13 for a 6-damage fall
and DC 20 for a 20-damage one. The damage is the fall's own, before armor and Feather Falling. A made check multiplies the damage by
`landing.success_multiplier` (0.5, half). A failed check takes it all. A fall that does no damage, or
damage of any other kind, rolls nothing. A fall happens once, so there is no cooldown.

## Stealth: sneaking

Passive, with no roll. While a player sneaks, each point their passive Stealth beats a mob's passive
Perception by shrinks how far off that mob notices them by `sneak.per_point` (0.05, 5%), down to
`sneak.min_visibility` (0.25) of the usual distance. This comes on top of vanilla's own sneaking
(80%), and of mob heads and invisibility. A mob whose passive Perception matches or beats the
player's passive Stealth notices them as usual. Stealth changes when a mob first notices a player; one
already after them usually keeps after them. Mobs always notice anyone within two blocks, as in vanilla.

For example, passive Stealth 14 against a zombie's passive Perception 10 sneaks to 80% of vanilla's
sneaking distance.

## Perception: spotting tripwires

Passive, with no roll. An armed tripwire is a string strung between hooks and not disarmed. While a
player's passive Perception meets `spot_tripwires.dc` (13), every armed tripwire within
`spot_tripwires.range` (8) blocks glints for that player alone, checked every
`spot_tripwires.interval_ticks` (20, a second). A newly spotted tripwire is pointed out above the
hotbar with a line from `checks.exploration.spot_tripwire.<n>`. A loose string never glints.

The glint is sparks the server sends that one player, so players without Checks see it too.

## Sleight of Hand: disarming a tripwire

Sneaking and using an armed tripwire with an empty hand rolls Sleight of Hand against
`disarm_tripwires.dc` (13):

- **Made:** the string comes off as with shears: its hooks let go without firing and the string drops.
- **Failed:** the string snaps and springs the trap: its hooks fire as if it were walked through, and
  the string drops.

Either way the string is gone, so a tripwire is only ever rolled for once and needs no cooldown.
Using a tripwire that is not armed, without sneaking, or with something in hand rolls nothing and is
left to vanilla; shears still disarm as in vanilla, with no roll. Players who may not break the block
(adventure mode, spawn protection) roll nothing.

## Investigation: searching looted chests

A chest, barrel or other container a loot table fills (as in structures) remembers that loot table the
first time a player uses it, before it opens. The note lives on the container itself, in its
`minecraft:custom_data` under `checks:search`, so it is shared by every player and survives restarts.
Once the container has been opened, a player who sneaks and uses it rolls Investigation against
`search_chests.dc` (13):

- **Made:** they find one more item: the loot table is rolled again as for a chest, and one item of one of
  its stacks goes into their inventory (or drops at their feet if it is full).
- **Failed:** a line says there is nothing else here.

Either way the container is searched for good, for everyone: it never rolls again. Both halves of a double
chest count as one container. The search takes the click, so the chest does not open on it.

Nothing rolls, and the use is left to vanilla, for a container no loot table ever filled (one a player
placed and stocked), one that has not been opened yet, without sneaking, or one the player may not open
(spawn protection, a lock). A loot container opened before Checks was installed, or emptied by a hopper
before any player used it, never learned its loot table and never rolls. Which generated stack the item
comes from is loot randomness, as vanilla's own filling of the chest, not a check.

## Nature, Arcana, Religion and History: monster lore

The first time a player sees a mob type up close, within `monster_lore.range` (16) blocks and in line of
sight, they roll the skill that covers it against `monster_lore.dc` (12), looked for every
`monster_lore.interval_ticks` (20, a second). Every mob counts, vanilla or modded; players and armor
stands are no mobs. Each look rolls for the nearest such mob only, so a crowd never rolls at once.
Invisible mobs are not seen.

- **Made:** a hint about that mob type shows below the roll, a weakness or a habit (`Zombies are undead:
  Smite cuts them down, …`). That player never rolls for that mob type again; it is saved per player.
- **Failed:** a line says nothing comes to mind. They may try again on a later encounter with that mob
  type, once `monster_lore.retry_ticks` (6000, five minutes) are up. The wait lives in memory only, so a
  restart ends it.

### Which skill

Entity type tags decide first, so packs can move any mob:

| Tag | Skill | Ships with |
| --- | --- | --- |
| `checks:lore/religion` | Religion | `#minecraft:undead`, the giant, the vex and the ghast |
| `checks:lore/arcana` | Arcana | the blaze, breeze, guardians, ender dragon, enderman, endermite, evoker, illusioner, iron golem, slimes and magma cubes, shulker, warden and witch |
| `checks:lore/nature` | Nature | the bat, bee, spiders, creeper, dolphin, goat, hoglin, llamas, panda, piglins, pillager, polar bear, ravager, silverfish, vindicator and wolf |
| `checks:lore/history` | History | the villager and the wandering trader |

A mob in more than one tag uses the first of Religion, Arcana, Nature, History. A mob in none falls
back on what it is:

1. `#minecraft:undead`: **Religion**.
2. An animal or water animal (the game's own `Animal` and `WaterAnimal` kinds of mob), or in
   `#minecraft:arthropod` or `#minecraft:aquatic`: **Nature**.
3. Anything else: **Arcana**.

So a modded wolf, fish or spider rolls Nature, a modded zombie Religion, and a modded golem or spirit
Arcana, with no tag needed. Among vanilla mobs the fallback covers the passive animals and fish (Nature),
and the allay and snow golem (Arcana).

### Hints, for other mods

Every hint is the lang key `checks.lore.<namespace>.<path>`, named after the mob's entity type id:
`checks.lore.minecraft.zombie`, or `checks.lore.mymod.frost_wisp` for `mymod:frost_wisp`. Checks ships
one for every vanilla mob. A mod can add the line for its own mobs to its own lang files, and a resource
pack can add or rewrite any of them:

```json
{
  "checks.lore.mymod.frost_wisp": "Frost wisps shatter in fire, and freeze whatever they touch."
}
```

The server sends the key, so a player whose client has it (from the mod or a resource pack) sees that
line in their own language.

Without a line for the mob, Checks builds one from the mob itself:

- about how many hearts it has: its max health in hearts, to the nearest half heart;
- if it has an attack, how many hearts a hit takes: its attack damage in hearts, to the nearest half heart;
- the enchantment it is weak to: Smite for `#minecraft:undead`, Bane of Arthropods for
  `#minecraft:arthropod`, Impaling for `#minecraft:aquatic` (the first that fits);
- `Fireproof.` if fire can't hurt it.

```
About 15 hearts, hits for 1.5. Weak to Smite.
```

It is one short line, under 70 characters in English for anything up to 999.5 hearts and 99.5 a hit;
only a fireproof arthropod with half hearts in the hundreds runs a few characters over. Its pieces are lang lines too
(`checks.exploration.lore_hint.*`), so a resource pack can translate or reword them.

## History: structure lore

The first time a player stands inside a piece of a kind of structure, they roll History against
`structure_lore.dc` (12), checked every `structure_lore.interval_ticks` (20, a second). Made and failed
work as for monster lore, with `structure_lore.retry_ticks` (6000) and the hint from
`checks.lore.structure.<kind>` (`checks.lore.structure.desert_pyramid`: the TNT under the pressure
plate); each kind is saved per player once recalled.

A kind of structure is a structure tag `checks:history/<kind>`, so packs can add structures to a kind or
add kinds of their own (a kind without a hint shows `checks.lore.structure.unknown`). Checks ships
sixteen: `village` (every village), `desert_pyramid`, `jungle_temple`, `ocean_monument`, `stronghold`,
`mansion`, `ancient_city`, `trial_chambers`, `fortress`, `bastion`, `outpost`, `ruined_portal` (every
variant), `shipwreck` (and beached), `mineshaft` (and mesa), `trail_ruins` and `igloo`. Inside means
inside one of the structure's pieces: a village's houses and paths, not the fields between them.

## Animal Handling: taming

Each attempt by a player to tame an animal rolls Animal Handling against `taming.dc` (12):

- feeding an untamed wolf a bone, a cat raw fish or a parrot seeds;
- riding an untamed horse, donkey, mule, llama or trader llama, at the moment it decides whether to
  accept its rider or throw them.

**Made:** the animal is tamed on that attempt. **Failed:** vanilla's own chance applies as usual (a wolf
or cat one in three, a parrot one in ten, a horse by its temper), so the animal may still accept them.
Another mod that cancels a taming (NeoForge's `AnimalTameEvent`) still can.

## Survival: hunger

Passive, with no roll. Hunger drains `hunger.per_point` (0.05, 5%) slower per point of a positive
Survival modifier, at most `hunger.max_reduction` (0.25, 25%): every bit of food exhaustion the player
gains (from sprinting, jumping, mining, fighting, healing) is that much smaller. This comes after CON's
own exhaustion extra (see [body.md](body.md)). A modifier of 0 or less drains as in vanilla.

## Medicine: ailments

Passive, with no roll. The effects in the mob effect tag `checks:shortened_by_medicine` (poison, wither,
hunger and nausea) last `ailments.per_point` (0.05, 5%) shorter per point of a positive Medicine modifier
on a player, at most `ailments.max_reduction` (0.25, 25%), rounded down to the tick. This comes after a
CON save against the effect (see [vanilla-saves.md](vanilla-saves.md)). Effects a command gives, and
infinite ones, last as given.

Survival and Medicine use the skill modifier (ability, proficiency, flat skill bonus); bonus sources do
not count, as for climbing.

## Stat screen

Each of these skills has an `In this mod: …` line in its tooltip
(`checks.skill.checks.<skill>.use`; see [stat-screen.md](stat-screen.md)).

## Config

In `config/checks/scores.json`, under `exploration` (see [scores-config.md](scores-config.md)):

```json
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
```

| Key | Value |
| --- | --- |
| `<use>.enabled` | the switch for that use alone |
| `<use>.dc` | the check's DC, or for `spot_tripwires` the passive Perception needed |
| `monster_lore.range` | blocks around the player a mob is seen within, not negative |
| `monster_lore.retry_ticks`, `structure_lore.retry_ticks` | ticks after a failed lore roll before that mob type or kind may roll again, not negative |
| `monster_lore.interval_ticks`, `structure_lore.interval_ticks` | ticks between looks, at least 1 |
| `hunger.per_point`, `ailments.per_point` | the share less per point of a positive Survival or Medicine modifier, not negative |
| `hunger.max_reduction`, `ailments.max_reduction` | the largest share less, `0..1` |
| `leap.cooldown_ticks` | ticks a leap's result is reused, not negative; `0` rolls every leap |
| `leap.boost` | blocks per tick a made leap adds along the facing, not negative |
| `leap.min_drop` | how many blocks the ground ahead must drop for a leap to roll, at least 1 |
| `cobwebs.retry_ticks` | ticks after a failed roll before a player still caught rolls again, not negative |
| `climbing.per_point` | the share faster per point of a positive Athletics modifier, not negative |
| `climbing.max_bonus` | the largest share faster, not negative |
| `landing.base_dc` | the landing DC before the damage is added |
| `landing.dc_per_damage` | what each point of fall damage adds to the DC, rounded down; not negative |
| `landing.success_multiplier` | what a made landing multiplies the fall damage by, not negative |
| `sneak.per_point` | the share of the distance each point of the Stealth margin takes off, not negative |
| `sneak.min_visibility` | the least share of the distance left, `0..1` |
| `spot_tripwires.range` | blocks around the player searched for tripwires, not negative |
| `spot_tripwires.interval_ticks` | ticks between looks, at least 1 |

A bad value falls back to its default with a warning. A full example is in [examples/scores.json](examples/scores.json).

## Toggles

`<use>.enabled: false` turns that use off alone and leaves the moment as in vanilla: jumps, falls,
cobwebs and sneaking behave as in vanilla, climbing goes back to vanilla's speed within a tick,
tripwires stop glinting at the next look, using a tripwire or a looted chest does nothing new, no lore
rolls, taming is vanilla's chance alone, and hunger and new ailments are as in vanilla. Nothing is
rolled. Lore already recalled stays saved, and chests already searched stay searched, for when the use
is switched back on.
