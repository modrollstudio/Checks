package studio.modroll.checks.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Reads a {@link JsonObject} leniently: unknown keys and wrong-typed scalars warn instead of failing.
 * Callers throw {@link IllegalArgumentException} for problems that must reject the whole file. Call
 * {@link #finish()} after reading every known key to warn about the leftovers.
 */
public final class LenientJson {

    private final JsonObject json;
    private final String context;
    private final Consumer<String> warn;
    private final Set<String> consumed = new HashSet<>();
    private final List<LenientJson> children = new ArrayList<>();

    public LenientJson(JsonObject json, String context, Consumer<String> warn) {
        this.json = json;
        this.context = context;
        this.warn = warn;
    }

    public boolean has(String key) {
        return json.has(key);
    }

    public Set<String> keys() {
        return Set.copyOf(json.keySet());
    }

    private JsonElement raw(String key) {
        consumed.add(key);
        return json.get(key);
    }

    public int getInt(String key, int def) {
        return optionalInt(key).orElse(def);
    }

    public OptionalInt optionalInt(String key) {
        JsonElement e = raw(key);
        if (e == null) {
            return OptionalInt.empty();
        }
        if (isNumber(e)) {
            return OptionalInt.of(e.getAsInt());
        }
        warnType(key, "a number");
        return OptionalInt.empty();
    }

    public double getDouble(String key, double def) {
        JsonElement e = raw(key);
        if (e == null) {
            return def;
        }
        if (isNumber(e)) {
            return e.getAsDouble();
        }
        warnType(key, "a number");
        return def;
    }

    public boolean getBool(String key, boolean def) {
        JsonElement e = raw(key);
        if (e == null) {
            return def;
        }
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) {
            return e.getAsBoolean();
        }
        warnType(key, "true or false");
        return def;
    }

    public Optional<String> optionalString(String key) {
        JsonElement e = raw(key);
        if (e == null) {
            return Optional.empty();
        }
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            return Optional.of(e.getAsString());
        }
        warnType(key, "a string");
        return Optional.empty();
    }

    public List<String> stringList(String key) {
        JsonElement e = raw(key);
        if (e == null) {
            return List.of();
        }
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            return List.of(e.getAsString());
        }
        if (e.isJsonArray()) {
            List<String> out = new ArrayList<>();
            for (JsonElement item : e.getAsJsonArray()) {
                if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
                    out.add(item.getAsString());
                } else {
                    warn.accept(context + ": '" + key + "' entries must be strings, skipped " + item);
                }
            }
            return out;
        }
        warnType(key, "a string or list of strings");
        return List.of();
    }

    /** Empty when absent, or with a warning when it is not a list of numbers. */
    public Optional<List<Integer>> optionalIntList(String key) {
        JsonElement e = raw(key);
        if (e == null) {
            return Optional.empty();
        }
        if (!e.isJsonArray() || !e.getAsJsonArray().asList().stream().allMatch(LenientJson::isNumber)) {
            warnType(key, "a list of numbers");
            return Optional.empty();
        }
        return Optional.of(
                e.getAsJsonArray().asList().stream().map(JsonElement::getAsInt).toList());
    }

    /** Empty when absent, or with a warning when it is not a list of lists of numbers. */
    public Optional<List<List<Integer>>> optionalIntLists(String key) {
        JsonElement e = raw(key);
        if (e == null) {
            return Optional.empty();
        }
        if (!e.isJsonArray() || !e.getAsJsonArray().asList().stream().allMatch(LenientJson::isNumberList)) {
            warnType(key, "a list of lists of numbers");
            return Optional.empty();
        }
        return Optional.of(e.getAsJsonArray().asList().stream()
                .map(list -> list.getAsJsonArray().asList().stream()
                        .map(JsonElement::getAsInt)
                        .toList())
                .toList());
    }

    /** Each object in the list, read leniently; entries that are not objects are skipped with a warning. */
    public List<LenientJson> objectList(String key) {
        JsonElement e = raw(key);
        if (e == null) {
            return List.of();
        }
        if (!e.isJsonArray()) {
            warnType(key, "a list of objects");
            return List.of();
        }
        List<LenientJson> objects = new ArrayList<>();
        for (int i = 0; i < e.getAsJsonArray().size(); i++) {
            JsonElement item = e.getAsJsonArray().get(i);
            if (!item.isJsonObject()) {
                warn.accept(context + ": '" + key + "' entries must be objects, skipped " + item);
                continue;
            }
            LenientJson child = new LenientJson(item.getAsJsonObject(), context + "." + key + "[" + i + "]", warn);
            children.add(child);
            objects.add(child);
        }
        return objects;
    }

    public LenientJson object(String key) {
        JsonElement e = raw(key);
        JsonObject nested;
        if (e == null) {
            nested = new JsonObject();
        } else if (e.isJsonObject()) {
            nested = e.getAsJsonObject();
        } else {
            warnType(key, "an object");
            nested = new JsonObject();
        }
        LenientJson child = new LenientJson(nested, context + "." + key, warn);
        children.add(child);
        return child;
    }

    public void checkFormatVersion(int supported) {
        int version = getInt("format_version", 1);
        if (version > supported) {
            warn.accept(context + ": format_version " + version + " is newer than supported version " + supported
                    + " — parsing anyway, values may be misread");
        }
    }

    public void finish() {
        for (String key : json.keySet()) {
            if (!consumed.contains(key)) {
                warn.accept(context + ": unknown key '" + key + "' (ignored)");
            }
        }
        for (LenientJson child : children) {
            child.finish();
        }
    }

    public void warn(String message) {
        warn.accept(context + ": " + message);
    }

    private static boolean isNumber(JsonElement e) {
        return e.isJsonPrimitive() && ((JsonPrimitive) e).isNumber();
    }

    private static boolean isNumberList(JsonElement e) {
        return e.isJsonArray() && e.getAsJsonArray().asList().stream().allMatch(LenientJson::isNumber);
    }

    private void warnType(String key, String expected) {
        warn.accept(context + ": '" + key + "' must be " + expected + ", using default");
    }
}
