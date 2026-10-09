package studio.modroll.checks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/**
 * Makes the mod's en_us lang file the active {@link Language}, for tests that read translated text, or
 * an empty one, as on a client without Checks.
 */
public final class ModLang {

    private static final Path EN_US = Path.of("src/main/resources/assets/checks/lang/en_us.json");

    private ModLang() {}

    public static void injectWithoutChecks() {
        Language.inject(new Language() {
            @Override
            public String getOrDefault(String key, String fallback) {
                return fallback;
            }

            @Override
            public boolean has(String key) {
                return false;
            }

            @Override
            public boolean isDefaultRightToLeft() {
                return false;
            }

            @Override
            public FormattedCharSequence getVisualOrder(FormattedText text) {
                return FormattedCharSequence.EMPTY;
            }
        });
    }

    public static void inject() throws IOException {
        Map<String, String> entries = new HashMap<>();
        try (InputStream in = Files.newInputStream(EN_US)) {
            Language.loadFromJson(in, entries::put);
        }
        Language.inject(new Language() {
            @Override
            public String getOrDefault(String key, String fallback) {
                return entries.getOrDefault(key, fallback);
            }

            @Override
            public boolean has(String key) {
                return entries.containsKey(key);
            }

            @Override
            public boolean isDefaultRightToLeft() {
                return false;
            }

            @Override
            public FormattedCharSequence getVisualOrder(FormattedText text) {
                return FormattedCharSequence.EMPTY;
            }
        });
    }
}
