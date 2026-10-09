package studio.modroll.checks.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LenientJsonTest {

    private static JsonObject obj(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Test
    void wrongTypedScalarFallsBackWithWarning() {
        List<String> warnings = new ArrayList<>();
        LenientJson j = new LenientJson(obj("{\"n\": \"oops\"}"), "ctx", warnings::add);
        assertEquals(5, j.getInt("n", 5));
        assertEquals(1, warnings.size());
    }

    @Test
    void unknownKeyWarnsOnFinish() {
        List<String> warnings = new ArrayList<>();
        LenientJson j = new LenientJson(obj("{\"known\": 1, \"stray\": 2}"), "ctx", warnings::add);
        j.getInt("known", 0);
        j.finish();
        assertTrue(warnings.stream().anyMatch(w -> w.contains("stray")));
    }

    @Test
    void newerFormatVersionWarnsButParses() {
        List<String> warnings = new ArrayList<>();
        LenientJson j = new LenientJson(obj("{\"format_version\": 99}"), "ctx", warnings::add);
        j.checkFormatVersion(1);
        assertTrue(warnings.stream().anyMatch(w -> w.contains("format_version")));
    }

    @Test
    void stringListAcceptsSingleString() {
        LenientJson j = new LenientJson(obj("{\"m\": \"one\"}"), "ctx", w -> {});
        assertEquals(List.of("one"), j.stringList("m"));
    }
}
