package studio.modroll.checks.api;

import java.util.List;
import net.minecraft.network.chat.Component;

public record SheetRow(Component label, Component value, List<Component> tooltip) {

    public SheetRow {
        tooltip = List.copyOf(tooltip);
    }

    public SheetRow(Component label, Component value) {
        this(label, value, List.of());
    }
}
