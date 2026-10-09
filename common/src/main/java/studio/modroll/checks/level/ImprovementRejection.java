package studio.modroll.checks.level;

import net.minecraft.network.chat.Component;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.data.ScoresConfig.ImprovementSettings;
import studio.modroll.checks.sheet.SheetText;

/** Why the server refused an ability score improvement; each has a lang line under {@code checks.level.rejected}. */
public enum ImprovementRejection {
    LEVELLING_DISABLED("levelling_disabled"),
    NONE_PENDING("none_pending"),
    WRONG_SPLIT("wrong_split"),
    OVER_MAX("over_max");

    private final String id;

    ImprovementRejection(String id) {
        this.id = id;
    }

    public Component message(ImprovementSettings settings) {
        String key = "checks.level.rejected." + id;
        return switch (this) {
            case WRONG_SPLIT ->
                Component.translatable(key, SheetText.bonusSplits(settings.options(), Ability.values().length));
            case OVER_MAX -> Component.translatable(key, settings.maxScore());
            default -> Component.translatable(key);
        };
    }
}
