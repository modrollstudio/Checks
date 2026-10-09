package studio.modroll.checks.social;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * Villagers who see a crime on a villager, a caught pickpocket or a failed threat: every villager within
 * the gossip radius of the target that can see the player or the target. Each refuses the player for a
 * share of the target's refusal and runs from them, angry at a thief or scared of a bully, and says a line
 * from the target's pool for that failure.
 */
final class Witnesses {

    private Witnesses() {}

    static void saw(ServerPlayer player, Villager target, SocialAction action, int targetRefuseTicks) {
        ScoresConfig.SocialSettings settings = ScoresRuntime.config().social();
        ScoresConfig.WitnessSettings witnesses = settings.witnesses();
        if (!witnesses.enabled()) {
            return;
        }
        int refuseTicks = (int) Math.round(targetRefuseTicks * witnesses.refuseFraction());
        SocialMemory memory = SocialMemory.get(player.server);
        for (Villager witness : around(player, target, settings.gossipRadius())) {
            memory.refuseAsWitness(
                    action, player.getUUID(), witness.getUUID(), Socials.now(player.server), refuseTicks);
            SocialReactions.flee(player, witness, witnesses.fleeDistance(), settings.reactions());
            if (action == SocialAction.INTIMIDATE) {
                SocialReactions.showScared(witness);
            } else {
                SocialReactions.showAngry(witness);
            }
            SocialFeel.witnessed(player, witness, action);
        }
    }

    private static List<Villager> around(ServerPlayer player, Villager target, double radius) {
        return target.level()
                .getEntitiesOfClass(
                        Villager.class,
                        target.getBoundingBox().inflate(radius),
                        witness -> witness != target
                                && (witness.hasLineOfSight(player) || witness.hasLineOfSight(target)));
    }
}
