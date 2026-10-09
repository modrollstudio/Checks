package studio.modroll.checks.sheet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.SheetSection;

/** The stat screen sections registered through {@link studio.modroll.checks.api.StatSheetSections}. */
public final class SheetSectionRegistry {

    private static final Map<ResourceLocation, Supplier<Optional<SheetSection>>> SECTIONS = new LinkedHashMap<>();

    private SheetSectionRegistry() {}

    public static synchronized void register(ResourceLocation id, Supplier<Optional<SheetSection>> section) {
        SECTIONS.put(id, section);
    }

    /** None while the server has mod sections switched off. */
    public static synchronized List<SheetSection> sectionsFor(StatSheet sheet) {
        if (!sheet.modSections()) {
            return List.of();
        }
        List<SheetSection> sections = new ArrayList<>();
        SECTIONS.forEach((id, section) -> collect(id, section).ifPresent(sections::add));
        return sections;
    }

    private static Optional<SheetSection> collect(ResourceLocation id, Supplier<Optional<SheetSection>> section) {
        try {
            return section.get();
        } catch (RuntimeException e) {
            Checks.LOG.error("Stat screen section {} threw; leaving it out", id, e);
            return Optional.empty();
        }
    }

    /** For tests. */
    public static synchronized void clear() {
        SECTIONS.clear();
    }
}
