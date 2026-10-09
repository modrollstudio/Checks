package studio.modroll.checks.preset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.body.Size;
import studio.modroll.checks.body.SizeOption;

class PresetParseTest {

    private static final ResourceLocation ID = ResourceLocation.parse("pack:thing");
    private static final ResourceLocation STEALTH = ResourceLocation.parse("checks:stealth");
    private static final ResourceLocation ATHLETICS = ResourceLocation.parse("checks:athletics");
    private static final ResourceLocation BREAD = ResourceLocation.parse("minecraft:bread");

    private final List<String> warnings = new ArrayList<>();

    @Test
    void speciesTraitsDefaultToTheFilesNamespace() {
        Species species = Species.parse(
                ID,
                json("{\"traits\": [\"darkvision\", \"other:luck\"]}"),
                PresetParseTest::isVanillaItem,
                warnings::add);
        assertEquals(
                List.of(ResourceLocation.parse("pack:darkvision"), ResourceLocation.parse("other:luck")),
                species.traits());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void speciesWithAnInvalidTraitIdIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Species.parse(
                        ID, json("{\"traits\": [\"Not An Id\"]}"), PresetParseTest::isVanillaItem, warnings::add));
    }

    @Test
    void speciesSizesKeepTheirOrderWithTheFirstAsDefault() {
        Species species = Species.parse(
                ID,
                json("{\"sizes\": [{\"size\": \"medium\", \"scale\": 1.0},"
                        + " {\"size\": \"small\", \"scale\": 0.6}]}"),
                PresetParseTest::isVanillaItem,
                warnings::add);

        assertEquals(List.of(new SizeOption(Size.MEDIUM, 1.0), new SizeOption(Size.SMALL, 0.6)), species.sizes());
        assertEquals(Optional.of(new SizeOption(Size.MEDIUM, 1.0)), species.size(Optional.empty()));
        assertEquals(Optional.of(new SizeOption(Size.SMALL, 0.6)), species.size(Optional.of(Size.SMALL)));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void aSizeTheSpeciesLacksFallsBackToItsDefault() {
        Species halfling = Species.parse(
                ID,
                json("{\"sizes\": [{\"size\": \"small\", \"scale\": 0.5}]}"),
                PresetParseTest::isVanillaItem,
                warnings::add);

        assertEquals(Optional.of(new SizeOption(Size.SMALL, 0.5)), halfling.size(Optional.of(Size.MEDIUM)));
        assertFalse(halfling.offersSize(Size.MEDIUM));
    }

    @Test
    void speciesWithoutSizesLeavesSizeAlone() {
        Species species = Species.parse(ID, json("{\"traits\": []}"), PresetParseTest::isVanillaItem, warnings::add);

        assertEquals(Optional.empty(), species.size(Optional.of(Size.SMALL)));
    }

    @Test
    void speciesWithBadSizesIsRejected() {
        for (String sizes : List.of(
                "[{\"size\": \"large\", \"scale\": 2.0}]",
                "[{\"size\": \"small\"}]",
                "[{\"size\": \"small\", \"scale\": 0.01}]",
                "[{\"size\": \"small\", \"scale\": 17}]",
                "[{\"size\": \"small\", \"scale\": 0.5}, {\"size\": \"small\", \"scale\": 0.6}]")) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Species.parse(
                            ID, json("{\"sizes\": " + sizes + "}"), PresetParseTest::isVanillaItem, warnings::add),
                    sizes);
        }
    }

    @Test
    void backgroundReadsAbilitiesSkillsAndKit() {
        Background background = background(
                "{\"abilities\": [\"str\", \"dex\", \"con\"], \"skills\": [\"athletics\", \"stealth\"],"
                        + " \"kit\": [{\"item\": \"minecraft:bread\", \"count\": 3}, {\"item\": \"minecraft:torch\"}]}");
        assertEquals(List.of(Ability.STRENGTH, Ability.DEXTERITY, Ability.CONSTITUTION), background.abilities());
        assertEquals(List.of(ATHLETICS, STEALTH), background.skills());
        assertEquals(
                List.of(new KitItem(BREAD, 3), new KitItem(ResourceLocation.parse("minecraft:torch"), 1)),
                background.kit());
    }

    @Test
    void backgroundSkipsUnknownItemsAndBadCounts() {
        Background background = background("{\"abilities\": [\"str\"], \"kit\": [{\"item\": \"minecraft:bread\","
                + " \"count\": 0}, {\"item\": \"pack:unobtainium\"}, {\"item\": \"minecraft:bread\", \"count\": 2}]}");
        assertEquals(List.of(new KitItem(BREAD, 2)), background.kit());
        assertEquals(2, warnings.size());
    }

    @Test
    void backgroundsWithBadAbilitiesOrUnknownSkillsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> background("{\"abilities\": []}"));
        assertThrows(IllegalArgumentException.class, () -> background("{\"abilities\": [\"luck\"]}"));
        assertThrows(IllegalArgumentException.class, () -> background("{\"abilities\": [\"str\", \"str\"]}"));
        assertThrows(
                IllegalArgumentException.class,
                () -> background("{\"abilities\": [\"str\"], \"skills\": [\"sailing\"]}"));
    }

    @Test
    void classReadsSavesSkillsOrderAndHitDie() {
        ClassPreset classPreset = classPreset("{\"hit_die\": 10, \"saves\": [\"str\", \"con\"],"
                + " \"skills\": {\"choose\": 2, \"from\": [\"athletics\", \"sailing\", \"stealth\"]},"
                + " \"suggested_order\": [\"con\", \"str\"]}");
        assertEquals(10, classPreset.hitDie());
        assertEquals(List.of(Ability.STRENGTH, Ability.CONSTITUTION), classPreset.saves());
        assertEquals(2, classPreset.skillChoices());
        assertEquals(List.of(ATHLETICS, STEALTH), classPreset.skillOptions());
        assertEquals(
                List.of(
                        Ability.CONSTITUTION,
                        Ability.STRENGTH,
                        Ability.DEXTERITY,
                        Ability.INTELLIGENCE,
                        Ability.WISDOM,
                        Ability.CHARISMA),
                classPreset.suggestedOrder());
        assertEquals(1, warnings.size(), "the unknown skill warns: " + warnings);
    }

    @Test
    void classSkillsFromAnyMeansEverySkill() {
        ClassPreset classPreset = classPreset("{\"hit_die\": 8, \"skills\": {\"choose\": 3, \"from\": \"any\"}}");
        assertEquals(List.of(ATHLETICS, STEALTH), classPreset.skillOptions());
    }

    @Test
    void classesWithoutAHitDieOrWithNegativeChoicesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> classPreset("{\"saves\": [\"str\"]}"));
        assertThrows(IllegalArgumentException.class, () -> classPreset("{\"hit_die\": 0}"));
        assertThrows(
                IllegalArgumentException.class, () -> classPreset("{\"hit_die\": 8, \"skills\": {\"choose\": -1}}"));
    }

    @Test
    void presetsWithoutAnIconShowTheirKindsDefault() {
        assertEquals(PresetKind.SPECIES.defaultIcon(), species("{}").icon());
        assertEquals(
                PresetKind.BACKGROUND.defaultIcon(),
                background("{\"abilities\": [\"str\"]}").icon());
        assertEquals(
                PresetKind.CLASS.defaultIcon(), classPreset("{\"hit_die\": 8}").icon());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void presetsShowTheirRegisteredIcon() {
        assertEquals(BREAD, species("{\"icon\": \"minecraft:bread\"}").icon());
        assertEquals(
                BREAD,
                background("{\"abilities\": [\"str\"], \"icon\": \"minecraft:bread\"}")
                        .icon());
        assertEquals(
                BREAD,
                classPreset("{\"hit_die\": 8, \"icon\": \"minecraft:bread\"}").icon());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void anUnregisteredIconFallsBackToTheDefaultWithAWarning() {
        assertEquals(
                PresetKind.SPECIES.defaultIcon(),
                species("{\"icon\": \"pack:missing\"}").icon());
        assertEquals(
                PresetKind.CLASS.defaultIcon(),
                classPreset("{\"hit_die\": 8, \"icon\": \"Not An Id\"}").icon());
        assertEquals(2, warnings.size());
    }

    private Species species(String json) {
        return Species.parse(ID, json(json), PresetParseTest::isVanillaItem, warnings::add);
    }

    private Background background(String json) {
        return Background.parse(ID, json(json), PresetParseTest::skill, PresetParseTest::isVanillaItem, warnings::add);
    }

    private ClassPreset classPreset(String json) {
        return ClassPreset.parse(
                ID,
                json(json),
                PresetParseTest::skill,
                List.of(STEALTH, ATHLETICS),
                PresetParseTest::isVanillaItem,
                warnings::add);
    }

    private static boolean isVanillaItem(ResourceLocation item) {
        return item.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE);
    }

    /** Knows athletics and stealth; a bare name means the checks: skill, as with the skill store. */
    private static Optional<Skill> skill(ResourceLocation id) {
        ResourceLocation checksId = ResourceLocation.fromNamespaceAndPath("checks", id.getPath());
        return Set.of(ATHLETICS, STEALTH).contains(checksId)
                ? Optional.of(new Skill(checksId, Ability.STRENGTH))
                : Optional.empty();
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }
}
