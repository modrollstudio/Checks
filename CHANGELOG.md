# Changelog

## 0.1.4 — unreleased (alpha)

Requires Critfall 0.2.18 or newer.

- Armor class follows Critfall 0.2.18's armor rule: 10, plus a quarter of your armor and a quarter of your toughness, each rounded down, then your DEX.
- The Armor Class tooltip takes its numbers from Critfall itself, so it always matches the AC attacks roll against, even when a pack changes Critfall's `armor_ac_divisor` or `toughness_ac_divisor`. Its armor and toughness lines read `Armor 13: +3`.
- Putting on armor never lowers your AC. When a chestplate's DEX cap would leave you below your AC without it, you keep the higher one: DEX 20 in a chainmail chestplate is AC 15, not 13. The Armor Class tooltip shows the difference on its own line, e.g. `Never below your AC without body armor: +2`.
- A 0.1.4 client and a 0.1.3 server (or the other way round) refuse to connect when joining, with a message naming the version to install.

## 0.1.3 — 2026-10-20 (alpha)

Requires Critfall 0.2.14 or newer.

- Armor class: only body armor (the chestplate) limits DEX now, as in 5e. A helmet, leggings or boots never lower your AC.
- The Armor Class tooltip shows where the number comes from: base 10, what your armor and toughness points add, and your DEX.
- Athletics leaps only ever add to a jump. A made check pushes you further; no roll can stop or shorten a jump.
- Every Athletics leap rolls, even right after another: the two-second leap cooldown, which reused the last result without a roll or a readout, is gone. A leap rolls once and the next waits until you land. The old `leap.cooldown_ticks` setting is ignored.
- One roll per hit: a Fire Aspect hit that Critfall rolled for no longer also calls for a DEX save against catching fire.
- Mismatched versions: a client and a server whose Checks can't talk to each other, such as 0.1.3 and 0.1.2, now refuse to connect when joining, with a message naming the version to install, instead of failing later. Works on NeoForge and Fabric.

## 0.1.2 — 2026-10-14 (alpha)

Requires Critfall 0.2.14 or newer.

- Luck (halfling) now rerolls a natural 1 on Critfall hazard saves and attack rolls too.
- A trait `roll_bonus` now adds its flat bonus to hazard saves, not just its advantage or disadvantage.
- Datapack bonus sources on saves now apply to hazard saves: their bonus, advantage and disadvantage.

## 0.1.1 — 2026-10-13 (alpha)

Requires Critfall 0.2.13 or newer.

- Checks now counts against Critfall hazards, such as Dungeon Crawl's traps and other mods' hazards.
- A hazard save uses your ability modifier, plus your proficiency bonus if you're proficient in that save.
- The six `critfall:<ability>` save keys are mapped by default. Existing configs get them without changes.
- Dwarven Resilience gives advantage on saves against any hazard whose damage type is tagged `c:is_poison`.
- Gnomish Cunning gives advantage on INT, WIS and CHA hazard saves.
- One roll per hit: if a Critfall attack roll or hazard save already decided the damage, it no longer also triggers Checks' DEX save against explosions or Acrobatics landing check.

## 0.1.0 — 2026-10-09 (alpha)

First release. Requires Critfall 0.2.10 or newer on NeoForge or Fabric, Minecraft 1.21.1.

### Abilities, skills and saves

- Six ability scores (STR, DEX, CON, INT, WIS, CHA) with their modifiers for every living entity.
- Mobs, modded ones included, get scores from their attributes out of the box.
- The 18 standard skills with passive scores, plus any skill a datapack adds.
- A proficiency bonus, skill proficiency and expertise, and saving-throw proficiency.

### Character creation

- A first-join screen walks you through species, background, class, ability scores and skills.
- Set scores with the standard array, point buy, 4d6 drop lowest or hardcore 3d6, rolled through Critfall.
- The nine SRD species, eight backgrounds (four SRD, four Minecraft-flavoured) and twelve SRD classes.
- Backgrounds hand out ability bonuses, two skills and a starting kit; classes give save proficiencies and a skill list.
- The Confirm page tells your character's story before you lock it in.

### Levelling

- Levels 1–20 from vanilla XP and advancements, announced in chat.
- Proficiency grows with your level, from +2 to +6.
- Ability score improvements at levels 4, 8, 12, 16 and 19, chosen from a Level up! button.

### Stat screen

- A character sheet on a key (K by default) with Overview and Skills tabs.
- Hover any value for a breakdown of every bonus behind it.

### Combat and body

- Attacks roll with STR (melee) or DEX (ranged) plus proficiency, and add that modifier to damage.
- DEX adds to your Armor Class by the light, medium and heavy armor rules.
- CON raises your max health.
- Species set your size, from a half-size Halfling to a towering Goliath.
- STR, DEX and CON tweak knockback, mining speed, bow draw, crossbow reload, breath and exhaustion.
- Critfall spell saves use your Checks save modifier.

### Saves against the world

- DEX saves halve explosion damage and can keep you from catching fire.
- CON saves halve poison, wither and hunger.
- STR saves halve big knockback, and WIS saves halve the Warden's darkness.

### Species traits

- 23 working traits: darkvision, save advantage, resistances, rerolls, a once-a-day last stand, mobs that ignore you and more.

### Social skills

- Press G at a villager, wandering trader or piglin to Persuade, Deceive, Intimidate or Pickpocket.
- Prices, refusals, gossip and iron golems react to how it goes.
- Caught? Witnesses turn on you, golems call for backup, and you can Plead or Lie your way out.
- Intimidate weak hostile mobs into running, or Calm an angry wolf, bee, enderman or golem.
- Passive Insight marks mobs that are after you and warns you of a lit creeper.
- Play a note block or goat horn for a Performance check that charms villagers and piglins.
- Speech bubbles, voices, animations and particles bring every reaction to life.

### Exploration skills

- Athletics for long leaps, tearing through cobwebs and faster climbing.
- Acrobatics to halve fall damage.
- Stealth lets you sneak closer before mobs notice you.
- Perception spots tripwires, and Sleight of Hand disarms them.
- Investigation finds one more item in an already looted chest.
- Nature, Arcana and Religion recall lore about every mob; History about villagers and structures.
- Animal Handling helps you tame, Survival slows hunger, and Medicine shortens poison and other ailments.

### Story and feedback

- Roll results show above the hotbar with a flavor line for every outcome.
- Story death messages when a failed check gets you killed.

### Commands

- `/checks get` for everyone, `/checks help` for a list of commands.
- `/checks set`, `prof`, `roll`, `check`, `level`, `xp` and `reset` for ops.
- `/checks check` and `/checks roll` work with `execute store` in datapack functions.

### Datapacks and config

- Entity profiles set scores, skill bonuses and proficiency per entity type, tag or namespace.
- Species, backgrounds, classes, traits and skills are datapack JSON packs can add, replace or remove.
- Bonus sources: items, armor and effects that give bonuses, advantage or disadvantage.
- Check triggers: use a block, entity or item to roll a check and run a function or loot table.
- Every feature can be switched off on its own in `config/checks/scores.json`.
- Clients without Checks see English text instead of raw translation keys.

### For mod and pack authors

- `ChecksApi` for scores, modifiers, checks, saves and contests, all rolled through Critfall.
- Check events to change or cancel any roll, and levelling events.
- Other mods can add their own sections to the stat screen.
- KubeJS scripts can drive the API; examples in `docs/examples/kubejs/`.
