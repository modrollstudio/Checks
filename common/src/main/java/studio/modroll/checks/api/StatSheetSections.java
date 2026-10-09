package studio.modroll.checks.api;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.sheet.SheetSectionRegistry;

/**
 * Client side: lets another mod add sections to the stat screen, below the skills. Checks only draws
 * them; the data and its syncing are the other mod's.
 */
public final class StatSheetSections {

    private StatSheetSections() {}

    /**
     * {@code section} is called on the client thread each time the stat screen opens; return empty to
     * show nothing this time. Sections appear in registration order; registering an id again replaces
     * its section in place. A supplier that throws is logged and skipped.
     */
    public static void register(ResourceLocation id, Supplier<Optional<SheetSection>> section) {
        SheetSectionRegistry.register(id, section);
    }
}
