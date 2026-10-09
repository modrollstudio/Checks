package studio.modroll.checks.critfall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.critfall.api.AttackDelivery;

class ChecksModifierProviderTest {

    private static final ScoresConfig.CritfallSettings DEFAULTS = ScoresConfig.DEFAULTS.critfall();

    @Test
    void meleeUsesStrengthAndRangedUsesDexterity() {
        assertEquals(Optional.of(Ability.STRENGTH), ChecksModifierProvider.weaponAbility(AttackDelivery.MELEE));
        assertEquals(Optional.of(Ability.DEXTERITY), ChecksModifierProvider.weaponAbility(AttackDelivery.PROJECTILE));
        assertEquals(Optional.of(Ability.DEXTERITY), ChecksModifierProvider.weaponAbility(AttackDelivery.THROWN));
    }

    @Test
    void spellsHaveNoWeaponAbility() {
        assertEquals(Optional.empty(), ChecksModifierProvider.weaponAbility(AttackDelivery.SPELL));
    }

    @Test
    void playersAndProfiledMobsAreCovered() {
        assertTrue(ChecksModifierProvider.covers(DEFAULTS, true, false));
        assertTrue(ChecksModifierProvider.covers(DEFAULTS, false, true));
    }

    @Test
    void unprofiledMobsAreCoveredOnlyWhenEnabled() {
        assertFalse(ChecksModifierProvider.covers(DEFAULTS, false, false));
        assertTrue(
                ChecksModifierProvider.covers(new ScoresConfig.CritfallSettings(true, true, Map.of()), false, false));
    }
}
