package studio.modroll.checks.sheet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.LevelUpChoice;

public final class LevelUpChoiceRegistry {

    private static final Map<ResourceLocation, Supplier<Optional<LevelUpChoice>>> CHOICES = new LinkedHashMap<>();

    private LevelUpChoiceRegistry() {}

    public static synchronized void register(ResourceLocation id, Supplier<Optional<LevelUpChoice>> choice) {
        CHOICES.put(id, choice);
    }

    public static synchronized List<LevelUpChoice> choicesFor(StatSheet.LevelLine line) {
        if (!line.modChoices()) {
            return List.of();
        }
        List<LevelUpChoice> choices = new ArrayList<>();
        CHOICES.forEach((id, choice) -> collect(id, choice).ifPresent(choices::add));
        return choices;
    }

    public static boolean pending(StatSheet.LevelLine line) {
        return line.choicesPending() || choicesFor(line).stream().anyMatch(choice -> choice.count() > 0);
    }

    public static Component label(LevelUpChoice choice) {
        return choice.count() > 0
                ? Component.translatable("screen.checks.level_up.choice", choice.label(), choice.count())
                : choice.label();
    }

    public static void open(LevelUpChoice choice) {
        try {
            choice.open().run();
        } catch (RuntimeException e) {
            Checks.LOG.error("A Level up! button from another mod threw", e);
        }
    }

    private static Optional<LevelUpChoice> collect(ResourceLocation id, Supplier<Optional<LevelUpChoice>> choice) {
        try {
            return choice.get();
        } catch (RuntimeException e) {
            Checks.LOG.error("Level up! button {} threw; leaving it out", id, e);
            return Optional.empty();
        }
    }

    public static synchronized void clear() {
        CHOICES.clear();
    }
}
