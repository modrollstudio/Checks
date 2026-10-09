package studio.modroll.checks.preset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.Checks;
import studio.modroll.checks.ModLang;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.body.Size;
import studio.modroll.checks.body.SizeOption;
import studio.modroll.checks.skill.ShippedSkills;

/** The species, backgrounds and classes Checks ships, parsed straight from the source tree. */
class ShippedPresetsTest {

    private static final Path DATA = Path.of("src/main/resources/data/checks/checks");
    private static final String SUFFIX = ".json";

    private final List<String> warnings = new ArrayList<>();
    private final Map<ResourceLocation, Skill> skills = ShippedSkills.load(warnings::add);
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
    void everyShippedPresetParsesCleanly() {
        assertEquals(9, species().size());
        assertEquals(8, backgrounds().size());
        assertEquals(12, classes().size());
        assertEquals(List.of(), warnings);
    }

    /** SRD 5.2: Gnome and Halfling are Small, Human and Tiefling choose Small or Medium, the rest are Medium. */
    @Test
    void speciesHaveTheirSrdSizes() {
        Map<String, List<Size>> expected = Map.of(
                "dragonborn", List.of(Size.MEDIUM),
                "dwarf", List.of(Size.MEDIUM),
                "elf", List.of(Size.MEDIUM),
                "gnome", List.of(Size.SMALL),
                "goliath", List.of(Size.MEDIUM),
                "halfling", List.of(Size.SMALL),
                "human", List.of(Size.MEDIUM, Size.SMALL),
                "orc", List.of(Size.MEDIUM),
                "tiefling", List.of(Size.MEDIUM, Size.SMALL));
        Map<ResourceLocation, Species> species = species();
        expected.forEach((path, sizes) -> assertEquals(
                sizes,
                species.get(Checks.id(path)).sizes().stream()
                        .map(SizeOption::size)
                        .toList(),
                path));
    }

    @Test
    void backgroundsListThreeAbilitiesTwoSkillsAndAKit() {
        for (Background background : backgrounds().values()) {
            assertEquals(3, background.abilities().size(), background.id().toString());
            assertEquals(2, background.skills().size(), background.id().toString());
            assertTrue(!background.kit().isEmpty(), background.id().toString());
        }
    }

    @Test
    void classesHaveTwoSavesAndEnoughSkillsToChooseFrom() {
        for (ClassPreset classPreset : classes().values()) {
            assertEquals(2, classPreset.saves().size(), classPreset.id().toString());
            assertTrue(
                    classPreset.skillOptions().size() > classPreset.skillChoices(),
                    classPreset.id().toString());
            assertEquals(Ability.values().length, classPreset.suggestedOrder().size());
        }
        assertEquals(
                skills.size(), classes().get(Checks.id("bard")).skillOptions().size());
        assertEquals(4, classes().get(Checks.id("rogue")).skillChoices());
    }

    @Test
    void everyPresetAndTraitHasANameAndDescription() {
        List<String> keys = new ArrayList<>();
        species().values().forEach(species -> {
            keys.add(key("species", species.id()));
            species.traits().forEach(trait -> keys.add(key("trait", trait)));
        });
        backgrounds().keySet().forEach(id -> keys.add(key("background", id)));
        classes().keySet().forEach(id -> keys.add(key("class", id)));
        for (String key : keys) {
            assertTrue(Language.getInstance().has(key), "missing lang " + key);
            assertTrue(Language.getInstance().has(key + ".desc"), "missing lang " + key + ".desc");
        }
    }

    /** Whether each icon is a registered item is checked in-game; here, that every preset picks its own. */
    @Test
    void everyShippedPresetNamesAVanillaIcon() {
        List<Preset> presets = new ArrayList<>();
        presets.addAll(species().values());
        presets.addAll(backgrounds().values());
        presets.addAll(classes().values());
        for (Preset preset : presets) {
            assertEquals(
                    ResourceLocation.DEFAULT_NAMESPACE,
                    preset.icon().getNamespace(),
                    preset.id().toString());
        }
        assertEquals(
                presets.size(), presets.stream().map(Preset::icon).distinct().count());
    }

    @Test
    void docsExamplesParseCleanly() throws IOException {
        ResourceLocation id = ResourceLocation.parse("mypack:example");
        Species.parse(id, example("species"), item -> true, warnings::add);
        Background background = Background.parse(id, example("background"), this::skill, item -> true, warnings::add);
        ClassPreset classPreset =
                ClassPreset.parse(id, example("class"), this::skill, skills.keySet(), item -> true, warnings::add);
        assertEquals(3, background.kit().size());
        assertEquals(9, classPreset.skillOptions().size());
        assertEquals(List.of(), warnings);
    }

    private static JsonObject example(String name) throws IOException {
        return JsonParser.parseString(Files.readString(Path.of("../docs/examples/" + name + SUFFIX)))
                .getAsJsonObject();
    }

    private static String key(String kind, ResourceLocation id) {
        return "checks." + kind + "." + id.getNamespace() + "." + id.getPath();
    }

    private Map<ResourceLocation, Species> species() {
        return load(PresetKind.SPECIES, (id, json) -> Species.parse(id, json, item -> true, warnings::add));
    }

    private Map<ResourceLocation, Background> backgrounds() {
        return load(
                PresetKind.BACKGROUND,
                (id, json) -> Background.parse(id, json, this::skill, item -> true, warnings::add));
    }

    private Map<ResourceLocation, ClassPreset> classes() {
        return load(
                PresetKind.CLASS,
                (id, json) -> ClassPreset.parse(id, json, this::skill, skills.keySet(), item -> true, warnings::add));
    }

    private Optional<Skill> skill(ResourceLocation id) {
        return Optional.ofNullable(skills.get(Checks.id(id.getPath())));
    }

    private static <T> Map<ResourceLocation, T> load(
            PresetKind kind, BiFunction<ResourceLocation, JsonObject, T> parser) {
        Map<ResourceLocation, T> loaded = new HashMap<>();
        try (Stream<Path> files = Files.list(DATA.resolve(kind.id()))) {
            for (Path file :
                    files.filter(path -> path.toString().endsWith(SUFFIX)).toList()) {
                String name = file.getFileName().toString();
                ResourceLocation id = Checks.id(name.substring(0, name.length() - SUFFIX.length()));
                loaded.put(
                        id,
                        parser.apply(
                                id,
                                JsonParser.parseString(Files.readString(file)).getAsJsonObject()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return loaded;
    }
}
