package studio.modroll.checks.data;

import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/**
 * One entry of a profile's {@code matches} list: an exact id, a {@code #tag} or a {@code namespace:*}.
 * Tag membership comes in as a predicate so this stays testable without a loaded registry.
 */
public sealed interface MatchEntry {

    boolean matches(ResourceLocation id, Predicate<ResourceLocation> tagTest);

    /** Breaks priority ties: exact id = 3, tag = 2, namespace wildcard = 1. */
    int specificity();

    static MatchEntry parse(String text) {
        if (text.isBlank()) {
            throw new IllegalArgumentException("empty matches entry");
        }
        if (text.startsWith("#")) {
            return new Tag(parseId(text.substring(1), text));
        }
        if (text.endsWith(":*")) {
            String namespace = text.substring(0, text.length() - 2);
            if (!ResourceLocation.isValidNamespace(namespace)) {
                throw new IllegalArgumentException("invalid namespace in matches entry \"" + text + "\"");
            }
            return new Namespace(namespace);
        }
        return new Exact(parseId(text, text));
    }

    private static ResourceLocation parseId(String id, String entry) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null || parsed.getPath().isEmpty()) {
            throw new IllegalArgumentException("invalid id in matches entry \"" + entry + "\"");
        }
        return parsed;
    }

    record Exact(ResourceLocation id) implements MatchEntry {
        @Override
        public boolean matches(ResourceLocation candidate, Predicate<ResourceLocation> tagTest) {
            return id.equals(candidate);
        }

        @Override
        public int specificity() {
            return 3;
        }
    }

    record Tag(ResourceLocation tagId) implements MatchEntry {
        @Override
        public boolean matches(ResourceLocation candidate, Predicate<ResourceLocation> tagTest) {
            return tagTest.test(tagId);
        }

        @Override
        public int specificity() {
            return 2;
        }
    }

    record Namespace(String namespace) implements MatchEntry {
        @Override
        public boolean matches(ResourceLocation candidate, Predicate<ResourceLocation> tagTest) {
            return candidate.getNamespace().equals(namespace);
        }

        @Override
        public int specificity() {
            return 1;
        }
    }
}
