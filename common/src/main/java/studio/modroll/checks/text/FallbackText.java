package studio.modroll.checks.text;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;

/** For text a client without Checks may see, so it shows English instead of a raw key. */
public final class FallbackText {

    private static final String EN_US = "/assets/checks/lang/en_us.json";
    private static final Map<String, String> ENGLISH = load();

    private FallbackText() {}

    public static MutableComponent of(String key, Object... args) {
        return Component.translatableWithFallback(key, english(key), args);
    }

    public static MutableComponent ofOr(String key, String fallbackKey, Object... args) {
        String english = ENGLISH.containsKey(key) ? ENGLISH.get(key) : english(fallbackKey);
        return Component.translatableWithFallback(key, english, args);
    }

    public static int poolSize(String key) {
        int size = 0;
        while (ENGLISH.containsKey(key + "." + (size + 1))) {
            size++;
        }
        return size;
    }

    /** Cosmetic variety, never a game roll. */
    public static MutableComponent oneOf(String key, RandomSource random, Object... args) {
        int size = poolSize(key);
        if (size == 0) {
            throw new IllegalArgumentException("en_us.json has no '" + key + ".1'");
        }
        return of(key + "." + (random.nextInt(size) + 1), args);
    }

    public static MutableComponent ofOrElse(String key, Supplier<String> fallback) {
        String text = ENGLISH.get(key);
        if (text == null) {
            Language language = Language.getInstance();
            text = language.has(key) ? language.getOrDefault(key) : fallback.get();
        }
        return Component.translatableWithFallback(key, text);
    }

    private static String english(String key) {
        String english = ENGLISH.get(key);
        if (english == null) {
            throw new IllegalArgumentException("en_us.json has no '" + key + "'");
        }
        return english;
    }

    private static Map<String, String> load() {
        Map<String, String> entries = new HashMap<>();
        try (InputStream in = FallbackText.class.getResourceAsStream(EN_US)) {
            if (in == null) {
                throw new IllegalStateException(EN_US + " is missing from the mod jar");
            }
            Language.loadFromJson(in, entries::put);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Map.copyOf(entries);
    }
}
