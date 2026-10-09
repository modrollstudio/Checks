# Critfall: Checks

Ability scores, skills and saves for Minecraft, built on [Critfall](https://github.com/modrollstudio/Critfall)'s
d20 engine.

Checks adds the character sheet that Critfall leaves out: six ability scores, skills, saving throws
and a proficiency bonus for every player and mob, with every die rolled through Critfall. New
players build a character on first join and level from 1 to 20 as they play. Critfall works
without it.

## Requirements

- Minecraft 1.21.1 with NeoForge 21.1+, or Fabric Loader 0.16.9+ with Fabric API
- [Critfall](https://github.com/modrollstudio/Critfall) 0.2.10 or newer
- The Checks jar for your loader, on the server and every client

## Features

- **Ability scores, skills and saves**: STR, DEX, CON, INT, WIS and CHA for every living entity,
  the 18 standard skills plus any a datapack adds, saving throws, passive scores and proficiency.
  Mobs, modded ones included, get scores from their attributes.
- **Character creation**: species, background and class, then standard array, point buy or dice
  rolled through Critfall, then skills.
- **Levels 1–20**: character XP from vanilla XP and advancements, with ability score improvements.
- **Stat screen**: a character sheet on a key (K by default) with a breakdown of every bonus.
- **Combat and body**: ability modifiers feed Critfall's attacks, damage, AC and saves; CON raises
  max health, and species set your size.
- **Saves against the world**: explosions, fire, poison, wither, knockback and the Warden's
  darkness call for a saving throw.
- **Skills in play**: talk, haggle, lie, threaten or pickpocket villagers, traders and piglins;
  leap, climb, sneak, disarm tripwires, search chests and recall monster lore.
- **Species traits**: darkvision, resistances, rerolls, a last stand and more.
- **Datapacks and API**: presets, skills, mob scores, bonus sources and check triggers are datapack
  JSON, and other mods read scores and roll checks through `ChecksApi`.

Every feature can be switched off on its own in `config/checks/scores.json`.

## Docs

- Config: [docs/scores-config.md](docs/scores-config.md)
- Datapacks: [skills](docs/skills.md), [entity profiles](docs/entity-score-profiles.md),
  [species, backgrounds and classes](docs/presets.md), [traits](docs/traits.md),
  [bonus sources](docs/bonus-sources.md), [check triggers](docs/check-triggers.md)
- API: [docs/api.md](docs/api.md)
- Everything else is in [docs/](docs).

## Building

```
./gradlew build    # checks-neoforge-*.jar and checks-fabric-*.jar
```

## License

MIT © 2026 Modroll Studio. See [LICENSE](LICENSE) and [NOTICE](NOTICE).

This work includes material from the System Reference Document 5.2 ("SRD 5.2") by Wizards of the
Coast LLC, available at https://www.dndbeyond.com/srd. The SRD 5.2 is licensed under the Creative
Commons Attribution 4.0 International License, available at
https://creativecommons.org/licenses/by/4.0/legalcode.

[modroll.studio](https://modroll.studio)
