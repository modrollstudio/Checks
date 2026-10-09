package studio.modroll.checks.sheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.ModLang;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.api.SheetSection;
import studio.modroll.checks.body.ArmorCategory;
import studio.modroll.checks.body.Extra;
import studio.modroll.checks.body.Size;
import studio.modroll.checks.body.SizeOption;
import studio.modroll.checks.bonus.BonusPart;
import studio.modroll.checks.skill.ShippedSkills;
import studio.modroll.critfall.api.dice.RollMode;

class SheetTooltipsTest {

    private static final StatSheet.AbilityLine CON = new StatSheet.AbilityLine(
            Ability.CONSTITUTION, 10, 0, 3, Proficiency.PROFICIENT, StatSheet.AbilityBonuses.NONE);
    private static final StatSheet SHEET = new StatSheet(
            3,
            List.of(
                    new StatSheet.AbilityLine(
                            Ability.DEXTERITY, 14, 2, 2, Proficiency.NONE, StatSheet.AbilityBonuses.NONE),
                    CON,
                    new StatSheet.AbilityLine(
                            Ability.WISDOM, 12, 1, 1, Proficiency.NONE, StatSheet.AbilityBonuses.NONE)),
            List.of(),
            List.of(),
            false,
            Optional.empty(),
            Optional.empty(),
            true,
            StatSheet.Body.NONE);

    private final Language before = Language.getInstance();

    @BeforeEach
    void loadModLang() throws IOException {
        ModLang.inject();
    }

    @AfterEach
    void restoreLang() {
        Language.inject(before);
    }

    @Test
    void skillBreakdownListsEveryNonZeroPart() {
        StatSheet.SkillLine stealth = new StatSheet.SkillLine(
                ResourceLocation.parse("checks:stealth"), Ability.DEXTERITY, 10, Proficiency.EXPERTISE, 2, List.of());

        assertEquals(
                List.of(
                        "Uses Dexterity.",
                        "Moving unseen and unheard.",
                        "In this mod: while you sneak, mobs whose passive Perception your passive Stealth beats"
                                + " notice you from closer.",
                        "DEX +2, expertise +6, bonus +2 = +10"),
                strings(SheetTooltips.skill(stealth, SHEET)));
    }

    @Test
    void theLegendExplainsEveryDot() {
        assertEquals(
                List.of(
                        "The dot before a save or skill shows proficiency.",
                        "Hollow dot: not proficient",
                        "One dot: proficient",
                        "Two dots: expertise"),
                strings(SheetTooltips.legend()));
    }

    @Test
    void skillBreakdownOmitsZeroParts() {
        StatSheet.SkillLine perception = new StatSheet.SkillLine(
                ResourceLocation.parse("checks:perception"), Ability.WISDOM, 1, Proficiency.NONE, 0, List.of());

        assertEquals(
                "WIS +1 = +1", strings(SheetTooltips.skill(perception, SHEET)).getLast());
    }

    @Test
    void bonusSourcesJoinTheBreakdownAndNameTheirModes() {
        StatSheet.SkillLine stealth = new StatSheet.SkillLine(
                ResourceLocation.parse("checks:stealth"),
                Ability.DEXTERITY,
                5,
                Proficiency.NONE,
                0,
                List.of(
                        new BonusPart(ResourceLocation.parse("pack:elven_boots"), 0, RollMode.ADVANTAGE),
                        new BonusPart(ResourceLocation.parse("pack:shadow_cloak"), 3, RollMode.NORMAL),
                        new BonusPart(ResourceLocation.parse("pack:heavy/plate_armor"), 0, RollMode.DISADVANTAGE)));

        assertEquals(
                List.of(
                        "DEX +2, Shadow Cloak +3 = +5",
                        "Advantage from Elven Boots.",
                        "Disadvantage from Plate Armor.",
                        "Advantage and disadvantage cancel out: rolls normally."),
                strings(SheetTooltips.skill(stealth, SHEET)).subList(3, 7));
    }

