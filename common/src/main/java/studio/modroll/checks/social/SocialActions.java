package studio.modroll.checks.social;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import studio.modroll.checks.api.CheckRoll;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.check.RollMessages;
import studio.modroll.checks.check.RollText;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.death.DeathStories;
import studio.modroll.checks.death.DeathStory;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.text.FallbackText;
import studio.modroll.checks.text.Names;
import studio.modroll.critfall.api.combat.SaveResult;
import studio.modroll.critfall.api.dice.RollMode;

/**
 * Rolls a social action through {@link ChecksApi} and carries out its outcome. A roll a listener cancels
 * does nothing and starts no cooldown.
 */
final class SocialActions {

    private static final int NO_MARGIN = 0;

    private SocialActions() {}

    /** {@code option} must be available: the caller checked target, trades and cooldown. */
    static void perform(ServerPlayer player, LivingEntity target, SocialMenu.Option option) {
        SocialAction action = option.action();
        ScoresConfig.SocialSettings settings = ScoresRuntime.config().social();
        CheckRoll roll = roll(player, target, action, option.mode(), settings);
        if (roll.canceled()) {
            return;
        }
        SaveResult result = roll.result();
        SocialMemory.get(player.server)
                .startCooldown(
                        action,
                        player.getUUID(),
                        target.getUUID(),
                        Socials.now(player.server),
                        settings.cooldownTicks(action));
        SocialOutcome outcome =
                SocialOutcome.of(result.natural(), result.saveTotal(), result.dc(), barelyMargin(action, settings));
        Component flavor =
                switch (target) {
                    case Piglin piglin -> withPiglin(player, piglin, action, outcome, settings);
                    case AbstractVillager trader -> withTrader(player, trader, action, outcome, settings);
                    case Mob mob -> withMob(player, mob, action, outcome, settings);
                    default -> throw new IllegalStateException(action.id() + " has no outcome on " + target.getType());
                };
        SocialTarget.of(target).ifPresent(kind -> {
            SocialReactions.play(player, target, SocialReaction.of(action, kind, outcome));
            SocialFeel.react(player, target, kind, action, outcome);
        });
        if (action == SocialAction.INTIMIDATE && !outcome.succeeded()) {
            DeathStories.record(
                    player,
                    outcome == SocialOutcome.CRITICAL_FAILURE
                            ? DeathStory.INTIMIDATE_NATURAL_ONE
                            : DeathStory.FAILED_INTIMIDATE);
        }
        announce(player, action.skill(), result, outcome, flavor);
    }

    /** Against the target's passive skill when the action has one, else against the configured DC. */
    private static CheckRoll roll(
            ServerPlayer player,
            LivingEntity target,
            SocialAction action,
            RollMode mode,
            ScoresConfig.SocialSettings settings) {
        Skill skill = loaded(action.skill());
        return action.passiveDc()
                .map(passive -> ChecksApi.checkAgainstPassive(player, skill, target, loaded(passive), mode))
                .orElseGet(() -> ChecksApi.check(player, skill, configuredDc(action, settings), mode));
    }

    private static int configuredDc(SocialAction action, ScoresConfig.SocialSettings settings) {
        return switch (action) {
            case PERSUADE -> settings.persuade().dc();
            case PLEAD -> settings.onGuard().plead().dc();
            case LIE -> settings.onGuard().lie().dc();
            case INTIMIDATE_MOB -> settings.intimidateMob().dc();
            case CALM -> settings.calm().dc();
            case DECEIVE, INTIMIDATE, PICKPOCKET -> settings.intimidate().dc();
        };
    }

    private static Skill loaded(ResourceLocation id) {
        return ChecksApi.skill(id).orElseThrow(() -> new IllegalStateException("skill " + id + " is not loaded"));
    }

    private static int barelyMargin(SocialAction action, ScoresConfig.SocialSettings settings) {
        return switch (action) {
            case PERSUADE -> settings.persuade().deal().barelyMargin();
            case DECEIVE -> settings.deceive().deal().barelyMargin();
            case PLEAD, LIE, INTIMIDATE, PICKPOCKET, INTIMIDATE_MOB, CALM -> NO_MARGIN;
        };
    }

