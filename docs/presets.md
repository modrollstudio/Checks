# Species, backgrounds and classes

Character creation (see [character-creation.md](character-creation.md)) starts by choosing a
species, a background and a class. Each is a datapack file, and each list also has **Custom**: no
traits, no bonuses, no kit, and free skill picks.

Checks ships these presets in its built-in datapack under the `checks` namespace. Names and
mechanics come from the SRD 5.2 (see [Attribution](#attribution)); descriptions are our own. Miner,
Farmer, Explorer and Smith are Minecraft-flavoured backgrounds of our own.

| Species | Backgrounds | Classes |
| --- | --- | --- |
| `dragonborn`, `dwarf`, `elf`, `gnome`, `goliath`, `halfling`, `human`, `orc`, `tiefling` | `acolyte`, `criminal`, `sage`, `soldier`, `miner`, `farmer`, `explorer`, `smith` | `barbarian`, `bard`, `cleric`, `druid`, `fighter`, `monk`, `paladin`, `ranger`, `rogue`, `sorcerer`, `warlock`, `wizard` |

## What each one does

- **Species:** a description, a list of traits, each with a name and description, and the sizes it
  can be. Traits are shown on the creation and stat screens; most also do something, from advantage
  on a save to darkvision (see [traits.md](traits.md)). The size sets the player's scale (see [body.md](body.md#size)); a species with two sizes lets the
  player choose on its page, the first being the default.
- **Background:** three abilities, two skill proficiencies and a starting kit.
  - The player splits the background's ability bonuses among its abilities: raise one by 2 and
    another by 1, or all three by 1, by default (`creation.presets.bonus_options`). A bonus never
    raises a score past `20` (`creation.presets.bonus_max_score`).
  - Bonuses are added after the scores are placed and shown separately, e.g. `15 +2 = 17`.
  - Its skills are proficient automatically.
  - Its kit is handed out once, when the character is confirmed. Items that do not fit in the
    inventory drop at the player's feet.
- **Class:** two saving-throw proficiencies, a skill list with how many to pick, a suggested ability
  order, and a hit die, recorded at each level gained for optional per-level health (see
  [body.md](body.md#max-health)). There are no class features.

## Skills

- A background's skills are fixed. A class adds its number of picks from its list.
- **Overlap:** each background skill that is also on the class list would be a wasted pick, so it
  frees one pick for any other skill instead (the 5e rule). A Soldier Fighter already has Athletics
  and Intimidation, so both Fighter picks may be any skill.
- A **Custom background** gives `creation.skill_choices` free picks (default 2) from every skill.
  A **Custom class** adds no picks and no list.
- A species whose traits give **skill choices** (Human: two of any skill; Elf: one of Insight,
  Perception or Survival) adds those picks as their own group (see [traits.md](traits.md)).
- Picks never exceed the skills left to pick, so a datapack with few skills cannot block creation.

## Files

| Kind | Path | Id |
| --- | --- | --- |
| Species | `data/<namespace>/checks/species/<name>.json` | `<namespace>:<name>` |
| Background | `data/<namespace>/checks/background/<name>.json` | `<namespace>:<name>` |
| Class | `data/<namespace>/checks/class/<name>.json` | `<namespace>:<name>` |

Every file has `format_version` (currently `1`; a newer version warns and parses anyway). Unknown
keys warn and are ignored. A file that is rejected is logged as an error and skipped while the rest
load normally.

Every kind also takes an optional `icon`: the id of the item drawn for it on the creation screen.
Without one a species shows `minecraft:armor_stand`, a background `minecraft:book` and a class
`minecraft:wooden_sword`; an `icon` naming no registered item warns and shows that default, so a
pack can use another mod's item and still load without that mod. Every shipped preset has an icon
of its own, e.g. `minecraft:dragon_breath` for the dragonborn and `minecraft:anvil` for the smith.

### Species

Example: [examples/species.json](examples/species.json)

```json
{
  "format_version": 1,
  "icon": "minecraft:lantern",
  "traits": ["darkvision", "mypack:tunnel_sense"],
  "sizes": [
    { "size": "small", "scale": 0.6 },
    { "size": "medium", "scale": 0.9 }
  ]
}
```

- `traits`: trait ids. An id without a namespace takes the file's namespace, so `darkvision` in
  `checks:dwarf` is `checks:darkvision`. What a trait does is its own file (see
  [traits.md](traits.md)); a trait with no file is flavor only.
- `sizes`: each a `size` (`small` or `medium`) and the `scale` it gives, as a multiple of normal
  player size (`0.0625..16`). The first is the default; with more than one, the player chooses. A
  species without `sizes` leaves the player's size alone.

Rejected when a size is unknown or listed twice, or a scale is missing or out of range.

### Background

Example: [examples/background.json](examples/background.json)

```json
{
  "format_version": 1,
  "icon": "minecraft:stone_pickaxe",
  "abilities": ["str", "con", "wis"],
  "skills": ["athletics", "perception"],
  "kit": [
    { "item": "minecraft:stone_pickaxe", "count": 1 },
    { "item": "minecraft:torch", "count": 16 }
  ]
}
```

- `abilities` (required): at least one of `str`, `dex`, `con`, `int`, `wis`, `cha`, each once. A
  bonus split that needs more abilities than listed is not offered for that background.
- `skills`: skill ids (see [skills.md](skills.md#skill-ids)).
- `kit`: stacks with an `item` id and a `count` (default `1`). An entry naming no registered item,
  or with a count below `1`, is skipped with a warning, so a kit naming another mod's items still
  loads without that mod.

Rejected when `abilities` is missing, empty, repeats an ability or names an unknown one, or when a
skill is not loaded.

### Class

Example: [examples/class.json](examples/class.json)

```json
{
  "format_version": 1,
  "icon": "minecraft:crossbow",
  "hit_die": 10,
  "saves": ["str", "con"],
  "skills": { "choose": 2, "from": ["athletics", "history", "insight", "perception"] },
  "suggested_order": ["str", "con", "dex", "wis", "int", "cha"]
}
```

- `hit_die` (required): the die's number of sides, at least `1`.
- `saves`: abilities whose saving throws become proficient.
- `skills.choose`: how many skills to pick (default `0`).
- `skills.from`: skill ids, or `"any"` for every loaded skill. An id naming no loaded skill is
  skipped with a warning.
- `suggested_order`: abilities from most to least important. It may list only the first few; the
  rest follow in STR to CHA order. The class page reads it as "Highest score in STR, then CON, …,
  lowest in CHA", and **Use Suggested** on the scores page places the highest values in this order.

Rejected when `hit_die` is missing or below `1`, `skills.choose` is negative, or an ability is
unknown or repeated.

Backgrounds and classes resolve skills against the skills loaded in the same reload, so a pack can
add a skill and a background that uses it together.

## Adding, replacing and removing

- **Add:** a file under your own namespace.
- **Replace:** a file at the same path as a shipped one, e.g.
  `data/checks/checks/background/soldier.json`.
- **Remove:** a file at the same path containing `{ "removed": true }`.
- **Hide every shipped preset:** `creation.presets.include_shipped: false`. This hides every preset
  in the `checks` namespace, so a pack that replaces a shipped preset under its `checks:` id should
  add it under its own namespace instead.

Presets hot-reload on `/reload`. A player's confirmed character keeps what its presets granted when
it was confirmed, even if a pack later changes or removes them.

## Names

Names and descriptions come from the lang file, like skills:

```json
{
  "checks.species.mypack.mole_folk": "Mole Folk",
  "checks.species.mypack.mole_folk.desc": "Burrowing folk at home underground.",
  "checks.trait.mypack.tunnel_sense": "Tunnel Sense",
  "checks.trait.mypack.tunnel_sense.desc": "You always know which way is up.",
  "checks.background.mypack.knight": "Knight",
  "checks.class.mypack.alchemist": "Alchemist"
}
```

The keys are `checks.<species|trait|background|class>.<namespace>.<path>`, and `.desc` for the
description. Without a translation, the path is shown in title case and there is no description.

## Config

```json
"creation": {
  "presets": {
    "enabled": true,
    "include_shipped": true,
    "bonus_options": [[2, 1], [1, 1, 1]],
    "bonus_max_score": 20
  }
}
```

- `bonus_options`: each option is a list of bonuses, one per ability. An option that is empty or
  has a bonus below `1` is skipped with a warning; with no valid option, backgrounds give no
  bonuses.
- `bonus_max_score`: a score in `1..30`, or the default is used.

## Toggle

`creation.presets.enabled: false` turns presets off: creation goes straight to the scores page with
free skill picks, and submissions naming a preset are refused. What confirmed characters' presets
granted (bonuses, background skills, class saves) is ignored while it is off;
their base scores and picked skills still apply. It all applies again when presets are turned back
on.

## Validation

The server checks every submission against the offer the screen was built from; see
[character-creation.md](character-creation.md#validation).

## Persistence

The chosen species, background, class and size, and what they granted, are saved with the character
in `data/checks_characters.dat`. `/checks reset <player>` clears them with the rest of the character.

## Attribution

This work includes material from the System Reference Document 5.2 ("SRD 5.2") by Wizards of the
Coast LLC, available at https://www.dndbeyond.com/srd. The SRD 5.2 is licensed under the Creative
Commons Attribution 4.0 International License, available at
https://creativecommons.org/licenses/by/4.0/legalcode.
