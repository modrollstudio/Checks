package studio.modroll.checks.text;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.locale.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.ModLang;

/**
 * Every flavor, speech and death line reads naturally with names put in. A mob is named "the Iron Golem"
 * unless it has a custom name, so a mob's name never starts a sentence, where the "the" would be
 * lowercase.
 */
class FlavorLinesTest {

    private static final Path EN_US = Path.of("src/main/resources/assets/checks/lang/en_us.json");
    private static final Path TAGS = Path.of("src/main/resources/data/checks/tags");
    private static final Pattern POOL_LINE = Pattern.compile(".*\\.\\d+");
    /** Lang key prefix → the argument in its lines that is a mob's name. */
    private static final Map<String, String> MOB_NAME_ARGUMENT = Map.of(
            "checks.social.persuade.", "%1$s",
            "checks.social.deceive.", "%1$s",
            "checks.social.intimidate.", "%1$s",
            "checks.social.pickpocket.", "%1$s",
            "checks.social.plead.", "%1$s",
            "checks.social.lie.", "%1$s",
            "checks.social.intimidate_mob.", "%1$s",
            "checks.social.calm.", "%1$s",
            "checks.death.", "%2$s",
            "checks.social.refuses.", "%1$s");

    private static final List<String> POOLS = List.of(
            "checks.social.persuade.",
            "checks.social.deceive.",
            "checks.social.intimidate.",
            "checks.social.pickpocket.",
            "checks.social.plead.",
            "checks.social.lie.",
            "checks.social.refuses.",
            "checks.social.intimidate_mob.",
            "checks.social.calm.",
            "checks.social.performance.",
            "checks.speech.",
            "checks.death.",
            "checks.exploration.");

    private static final List<String> OUTCOMES =
            List.of("critical_failure", "failure", "barely", "success", "critical_success");
    private static final int MIN_POOL = 4;
    private static final int MAX_POOL = 6;

    private Language before;

    @BeforeEach
    void loadModLang() throws IOException {
        before = Language.getInstance();
        ModLang.inject();
    }

    @AfterEach
    void restoreLang() {
        Language.inject(before);
    }

    @Test
    void aMobsNameNeverStartsASentence() throws IOException {
        JsonObject lang = lang();
        MOB_NAME_ARGUMENT.forEach((prefix, argument) -> {
            Pattern sentenceStart = Pattern.compile("(^|[.!?]\"?\\s+)\"?" + Pattern.quote(argument));
            lang.keySet().stream().filter(key -> key.startsWith(prefix)).forEach(key -> {
                String line = lang.get(key).getAsString();
                assertFalse(sentenceStart.matcher(line).find(), key + " starts a sentence with a name: " + line);
            });
        });
    }

    @Test
    void everyPoolLineFillsInAllItsArguments() throws IOException {
        JsonObject lang = lang();
        lang.keySet().stream()
                .filter(key -> POOLS.stream().anyMatch(key::startsWith))
                .filter(key -> POOL_LINE.matcher(key).matches())
                .forEach(key -> {
                    String line = FallbackText.of(key, "Steve", "the Iron Golem", "Emerald")
                            .getString();
                    assertTrue(!line.contains("%") && !line.isBlank(), key + " renders as: " + line);
                });
    }

    @Test
    void wanderingTradersHaveTheirOwnPersuadeAndDeceivePools() {
        for (String action : List.of("persuade", "deceive")) {
            for (String outcome : OUTCOMES) {
                expectPool("checks.social." + action + ".wandering_trader." + outcome, MIN_POOL, MAX_POOL);
            }
        }
    }

    @Test
    void pleadAndLieHaveFlavorAndSpeechPoolsForEachOutcome() {
        for (String action : List.of("plead", "lie")) {
            for (String outcome : List.of("critical_failure", "failure", "success", "critical_success")) {
                expectPool("checks.social." + action + "." + outcome, MIN_POOL, MAX_POOL);
                expectPool("checks.speech.villager." + action + "." + outcome, MIN_POOL, MAX_POOL);
            }
        }
    }

    @Test
    void intimidatingAMobHasPoolsForEachWayItGoes() {
        for (String outcome : List.of("critical_failure", "failure", "success")) {
            expectPool("checks.social.intimidate_mob." + outcome, MIN_POOL, MAX_POOL);
        }
    }

    @Test
    void calmingAMobHasSuccessAndFailurePools() {
        for (String outcome : List.of("failure", "success")) {
            expectPool("checks.social.calm." + outcome, MIN_POOL, MAX_POOL);
        }
    }

    @Test
    void performanceHasFlavorAndVillagerAndPiglinSpeechPoolsForEachOutcome() {
        for (String outcome : List.of("critical_failure", "failure", "success", "critical_success")) {
            expectPool("checks.social.performance." + outcome, MIN_POOL, MAX_POOL);
            expectPool("checks.speech.villager.performance." + outcome, MIN_POOL, MAX_POOL);
            expectPool("checks.speech.piglin.performance." + outcome, MIN_POOL, MAX_POOL);
        }
    }

    @Test
    void explorationChecksHaveSuccessAndFailurePoolsAndSpottingATripwireHasOne() {
        for (String use : List.of("leap", "landing", "cobweb", "disarm")) {
            for (String outcome : List.of("failure", "success")) {
                expectPool("checks.exploration." + use + "." + outcome, MIN_POOL, MAX_POOL);
            }
        }
        expectPool("checks.exploration.spot_tripwire", MIN_POOL, MAX_POOL);
    }

    @Test
    void searchingAndTamingHaveSuccessAndFailurePoolsAndLoreHasFailurePools() {
        for (String use : List.of("search", "taming")) {
            for (String outcome : List.of("failure", "success")) {
                expectPool("checks.exploration." + use + "." + outcome, MIN_POOL, MAX_POOL);
            }
        }
        expectPool("checks.exploration.monster_lore.failure", MIN_POOL, MAX_POOL);
        expectPool("checks.exploration.structure_lore.failure", MIN_POOL, MAX_POOL);
    }

    @Test
    void everyMobNamedInALoreTagAndEveryKindOfStructureHasAHint() throws IOException {
        JsonObject lang = lang();
        for (String skill : List.of("religion", "arcana", "nature", "history")) {
            for (String entry : tagValues("entity_type/lore/" + skill)) {
                if (!entry.startsWith("#")) {
                    String key = "checks.lore." + entry.replace(':', '.');
                    assertTrue(lang.has(key), entry + " has no hint " + key);
                }
            }
        }
        try (Stream<Path> kinds = Files.list(TAGS.resolve("worldgen/structure/history"))) {
            kinds.map(file -> file.getFileName().toString().replace(".json", ""))
                    .forEach(kind ->
                            assertTrue(lang.has("checks.lore.structure." + kind), kind + " has no structure hint"));
        }
        assertTrue(lang.has("checks.lore.structure.unknown"));
    }

    @Test
    void aRefusalIsPickedFromAPool() {
        expectPool("checks.social.refuses", MIN_POOL, MAX_POOL);
    }

    private static void expectPool(String pool, int min, int max) {
        int size = FallbackText.poolSize(pool);
        assertTrue(size >= min && size <= max, pool + " has " + size + " lines");
    }

    private static List<String> tagValues(String tag) throws IOException {
        try (Reader reader = Files.newBufferedReader(TAGS.resolve(tag + ".json"))) {
            JsonArray values = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("values");
            return values.asList().stream().map(JsonElement::getAsString).toList();
        }
    }

    private static JsonObject lang() throws IOException {
        try (Reader reader = Files.newBufferedReader(EN_US)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
