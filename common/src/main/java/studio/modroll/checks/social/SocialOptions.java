package studio.modroll.checks.social;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.bonus.BonusSources;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * Which social actions a player may try on an entity right now, and why not. A villager on guard offers
 * Plead and Lie in place of Persuade and Deceive, and greys out Pickpocket and Intimidate. A mob is
 * offered only the actions for mobs.
 */
final class SocialOptions {

    private SocialOptions() {}

    /** One option per action that is switched on and whose skills are loaded, in action order. */
    static SocialMenu menu(ServerPlayer player, LivingEntity entity) {
        return new SocialMenu(
                entity.getId(),
                Arrays.stream(SocialAction.values())
                        .flatMap(action -> option(player, entity, action).stream())
                        .toList());
    }

    static Optional<SocialMenu.Option> option(ServerPlayer player, LivingEntity entity, SocialAction action) {
        ScoresConfig.SocialSettings settings = ScoresRuntime.config().social();
        boolean passiveDcLoaded =
                action.passiveDc().map(id -> ChecksApi.skill(id).isPresent()).orElse(true);
        boolean onGuard = GolemAlarms.onGuard(player, entity);
        boolean mob = SocialTarget.of(entity).map(SocialTarget::mob).orElse(false);
        if (!settings.active(action) || !passiveDcLoaded || !offered(action, onGuard, mob)) {
            return Optional.empty();
        }
        return ChecksApi.skill(action.skill()).map(skill -> option(player, entity, action, skill, settings, onGuard));
    }

    private static boolean offered(SocialAction action, boolean onGuard, boolean mob) {
        if (action.onMobs() != mob) {
            return false;
        }
        return switch (action) {
            case PERSUADE, DECEIVE -> !onGuard;
            case PLEAD, LIE -> onGuard;
            case INTIMIDATE, PICKPOCKET, INTIMIDATE_MOB, CALM -> true;
        };
    }

    private static SocialMenu.Option option(
            ServerPlayer player,
            LivingEntity entity,
            SocialAction action,
            Skill skill,
            ScoresConfig.SocialSettings settings,
            boolean onGuard) {
        OptionalLong cooldown = SocialMemory.get(player.server)
                .cooldownLeft(action, player.getUUID(), entity.getUUID(), Socials.now(player.server));
        return new SocialMenu.Option(
                action,
                BonusSources.skillModifier(player, skill),
                advantage(player, entity, action, settings),
                wary(player, entity, action, settings.wary()),
                availability(player, entity, action, cooldown, onGuard),
                cooldown.orElse(0));
    }

    private static SocialMenu.Availability availability(
            ServerPlayer player, LivingEntity entity, SocialAction action, OptionalLong cooldown, boolean onGuard) {
        Optional<SocialTarget> target = SocialTarget.of(entity).filter(action::worksOn);
        if (target.isEmpty()) {
            return SocialMenu.Availability.WRONG_TARGET;
        }
        if (onGuard && action.villagersGuardAgainst()) {
            return SocialMenu.Availability.ON_GUARD;
        }
        if (action.deEscalates()) {
            return GolemAlarms.tried(player, action)
                    ? SocialMenu.Availability.TRIED
                    : SocialMenu.Availability.AVAILABLE;
        }
        if (target.get().trades() && ((AbstractVillager) entity).getOffers().isEmpty()) {
            return SocialMenu.Availability.NO_TRADES;
        }
        if (action == SocialAction.INTIMIDATE_MOB && SocialMobs.fearless(entity)) {
            return SocialMenu.Availability.FEARLESS;
        }
        if (action == SocialAction.CALM && !SocialMobs.angryAt(entity, player)) {
            return SocialMenu.Availability.NOT_ANGRY;
        }
        if (cooldown.isPresent()) {
            return SocialMenu.Availability.COOLDOWN;
        }
        return SocialMenu.Availability.AVAILABLE;
    }

    /**
     * Pickpocket or Intimidate against a villager that still refuses the player, or thinks badly enough of
     * them: it sees them coming.
     */
    private static boolean wary(
            ServerPlayer player, LivingEntity entity, SocialAction action, ScoresConfig.WarySettings wary) {
        return wary.enabled()
                && action.villagersGuardAgainst()
                && entity instanceof Villager villager
                && (Socials.refuses(player, villager) || villager.getPlayerReputation(player) <= wary.maxReputation());
    }

    /** Picking a pocket from behind the target. */
    private static boolean advantage(
            ServerPlayer player, LivingEntity entity, SocialAction action, ScoresConfig.SocialSettings settings) {
        return action == SocialAction.PICKPOCKET
                && SocialRules.behind(
                        entity.getYHeadRot(),
                        player.getX() - entity.getX(),
                        player.getZ() - entity.getZ(),
                        settings.pickpocket().behindDegrees());
    }
}
