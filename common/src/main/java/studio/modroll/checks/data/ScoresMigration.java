package studio.modroll.checks.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;

/**
 * Moves a {@code scores.json} written before 0.2.0 ({@code format_version} 1, or none) to the current format, once.
 * Those files all say {@code "health": { "per_level": { "enabled": false } }}, but only because it was the shipped
 * default, and 0.2.0 turns hit points per level on. The move switches it on; nothing else changes, and keys the file
 * leaves out take the new defaults. The migrated file says {@code format_version} 2, so a pack maker who turns it back
 * off keeps that.
 */
public final class ScoresMigration {

    /** Logged once, with the file, when the loader saves a migrated file. */
    public static final String MESSAGE = "Moved {} to Checks 0.2.0: hit points per level are now on."
            + " Set health.per_level.enabled to false for the old rule.";

    private ScoresMigration() {}

    /** Migrates {@code root} in place. @return whether it was older than the current format and changed */
    public static boolean migrate(JsonObject root) {
        if (version(root) >= ScoresConfig.FORMAT_VERSION) {
            return false;
        }
        JsonObject health = child(root, "health");
        child(health, "per_level").addProperty("enabled", true);
        withVersionFirst(root);
        return true;
    }

    /** The object under {@code key}, added when the file has none or something else there. */
    private static JsonObject child(JsonObject parent, String key) {
        JsonElement existing = parent.get(key);
        if (existing != null && existing.isJsonObject()) {
            return existing.getAsJsonObject();
        }
        JsonObject created = new JsonObject();
        parent.add(key, created);
        return created;
    }

    /** The file's {@code format_version}, 1 when it has none (as {@link LenientJson#checkFormatVersion} reads it). */
    private static int version(JsonObject root) {
        JsonElement version = root.get("format_version");
        return version != null
                        && version.isJsonPrimitive()
                        && version.getAsJsonPrimitive().isNumber()
                ? version.getAsInt()
                : 1;
    }

    /** Sets the current {@code format_version}, keeping it the file's first key as the shipped file has it. */
    private static void withVersionFirst(JsonObject root) {
        Map<String, JsonElement> rest = root.asMap();
        JsonObject copy = new JsonObject();
        copy.addProperty("format_version", ScoresConfig.FORMAT_VERSION);
        rest.forEach((key, value) -> {
            if (!key.equals("format_version")) {
                copy.add(key, value);
            }
        });
        for (String key : root.keySet().toArray(String[]::new)) {
            root.remove(key);
        }
        copy.asMap().forEach(root::add);
    }
}
