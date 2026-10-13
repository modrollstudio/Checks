package studio.modroll.checks.critfall;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.CheckKind;
import studio.modroll.checks.bonus.BonusPart;
import studio.modroll.checks.save.VanillaSave;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.trait.TraitHooks;
import studio.modroll.critfall.api.HazardSave;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.critfall.api.event.CritfallEvents;
import studio.modroll.critfall.api.event.HazardRollEvent;

/**
 * Traits on Critfall's hazard saves, from any mod: a hazard whose damage type is in {@link #POISON} counts as
 * the vanilla poison save, and a trait roll bonus counts its advantage or disadvantage on the save's ability.
 * The event takes only a roll mode, so a roll bonus's flat bonus and rerolls stay with Checks' own rolls.
 * Hazard attacks are left alone: no trait changes an attack roll against the player.
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
            for (RollMode mode : modes(event.target(), save)) {
                if (mode == RollMode.ADVANTAGE) {
                    event.grantAdvantage();
                } else if (mode == RollMode.DISADVANTAGE) {
                    event.imposeDisadvantage();
                }
            }
        });
    }

    /** One mode per trait source; Critfall combines them. */
    private static List<RollMode> modes(LivingEntity target, HazardSave save) {
        List<RollMode> modes = new ArrayList<>();
        if (poison(target, save.damageType())) {
            modes.add(TraitHooks.vanillaSaveMode(target, VanillaSave.POISON));
        }
        ability(save.saveKey()).ifPresent(ability -> TraitHooks.rollBonuses(target, CheckKind.SAVE, ability).stream()
                .map(BonusPart::mode)
                .forEach(modes::add));
        return modes;
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