    private static Component withTrader(
            ServerPlayer player,
            AbstractVillager trader,
            SocialAction action,
            SocialOutcome outcome,
            ScoresConfig.SocialSettings settings) {
        return switch (action) {
            case PERSUADE ->
                haggle(player, trader, action, outcome, settings.persuade().deal());
            case DECEIVE -> deceiveTrader(player, trader, outcome, settings.deceive());
            case PLEAD, LIE -> deEscalate(player, trader, action, outcome, settings.nearbyRadius());
            case INTIMIDATE ->
                outcome.succeeded()
                        ? takeItem(player, trader, action, settings.intimidate().success())
                        : crime(player, trader, action, settings.intimidate().failure());
            case PICKPOCKET ->
                outcome.succeeded()
                        ? pickpocket(player, trader, outcome)
                        : caughtPickpocketing(player, trader, settings.pickpocket());
            case INTIMIDATE_MOB, CALM -> throw new IllegalStateException(action.id() + " only works on mobs");
        };
    }

    private static Component pickpocket(ServerPlayer player, AbstractVillager trader, SocialOutcome outcome) {
        ItemStack item = SocialEffects.takeTradeItem(player, trader);
        SocialEffects.apply(player, trader, SocialAction.PICKPOCKET, SocialEffect.NONE);
        SocialFeel.stolen(player, trader, item, outcome == SocialOutcome.BARELY);
        return flavor(trader, SocialAction.PICKPOCKET, "success", item.getHoverName());
    }

    /** The thief never gets the item: which one they reached for only shows, so Minecraft's randomness picks it. */
    private static Component caughtPickpocketing(
            ServerPlayer player, AbstractVillager trader, ScoresConfig.PickpocketSettings settings) {
        MerchantOffers offers = trader.getOffers();
        ItemStack item = offers.get(trader.getRandom().nextInt(offers.size()))
                .getResult()
                .copyWithCount(1);
        SocialFeel.caught(player, trader, item);
        DeathStories.record(player, DeathStory.CAUGHT_PICKPOCKETING, item.getHoverName());
        return crime(player, trader, SocialAction.PICKPOCKET, settings.failure());
    }

    /** A failure the village takes as a crime: villagers who saw it turn on the player too. */
    private static Component crime(
            ServerPlayer player, AbstractVillager trader, SocialAction action, SocialEffect effect) {
        Component flavor = effect(player, trader, action, "failure", effect);
        if (trader instanceof Villager villager) {
            Witnesses.saw(player, villager, action, effect.refuseTicks());
        }
        return flavor;
    }

    /**
     * Plead or Lie, once per alarm: a success ends it, a natural 20 also halves what is left of the refusals
     * the player's thefts and threats earned, and a natural 1 starts it over and calls the golems nearby
     * again. A failure changes nothing.
     */
    private static Component deEscalate(
            ServerPlayer player, AbstractVillager trader, SocialAction action, SocialOutcome outcome, double radius) {
        GolemAlarms.markTried(player, action);
        switch (outcome) {
            case CRITICAL_SUCCESS -> {
                GolemAlarms.standDown(player);
                SocialMemory.get(player.server)
                        .halveRefusals(
                                player.getUUID(), Socials.now(player.server), SocialAction::villagersGuardAgainst);
            }
            case SUCCESS, BARELY -> GolemAlarms.standDown(player);
            case FAILURE -> {}
            case CRITICAL_FAILURE -> GolemAlarms.restart(player, trader, radius);
        }
        return flavor(trader, action, outcome.poolId(action));
    }

    private static Component haggle(
            ServerPlayer player,
            AbstractVillager trader,
            SocialAction action,
            SocialOutcome outcome,
            ScoresConfig.Deal deal) {
        return effect(player, trader, action, outcome.id(), deal.effect(outcome));
    }

    /** A wandering trader who catches the lie also refuses the player. */
    private static Component deceiveTrader(
            ServerPlayer player,
            AbstractVillager trader,
            SocialOutcome outcome,
            ScoresConfig.DeceiveSettings settings) {
        if (trader instanceof WanderingTrader && !outcome.succeeded()) {
            SocialMemory.get(player.server)
                    .refuse(
                            SocialAction.DECEIVE,
                            player.getUUID(),
                            trader.getUUID(),
                            Socials.now(player.server),
                            settings.wanderingTrader().refuseTicks());
        }
        return haggle(player, trader, SocialAction.DECEIVE, outcome, settings.deal());
    }

    private static Component effect(
            ServerPlayer player, AbstractVillager trader, SocialAction action, String result, SocialEffect effect) {
        SocialEffects.apply(player, trader, action, effect);
        return flavor(trader, action, result);
    }

    private static Component takeItem(
            ServerPlayer player, AbstractVillager trader, SocialAction action, SocialEffect effect) {
        ItemStack item = SocialEffects.takeTradeItem(player, trader);
        SocialEffects.apply(player, trader, action, effect);
        return flavor(trader, action, "success", item.getHoverName());
    }

