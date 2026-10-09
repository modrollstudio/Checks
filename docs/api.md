# Public API

Other mods use Checks through one class, `studio.modroll.checks.api.ChecksApi`. Every type in its
signatures is either in `studio.modroll.checks.api`, in Critfall's public API, or vanilla.

Checks only supplies modifiers. Every die is rolled by Critfall's `RollService`, and every roll
returns Critfall's own result wrapped in a small Checks record that adds whether a listener canceled
the roll (see [Rolling](#rolling)). Call it on the server, with server-side entities.

## Stats

A check rolls on a `Stat`, which is either:

- an `Ability`: `STRENGTH`, `DEXTERITY`, `CONSTITUTION`, `INTELLIGENCE`, `WISDOM`, `CHARISMA`
- a `Skill`: a loaded skill, looked up by id with `ChecksApi.skill(id)`

`Proficiency` (`NONE`, `PROFICIENT`, `EXPERTISE`) is the level an entity has in a skill or save.

Skills come from datapacks and change on `/reload`, so look a skill up when you need it instead of
keeping it. `ChecksApi.skill` accepts a namespace-less id (`stealth`) for the standard `checks:`
skills.

## Reading scores

| Method | Value |
| --- | --- |
| `abilityScore(entity, ability)` | the effective score, `1..30` |
| `abilityModifier(entity, ability)` | `floor((score - 10) / 2)` |
| `proficiencyBonus(entity)` | the proficiency bonus, `0..10` |
| `skillProficiency(entity, skill)` | a `Proficiency`: `NONE`, `PROFICIENT` or `EXPERTISE` |
| `saveProficiency(entity, ability)` | a `Proficiency` for that saving throw: `NONE` or `PROFICIENT` |
| `skillBonus(entity, skill)` | the flat skill bonus, `-30..30`; `0` while skills are disabled |
| `skillModifier(entity, skill)` | governing ability modifier + proficiency bonus × (0, 1 or 2) + skill bonus |
| `saveModifier(entity, ability)` | ability modifier + proficiency bonus if proficient in that save |
| `passiveScore(entity, skill)` | `10 +` skill modifier, plus [bonus sources](bonus-sources.md#passive-scores) (flat bonuses, ±5 for advantage or disadvantage) |
| `modifier(entity, stat)` | the ability modifier or skill modifier a check on `stat` adds |

When `skills.enabled` is `false` in `config/checks/scores.json`, every flat skill bonus counts as
`0`. When `proficiency.enabled` is `false`, `proficiencyBonus` is `0` and every proficiency is
`NONE`, so skill and save modifiers drop back to the ability modifier (plus the flat skill bonus).
See [proficiency.md](proficiency.md). While levelling is on, a player's `proficiencyBonus` follows
their level and `abilityScore` includes their chosen ability score improvements.

## Levels

| Method | Value |
| --- | --- |
| `level(player)` | the player's character level, `1..20`; `1` while levelling is disabled |
| `experience(player)` | the player's total character XP; `0` while levelling is disabled |
| `onLevelUp(listener)` | registers a `Consumer<LevelUpEvent>` |

Both reads take a server-side `ServerPlayer`. A `LevelUpEvent` carries the `player`, `oldLevel` and
`newLevel`. It fires on the server thread once per level gained, lowest first, after the new level
is stored: going from level 1 to 4 fires `1 → 2`, `2 → 3`, `3 → 4`. Lowering a level fires nothing.
Register once, while your mod starts.

```java
ChecksApi.onLevelUp(event -> {
    if (event.newLevel() == 5) {
        grantExtraAttack(event.player());
    }
});
```

See [levelling.md](levelling.md).

## Rolling

Every roll method has an overload taking Critfall's `RollMode` (`NORMAL`, `ADVANTAGE`,
`DISADVANTAGE`); without it the roll asks for `NORMAL`. Bonus sources and listeners can add advantage
or disadvantage on top (see [check events](#check-events)).

| Method | Rolls | Returns |
| --- | --- | --- |
| `check(entity, stat, dc)` | d20 + modifier vs `dc` | `CheckRoll` |
| `savingThrow(entity, ability, dc)` | d20 + save modifier vs `dc` | `CheckRoll` |
| `contest(initiator, initiatorStat, opponent, opponentStat)` | d20 + modifier per side | `ContestRoll` |
| `checkAgainstPassive(actor, stat, target, passiveSkill)` | d20 + modifier vs the target's passive score | `CheckRoll` |
| `roll(entity, stat)` | d20 + modifier, no DC | `OpenRoll` |

Each is a record of Critfall's result and a `canceled` flag:

| Record | `result()` | Also |
| --- | --- | --- |
| `CheckRoll` | Critfall's `SaveResult` | `canceled()` |
| `ContestRoll` | Critfall's `ContestResult` | `canceled()` |
| `OpenRoll` | Critfall's `RollResult` | `canceled()`, and `mode()`: the mode the d20 was rolled with |

`SaveResult` and `ContestResult` carry their own `RollDetail` (mode, kept face, dropped face).
`RollResult` does not, so `OpenRoll.mode()` reports the mode actually rolled, after bonus sources and
listeners, which may differ from the one asked for: `RollDetail.of(roll.mode(), roll.result())` gives
the d20s.

Every roll also adds the entity's datapack [bonus sources](bonus-sources.md) and fires the
[check events](#check-events). Of the reads in the table above, only `passiveScore` includes bonus
sources; the modifiers leave them out, since a roll adds them itself.

- **Against a DC** (`check`, `savingThrow`, `checkAgainstPassive`): meeting the DC succeeds
  (`result().saved()`). There is no nat-1/nat-20 rule. A saving throw adds the save modifier
  (with save proficiency); an ability check on the same ability adds the plain ability modifier.
- **Against a passive score**: the target's passive score, its bonus sources included, is the DC and
  the target rolls nothing.
  An active Stealth check that meets the watcher's passive Perception goes unnoticed.
- **Contest**: the mode overload takes a mode per side,
  `contest(initiator, initiatorStat, initiatorMode, opponent, opponentStat, opponentMode)`. Ties go to
  the opponent (Critfall's contest rule), so the initiator wins only with a strictly higher total.
  The initiator rolls first.
- **Open roll**: `result().total()` is d20 + modifier and `result().modifier()` is the modifier plus
  any bonus sources and listener bonuses.

```java
Skill stealth = ChecksApi.skill(ResourceLocation.withDefaultNamespace("stealth")).orElseThrow();
Skill perception = ChecksApi.skill(ResourceLocation.withDefaultNamespace("perception")).orElseThrow();
Skill athletics = ChecksApi.skill(ResourceLocation.withDefaultNamespace("athletics")).orElseThrow();

boolean hidden = ChecksApi.checkAgainstPassive(rogue, stealth, guard, perception).result().saved();
boolean shoved = ChecksApi.contest(attacker, Ability.STRENGTH, target, athletics).result().initiatorWins();
CheckRoll save = ChecksApi.savingThrow(target, Ability.DEXTERITY, 15, RollMode.ADVANTAGE);

OpenRoll sneak = ChecksApi.roll(rogue, stealth);
if (!sneak.canceled()) {
    RollDetail d20s = RollDetail.of(sneak.mode(), sneak.result());
    int kept = d20s.kept();
    int total = sneak.result().total();
}
```

## Check events

| Method | Called |
| --- | --- |
| `onBeforeCheck(listener)` | before every check, save and contest side is rolled, with a `BeforeCheckEvent` |
| `onAfterCheck(listener)` | after it is rolled, with an `AfterCheckEvent` |

Both fire on the server thread for every roll made through `ChecksApi`, so for `/checks roll`,
`/checks check` and [check triggers](check-triggers.md) too, on NeoForge and Fabric alike. A
contest fires one event per side, initiator first; both before events fire before either side rolls.
A listener that throws is logged and skipped. Register once, while your mod starts.

A `BeforeCheckEvent` carries the `entity`, the `stat`, the `kind` (`CHECK`, `SAVE` or `CONTEST`),
the `opponent` (the other contest side, or the entity whose passive score is the DC), the `dc`
(empty for a contest side or an open roll), the stat's own `modifier` and the `bonus` so far, which
starts with the entity's bonus sources. A listener may:

| Call | Effect |
| --- | --- |
| `dc(value)` | changes the DC; throws for a roll without one |
| `addBonus(value)` | adds to the bonus, may be negative |
| `grantAdvantage()` / `imposeDisadvantage()` | adds advantage or disadvantage |
| `cancel()` | rolls nothing |

Advantage and disadvantage combine the 5e way: having both rolls normally, however many sources
grant each. The caller's roll mode and the bonus sources' modes count too. `mode()` is the result.

A canceled roll draws no dice and fires no after event, and its result's `canceled()` is true. Its
Critfall result then holds placeholders a caller can ignore: a check or save fails, a contest goes to
the opponent, and an open roll has no dice and totals 0.

An `AfterCheckEvent` carries the same `entity`, `stat`, `kind` and `opponent`, and a `CheckResult`:
the `roll` (`RollDetail`: mode, kept face, dropped face), `natural()`, `modifier`, `bonus`, `total`,
`dc` and `success` (the DC was met, or this side won the contest; always false for an open roll),
plus `isNatural1()` and `isNatural20()`.

```java
Skill stealth = ChecksApi.skill(ResourceLocation.withDefaultNamespace("stealth")).orElseThrow();
ChecksApi.onBeforeCheck(event -> {
    if (event.stat().equals(stealth) && event.entity().isCrouching()) {
        event.grantAdvantage();
    }
});
ChecksApi.onAfterCheck(event -> {
    if (event.result().isNatural20() && event.entity() instanceof ServerPlayer player) {
        player.sendSystemMessage(Component.literal("Natural 20!"));
    }
});
```

`events.enabled: false` in `config/checks/scores.json` stops calling listeners; rolls go on as if
none were registered.

## Stat screen sections

`StatSheetSections.register(id, section)` adds a section to the [stat screen](stat-screen.md)'s
Overview tab, as a block in its columns after the body. A tall one makes the Overview scroll on small
windows, so keep sections short. It is client side: call it from your client setup. `section` is a
`Supplier<Optional<SheetSection>>` called on the client thread each time the screen opens; return
empty to show nothing this time. A `SheetSection` is a `title` and a list of `SheetRow`s, each a
`label`, a `value` and the `tooltip` lines shown on hover.

```java
StatSheetSections.register(ResourceLocation.fromNamespaceAndPath("mymod", "reputation"), () ->
        MyClientData.reputation().map(rep -> new SheetSection(
                Component.literal("Reputation"),
                List.of(new SheetRow(
                        Component.literal("Villagers"),
                        Component.literal(rep.villagers()),
                        List.of(Component.literal("Better trades at Friendly and above.")))))));
```

Checks only draws the section; the data and getting it to the client are yours. Sections appear in
registration order; registering an id again replaces its section in place. A supplier that throws
is logged and left out. `stat_screen_sections.enabled: false` on the server hides every section.

## KubeJS

Checks does not depend on KubeJS and ships no KubeJS plugin, the same as Critfall. KubeJS exposes
public Java classes to scripts, so a **server script** calls `ChecksApi` with `Java.loadClass` and
registers check event listeners as plain functions:

```js
const ChecksApi = Java.loadClass('studio.modroll.checks.api.ChecksApi')

ChecksApi.onBeforeCheck(event => {
  // An ability's id() is 'dex'; a skill's is its resource location.
  if (String(event.stat().id()) == 'checks:stealth' && event.entity().isCrouching()) {
    event.grantAdvantage()
  }
})
```

Runnable scripts are in [`examples/kubejs/`](examples/kubejs/). `ChecksApi` calls are the stable
part; KubeJS's own event names (`ItemEvents`, `PlayerEvents`, …) vary between KubeJS versions, so
adapt those to yours. Register listeners once: a `/reload` reruns server scripts, so the examples
guard against registering twice.

### Manual check

No automated test loads KubeJS. To check the bindings by hand on a NeoForge dev client
(`./gradlew :neoforge:runClient`) with KubeJS in `neoforge/run/mods`:

1. Copy the three files from `docs/examples/kubejs/` into `neoforge/run/kubejs/server_scripts/`.
2. Start a world and give yourself op.
3. Crouch and run `/checks roll @s stealth`: chat says `Sneaking: advantage on Stealth.` once.
4. Right-click with a stick: chat shows an Athletics check against DC 12, success or failure.
5. Run `/checks roll @s str` until a natural 20 comes up: chat says `Natural 20 on str!`.
6. Run `/reload` and repeat step 3: the line still appears once, so no listener registered twice.

## Testing

The rolls use Critfall's roller, so Critfall's test-only seam forces their faces:
`RollService.setRoller(new DiceRoller(rng))`, then `RollService.resetRoller()`. Faces are drawn in
roll order: both advantage dice of one roll, then the next roll; the initiator before the opponent.

## `/checks roll`

`/checks roll <target> <ability/skill> [dc]` rolls a check through this API and prints it, for
example `Zombie rolls stealth vs DC 15: d20 13 (+5) = 18, success`. Under advantage or disadvantage
it names the mode and shows both d20s: `Zombie rolls stealth vs DC 15: advantage, d20 17 and 4, keeps
17 (+5) = 22, success`. Without a DC it is an open roll.
Its command result is the total, so `execute store result` records it. A roll a listener cancels
prints that it was canceled. Requires op (permission level 2).

## `/checks check`

`/checks check <target> <ability/skill> <dc>` rolls a check through this API for datapacks. A
success prints the roll and returns 1. A failure fails the command with the roll as its error: in
1.21, `execute store success` counts every command that does not fail as a success, whatever it
returns, so failing is what lets both `store success` and `store result` record 0.

```mcfunction
execute store success score @s sneaked run checks check @s stealth 15
execute if score @s sneaked matches 1 run say Nobody noticed.
```

Requires op (permission level 2), which functions have. `check_command.enabled: false` hides it.
