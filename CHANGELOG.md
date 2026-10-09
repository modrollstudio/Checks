# Changelog

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