    private static Component withPiglin(
            ServerPlayer player,
            Piglin piglin,
            SocialAction action,
            SocialOutcome outcome,
            ScoresConfig.SocialSettings settings) {
        return switch (action) {
            case DECEIVE ->
                deceivePiglin(player, piglin, outcome, settings.deceive().piglin());
            case INTIMIDATE -> intimidatePiglin(player, piglin, outcome, settings.intimidate());
            case PERSUADE, PICKPOCKET, PLEAD, LIE, INTIMIDATE_MOB, CALM ->
                throw new IllegalStateException(action.id() + " does not work on piglins");
        };
    }

    private static Component withMob(
            ServerPlayer player,
            Mob mob,
            SocialAction action,
            SocialOutcome outcome,
            ScoresConfig.SocialSettings settings) {
        return switch (action) {
            case INTIMIDATE_MOB -> scare(player, (PathfinderMob) mob, outcome, settings.intimidateMob());
            case CALM -> calm(player, mob, outcome);
            case PERSUADE, DECEIVE, PLEAD, LIE, INTIMIDATE, PICKPOCKET ->
                throw new IllegalStateException(action.id() + " does not work on mobs");
        };
    }

    /** A made check sends the mob running, a failed one changes nothing, and a natural 1 sets it on the player. */
    private static Component scare(
            ServerPlayer player,
            PathfinderMob mob,
            SocialOutcome outcome,
            ScoresConfig.IntimidateMobSettings settings) {
        if (outcome.succeeded()) {
            SocialMobs.frighten(player, mob, settings);
            return flavor(mob, SocialAction.INTIMIDATE_MOB, SocialOutcome.SUCCESS.id());
        }
        if (outcome == SocialOutcome.CRITICAL_FAILURE) {
            SocialMobs.rush(player, mob, settings);
        }
        return flavor(mob, SocialAction.INTIMIDATE_MOB, outcome.id());
    }

    private static Component calm(ServerPlayer player, Mob mob, SocialOutcome outcome) {
        if (!outcome.succeeded()) {
            return flavor(mob, SocialAction.CALM, SocialOutcome.FAILURE.id());
        }
        SocialMobs.calm(player, mob);
        return flavor(mob, SocialAction.CALM, SocialOutcome.SUCCESS.id());
    }

    private static Component deceivePiglin(
            ServerPlayer player, Piglin piglin, SocialOutcome outcome, ScoresConfig.PiglinDeceit settings) {
        if (!outcome.succeeded()) {
            SocialEffects.angerPiglins(player, piglin, false, settings.angerTicks());
            return flavor(piglin, SocialAction.DECEIVE, "piglin.failure");
        }
        boolean critical = outcome == SocialOutcome.CRITICAL_SUCCESS;
        SocialMemory.get(player.server)
                .disguise(
                        player.getUUID(),
                        Socials.now(player.server),
                        critical ? settings.criticalDisguiseTicks() : settings.disguiseTicks());
        return flavor(piglin, SocialAction.DECEIVE, critical ? "piglin.critical_success" : "piglin.success");
    }

    private static Component intimidatePiglin(
            ServerPlayer player, Piglin piglin, SocialOutcome outcome, ScoresConfig.IntimidateSettings settings) {
        if (!outcome.succeeded()) {
            SocialEffects.angerPiglins(player, piglin, true, settings.piglinAngerTicks());
            return flavor(piglin, SocialAction.INTIMIDATE, "piglin.failure");
        }
        List<Component> items = SocialEffects.barter(player, piglin).stream()
                .map(ItemStack::getHoverName)
                .toList();
        return flavor(
                piglin,
                SocialAction.INTIMIDATE,
                "piglin.success",
                ComponentUtils.formatList(items, FallbackText.of("checks.list.comma")));
    }

    /** A line from the outcome's pool; each line may use the target's name ({@code %1$s}) and what changed hands. */
    private static Component flavor(LivingEntity target, SocialAction action, String result, Object... items) {
        Object[] args = new Object[items.length + 1];
        args[0] = Names.of(target);
        System.arraycopy(items, 0, args, 1, items.length);
        return FallbackText.oneOf(
                SocialText.flavorPool(action, SocialTarget.of(target), result), target.getRandom(), args);
    }

    /** The roll above the hotbar, with the flavor line below it. */
    static void announce(
            ServerPlayer player, ResourceLocation skill, SaveResult result, SocialOutcome outcome, Component flavor) {
        RollMessages.showResult(
                player,
                FallbackText.of(
                        "checks.social.roll",
                        SheetText.skillName(skill),
                        result.dc(),
                        RollText.d20(result.roll()),
                        SheetText.signed(result.saveBonus()),
                        result.saveTotal(),
                        FallbackText.of("checks.social.outcome." + outcome.id())),
                Optional.of(flavor));
    }
}
