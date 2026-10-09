package studio.modroll.checks.body;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.creation.CharacterBuild;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.level.Levelling;
import studio.modroll.checks.level.PlayerLevel;
import studio.modroll.checks.preset.Presets;
import studio.modroll.checks.score.ScoreService;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.trait.TraitEffect;
import studio.modroll.checks.trait.Traits;

/**
 * What a creature's scores do to its body right now, each part empty or zero while its switch is off.
 * Health, size and the extras are for players only; armor class is for any entity.
 */
public final class BodyValues {

    private BodyValues() {}

    /** The max health CON gives, and the hit dice of the levels gained when those count too. */
    public record HealthBonus(int conModifier, double conHealth, double levelHealth) {

        public double total() {
            return conHealth + levelHealth;
        }
    }

    /** What DEX adds to AC in the armor worn: {@code bonus} out of {@code dexModifier}. */
    public record ArmorBonus(int dexModifier, ArmorCategory category, int bonus) {}

    public static Optional<HealthBonus> health(ServerPlayer player) {
        ScoresConfig.HealthSettings settings = settings().health();
        if (!settings.enabled()) {
            return Optional.empty();
        }
        int conModifier = ScoreService.modifier(player, Ability.CONSTITUTION);
        PlayerLevel level = Levelling.activeLevel(player);
        double levelHealth =
                settings.perLevelEnabled() ? BodyRules.levelHealth(level.level(), level.hitDice(), settings) : 0.0;
        return Optional.of(new HealthBonus(conModifier, BodyRules.conHealth(conModifier, settings), levelHealth));
    }

    /** The confirmed character's species size, while sizes and presets are on. */
    public static Optional<SizeOption> size(ServerPlayer player) {
        if (!settings().sizeEnabled()
                || !ScoresRuntime.config().creation().presets().enabled()) {
            return Optional.empty();
        }
        Optional<PresetChoices> choices = CharacterCreation.activeBuild(player).map(CharacterBuild::choices);
        return choices.flatMap(PresetChoices::species)
                .flatMap(Presets.SPECIES::find)
                .flatMap(species -> species.size(choices.get().size()));
    }

    /** What the species' extra health traits add at the player's level, each rounded down. */
    public static double traitHealth(ServerPlayer player) {
        int level = Levelling.activeLevel(player).level();
        return Traits.effects(player, TraitEffect.ExtraHealth.class).stream()
                .mapToDouble(health -> BodyRules.levelHealth(level, health.perLevel()))
                .sum();
    }

    /** Zero for anything but a server-side player, and while the extra is off. */
    public static double extra(LivingEntity entity, Extra extra) {
        ScoresConfig.ExtraSetting setting = settings().extra(extra);
        if (!(entity instanceof ServerPlayer player) || !setting.enabled()) {
            return 0.0;
        }
        return BodyRules.extraAmount(ScoreService.modifier(player, extra.ability()), setting);
    }

    /** Empty while armor class is off; which entities Critfall asks about is the provider's call. */
    public static Optional<ArmorBonus> armorClass(LivingEntity entity) {
        ScoresConfig.ArmorClassSettings settings = settings().armorClass();
        if (!settings.enabled()) {
            return Optional.empty();
        }
        int dexModifier = ScoreService.modifier(entity, Ability.DEXTERITY);
        ArmorCategory category = ArmorCategory.heaviest(entity.getArmorSlots());
        return Optional.of(new ArmorBonus(
                dexModifier, category, BodyRules.armorClassBonus(dexModifier, category, settings.mediumMaxDex())));
    }

    private static ScoresConfig.BodySettings settings() {
        return ScoresRuntime.config().body();
    }
}
