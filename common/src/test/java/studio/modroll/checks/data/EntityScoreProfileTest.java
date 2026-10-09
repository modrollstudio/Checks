package studio.modroll.checks.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.skill.ShippedSkills;

class EntityScoreProfileTest {

    private static final Skill SAILING = new Skill(ResourceLocation.parse("pack:sailing"), Ability.WISDOM);

    private Map<ResourceLocation, Skill> shipped;

    @BeforeEach
    void loadSkills() {
        shipped = ShippedSkills.load(w -> {});
        Map<ResourceLocation, Skill> skills = new HashMap<>(shipped);
        skills.put(SAILING.id(), SAILING);
        SkillStore.setSkills(skills);
    }

    @AfterEach
    void reset() {
        SkillStore.clear();
    }

    private static EntityScoreProfile parse(String json, List<String> warnings) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        return EntityScoreProfile.parse(ResourceLocation.parse("pack:test"), obj, SkillStore::find, warnings::add);
    }

    private static EntityScoreProfile parse(String json) {
        return parse(json, new ArrayList<>());
    }

    private Skill shipped(String path) {
        return shipped.get(ResourceLocation.fromNamespaceAndPath("checks", path));
    }

    @Test
    void docsExampleParsesWithoutWarnings() throws IOException {
        List<String> warnings = new ArrayList<>();
        EntityScoreProfile p = parse(Files.readString(Path.of("../docs/examples/entity_profile.json")), warnings);
        assertEquals(List.of(), warnings);
        assertEquals(3, p.matches().size());
        assertEquals(OptionalInt.of(14), p.score(Ability.CONSTITUTION));
        assertEquals(OptionalInt.of(2), p.skillBonus(shipped("athletics")));
        assertEquals(OptionalInt.of(1), p.skillBonus(shipped("perception")));
        assertEquals(OptionalInt.of(3), p.proficiencyBonus());
        assertEquals(Optional.of(Proficiency.EXPERTISE), p.skillProficiency(shipped("athletics")));
        assertEquals(Optional.of(Proficiency.PROFICIENT), p.skillProficiency(shipped("perception")));
        assertEquals(Optional.of(Proficiency.PROFICIENT), p.saveProficiency(Ability.CONSTITUTION));
        assertEquals(Optional.empty(), p.saveProficiency(Ability.DEXTERITY));
    }

    @Test
    void parsesMatchesAndAbilities() {
        EntityScoreProfile p =
                parse("{\"matches\": [\"minecraft:zombie\"], \"abilities\": {\"str\": 13, \"con\": 14}}");
        assertEquals(1, p.matches().size());
        assertEquals(OptionalInt.of(13), p.score(Ability.STRENGTH));
        assertEquals(OptionalInt.of(14), p.score(Ability.CONSTITUTION));
        assertEquals(OptionalInt.empty(), p.score(Ability.DEXTERITY));
    }

    @Test
    void missingMatchesIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> parse("{\"abilities\": {\"str\": 10}}"));
    }

    @Test
    void scoreOutOfRangeIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("{\"matches\": [\"minecraft:zombie\"], \"abilities\": {\"str\": 99}}"));
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("{\"matches\": [\"minecraft:zombie\"], \"abilities\": {\"str\": 0}}"));
    }

    @Test
    void nonIntegerScoreIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("{\"matches\": [\"minecraft:zombie\"], \"abilities\": {\"str\": \"big\"}}"));
    }

    @Test
    void unknownAbilityKeyWarnsButParses() {
        List<String> warnings = new ArrayList<>();
        EntityScoreProfile p =
                parse("{\"matches\": [\"minecraft:zombie\"], \"abilities\": {\"luck\": 5, \"str\": 10}}", warnings);
        assertEquals(OptionalInt.of(10), p.score(Ability.STRENGTH));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("luck")));
    }

    @Test
    void parsesSkillBonusesByFullOrNamespacelessId() {
        EntityScoreProfile p = parse("{\"matches\": [\"minecraft:zombie\"], \"skills\":"
                + " {\"stealth\": 3, \"checks:athletics\": -1, \"pack:sailing\": 5}}");
        assertEquals(OptionalInt.of(3), p.skillBonus(shipped("stealth")));
        assertEquals(OptionalInt.of(-1), p.skillBonus(shipped("athletics")));
        assertEquals(OptionalInt.of(5), p.skillBonus(SAILING));
        assertEquals(OptionalInt.empty(), p.skillBonus(shipped("arcana")));
    }

    @Test
    void unknownSkillIsSkippedWithWarning() {
        List<String> warnings = new ArrayList<>();
        EntityScoreProfile p = parse(
                "{\"matches\": [\"minecraft:zombie\"], \"skills\": {\"basket_weaving\": 4, \"stealth\": 2}}", warnings);
        assertEquals(Map.of(shipped("stealth").id(), 2), p.skillBonuses());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("unknown skill 'basket_weaving'")));
    }

    @Test
    void skillBonusOutOfRangeIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("{\"matches\": [\"minecraft:zombie\"], \"skills\": {\"stealth\": 31}}"));
    }

    @Test
    void nonIntegerSkillBonusIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("{\"matches\": [\"minecraft:zombie\"], \"skills\": {\"stealth\": \"lots\"}}"));
    }

    @Test
    void parsesProficiencyBonusAndLevels() {
        EntityScoreProfile p = parse("{\"matches\": [\"minecraft:zombie\"], \"proficiency\": {\"bonus\": 4,"
                + " \"skills\": {\"stealth\": \"expertise\", \"pack:sailing\": \"proficient\", \"arcana\": \"none\"},"
                + " \"saves\": {\"dex\": \"proficient\", \"wis\": \"none\"}}}");
        assertEquals(OptionalInt.of(4), p.proficiencyBonus());
        assertEquals(Optional.of(Proficiency.EXPERTISE), p.skillProficiency(shipped("stealth")));
        assertEquals(Optional.of(Proficiency.PROFICIENT), p.skillProficiency(SAILING));
        assertEquals(Optional.of(Proficiency.NONE), p.skillProficiency(shipped("arcana")));
        assertEquals(Optional.empty(), p.skillProficiency(shipped("athletics")));
        assertEquals(Optional.of(Proficiency.PROFICIENT), p.saveProficiency(Ability.DEXTERITY));
        assertEquals(Optional.of(Proficiency.NONE), p.saveProficiency(Ability.WISDOM));
    }

    @Test
    void profileWithoutProficiencyHasNone() {
        EntityScoreProfile p = parse("{\"matches\": [\"minecraft:zombie\"]}");
        assertEquals(OptionalInt.empty(), p.proficiencyBonus());
        assertEquals(Map.of(), p.skillProficiencies());
        assertEquals(Map.of(), p.saveProficiencies());
    }

    @Test
    void proficiencyBonusOutOfRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> parseProficiency("{\"bonus\": 11}"));
        assertThrows(IllegalArgumentException.class, () -> parseProficiency("{\"bonus\": -1}"));
    }

    @Test
    void nonIntegerProficiencyBonusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> parseProficiency("{\"bonus\": \"two\"}"));
    }

    @Test
    void unknownProficiencyLevelIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> parseProficiency("{\"skills\": {\"stealth\": \"master\"}}"));
        assertThrows(IllegalArgumentException.class, () -> parseProficiency("{\"skills\": {\"stealth\": 2}}"));
        assertThrows(IllegalArgumentException.class, () -> parseProficiency("{\"saves\": {\"dex\": true}}"));
    }

    @Test
    void expertiseOnASaveIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> parseProficiency("{\"saves\": {\"dex\": \"expertise\"}}"));
    }

    @Test
    void unknownSkillProficiencyIsSkippedWithWarning() {
        List<String> warnings = new ArrayList<>();
        EntityScoreProfile p = parse(
                "{\"matches\": [\"minecraft:zombie\"], \"proficiency\":"
                        + " {\"skills\": {\"basket_weaving\": \"expertise\", \"stealth\": \"proficient\"}}}",
                warnings);
        assertEquals(Map.of(shipped("stealth").id(), Proficiency.PROFICIENT), p.skillProficiencies());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("unknown skill 'basket_weaving'")));
    }

    @Test
    void unknownSaveKeyWarnsButParses() {
        List<String> warnings = new ArrayList<>();
        EntityScoreProfile p = parse(
                "{\"matches\": [\"minecraft:zombie\"], \"proficiency\":"
                        + " {\"saves\": {\"luck\": \"proficient\", \"dex\": \"proficient\"}}}",
                warnings);
        assertEquals(Map.of(Ability.DEXTERITY, Proficiency.PROFICIENT), p.saveProficiencies());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("luck")));
    }

    private static EntityScoreProfile parseProficiency(String proficiency) {
        return parse("{\"matches\": [\"minecraft:zombie\"], \"proficiency\": " + proficiency + "}");
    }
}
