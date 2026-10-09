package studio.modroll.checks.bonus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.CheckKind;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.data.MatchEntry;
import studio.modroll.checks.data.SkillStore;
import studio.modroll.checks.skill.ShippedSkills;
import studio.modroll.critfall.api.dice.RollMode;

class BonusSourceTest {

    private static final Set<String> ITEMS = Set.of("minecraft:leather_boots", "minecraft:iron_sword");
    private static final Set<String> EFFECTS = Set.of("minecraft:invisibility", "minecraft:slowness");

    private final List<String> warnings = new ArrayList<>();
    private Map<ResourceLocation, Skill> skills;

    @BeforeEach
    void loadSkills() {
        skills = ShippedSkills.load(w -> {});
        SkillStore.setSkills(skills);
    }

    @AfterEach
    void reset() {
        SkillStore.clear();
    }

    private BonusSource parse(String id, String json) {
        return BonusSource.parse(
                ResourceLocation.parse(id),
                JsonParser.parseString(json).getAsJsonObject(),
                SkillStore::find,
                item -> ITEMS.contains(item.toString()),
                effect -> EFFECTS.contains(effect.toString()),
                warnings::add);
    }

    private Skill skill(String path) {
        return skills.get(ResourceLocation.parse("checks:" + path));
    }

    /** Wears exactly {@code item} in {@code slot} and has exactly {@code effects}. */
    private static BonusSource.Bearer bearer(EquipmentSlot slot, String item, Set<String> effects) {
        return new BonusSource.Bearer() {
            @Override
            public boolean wears(List<EquipmentSlot> slots, MatchEntry match) {
                return slots.contains(slot) && match.matches(ResourceLocation.parse(item), tag -> false);
            }

            @Override
            public boolean hasEffect(ResourceLocation effect) {
                return effects.contains(effect.toString());
            }
        };
    }

    @Test
    void docsExampleIsBootsWithAdvantageOnStealth() throws IOException {
        BonusSource boots = parse("pack:elven_boots", Files.readString(Path.of("../docs/examples/bonus_source.json")));

        assertEquals(List.of(), warnings);
        assertEquals(RollMode.ADVANTAGE, boots.mode());
        assertEquals(0, boots.bonus());
        BonusSource.Bearer wearing = bearer(EquipmentSlot.FEET, "minecraft:leather_boots", Set.of());
        assertTrue(boots.appliesTo(wearing, CheckKind.CHECK, skill("stealth")));
        assertFalse(boots.appliesTo(wearing, CheckKind.CHECK, skill("acrobatics")));
        BonusSource.Bearer holding = bearer(EquipmentSlot.MAINHAND, "minecraft:leather_boots", Set.of());
        assertFalse(boots.appliesTo(holding, CheckKind.CHECK, skill("stealth")), "boots must be worn as armor");
    }

    @Test
    void mainhandItemGivesAFlatBonus() {
        BonusSource sword = parse(
                "pack:sword",
                "{\"source\": {\"item\": \"minecraft:iron_sword\", \"slot\": \"mainhand\"},"
                        + " \"applies_to\": {\"skills\": [\"athletics\"]}, \"bonus\": 2}");

        assertTrue(sword.appliesTo(
                bearer(EquipmentSlot.MAINHAND, "minecraft:iron_sword", Set.of()), CheckKind.CHECK, skill("athletics")));
        assertFalse(sword.appliesTo(
                bearer(EquipmentSlot.OFFHAND, "minecraft:iron_sword", Set.of()), CheckKind.CHECK, skill("athletics")));
        assertEquals(new BonusPart(sword.id(), 2, RollMode.NORMAL), sword.part());
    }

    @Test
    void itemTagSourcesAreNotCheckedAgainstTheItemRegistry() {
        BonusSource tagged = parse(
                "pack:any_boots",
                "{\"source\": {\"item\": \"#minecraft:foot_armor\", \"slot\": \"armor\"},"
                        + " \"applies_to\": {\"all_checks\": true}, \"bonus\": 1}");

        assertInstanceOf(MatchEntry.Tag.class, ((BonusSource.Equipped) tagged.condition()).item());
    }

    @Test
    void effectSourceAppliesWhileTheEffectIsActive() {
        BonusSource invisible = parse(
                "pack:invisible",
                "{\"source\": {\"effect\": \"minecraft:invisibility\"}, \"applies_to\": {\"saves\": [\"dex\"]},"
                        + " \"bonus\": 3}");

        BonusSource.Bearer active = bearer(EquipmentSlot.MAINHAND, "minecraft:air", Set.of("minecraft:invisibility"));
        assertTrue(invisible.appliesTo(active, CheckKind.SAVE, Ability.DEXTERITY));
        assertFalse(invisible.appliesTo(active, CheckKind.CHECK, Ability.DEXTERITY), "a save target is not a check");
        assertFalse(invisible.appliesTo(
                bearer(EquipmentSlot.MAINHAND, "minecraft:air", Set.of()), CheckKind.SAVE, Ability.DEXTERITY));
    }

    @Test
    void abilityTargetCoversItsSkillsAndContestsButNotSaves() {
        BonusTargets strength = new BonusTargets(Set.of(), Set.of(Ability.STRENGTH), Set.of(), false, false);

        assertTrue(strength.appliesTo(CheckKind.CHECK, Ability.STRENGTH));
        assertTrue(strength.appliesTo(CheckKind.CHECK, skill("athletics")));
        assertTrue(strength.appliesTo(CheckKind.CONTEST, skill("athletics")));
        assertFalse(strength.appliesTo(CheckKind.CHECK, skill("stealth")));
        assertFalse(strength.appliesTo(CheckKind.SAVE, Ability.STRENGTH));
    }

