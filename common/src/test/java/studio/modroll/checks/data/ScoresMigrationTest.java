package studio.modroll.checks.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScoresMigrationTest {

    @TempDir
    Path folder;

    @Test
    void anOldFileGetsHealthPerLevelOnceAndKeepsEverythingElse() {
        JsonObject root = json("{\"format_version\": 1, \"skills\": {\"enabled\": false},"
                + " \"health\": {\"per_con_point\": 3, \"per_level\": {\"enabled\": false, \"multiplier\": 0.1}}}");

        assertTrue(ScoresMigration.migrate(root));

        assertEquals(
                json(
                        "{\"format_version\": 2, \"skills\": {\"enabled\": false},"
                                + " \"health\": {\"per_con_point\": 3, \"per_level\": {\"enabled\": true, \"multiplier\": 0.1}}}"),
                root);
        assertEquals("format_version", root.keySet().iterator().next());
        assertFalse(ScoresMigration.migrate(root));
    }

    @Test
    void aFileWithNoVersionOrHealthIsOld() {
        JsonObject root = json("{\"skills\": {\"enabled\": true}}");

        assertTrue(ScoresMigration.migrate(root));

        assertEquals(
                json("{\"format_version\": 2, \"skills\": {\"enabled\": true},"
                        + " \"health\": {\"per_level\": {\"enabled\": true}}}"),
                root);
    }

    @Test
    void aCurrentFileThatTurnsItOffKeepsThat() {
        JsonObject root = json("{\"format_version\": 2, \"health\": {\"per_level\": {\"enabled\": false}}}");

        assertFalse(ScoresMigration.migrate(root));
        assertFalse(
                ScoresConfigLoader.parse(root, warning -> {}).body().health().perLevelEnabled());
    }

    @Test
    void theLoaderSavesTheMigratedFileOnce() throws IOException {
        Path file = folder.resolve("scores.json");
        Files.writeString(file, "{\"format_version\": 1, \"health\": {\"per_level\": {\"enabled\": false}}}");

        assertTrue(ScoresConfigLoader.load(file).body().health().perLevelEnabled());
        String saved = Files.readString(file);
        assertEquals(
                List.of(
                        "{",
                        "  \"format_version\": 2,",
                        "  \"health\": {",
                        "    \"per_level\": { \"enabled\": true }",
                        "  }",
                        "}"),
                saved.lines().toList());

        ScoresConfigLoader.load(file);
        assertEquals(saved, Files.readString(file));
    }

    @Test
    void theShippedFileIsCurrent() {
        assertFalse(ScoresMigration.migrate(json(ScoresConfigLoader.DEFAULT_FILE)));
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }
}
