package studio.modroll.checks.critfall;

import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.trait.TraitHooks;
import studio.modroll.critfall.api.event.CritfallEvents;
import studio.modroll.critfall.api.event.PreAttackRollEvent;

/**
 * Traits on Critfall's attack rolls: a trait that rerolls natural 1s (Luck) rerolls the attacker's. Rerolls
 * that need the roll's outcome stay with Checks' own rolls, and bonus sources never reach attack rolls.
 */
public final class AttackRolls {

    private AttackRolls() {}

    /** Once at startup; Critfall's listeners stay for the game's lifetime. */
    public static void register() {
        CritfallEvents.onPreAttackRoll(AttackRolls::onPreAttackRoll);
    }

    static void onPreAttackRoll(PreAttackRollEvent event) {
        if (ScoresRuntime.config().critfall().enabled() && TraitHooks.rerollsOnes(event.attacker())) {
            event.rerollOnes();
        }
    }
}