    @Test
    void allChecksAndAllSavesStaySeparate() {
        BonusTargets checks = new BonusTargets(Set.of(), Set.of(), Set.of(), true, false);
        BonusTargets saves = new BonusTargets(Set.of(), Set.of(), Set.of(), false, true);

        assertTrue(checks.appliesTo(CheckKind.CHECK, skill("stealth")));
        assertFalse(checks.appliesTo(CheckKind.SAVE, Ability.WISDOM));
        assertTrue(saves.appliesTo(CheckKind.SAVE, Ability.WISDOM));
        assertFalse(saves.appliesTo(CheckKind.CHECK, Ability.WISDOM));
    }

    @Test
    void situationalBonusSumsBonusesAndKeepsBothModes() {
        BonusSource boots = parse(
                "pack:a_boots",
                "{\"source\": {\"item\": \"minecraft:leather_boots\", \"slot\": \"armor\"},"
                        + " \"applies_to\": {\"skills\": [\"stealth\"]}, \"bonus\": 1, \"mode\": \"advantage\"}");
        BonusSource slow = parse(
                "pack:b_slow",
                "{\"source\": {\"effect\": \"minecraft:slowness\"}, \"applies_to\": {\"abilities\": [\"dex\"]},"
                        + " \"bonus\": 2, \"mode\": \"disadvantage\"}");
        BonusSource.Bearer both = bearer(EquipmentSlot.FEET, "minecraft:leather_boots", Set.of("minecraft:slowness"));

        SituationalBonus bonus = SituationalBonus.of(List.of(boots, slow), both, CheckKind.CHECK, skill("stealth"));

        assertEquals(3, bonus.bonus());
        assertTrue(bonus.advantage());
        assertTrue(bonus.disadvantage());
        assertEquals(List.of(boots.part(), slow.part()), bonus.parts());
    }

    @Test
    void passiveScoresAddFlatBonusesAndFiveForAdvantage() {
        ResourceLocation id = ResourceLocation.parse("pack:source");
        BonusPart plusTwo = new BonusPart(id, 2, RollMode.NORMAL);
        BonusPart advantage = new BonusPart(id, 0, RollMode.ADVANTAGE);
        BonusPart disadvantage = new BonusPart(id, 0, RollMode.DISADVANTAGE);

        assertEquals(0, SituationalBonus.NONE.passiveBonus(5));
        assertEquals(2, new SituationalBonus(List.of(plusTwo)).passiveBonus(5));
        assertEquals(5, new SituationalBonus(List.of(advantage, advantage)).passiveBonus(5));
        assertEquals(-5, new SituationalBonus(List.of(disadvantage)).passiveBonus(5));
        assertEquals(0, new SituationalBonus(List.of(advantage, disadvantage)).passiveBonus(5));
        assertEquals(2, new SituationalBonus(List.of(plusTwo, advantage, disadvantage)).passiveBonus(5));
        assertEquals(5, new SituationalBonus(List.of(plusTwo, advantage)).passiveBonus(3));
    }

    @Test
    void unknownSkillTargetIsSkippedWithAWarning() {
        BonusSource source = parse(
                "pack:mixed",
                "{\"source\": {\"effect\": \"minecraft:slowness\"},"
                        + " \"applies_to\": {\"skills\": [\"stealth\", \"juggling\"]}, \"bonus\": -1}");

        assertEquals(
                Set.of(ResourceLocation.parse("checks:stealth")),
                source.targets().skills());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("juggling")));
    }

    @Test
    void badFilesAreRejected() {
        String targets = ", \"applies_to\": {\"all_checks\": true}";
        assertRejected("{\"source\": {}" + targets + ", \"bonus\": 1}", "exactly one");
        assertRejected(
                "{\"source\": {\"effect\": \"minecraft:slowness\", \"item\": \"minecraft:iron_sword\"}" + targets
                        + ", \"bonus\": 1}",
                "exactly one");
        assertRejected("{\"source\": {\"item\": \"minecraft:iron_sword\"}" + targets + ", \"bonus\": 1}", "slot");
        assertRejected(
                "{\"source\": {\"item\": \"minecraft:iron_sword\", \"slot\": \"belt\"}" + targets + ", \"bonus\": 1}",
                "slot");
        assertRejected(
                "{\"source\": {\"item\": \"minecraft:gold_crown\", \"slot\": \"armor\"}" + targets + ", \"bonus\": 1}",
                "unknown item");
        assertRejected("{\"source\": {\"effect\": \"minecraft:luck\"}" + targets + ", \"bonus\": 1}", "unknown effect");
        assertRejected("{\"source\": {\"effect\": \"minecraft:slowness\"}" + targets + "}", "non-zero");
        assertRejected(
                "{\"source\": {\"effect\": \"minecraft:slowness\"}" + targets + ", \"mode\": \"lucky\"}", "mode");
        assertRejected(
                "{\"source\": {\"effect\": \"minecraft:slowness\"}, \"applies_to\": {}, \"bonus\": 1}", "targets no");
        assertRejected(
                "{\"source\": {\"effect\": \"minecraft:slowness\"}, \"applies_to\": {\"saves\": [\"luck\"]},"
                        + " \"bonus\": 1}",
                "luck");
    }

    private void assertRejected(String json, String messagePart) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parse("pack:bad", json));
        assertTrue(e.getMessage().contains(messagePart), e.getMessage());
    }
}
