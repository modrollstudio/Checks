# Body: health, armor class, size and attribute extras

Checks turns a character's scores into their body: CON raises max health, DEX raises AC by the 5e
armor rules, the species sets the size, and small "extras" make STR, DEX and CON felt outside
rolls. Every part has its own switch in `config/checks/scores.json` (see [Config](#config)), and
Checks still rolls no dice: the AC part reaches Critfall through its modifier provider.

## Max health

- **CON:** the CON modifier times `health.per_con_point` hit points (default `2`): CON 16 (+3) gives
  +6 (three hearts), CON 6 (-2) gives -4.
- **Levels (off by default):** with `health.per_level.enabled`, each level gained adds its recorded
  class hit die's fixed 5e value (`sides / 2 + 1`, so a d10 is 6) times `health.per_level.multiplier`
  (default `0.2`), rounded down over all levels. Level 1 is Minecraft's own 20 HP; levels gained
  without a class (Custom) or while presets were off recorded no hit die and add nothing (see
  [levelling.md](levelling.md)). A d10 Fighter at level 5 with the default multiplier gets
  `floor(4 × 6 × 0.2) = +4`.
- **Traits:** a species trait with `extra_health` (Dwarven Toughness: half a hit point per level)
  adds its own modifier, `checks:trait_health` (see [traits.md](traits.md)).

Health and the extras below apply to **players** only. They update within a tick of any change: a
command, character creation, a level, an improvement, a `/reload`. Current health never exceeds the
new max. A player who respawns after dying starts at their full max health; one returning from the
End keeps their health.

## Armor class

Critfall computes AC from armor and toughness (or an entity profile's `armor_class`); Checks adds DEX
through Critfall's `acModifier`:

| Heaviest worn piece | DEX added |
| --- | --- |
| None, or untagged | all of it |
| Light | all of it |
| Medium | up to `armor_class.medium_max_dex` (default `+2`) |
| Heavy | none |

The heaviest piece decides, as in 5e: leather boots with an iron chestplate count as heavy. A DEX
penalty applies unarmored and in light or medium armor, never in heavy. Who gets it follows the same
rule as Critfall attack modifiers (see [critfall.md](critfall.md#who-gets-checks-values)): players
always, mobs with a Checks profile, and other mobs only with `critfall.unprofiled_mobs`.

### Armor categories

Categories are item tags, so packs and mods can add their armor:

| Tag | Shipped |
| --- | --- |
| `checks:light_armor` | leather |
| `checks:medium_armor` | chainmail, golden, turtle helmet |
| `checks:heavy_armor` | iron, diamond, netherite |

Example: [examples/armor_tag.json](examples/armor_tag.json), as
`data/checks/tags/item/medium_armor.json` in a datapack:

```json
{
  "replace": false,
  "values": ["mymod:bronze_chestplate", "#mymod:scale_armor"]
}
```

A piece in more than one tag counts as the heaviest of them. A piece in none (an elytra, a carved
pumpkin, untagged modded armor) counts as no armor.

## Size

A species lists its sizes and the scale each gives (see [presets.md](presets.md#species)); the
first is the default. Human and Tiefling let the player choose Small or Medium on the species page of
character creation, as in the SRD 5.2. The scale is applied with Minecraft's `scale` attribute,
which changes the player's hitbox, eye height and model.

| Species | Size | Scale |
| --- | --- | --- |
| Halfling | Small | 0.5 |
| Gnome | Small | 0.55 |
| Dwarf | Medium | 0.75 |
| Elf | Medium | 0.95 |
| Human, Tiefling | Medium or Small | 1.0 or 0.6 |
| Dragonborn, Orc | Medium | 1.05 |
| Goliath | Medium | 1.25 |

Scales are rough heights next to a 6-foot player: a 3-foot halfling is 0.5. SRD 5.2 sizes Dwarves
and Goliaths as Medium, so they keep the Medium category whatever their scale. The size category has
no other effect.

## Attribute extras

Each extra adds `per_point` per point of its ability's modifier, so a negative modifier works the
other way.

| Extra | Ability | Default `per_point` | Effect at +3 |
| --- | --- | --- | --- |
| `knockback` | STR | `0.1` | +0.3 attack knockback (`attack_knockback` attribute) |
| `mining_speed` | STR | `0.05` | +15% block breaking speed (`block_break_speed`) |
| `bow_draw` | DEX | `0.1` | bows draw 30% faster (full power in about 15 ticks instead of 20) |
| `crossbow_reload` | DEX | `0.1` | crossbows load 30% faster (19 ticks instead of 25) |
| `breath` | CON | `0.2` | +0.6 `oxygen_bonus`: breath lasts about 60% longer underwater |
| `exhaustion` | CON | `0.05` | 15% less food exhaustion |

The attributes keep their vanilla limits, so a low STR never takes knockback below zero and a low
CON never shortens breath. A very low DEX slows a bow or crossbow down to a tenth of its speed at
most. The bow and crossbow are drawn on the server; the client's draw animation does not know about
DEX, so a fast draw can reach full power before the animation shows it.

## How it is stored

Max health, size, knockback, mining speed and breath are attribute modifiers with the ids
`checks:con_health`, `checks:size`, `checks:str_knockback`, `checks:str_mining_speed` and
`checks:con_breath`. They are transient: never saved with the player, and applied again on login,
respawn and dimension change, and within a tick of any score or config change. Turning a part off
removes its modifier on the next tick.

Because vanilla caps the health it loads at the unmodified max, Checks keeps each player's health in
its own world data (`data/checks_health.dat`) whenever the player is saved (autosave, logout,
shutdown) and gives it back right after the modifiers return on login, so 26 of 26 HP stays 26. It
is only given back when the loaded health is exactly that health capped, so it never heals anyone.

A world loaded without Checks leaves players with vanilla attributes: nothing of Checks is in their
saved data, and health above the vanilla max is capped as usual.

## Stat screen

The stat screen shows a **Body** section, under the passive scores, with hit points, armor class and
size, each explaining where it comes from on hover, e.g. `CON +3: +6 max health` or `DEX +2 (medium armor, max +2)`. The AC row
appears while Checks supplies Critfall's modifiers. Each ability box lists its extras, e.g.
`Mining speed +15%`. See [stat-screen.md](stat-screen.md).

## Config

```json
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
}
```

A negative number falls back to its default with a warning.

## Toggles

- `health.enabled: false` removes the max health modifier; current health drops within the vanilla
  max.
- `health.per_level.enabled` (default `false`) adds the hit dice of levels gained.
- `armor_class.enabled: false` makes Checks answer nothing for AC, so Critfall uses its own.
  `critfall.enabled: false` turns off every Critfall modifier, AC included.
- `size.enabled: false` returns every player to normal size. So does
  `creation.presets.enabled: false`, since sizes come from species.
- Each `attribute_extras.<extra>.enabled: false` turns that extra off and nothing else.
