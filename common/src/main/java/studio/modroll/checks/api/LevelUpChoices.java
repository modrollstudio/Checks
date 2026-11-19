package studio.modroll.checks.api;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.sheet.LevelUpChoiceRegistry;

public final class LevelUpChoices {

    private LevelUpChoices() {}

    public static void register(ResourceLocation id, Supplier<Optional<LevelUpChoice>> choice) {
        LevelUpChoiceRegistry.register(id, choice);
    }
}
