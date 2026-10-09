# Character creation

A player's first join opens a **Create Your Character** screen. It walks through six pages with
**Back** and **Next**:

1. **Species**, 2. **Background**, 3. **Class**: pick one from the icon tiles on the left, or
   Custom (the name tag); hovering a tile names it. Below the tiles, what the character has so far.
   On the right, the selected one under a gold banner: its details, then its description. Each
   preset's icon comes from its file; see [presets.md](presets.md).
4. **Scores**: the abilities on the left; on the right the method, what it means, the scores still
   to place and what is left of the background's bonuses.
5. **Skills**: pick skill proficiencies, in two columns.
6. **Confirm**: the character on a sheet of parchment: a short story in the second person ("You are
   Dev1, Dwarf by birth, Soldier by upbringing and Fighter by calling. Your greatest strengths are
   …"), naming the species, background and class (a Custom one is left out), the two best abilities
   and the proficient skills, then the abilities, saving throws and starting kit. **Confirm** locks the
   character; **Start Over** begins again from the first page with nothing chosen (rolled scores stay
   rolled).

Every page is the same size, so nothing moves on Back or Next; the screen grows with the window up to
a maximum. **Next** is disabled until the page is complete; its tooltip says what is missing. With presets
turned off (`creation.presets.enabled: false`), the first three pages are skipped.

- **Skip** (or Escape) asks once for confirmation, then closes the screen and keeps the default
  scores. Until the character is confirmed, a **Create Character** button on the stat screen (K)
  reopens it. It does not open by itself on later joins.
- `/checks reset <player>` (op) clears the character, rolls included, and the player's level and XP
  (see [levelling.md](levelling.md)). The screen opens again on that player's next join.

## Methods

`creation.methods` in `config/checks/scores.json` lists the methods offered (default: all four).

| Method | Scores |
| --- | --- |
| Standard Array | `15, 14, 13, 12, 10, 8`, each given to one ability |
| Point Buy | every score starts at 8; 27 points; at most 15; 5e costs |
| Roll | 4d6, drop the lowest, six times, assigned freely |
| Hardcore | 3d6 six times, straight down STR → CHA, no assigning |

Point-buy costs: 8 → 0, 9 → 1, 10 → 2, 11 → 3, 12 → 4, 13 → 5, 14 → 7, 15 → 9. Points may be left
unspent.

## The scores page

- A line under the method buttons explains the selected method.
- **Array and Roll:** the six values sit in a row of chips under "Scores to place"; once all are
  placed it says so, and the chips' place stays so nothing moves. Click a chip, then click an ability to
  place it there. Placing onto a filled ability returns its old value to the pool, and clicking a
  placed value returns it. Empty abilities show an empty slot, and modifiers update as values land.
- **Point Buy:** - / + per ability, disabled when the step is not allowed, with points left shown as
  `Points left: 3/27`.
- **Roll and Hardcore** show each rolled die, the dropped die struck through, next to the ability it
  went to. Hovering a roll chip shows its dice before it is placed.
- **Use Suggested** (array and roll, once a class is chosen) puts the highest value in the class's
  main ability, the next highest in the next one, and so on down. They can still be moved
  afterwards.
- **Background bonuses:** each of the background's abilities gets its own - / + next to its score,
  shown as `15 +2 = 17`. Under "Background bonus" a line says what is left ("Bonuses left: +1", "All
  bonuses used"), and another says the allowed splits in plain words, e.g. "Raise one of these by
  2 and another by 1, or all three by 1." A + is disabled when one more point would not fit any
  split or would pass the score cap, and its tooltip says why.
- Hovering an ability or skill shows the same description as the stat screen.

## Rolling

Every die is rolled on the server through Critfall's `RollService`. Checks never rolls dice itself.
The client only asks for a roll.

- **Rolls are fixed.** The six totals are saved the moment they are rolled. Reopening the screen,
  relogging, skipping or asking again never rerolls; only `/checks reset` clears them.
- **Rolling commits the method.** After rolling for Roll or Hardcore, the character must be built
  from those rolls. Otherwise a player could roll, dislike the result and switch to the standard
  array.
- **Every roll is broadcast** to all players, each die shown and the dropped die struck through.
  Hardcore rolls are labelled by ability, free rolls are numbered until assigned:
  - Steve rolled STR: 1, 1, 2, ~~1~~ = 4
  - Steve rolled #2: 6, 5, 3, ~~2~~ = 14

## Skills

The page groups skills by where they come from: "From your background (Miner):" with its fixed
skills, "Choose 2 from your class (Barbarian):" with the class list, and, when background and class
overlap, a line explaining the free pick and an "Or any other skill" group (see
[presets.md](presets.md#skills)). Every skill's tooltip says where it comes from.

A species whose traits give skill choices adds a "Choose 1 from your species (Elf):" group first,
with its own picks; they never repeat a background or class skill (see [traits.md](traits.md)).

With a Custom background, the player picks `creation.skill_choices` skills (default 2) from every
loaded skill, datapack skills included, on top of any class picks. Picked and background skills
become `proficient`. If fewer skills are loaded than the count, the player picks all of them.

## Validation

The client only sends choices. The server checks the submission against the same rules the screen
was built from:

- standard array: the scores are exactly the array's values
- point buy: every score is in the cost table and the total cost fits the budget
- roll: the scores are exactly the saved rolls, in any order
- hardcore: the scores are the saved rolls, in the rolled order
- the species, background and class are offered (or Custom)
- background bonuses: only on the background's abilities, split as an allowed option, never past
  `creation.presets.bonus_max_score`; none with a Custom background
- skills: exactly the required count, all distinct, all loaded, none the background already gives,
  and from the class list except for the overlap allowance
- the method is allowed and, after rolling, is the rolled one

A refused submission saves nothing and hands out no kit. The player gets a chat line saying why, and
the screen reopens. An accepted one hands out the background's starting kit, once.

## How a character ranks

Confirmed scores, with their background bonuses, become the player's base scores. Highest first:

1. `/checks set <player> <ability> <n>` (an op's override)
2. the confirmed character
3. a datapack profile matching `minecraft:player`
4. `player_defaults`

A player's own choice beats a pack-wide player profile, and an op can still override any score. A
reset clears only the character; `/checks set` values stay. Picked and background skills, and the
class's saving throws, rank the same way for proficiency: `/checks prof` beats them, and they beat a
profile.

## Config

```json
"creation": {
  "enabled": true,
  "methods": ["standard_array", "point_buy", "roll", "hardcore"],
  "skill_choices": 2,
  "standard_array": [15, 14, 13, 12, 10, 8],
  "point_buy": {
    "budget": 27,
    "costs": { "8": 0, "9": 1, "10": 2, "11": 3, "12": 4, "13": 5, "14": 7, "15": 9 }
  },
  "roll_dice": "4d6kh3",
  "hardcore_dice": "3d6",
  "presets": {
    "enabled": true,
    "include_shipped": true,
    "bonus_options": [[2, 1], [1, 1, 1]],
    "bonus_max_score": 20
  }
}
```

- `methods`: unknown ids are skipped with a warning. A list with no known method allows them all.
- `standard_array`: six scores in `1..30`, or the default is used.
- `point_buy.costs`: score → cost. The lowest listed score is the starting score and the highest is
  the cap. Entries that are not a score in `1..30` with a cost of `0` or more are skipped.
- `roll_dice`, `hardcore_dice`: Critfall dice expressions, rolled once per ability. `kh`/`kl` keep
  the highest or lowest dice, and the rest show as dropped.
- `presets`: species, backgrounds and classes; see [presets.md](presets.md#config).

## Toggle

`creation.enabled: false` turns creation off: nothing opens on join, the stat screen shows no
button, and submissions are refused. Confirmed characters are ignored while it is off, so every
player resolves exactly as without character creation. They are kept in the save and apply again
when creation is turned back on. A player who joins while it is off is still offered creation on
their first join after it is turned on.

## Persistence

Characters (prompted flag, rolls with every die, and the confirmed build with its species,
background, class and what they granted) are stored per player UUID in the world save as
`data/checks_characters.dat`.
