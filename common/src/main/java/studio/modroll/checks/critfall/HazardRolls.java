package studio.modroll.checks.critfall;

import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.CheckKind;
import studio.modroll.checks.bonus.BonusSources;
import studio.modroll.checks.bonus.SituationalBonus;
import studio.modroll.checks.save.VanillaSave;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.trait.TraitHooks;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.critfall.api.event.CritfallEvents;
import studio.modroll.critfall.api.event.HazardRollEvent;

/**
 * Traits and bonus sources on Critfall's hazard saves, from any mod: a hazard whose damage type is in {@link
 * #POISON} counts as the vanilla poison save, the bonus sources and trait roll bonuses on saves of the save's
 * ability add their bonus and roll mode, and a trait that rerolls natural 1s (Luck) rerolls them. Rerolls that
 * need the roll's outcome (a failure, a once-a-day use) stay with Checks' own rolls. Hazard attacks are left
 * alone: no trait or bonus source changes an attack roll against the player.
 */
public final class HazardRolls {

    /** The conventional poison damage tag, so poison counts by what it is, not by which mod made it. */
    public static final TagKey<DamageType> POISON =
            TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("c", "is_poison"));

    private HazardRolls() {}

    /** Once at startup; Critfall's listeners stay for the game's lifetime. */
    public static void register() {
        CritfallEvents.onHazardRoll(HazardRolls::onHazardRoll);
    }

    static void onHazardRoll(HazardRollEvent event) {
        if (!ScoresRuntime.config().critfall().enabled()) {
            return;
        }
        event.save().ifPresent(save -> {
            LivingEntity target = event.target();
            if (poison(target, save.damageType())
                    && TraitHooks.vanillaSaveMode(target, VanillaSave.POISON) == RollMode.ADVANTAGE) {
                event.grantAdvantage();
            }
            ability(save.saveKey())
                    .ifPresent(ability -> apply(event, BonusSources.forCheck(target, CheckKind.SAVE, ability)));
            if (TraitHooks.rerollsOnes(target)) {
                event.rerollOnes();
            }
        });
    }

    /** Critfall combines the modes the 5e way, as Checks does for its own rolls. */
    private static void apply(HazardRollEvent event, SituationalBonus bonus) {
        event.addBonus(bonus.bonus());
        if (bonus.advantage()) {
            event.grantAdvantage();
        }
        if (bonus.disadvantage()) {
            event.imposeDisadvantage();
        }
    }

    /** The ability {@code save_abilities} maps the save key to, as for the save modifier. */
    private static Optional<Ability> ability(String saveKey) {
        return Optional.ofNullable(
                ScoresRuntime.config().critfall().saveAbilities().get(saveKey));
    }

    private static boolean poison(LivingEntity target, ResourceKey<DamageType> type) {
        return target.level()
                .registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(type)
                .map(holder -> holder.is(POISON))
                .orElse(false);
    }
}
