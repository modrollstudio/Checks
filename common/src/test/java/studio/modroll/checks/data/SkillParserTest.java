package studio.modroll.checks.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.skill.ShippedSkills;

class SkillParserTest {

    private static final ResourceLocation ID = ResourceLocation.parse("pack:sailing");

    private static Skill parse(String json) {
        return SkillParser.parse(ID, JsonParser.parseString(json).getAsJsonObject(), w -> {});
    }

    @Test
    void parsesGoverningAbility() {
        assertEquals(new Skill(ID, Ability.WISDOM), parse("{\"format_version\": 1, \"ability\": \"wis\"}"));
    }

    @Test
    void docsExampleParsesWithoutWarnings() throws IOException {
        List<String> warnings = new ArrayList<>();
        Skill skill = SkillParser.parse(
                ID,
                JsonParser.parseString(Files.readString(Path.of("../docs/examples/skill.json")))
                        .getAsJsonObject(),
                warnings::add);
        assertEquals(List.of(), warnings);
        assertEquals(Ability.WISDOM, skill.ability());
    }

    @Test
    void missingAbilityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> parse("{\"format_version\": 1}"));
    }

    @Test
    void unknownAbilityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> parse("{\"ability\": \"luck\"}"));
    }

    @Test
    void nonStringAbilityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> parse("{\"ability\": 3}"));
    }

    @Test
    void shipsTheEighteenStandardSkillsWithTheirAbilities() {
        List<String> warnings = new ArrayList<>();
        Map<ResourceLocation, Skill> shipped = ShippedSkills.load(warnings::add);

        Map<String, Ability> expected = Map.ofEntries(
                Map.entry("acrobatics", Ability.DEXTERITY),
                Map.entry("animal_handling", Ability.WISDOM),
                Map.entry("arcana", Ability.INTELLIGENCE),
                Map.entry("athletics", Ability.STRENGTH),
                Map.entry("deception", Ability.CHARISMA),
                Map.entry("history", Ability.INTELLIGENCE),
                Map.entry("insight", Ability.WISDOM),
                Map.entry("intimidation", Ability.CHARISMA),
                Map.entry("investigation", Ability.INTELLIGENCE),
                Map.entry("medicine", Ability.WISDOM),
                Map.entry("nature", Ability.INTELLIGENCE),
                Map.entry("perception", Ability.WISDOM),
                Map.entry("performance", Ability.CHARISMA),
                Map.entry("persuasion", Ability.CHARISMA),
                Map.entry("religion", Ability.INTELLIGENCE),
                Map.entry("sleight_of_hand", Ability.DEXTERITY),
                Map.entry("stealth", Ability.DEXTERITY),
                Map.entry("survival", Ability.WISDOM));

        assertEquals(List.of(), warnings);
        assertEquals(expected.size(), shipped.size());
        expected.forEach((path, ability) -> assertEquals(
                ability,
                shipped.get(ResourceLocation.fromNamespaceAndPath("checks", path))
                        .ability(),
                path));
    }
}
