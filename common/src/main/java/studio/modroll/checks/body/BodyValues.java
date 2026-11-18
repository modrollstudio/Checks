package studio.modroll.checks.body;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.critfall.CritfallArmorClass;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.level.Levelling;
import studio.modroll.checks.preset.ClassPreset;
import studio.modroll.checks.preset.Presets;
import studio.modroll.checks.score.ScoreService;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.trait.TraitEffect;
import studio.modroll.checks.trait.Traits;

public final class BodyValues {

    private BodyValues() {}

    public record HealthBonus(int conModifier, double conHealth, int hitDie, double levelHealth) {

        public double total() {
            return conHealth + levelHealth;
        }
    }

    public record ArmorBonus(int dexModifier, ArmorCategory category, int dexBonus, int floorBonus) {

        public int bonus() {
            return dexBonus + floorBonus;
        }
    }

    public static Optional<HealthBonus> health(ServerPlayer player) {
        return health(player, Levelling.activeLevel(player).level());
    }

    private static Optional<HealthBonus> health(ServerPlayer player, int level) {
        ScoresConfig.HealthSettings settings = settings().health();
        if (!settings.enabled()) {
            return Optional.empty();
        }
        int conModifier = ScoreService.modifier(player, Ability.CONSTITUTION);
        int hitDie = hitDie(player);
        double levelHealth = levelHealth(level, hitDie, conModifier, settings);
        return Optional.of(
                new HealthBonus(conModifier, BodyRules.conHealth(conModifier, settings), hitDie, levelHealth));
    }

    public static int hitDie(ServerPlayer player) {
        return Levelling.classPreset(player)
                .map(ClassPreset::hitDie)
                .orElse(settings().health().customHitDie());
    }

    public static double maxHealthBonus(ServerPlayer player) {
        return maxHealthBonus(player, Levelling.activeLevel(player).level());
    }

    public static double maxHealthBonus(ServerPlayer player, int level) {
        double bonus = health(player, level).map(HealthBonus::total).orElse(0.0) + traitHealth(player, level);
        return settings().health().wholeHearts() ? BodyRules.wholeHearts(bonus) : bonus;
    }

    private static double levelHealth(int level, int hitDie, int conModifier, ScoresConfig.HealthSettings settings) {
        return settings.perLevelEnabled() ? BodyRules.levelHealth(level, hitDie, conModifier, settings) : 0.0;
    }

    public static Optional<SizeOption> size(ServerPlayer player) {
        if (!settings().sizeEnabled()) {
            return Optional.empty();
        }
        Optional<PresetChoices> choices = CharacterCreation.activePresets(player);
        return choices.flatMap(PresetChoices::species)
                .flatMap(Presets.SPECIES::find)
                .flatMap(species -> species.size(choices.get().size()));
    }

    public static double traitHealth(ServerPlayer player) {
        return traitHealth(player, Levelling.activeLevel(player).level());
    }

    private static double traitHealth(ServerPlayer player, int level) {
        double multiplier = settings().health().perLevelMultiplier();
        return Traits.effects(player, TraitEffect.ExtraHealth.class).stream()
                .mapToDouble(health -> BodyRules.levelHealth(level, health.perLevel() * multiplier))
                .sum();
    }

    public static double extra(LivingEntity entity, Extra extra) {
        ScoresConfig.ExtraSetting setting = settings().extra(extra);
        if (!(entity instanceof ServerPlayer player) || !setting.enabled()) {
            return 0.0;
        }
        return BodyRules.extraAmount(ScoreService.modifier(player, extra.ability()), setting);
    }

    public static Optional<ArmorBonus> armorClass(LivingEntity entity) {
        ScoresConfig.ArmorClassSettings settings = settings().armorClass();
        if (!settings.enabled()) {
            return Optional.empty();
        }
        int dexModifier = ScoreService.modifier(entity, Ability.DEXTERITY);
        ArmorCategory category = ArmorCategory.worn(entity);
        int dexBonus = BodyRules.armorClassBonus(dexModifier, category, settings.mediumMaxDex());
        int floorBonus = dexBonus < dexModifier
                ? BodyRules.bodyArmorFloor(
                        CritfallArmorClass.worn(entity),
                        dexBonus,
                        CritfallArmorClass.withoutBodyArmor(entity),
                        dexModifier)
                : 0;
        return Optional.of(new ArmorBonus(dexModifier, category, dexBonus, floorBonus));
    }

    private static ScoresConfig.BodySettings settings() {
        return ScoresRuntime.config().body();
    }
}
