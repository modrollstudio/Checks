package studio.modroll.checks.trait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.preset.Species;
import studio.modroll.checks.skill.ShippedSkills;

/** The traits Checks ships, parsed straight from the source tree, against the species that list them. */
class ShippedTraitsTest {

    private static final Path DATA = Path.of("src/main/resources/data/checks/checks");
    private static final String SUFFIX = ".json";

    private final List<String> warnings = new ArrayList<>();
    private final Map<ResourceLocation, Skill> skills = ShippedSkills.load(warnings::add);

    @Test
    void everyShippedTraitParsesCleanlyAndBelongsToASpecies() throws IOException {
        Map<ResourceLocation, Trait> traits = traits();
        Set<ResourceLocation> listed = species().values().stream()
                .flatMap(species -> species.traits().stream())
                .collect(Collectors.toSet());
        assertEquals(23, traits.size());
        assertTrue(listed.containsAll(traits.keySet()), "a trait file no species lists");
        assertEquals(List.of(), warnings);
    }

    /** The per-species summary in docs/traits.md: every species gets three or four mechanical effects. */
    @Test
    void everySpeciesHasItsEffects() throws IOException {
        Map<ResourceLocation, Trait> traits = traits();
        Map<String, List<String>> expected = Map.of(
                "dragonborn", List.of("damage_resistance", "damage_resistance", "darkvision"),
                "dwarf", List.of("darkvision", "extra_health", "vanilla_save_advantage"),
                "elf", List.of("darkvision", "ignored_by", "skill_choices", "vanilla_save_advantage"),
                "gnome", List.of("darkvision", "ignored_by", "roll_bonus"),
                "goliath", List.of("attribute", "attribute", "attribute", "vanilla_save_advantage"),
                "halfling", List.of("reroll", "roll_bonus", "vanilla_save_advantage"),
                "human", List.of("reroll", "skill_choices", "skill_choices"),
                "orc", List.of("attribute", "darkvision", "last_stand"),
                "tiefling", List.of("damage_resistance", "darkvision", "ignored_by"));
        Map<String, List<String>> actual = new TreeMap<>();
        species()
                .forEach((id, species) -> actual.put(
                        id.getPath(),
                        species.traits().stream()
                                .flatMap(trait -> Optional.ofNullable(traits.get(trait)).stream())
                                .flatMap(trait -> trait.effects().stream())
                                .map(effect -> effect.type().id())
                                .sorted()
                                .toList()));
        assertEquals(new TreeMap<>(expected), actual);
    }

    private Map<ResourceLocation, Trait> traits() throws IOException {
        TraitEffect.Lookups lookups = new TraitEffect.Lookups(
                id -> Optional.ofNullable(skills.get(Checks.id(id.getPath()))), id -> true, id -> true);
        Map<ResourceLocation, Trait> traits = new HashMap<>();
        for (Path file : files("trait")) {
            ResourceLocation id = id(file);
            traits.put(
                    id,
                    Trait.parse(
                            id,
                            JsonParser.parseString(Files.readString(file)).getAsJsonObject(),
                            lookups,
                            warnings::add));
        }
        return traits;
    }

    private Map<ResourceLocation, Species> species() throws IOException {
        Map<ResourceLocation, Species> species = new HashMap<>();
        for (Path file : files("species")) {
            ResourceLocation id = id(file);
            species.put(
                    id,
                    Species.parse(
                            id,
                            JsonParser.parseString(Files.readString(file)).getAsJsonObject(),
                            item -> true,
                            warnings::add));
        }
        return species;
    }

    private static List<Path> files(String directory) throws IOException {
        try (Stream<Path> files = Files.list(DATA.resolve(directory))) {
            return files.filter(file -> file.toString().endsWith(SUFFIX)).toList();
        }
    }

    private static ResourceLocation id(Path file) {
        String name = file.getFileName().toString();
        return Checks.id(name.substring(0, name.length() - SUFFIX.length()));
    }
}
