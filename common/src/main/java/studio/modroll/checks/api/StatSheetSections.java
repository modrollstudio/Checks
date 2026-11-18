package studio.modroll.checks.api;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.sheet.SheetSectionRegistry;

public final class StatSheetSections {

    private StatSheetSections() {}

    public static void register(ResourceLocation id, Supplier<Optional<SheetSection>> section) {
        SheetSectionRegistry.register(id, section);
    }
}
