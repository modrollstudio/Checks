# Stat screen

Press **K** (rebindable under Controls → Critfall: Checks) to open a character sheet with your own
values. Its only actions are the tabs and the buttons at the bottom. Press K again or Escape to close it.

The sheet is one page of muted parchment with two tabs on its top edge, **Overview** and **Skills**.
It reopens on the tab it last showed, and keeps one size across both, so switching never moves it.

- **Header (both tabs):** your face and name; under the name your character's species, background
  and class (`Custom` for none), once it is confirmed and while presets are on; at the right your
  character level and an XP bar towards the next level, while levelling is on, and your proficiency
  bonus. Hovering a preset shows its description, and the species its traits (see
  [presets.md](presets.md)); hovering the level shows your XP, e.g. `700 / 1400 XP`, and how many
  ability score improvements wait.
- **Overview:** a card per ability, the modifier large in gold beside the abbreviation and the score;
  then, in three columns, saving throws, passive Perception, Investigation and Insight, **Body** (hit
  points, armor class and size, each while its switch is on, see [body.md](body.md)) and any
  [sections other mods add](api.md#stat-screen-sections). Each block goes under whichever column is
  shortest so far.
- **Skills:** every loaded skill, datapack-added ones included, with its modifier, down two columns.
  Skills you aren't proficient in are faded.

The sheet grows with the window up to a maximum width and fits inside it at every GUI scale Minecraft
allows, down to 854×480 at GUI scale 2 (427×240 scaled). When a tab's content is taller than the
window allows, everything under the header scrolls with the mouse wheel: the Overview a line at a
time, so a tall section from another mod never runs off-screen, and the skills a row at a time.

Until your character is confirmed, a **Create Character** button at the bottom reopens character
creation (see [character-creation.md](character-creation.md)). While levelling is on, a **Levels**
button opens the panel listing what each level gave; it reads **Level up!** while an ability score
improvement or Expertise waits to be chosen there (see [levelling.md](levelling.md#the-level-up-panel)).

A dot before each save and skill shows proficiency: hollow for none, one filled for proficient, two
filled for expertise. Hovering a dot or the Saving Throws heading shows this legend.

Hovering shows a short explanation in a tooltip:

- **Ability card:** what the ability covers; the big number is the modifier added to rolls, the
  small one the score. Then the [bonus sources](bonus-sources.md) on that ability's checks, e.g.
  `Ability checks: Gauntlets +1`, and the attribute extras its modifier changes, e.g.
  `Mining speed +15%`.
- **Proficiency bonus:** added to rolls you're proficient in, twice with expertise.
- **Save:** what saves are for, and the breakdown, e.g. `CON +0, proficient +3 = +3`.
- **Skill:** its governing ability, a one-line description of the skill, and the breakdown, e.g.
  `DEX +2, expertise +6, bonus +2 = +10`.
  Parts that are zero are left out, except the ability modifier.
- **Save and skill bonus sources:** each source's flat bonus joins the breakdown (`Elven Boots +1`),
  a line names each source granting advantage or disadvantage (`Advantage from Elven Boots.`), and
  having both says they cancel out. The save and skill modifiers shown include the flat bonuses, as
  a roll would. See [bonus-sources.md](bonus-sources.md#stat-screen).
- **Hit points:** your max health; what CON adds (`CON +3: +6 max health`) and what the levels after
  the first add with your class's hit die (`Levels after the first (d10 Hit Die): +4 max health`).
- **Hit Dice:** `3/5 d8`, the dice left of your pool. The tooltip is short: `Hit Dice: 3/5 (d8)`,
  `Rest at a lit campfire to heal with them.` and what sleeping does, by the server's settings, such as
  `Sleep to heal fully and get your Hit Dice back.` The details are in
  [rests.md](rests.md).
- **Armor class:** where Critfall's AC comes from, as Critfall breaks it down: the base 10 and what
  the armor and toughness points add (`Armor 13: +3`), then the DEX part and the body armor rule that
  limits it, e.g. `DEX +2 (medium body armor, max +2)`. When the body armor's cap would lower AC, a
  last line adds back what keeps it at the AC without the body armor
  (`Never below your AC without body armor: +2`). An AC Critfall did not derive from the armor, such
  as an entity profile's, shows as one line (`Set by Critfall: AC 15`).
- **Size:** the scale your species gives, e.g. `Scale ×0.6`.
- **Passive:** `10 +` skill modifier plus bonus sources (flat bonuses, +5 for advantage, -5 for
  disadvantage), used when you're not actively rolling.

The server builds the sheet through `ChecksApi` when you press the key and sends it to your client;
the client only displays it. So it always matches `/checks get` for you, bonus sources included, and
follows the `skills.enabled` and `proficiency.enabled` toggles like every other read.

## Sections from other mods

On the Overview tab, after the body, the sheet shows any sections other mods add: a title and rows of
label and value, each row with its own tooltip. Their mods supply and sync the data; see
[api.md](api.md#stat-screen-sections). `stat_screen_sections.enabled: false` on the server hides
them.

## Names

Ability and skill names, tooltips and the legend come from the lang file. A datapack skill
`<namespace>:<path>` is named by the key `checks.skill.<namespace>.<path>` (a `/` in the path becomes `.`), so a resource pack can
translate it:

```json
{
  "checks.skill.mypack.lockpicking": "Lockpicking",
  "checks.skill.mypack.lockpicking.desc": "Opening locks without the key."
}
```

The `.desc` key is the skill's tooltip description, here and on the character creation screen. A
skill without one shows no description line. A `.use` key adds a last line saying what Checks itself
uses the skill for; every shipped skill has one (`In this mod: …`), from its
[social](social.md#stat-screen) or [exploration](exploration.md#stat-screen) uses.

Without a translation, the last path segment is shown in title case (`mypack:lore/old_runes` →
`Old Runes`).

## Toggle

`stat_screen.enabled: false` in `config/checks/scores.json` makes the server ignore the key, so no
sheet opens. See [scores-config.md](scores-config.md).
