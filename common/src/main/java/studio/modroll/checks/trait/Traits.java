package studio.modroll.checks.trait;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.creation.SkillChoice;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.preset.Presets;
import studio.modroll.checks.preset.Species;
import studio.modroll.checks.score.ScoresRuntime;

/** Traits come from a confirmed character's species, so they apply to players only. */
public final class Traits {

    private static volatile Map<ResourceLocation, Trait> traits = Map.of();

    private Traits() {}

    public static void set(Map<ResourceLocation, Trait> loaded) {
        traits = Map.copyOf(loaded);
    }

    public static Map<ResourceLocation, Trait> all() {
        return traits;
    }

    public static void clear() {
        set(Map.of());
    }

    public record Granted<T extends TraitEffect>(ResourceLocation trait, int index, T effect) {}

    public static <T extends TraitEffect> List<Granted<T>> granted(LivingEntity entity, Class<T> type) {
        if (!(entity instanceof ServerPlayer player)) {
            return List.of();
        }
        return species(player).stream()
                .flatMap(species -> granted(species, type))
                .toList();
    }

    public static <T extends TraitEffect> List<T> effects(LivingEntity entity, Class<T> type) {
        return granted(entity, type).stream().map(Granted::effect).toList();
    }

    public static <T extends TraitEffect> Stream<Granted<T>> granted(Species species, Class<T> type) {
        return species.traits().stream()
                .flatMap(id -> Optional.ofNullable(traits.get(id)).stream())
                .flatMap(trait -> grantedBy(trait, type));
    }

    public static SkillChoice skillChoice(Species species) {
        return SkillChoice.pooled(granted(species, TraitEffect.SkillChoices.class)
                .map(granted -> new SkillChoice(
                        granted.effect().count(), granted.effect().from()))
                .toList());
    }

    private static <T extends TraitEffect> Stream<Granted<T>> grantedBy(Trait trait, Class<T> type) {
        List<TraitEffect> effects = trait.effects();
        return IntStream.range(0, effects.size())
                .filter(index -> type.isInstance(effects.get(index))
                        && settings().active(effects.get(index).type()))
                .mapToObj(index -> new Granted<>(trait.id(), index, type.cast(effects.get(index))));
    }

    private static Optional<Species> species(ServerPlayer player) {
        return CharacterCreation.activePresets(player)
                .flatMap(PresetChoices::species)
                .flatMap(Presets.SPECIES::find);
    }

    private static ScoresConfig.TraitSettings settings() {
        return ScoresRuntime.config().traits();
    }
}