    @Test
    void saveAndAbilityTooltipsListTheirBonusSources() {
        StatSheet.AbilityLine dex = new StatSheet.AbilityLine(
                Ability.DEXTERITY,
                14,
                2,
                4,
                Proficiency.NONE,
                new StatSheet.AbilityBonuses(
                        List.of(new BonusPart(ResourceLocation.parse("pack:gloves"), 1, RollMode.NORMAL)),
                        List.of(new BonusPart(ResourceLocation.parse("pack:cloak"), 2, RollMode.ADVANTAGE))));

        assertEquals(
                List.of("DEX +2, Cloak +2 = +4", "Advantage from Cloak."),
                strings(SheetTooltips.save(dex, SHEET.proficiencyBonus())).subList(1, 3));
        assertEquals(
                "Ability checks: Gloves +1",
                strings(SheetTooltips.ability(dex, List.of())).get(2));
    }

    @Test
    void datapackSkillWithoutADescriptionShowsOnlyItsAbility() {
        assertEquals(
                List.of("Uses Dexterity."),
                strings(SheetTooltips.about(ResourceLocation.parse("mypack:lockpicking"), Ability.DEXTERITY)));
    }

    @Test
    void aSkillWithASocialUseEndsWithItsInThisModLine() {
        assertEquals(
                List.of(
                        "Uses Charisma.",
                        "Convincing others through charm, reason or good manners.",
                        "In this mod: haggle with villagers and wandering traders for better prices, plead your way out"
                                + " of an alarm, or calm angry wolves, bees, endermen, zombified piglins and iron golems."),
                strings(SheetTooltips.about(ResourceLocation.parse("checks:persuasion"), Ability.CHARISMA)));
    }

    @Test
    void everyShippedSkillHasAnInThisModLine() {
        for (ResourceLocation skill : ShippedSkills.load(warning -> {}).keySet()) {
            assertTrue(SheetText.skillUse(skill).isPresent(), skill + " has no use line");
        }
        assertTrue(SheetText.skillUse(ResourceLocation.parse("mypack:lore/old_runes"))
                .isEmpty());
    }

    @Test
    void everyShippedSkillHasADescription() {
        for (ResourceLocation skill : ShippedSkills.load(warning -> {}).keySet()) {
            assertTrue(SheetText.skillDescription(skill).isPresent(), skill + " has no description");
        }
    }

    @Test
    void saveBreakdownKeepsAZeroAbilityModifier() {
        assertEquals(
                "CON +0, proficient +3 = +3",
                strings(SheetTooltips.save(CON, SHEET.proficiencyBonus())).get(1));
    }

    @Test
    void levelTooltipShowsXpTowardsTheNextLevelAndPendingImprovements() {
        assertEquals(
                List.of("700 / 1400 XP", "Ability score improvements to choose: 2"),
                strings(SheetTooltips.level(levelLine(5, 700, 650, Optional.of(1400), 2))));
    }

    @Test
    void levelTooltipAtTheTopLevelHasNoNextThreshold() {
        assertEquals(
                List.of("36000 XP (max level)"),
                strings(SheetTooltips.level(levelLine(20, 36000, 35500, Optional.empty(), 0))));
    }

    @Test
    void levelProgressRunsFromThisThresholdToTheNext() {
        assertEquals(0f, levelLine(5, 650, 650, Optional.of(1400), 0).progress());
        assertEquals(0.5f, levelLine(5, 1025, 650, Optional.of(1400), 0).progress());
        assertEquals(0f, levelLine(5, 100, 650, Optional.of(1400), 0).progress(), "XP below the level clamps");
        assertEquals(1f, levelLine(20, 35500, 35500, Optional.empty(), 0).progress());
    }

