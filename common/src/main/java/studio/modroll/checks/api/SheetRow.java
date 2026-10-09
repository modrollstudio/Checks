package studio.modroll.checks.api;

import java.util.List;
import net.minecraft.network.chat.Component;

/** One stat screen row: a label on the left, a value on the right, and the lines shown on hover. */
public record SheetRow(Component label, Component value, List<Component> tooltip) {

    public SheetRow {
        tooltip = List.copyOf(tooltip);
    }

    public SheetRow(Component label, Component value) {
        this(label, value, List.of());
    }
}
