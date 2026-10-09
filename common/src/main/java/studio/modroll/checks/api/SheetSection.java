package studio.modroll.checks.api;

import java.util.List;
import net.minecraft.network.chat.Component;

/** A section another mod adds to the stat screen: a title over rows. See {@link StatSheetSections}. */
public record SheetSection(Component title, List<SheetRow> rows) {

    public SheetSection {
        rows = List.copyOf(rows);
    }
}
