package studio.modroll.checks.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;

/**
 * Files from before 0.2.0 turn per level hit points off only because that was the shipped default, so the move switches
 * them on once; the migrated file says {@code format_version} 2, so turning them off again sticks.
 */
public final class ScoresMigration {

    public static final String MESSAGE = "Moved {} to Checks 0.2.0: hit points per level are now on."
            + " Set health.per_level.enabled to false for the old rule.";

    private ScoresMigration() {}

    public static boolean migrate(JsonObject root) {
        if (version(root) >= ScoresConfig.FORMAT_VERSION) {
            return false;
        }
        JsonObject health = child(root, "health");
        child(health, "per_level").addProperty("enabled", true);
        withVersionFirst(root);
        return true;
    }

    private static JsonObject child(JsonObject parent, String key) {
        JsonElement existing = parent.get(key);
        if (existing != null && existing.isJsonObject()) {
            return existing.getAsJsonObject();
        }
        JsonObject created = new JsonObject();
        parent.add(key, created);
        return created;
    }

    /** 1 when absent, as {@link LenientJson#checkFormatVersion} reads it. */
    private static int version(JsonObject root) {
        JsonElement version = root.get("format_version");
        return version != null
                        && version.isJsonPrimitive()
                        && version.getAsJsonPrimitive().isNumber()
                ? version.getAsInt()
                : 1;
    }

    /** First, as the shipped file has it. */
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
