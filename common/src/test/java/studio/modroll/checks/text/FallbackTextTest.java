package studio.modroll.checks.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.ModLang;

class FallbackTextTest {

    private static final Path EN_US = Path.of("src/main/resources/assets/checks/lang/en_us.json");
    private static final Path MAIN_SOURCES = Path.of("src/main/java");
    private static final Pattern KEY_LITERAL =
            Pattern.compile("\"((?:commands\\.|chat\\.)?checks\\.[a-z0-9_.]*[a-z0-9_])\"");

    private final Language before = Language.getInstance();

    @BeforeEach
    void useALanguageWithoutChecks() {
        ModLang.injectWithoutChecks();
    }

    @AfterEach
    void restoreLang() {
        Language.inject(before);
    }

    @Test
    void withoutATranslationTheEnglishTextIsShown() {
        assertEquals(
                "Set Steve's dex to 16",
                FallbackText.of("commands.checks.set.success", "Steve", "dex", "16")
                        .getString());
    }

    @Test
    void theFallbackIsTheEnUsEntry() throws IOException {
        TranslatableContents contents = (TranslatableContents)
                FallbackText.of("commands.checks.error.no_scores").getContents();

        assertEquals("commands.checks.error.no_scores", contents.getKey());
        assertEquals(english().get("commands.checks.error.no_scores"), contents.getFallback());
    }

    @Test
    void aKeyMissingFromTheLangFileFailsFast() {
        assertThrows(IllegalArgumentException.class, () -> FallbackText.of("commands.checks.no_such_key"));
    }

    @Test
    void aKeyChecksShipsFallsBackToItsEnglishWhateverElseIsOffered() throws IOException {
        TranslatableContents contents =
                (TranslatableContents) FallbackText.ofOrElse("checks.lore.minecraft.zombie", () -> "built")
                        .getContents();

        assertEquals("checks.lore.minecraft.zombie", contents.getKey());
        assertEquals(english().get("checks.lore.minecraft.zombie"), contents.getFallback());
    }

    @Test
    void aKeyNobodyShipsKeepsItsKeyForPacksAndFallsBackToWhatIsOffered() {
        TranslatableContents contents =
                (TranslatableContents) FallbackText.ofOrElse("checks.lore.othermod.wisp", () -> "built")
                        .getContents();

        assertEquals("checks.lore.othermod.wisp", contents.getKey());
        assertEquals("built", contents.getFallback());
    }

    /**
     * Catches a typo on a path no test runs, in every file that uses fallback text. Keys built from parts
     * (per help command, per ability) are checked by the tests of those files.
     */
    @Test
    void everyKeyNamedWhereFallbackTextIsUsedIsInTheLangFile() throws IOException {
        Map<String, String> english = english();
        List<String> keys = keyLiterals();
        assertTrue(keys.size() > 20, "expected the sources to name their keys, found " + keys);
        for (String key : keys) {
            assertTrue(
                    english.containsKey(key) || english.containsKey(key + ".1"),
                    "en_us.json has no '" + key + "', nor a pool of that name");
        }
    }

    private static List<String> keyLiterals() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            return files.filter(file -> file.toString().endsWith(".java"))
                    .map(FallbackTextTest::read)
                    .filter(source -> source.contains("FallbackText.of("))
                    .flatMap(source -> KEY_LITERAL.matcher(source).results().map(result -> result.group(1)))
                    .distinct()
                    .toList();
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, String> english() throws IOException {
        Map<String, String> entries = new HashMap<>();
        try (InputStream in = Files.newInputStream(EN_US)) {
            Language.loadFromJson(in, entries::put);
        }
        return entries;
    }
}