    @Test
    void armorClassNamesTheDexPartAndItsArmorRule() {
        assertEquals(
                List.of(
                        "What an attack roll must reach to hit you.",
                        "Armor and toughness: AC 12",
                        "DEX +2 (medium armor, max +2)"),
                strings(SheetTooltips.armorClass(new StatSheet.ArmorLine(14, 12, 4, ArmorCategory.MEDIUM, 2, 2))));
        assertEquals(
                "DEX +0 (heavy armor, no DEX)",
                strings(SheetTooltips.armorClass(new StatSheet.ArmorLine(19, 19, 4, ArmorCategory.HEAVY, 0, 2)))
                        .getLast());
        assertEquals(
                "DEX -1 (unarmored)",
                strings(SheetTooltips.armorClass(new StatSheet.ArmorLine(9, 10, -1, ArmorCategory.NONE, -1, 2)))
                        .getLast());
        assertEquals(
                "DEX +3 (light armor)",
                strings(SheetTooltips.armorClass(new StatSheet.ArmorLine(14, 11, 3, ArmorCategory.LIGHT, 3, 2)))
                        .getLast());
    }

    @Test
    void healthShowsConAndLevelsOnlyWhenTheyAddSomething() {
        assertEquals(
                List.of("Your maximum health.", "CON +3: +6 max health", "Levels gained: +4 max health"),
                strings(SheetTooltips.health(new StatSheet.HealthLine(30f, 3, 6.0, 4.0))));
        assertEquals(
                List.of("Your maximum health.", "CON -1: -1.5 max health"),
                strings(SheetTooltips.health(new StatSheet.HealthLine(18.5f, -1, -1.5, 0.0))));
    }

    @Test
    void bodySectionHasARowPerPartThatIsOn() {
        StatSheet.Body body = new StatSheet.Body(
                Optional.of(new StatSheet.HealthLine(26f, 3, 6.0, 0.0)),
                Optional.empty(),
                Optional.of(new SizeOption(Size.SMALL, 0.6)),
                List.of());

        SheetSection section = SheetTooltips.body(body).orElseThrow();

        assertEquals("Body", section.title().getString());
        assertEquals(
                List.of("Hit Points 26", "Size Small"),
                section.rows().stream()
                        .map(row -> row.label().getString() + " " + row.value().getString())
                        .toList());
        assertEquals("Scale ×0.6", section.rows().getLast().tooltip().getLast().getString());
        assertEquals(Optional.empty(), SheetTooltips.body(StatSheet.Body.NONE));
    }

    @Test
    void abilityTooltipListsItsNonZeroExtras() {
        List<StatSheet.ExtraLine> extras = List.of(
                new StatSheet.ExtraLine(Extra.BREATH, 0.6),
                new StatSheet.ExtraLine(Extra.EXHAUSTION, 0.15),
                new StatSheet.ExtraLine(Extra.KNOCKBACK, 0.3),
                new StatSheet.ExtraLine(Extra.MINING_SPEED, 0.0));
        StatSheet.AbilityLine strength =
                new StatSheet.AbilityLine(Ability.STRENGTH, 10, 0, 0, Proficiency.NONE, StatSheet.AbilityBonuses.NONE);

        assertEquals(
                List.of("Breath underwater +60%", "Hunger from exhaustion -15%"),
                strings(SheetTooltips.ability(CON, extras)).subList(2, 4));
        assertEquals(
                List.of("Knockback dealt +0.3"),
                strings(SheetTooltips.ability(strength, extras)).subList(2, 3));
    }

    private static StatSheet.LevelLine levelLine(
            int level, int xp, int levelXp, Optional<Integer> nextLevelXp, int pending) {
        return new StatSheet.LevelLine(
                level,
                xp,
                levelXp,
                nextLevelXp,
                new StatSheet.Improvements(pending, List.of(List.of(2), List.of(1, 1)), 20));
    }

    private static List<String> strings(List<Component> lines) {
        return lines.stream().map(Component::getString).toList();
    }
}
