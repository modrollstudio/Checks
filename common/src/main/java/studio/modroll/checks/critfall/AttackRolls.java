package studio.modroll.checks.critfall;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.trait.TraitHooks;
import studio.modroll.critfall.api.event.CritfallEvents;
import studio.modroll.critfall.api.event.PostAttackRollEvent;
import studio.modroll.critfall.api.event.PreAttackRollEvent;

/**
 * Traits on Critfall's attack rolls: a trait that rerolls natural 1s (Luck) rerolls the attacker's. Rerolls
 * that need the roll's outcome stay with Checks' own rolls, and bonus sources never reach attack rolls. Each
 * hit Critfall rolls is remembered for the rest of its tick, so what the hit does next (Fire Aspect setting the
 * target alight) calls for no second roll.
 */
public final class AttackRolls {

    /** Each target of a hit Critfall rolled, by the server tick of the hit; only this tick's are kept. */
    private static final Map<UUID, Long> ROLLED_HITS = new ConcurrentHashMap<>();

    private AttackRolls() {}

    /** Once at startup; Critfall's listeners stay for the game's lifetime. */
    public static void register() {
        CritfallEvents.onPreAttackRoll(AttackRolls::onPreAttackRoll);
        CritfallEvents.onPostAttackRoll(AttackRolls::onPostAttackRoll);
    }

    static void onPreAttackRoll(PreAttackRollEvent event) {
        if (ScoresRuntime.config().critfall().enabled() && TraitHooks.rerollsOnes(event.attacker())) {
            event.rerollOnes();
        }
    }

    /** A miss or fumble never lands, so nothing follows from it. */
    static void onPostAttackRoll(PostAttackRollEvent event) {
        MinecraftServer server = event.target().getServer();
        if (server != null && event.result().isHit()) {
            long now = server.getTickCount();
            ROLLED_HITS.values().removeIf(tick -> tick != now);
            ROLLED_HITS.put(event.target().getUUID(), now);
        }
    }

    /**
     * Whether a hit Critfall rolled landed on {@code target} earlier this tick. Asking takes the hit, so one
     * attack roll stands in for one follow-up save, never for a later one.
     */
    public static boolean takeRolledHit(LivingEntity target) {
        MinecraftServer server = target.getServer();
        Long tick = server == null ? null : ROLLED_HITS.remove(target.getUUID());
        return tick != null && tick == server.getTickCount();
    }

    public static void clear() {
        ROLLED_HITS.clear();
    }
}
