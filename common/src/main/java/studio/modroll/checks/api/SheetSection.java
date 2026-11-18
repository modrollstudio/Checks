package studio.modroll.checks.api;

import java.util.List;
import net.minecraft.network.chat.Component;

public record SheetSection(Component title, List<SheetRow> rows) {

    public SheetSection {
        rows = List.copyOf(rows);
    }
}
