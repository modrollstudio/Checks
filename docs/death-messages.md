# Death messages

A player who dies shortly after a related Checks event gets a story death message instead of
vanilla's, shown to everyone like any death message, on the death screen and in chat:

| Story | Told when the player | Lang pool |
| --- | --- | --- |
| Caught pickpocketing | is killed by an iron golem after being caught picking a pocket | `checks.death.caught_pickpocketing` |
| A natural 1 threat | is killed by any mob after rolling a natural 1 to intimidate | `checks.death.intimidate_natural_one` |
| A failed threat | is killed by an iron golem after failing to intimidate | `checks.death.failed_intimidate` |
| A failed DEX save | is killed by an explosion after failing a DEX save against an explosion | `checks.death.failed_explosion_save` |
| A spent last stand | dies after a last stand (such as Relentless Endurance) already kept them up | `checks.death.last_stand_spent` |

"Shortly after" is within `death_messages.window_ticks` of the event. When more than one story fits,
the first in this table is told. A death is told once: whatever the player went through before it is
forgotten, story or not. Stories are kept in memory only, so a restart forgets them.

Each pool holds three to six lines, `<pool>.1`, `<pool>.2`, …, picked with Minecraft's own
randomness. A line may use the player (`%1$s`), the killer (`%2$s`: "the Iron Golem" through
`checks.name.unnamed`, or its custom name such as "Bob"; `checks.death.killer.unknown` when there is
none; never at the start of a sentence) and the item the story is about (`%3$s`, the item a caught pickpocket reached
for; empty for the other stories). A resource pack can change or add lines. A client without Checks
shows the English line.

## Config

In `config/checks/scores.json` (see [scores-config.md](scores-config.md)):

```json
"death_messages": { "enabled": true, "window_ticks": 1200 }
```

| Key | Value |
| --- | --- |
| `enabled` | the switch for story death messages |
| `window_ticks` | how long after an event a death still tells its story, in ticks; not negative |

## Toggle

`death_messages.enabled: false` gives every death vanilla's message again.
