package studio.modroll.checks.critfall;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.body.BodyValues;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoreService;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.critfall.api.AttackDelivery;
import studio.modroll.critfall.api.ModifierProvider;
import studio.modroll.critfall.api.RollService;

/**
 * Supplies Critfall's attack, damage and save modifiers and the DEX part of AC from Checks scores.
 * Entities it does not cover get empty answers, which keep Critfall's own values.
 */
public final class ChecksModifierProvider implements ModifierProvider {

    private static final ChecksModifierProvider INSTANCE = new ChecksModifierProvider();

    private ChecksModifierProvider() {}

    /**
     * Holds Critfall's single provider slot while enabled and frees it while disabled. Another mod's
     * provider is never replaced or removed, so a {@code /reload} cannot take the slot back from it.
     */
    public static void sync(ScoresConfig.CritfallSettings settings) {
        Optional<ModifierProvider> current = RollService.modifierProvider();
        if (!settings.enabled()) {
            current.filter(p -> p == INSTANCE).ifPresent(p -> RollService.clearModifierProvider());
        } else if (current.isEmpty()) {
            RollService.registerModifierProvider(INSTANCE);
        } else if (current.get() != INSTANCE) {
            Checks.LOG.warn(
                    "Critfall's modifier provider slot is held by {}; Checks supplies no Critfall modifiers",
                    current.get().getClass().getName());
        }
    }

    @Override
    public OptionalInt attackModifier(LivingEntity attacker, LivingEntity target, AttackDelivery delivery) {
        Optional<Ability> ability = weaponAbility(delivery);
        if (ability.isEmpty() || !covers(attacker)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(ScoreService.modifier(attacker, ability.get()) + ScoreService.proficiencyBonus(attacker));
    }

    @Override
    public OptionalInt damageModifier(LivingEntity attacker, AttackDelivery delivery) {
        Optional<Ability> ability = weaponAbility(delivery);
        if (ability.isEmpty() || !covers(attacker)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(ScoreService.modifier(attacker, ability.get()));
    }

    @Override
    public OptionalInt saveModifier(LivingEntity entity, String saveKey) {
        Ability ability = settings().saveAbilities().get(saveKey);
        if (ability == null || !covers(entity)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(ScoreService.saveModifier(entity, ability));
    }

    /** DEX by the 5e armor rules; Critfall's own AC already counts the armor itself. */
    @Override
    public OptionalInt acModifier(LivingEntity defender, LivingEntity attacker) {
        if (!covers(defender)) {
            return OptionalInt.empty();
        }
        return BodyValues.armorClass(defender)
                .map(armor -> OptionalInt.of(armor.bonus()))
                .orElse(OptionalInt.empty());
    }

    /** Whether Checks holds Critfall's provider slot, so its answers reach Critfall's rolls. */
    public static boolean active() {
        return RollService.modifierProvider()
                .filter(provider -> provider == INSTANCE)
                .isPresent();
    }

    /** STR for melee, DEX for projectile and thrown; spells have none. */
    static Optional<Ability> weaponAbility(AttackDelivery delivery) {
        return switch (delivery) {
            case MELEE -> Optional.of(Ability.STRENGTH);
            case PROJECTILE, THROWN -> Optional.of(Ability.DEXTERITY);
            case SPELL -> Optional.empty();
        };
    }

    static boolean covers(ScoresConfig.CritfallSettings settings, boolean isPlayer, boolean hasProfile) {
        return isPlayer || hasProfile || settings.unprofiledMobs();
    }

    private static boolean covers(LivingEntity entity) {
        return covers(settings(), entity instanceof Player, ScoreService.hasProfile(entity));
    }

    private static ScoresConfig.CritfallSettings settings() {
        return ScoresRuntime.config().critfall();
    }
}
