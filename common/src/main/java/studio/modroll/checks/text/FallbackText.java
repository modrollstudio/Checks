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

/**
 * Lang-file translations that carry their en_us text as the fallback, for text a client without Checks
 * may see: command feedback and chat broadcast to every player. Such a client shows English instead of
 * a raw key. The en_us text is read once from the mod's own lang file, which stays the only copy of
 * every string.
 */
public final class FallbackText {

    private static final String EN_US = "/assets/checks/lang/en_us.json";
    private static final Map<String, String> ENGLISH = load();

    private FallbackText() {}

    /** Fails fast on a key the lang file lacks, so a typo cannot reach a player as a raw key. */
    public static MutableComponent of(String key, Object... args) {
        return Component.translatableWithFallback(key, english(key), args);
    }

    /**
     * As {@link #of}, for a key the lang file may lack, such as one named after a mob from another mod: then
     * the en_us text of {@code fallbackKey} stands in, and a resource pack can still translate {@code key}.
     */
    public static MutableComponent ofOr(String key, String fallbackKey, Object... args) {
        String english = ENGLISH.containsKey(key) ? ENGLISH.get(key) : english(fallbackKey);
        return Component.translatableWithFallback(key, english, args);
    }

    /** How many lines the pool {@code key.1}, {@code key.2}, … has. */
    public static int poolSize(String key) {
        int size = 0;
        while (ENGLISH.containsKey(key + "." + (size + 1))) {
            size++;
        }
        return size;
    }

    /**
     * One line of the pool {@code key.1}, {@code key.2}, …, chosen with {@code random}: cosmetic variety,
     * never a game roll.
     */
    public static MutableComponent oneOf(String key, RandomSource random, Object... args) {
        int size = poolSize(key);
        if (size == 0) {
            throw new IllegalArgumentException("en_us.json has no '" + key + ".1'");
        }
        return of(key + "." + (random.nextInt(size) + 1), args);
    }

    /**
     * For a key Checks may not ship, such as one a mod or resource pack adds for its own mob: the en_us text
     * Checks has, else the server's own translation, else {@code fallback}. A client with the key shows its
     * own line either way.
     */
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
