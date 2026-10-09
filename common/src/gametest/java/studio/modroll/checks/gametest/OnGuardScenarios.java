package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withSocial;
import static studio.modroll.checks.gametest.SocialScenarios.IN_FRONT;
import static studio.modroll.checks.gametest.SocialScenarios.SECOND_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.TARGET_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.act;
import static studio.modroll.checks.gametest.SocialScenarios.golem;
import static studio.modroll.checks.gametest.SocialScenarios.settings;
import static studio.modroll.checks.gametest.SocialScenarios.survivor;
import static studio.modroll.checks.gametest.SocialScenarios.villager;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.social.SocialAction;
import studio.modroll.checks.social.SocialEffect;
import studio.modroll.checks.social.SocialMenu;
import studio.modroll.checks.social.Socials;
import studio.modroll.critfall.api.dice.RollMode;

/**
 * GameTest bodies for villagers on guard during a golem alarm, and wary after it. In the {@code wide}
 * structure, each in a batch of its own: an alarm and its guarded area reach far past the structure. A
 * caught pickpocket on the villager at the target spot raises the alarm through the golem next to it; the
 * neighbour a step away is the one the player then talks to. Players are new survival players with every
 * score 10 against unprofiled villagers, and Plead and Lie keep their default DC of 15.
 */
public final class OnGuardScenarios {

    private static final int NAT_ONE = 1;
    private static final int FAIL = 2;
    private static final int GOOD = 18;
    private static final int NAT_TWENTY = 20;
    private static final BlockPos NEIGHBOUR_SPOT = new BlockPos(3, 2, 5);
    private static final BlockPos FAR_GOLEM = new BlockPos(24, 2, 3);
    private static final int SHORT_ALARM_TICKS = 20;
    private static final int BEFORE_THE_FIRST_ALARM_ENDS = 10;
    private static final int AFTER_THE_FIRST_ALARM_WOULD_END = 25;
    private static final int BRIEF_ALARM_TICKS = 5;
    private static final int AFTER_THE_BRIEF_ALARM = 10;
    private static final int SHORT_REFUSAL_TICKS = 40;
    /** Past half the short refusal, not past all of it. */
    private static final int PAST_HALF_THE_REFUSAL = 30;

    private static final double WITNESS_RADIUS = 6;
    private static final ScoresConfig.SocialSettings DEFAULTS = ScoresConfig.DEFAULTS.social();

    private OnGuardScenarios() {}

    /**
     * During the alarm the neighbour offers Plead and Lie instead of Persuade and Deceive, and greys out
     * Pickpocket and Intimidate, which do nothing when tried. Each plea can be made once: a failed one
     * leaves the alarm on, and trying again does nothing.
     */
    public static void onGuardVillagersOfferPleaAndLieOnce(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "guard-menu", IN_FRONT);
        Villager victim = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager neighbour = villager(helper, VillagerProfession.FARMER, NEIGHBOUR_SPOT);
        IronGolem golem = golem(helper, SECOND_SPOT, false);
        try {
            guarded(new Guard(), () -> {
                act(player, victim, SocialAction.PICKPOCKET, FAIL);
                expect(helper, golem.getTarget() == player, "the golem must turn on the thief");
                expect(
                        helper,
                        actions(player, neighbour)
                                .equals(List.of(
                                        SocialAction.PLEAD,
                                        SocialAction.LIE,
                                        SocialAction.INTIMIDATE,
                                        SocialAction.PICKPOCKET)),
                        "an on-guard villager must offer Plead and Lie for Persuade and Deceive, was "
                                + actions(player, neighbour));
                expectAvailability(
                        helper, player, neighbour, SocialAction.PICKPOCKET, SocialMenu.Availability.ON_GUARD);
                expectAvailability(
                        helper, player, neighbour, SocialAction.INTIMIDATE, SocialMenu.Availability.ON_GUARD);
                act(player, neighbour, SocialAction.PICKPOCKET, FAIL);
                act(player, neighbour, SocialAction.INTIMIDATE, FAIL);
                expect(helper, !Socials.refuses(player, neighbour), "nothing greyed out may be tried");

                act(player, neighbour, SocialAction.PLEAD, FAIL);
                expect(helper, golem.getTarget() == player, "a failed plea must leave the alarm on");
                expectAvailability(helper, player, neighbour, SocialAction.PLEAD, SocialMenu.Availability.TRIED);
                expectAvailability(helper, player, neighbour, SocialAction.LIE, SocialMenu.Availability.AVAILABLE);
                act(player, neighbour, SocialAction.LIE, FAIL);
                expectAvailability(helper, player, neighbour, SocialAction.LIE, SocialMenu.Availability.TRIED);
                act(player, neighbour, SocialAction.PLEAD, NAT_TWENTY);
                expect(helper, golem.getTarget() == player, "a plea already made may not be made again");
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A successful plea ends the alarm: the golems it called stand down, the villagers running from the
     * player calm down, and the neighbour talks normally again. The refusals stay.
     */
    public static void aSuccessfulPleaEndsTheAlarm(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "guard-plead", IN_FRONT);
        Villager victim = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager neighbour = villager(helper, VillagerProfession.FARMER, NEIGHBOUR_SPOT);
        IronGolem near = golem(helper, SECOND_SPOT, false);
        IronGolem far = golem(helper, FAR_GOLEM, false);
        Guard guard = new Guard();
        guard.witnesses = true;
        try {
            guarded(guard, () -> {
                act(player, victim, SocialAction.PICKPOCKET, FAIL);
                expect(helper, far.getTarget() == player, "the far golem must join the chase");
                expect(helper, runningFrom(neighbour, player), "the neighbour must run from the thief");
                act(player, neighbour, SocialAction.PLEAD, GOOD);
                expect(helper, near.getTarget() == null && far.getTarget() == null, "the golems must stand down");
                expect(
                        helper,
                        near.getPersistentAngerTarget() == null && far.getPersistentAngerTarget() == null,
                        "the golems must stop being angry at the player");
                expect(
                        helper,
                        !runningFrom(neighbour, player) && !runningFrom(victim, player),
                        "nobody may keep running from the player");
                expect(
                        helper,
                        actions(player, neighbour).contains(SocialAction.PERSUADE)
                                && option(player, neighbour, SocialAction.PICKPOCKET)
                                        .map(option -> option.availability() != SocialMenu.Availability.ON_GUARD)
                                        .orElse(false),
                        "after the alarm the neighbour must talk normally, offered " + actions(player, neighbour));
                expect(
                        helper,
                        Socials.refuses(player, victim) && Socials.refuses(player, neighbour),
                        "the refusals must stay");
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A natural 20 plea also halves the time left on the thief's refusals. */
    public static void aNaturalTwentyPleaHalvesTheRefusals(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "guard-twenty", IN_FRONT);
        Villager victim = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager neighbour = villager(helper, VillagerProfession.FARMER, NEIGHBOUR_SPOT);
        golem(helper, SECOND_SPOT, false);
        Guard guard = new Guard();
        guard.pickpocketRefuseTicks = SHORT_REFUSAL_TICKS;
        guarded(guard, () -> {
            act(player, victim, SocialAction.PICKPOCKET, FAIL);
            act(player, neighbour, SocialAction.PLEAD, NAT_TWENTY);
        });
        helper.runAfterDelay(PAST_HALF_THE_REFUSAL, () -> {
            try {
                guarded(
                        guard,
                        () -> expect(
                                helper, !Socials.refuses(player, victim), "the refusal must end at half its time"));
            } finally {
                logout(helper, player);
            }
            helper.succeed();
        });
    }

    /** A natural 1 lie starts the alarm over at full length and calls the golems nearby again. */
    public static void aNaturalOneLieRestartsTheAlarm(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "guard-one", IN_FRONT);
        Villager victim = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager neighbour = villager(helper, VillagerProfession.FARMER, NEIGHBOUR_SPOT);
        IronGolem golem = golem(helper, SECOND_SPOT, false);
        Guard guard = new Guard();
        guard.alarmTicks = SHORT_ALARM_TICKS;
        guarded(guard, () -> act(player, victim, SocialAction.PICKPOCKET, FAIL));
        golem.stopBeingAngry();
        helper.runAfterDelay(
                BEFORE_THE_FIRST_ALARM_ENDS,
                () -> guarded(guard, () -> {
                    act(player, neighbour, SocialAction.LIE, NAT_ONE);
                    expect(helper, golem.getTarget() == player, "the lie must call the golem nearby again");
                }));
        helper.runAfterDelay(AFTER_THE_FIRST_ALARM_WOULD_END, () -> {
            try {
                guarded(
                        guard,
                        () -> expect(
                                helper,
                                actions(player, neighbour).contains(SocialAction.PLEAD),
                                "the restarted alarm must outlast the first one"));
            } finally {
                logout(helper, player);
            }
            helper.succeed();
        });
    }

    /**
     * After the alarm, Pickpocket and Intimidate roll with disadvantage against a villager that thinks badly
     * of the thief: a d20 of 18 then 2 keeps the 2. Persuade does not, nor does another player.
     */
    public static void waryVillagersSeeTheThiefComing(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "guard-wary", IN_FRONT);
        ServerPlayer stranger = survivor(helper, "guard-stranger", IN_FRONT);
        Villager victim = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager neighbour = villager(helper, VillagerProfession.FARMER, NEIGHBOUR_SPOT);
        golem(helper, SECOND_SPOT, false);
        Guard guard = new Guard();
        guard.alarmTicks = BRIEF_ALARM_TICKS;
        guarded(guard, () -> act(player, victim, SocialAction.PICKPOCKET, FAIL));
        helper.runAfterDelay(AFTER_THE_BRIEF_ALARM, () -> {
            try {
                guarded(guard, () -> {
                    expectMode(helper, player, victim, SocialAction.PICKPOCKET, RollMode.DISADVANTAGE);
                    expectMode(helper, player, neighbour, SocialAction.INTIMIDATE, RollMode.DISADVANTAGE);
                    expectMode(helper, player, neighbour, SocialAction.PERSUADE, RollMode.NORMAL);
                    expectMode(helper, stranger, neighbour, SocialAction.PICKPOCKET, RollMode.NORMAL);
                    act(player, neighbour, SocialAction.PICKPOCKET, GOOD, FAIL);
                    expect(helper, player.getInventory().isEmpty(), "the wary villager must catch the thief");
                    expect(helper, Socials.refuses(player, neighbour), "a caught thief is refused");
                });
            } finally {
                logout(helper, player);
                logout(helper, stranger);
            }
            helper.succeed();
        });
    }

    /** With on-guard off the menu stays as it was during the alarm; with wariness off nothing rolls worse. */
    public static void onGuardAndWarinessSwitchOff(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "guard-off", IN_FRONT);
        Villager victim = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        Villager neighbour = villager(helper, VillagerProfession.FARMER, NEIGHBOUR_SPOT);
        IronGolem golem = golem(helper, SECOND_SPOT, false);
        Guard guard = new Guard();
        guard.onGuard = false;
        guard.wary = false;
        try {
            guarded(guard, () -> {
                act(player, victim, SocialAction.PICKPOCKET, FAIL);
                expect(helper, golem.getTarget() == player, "the alarm must still be raised");
                expect(
                        helper,
                        actions(player, neighbour)
                                .equals(List.of(
                                        SocialAction.PERSUADE,
                                        SocialAction.DECEIVE,
                                        SocialAction.INTIMIDATE,
                                        SocialAction.PICKPOCKET)),
                        "with on-guard off the menu must stay as it was, was " + actions(player, neighbour));
                expectAvailability(
                        helper, player, neighbour, SocialAction.PICKPOCKET, SocialMenu.Availability.AVAILABLE);
                expectMode(helper, player, victim, SocialAction.PICKPOCKET, RollMode.NORMAL);
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** What the tests change from the shared test settings, which keep the alarm, witnesses and wariness off. */
    private static final class Guard {
        private int alarmTicks = DEFAULTS.golemAlarm().durationTicks();
        private boolean onGuard = true;
        private boolean wary = true;
        private boolean witnesses = false;
        private int pickpocketRefuseTicks = DEFAULTS.pickpocket().failure().refuseTicks();
    }

    private static void guarded(Guard guard, Runnable body) {
        ScoresConfig.SocialSettings base = settings(action -> true, false, 0);
        SocialEffect failure = base.pickpocket().failure();
        ScoresConfig.SocialSettings guarded = new ScoresConfig.SocialSettings(
                true,
                base.nearbyRadius(),
                guard.witnesses ? WITNESS_RADIUS : base.gossipRadius(),
                base.persuade(),
                base.deceive(),
                base.intimidate(),
                new ScoresConfig.PickpocketSettings(
                        true,
                        base.pickpocket().cooldownTicks(),
                        base.pickpocket().behindDegrees(),
                        new SocialEffect(
                                failure.pricePercent(),
                                failure.priceTicks(),
                                failure.gossip(),
                                guard.pickpocketRefuseTicks,
                                failure.angersGolems())),
                base.passivePrices(),
                base.reactions(),
                new ScoresConfig.WitnessSettings(
                        guard.witnesses,
                        DEFAULTS.witnesses().refuseFraction(),
                        DEFAULTS.witnesses().fleeDistance()),
                new ScoresConfig.GolemAlarmSettings(true, DEFAULTS.golemAlarm().radius(), guard.alarmTicks),
                new ScoresConfig.OnGuardSettings(
                        guard.onGuard,
                        DEFAULTS.onGuard().plead(),
                        DEFAULTS.onGuard().lie()),
                new ScoresConfig.WarySettings(guard.wary, DEFAULTS.wary().maxReputation()),
                base.feel(),
                base.intimidateMob(),
                base.calm(),
                base.insight(),
                base.performance());
        withSocial(
                guarded,
                () -> unprofiled(() -> {
                    body.run();
                    return null;
                }));
    }

    private static List<SocialAction> actions(ServerPlayer player, LivingEntity target) {
        return Socials.menu(player, target.getId()).orElseThrow().options().stream()
                .map(SocialMenu.Option::action)
                .toList();
    }

    private static Optional<SocialMenu.Option> option(ServerPlayer player, LivingEntity target, SocialAction action) {
        return Socials.menu(player, target.getId()).orElseThrow().options().stream()
                .filter(option -> option.action() == action)
                .findFirst();
    }

    private static void expectAvailability(
            GameTestHelper helper,
            ServerPlayer player,
            LivingEntity target,
            SocialAction action,
            SocialMenu.Availability expected) {
        Optional<SocialMenu.Availability> actual =
                option(player, target, action).map(SocialMenu.Option::availability);
        expect(helper, actual.equals(Optional.of(expected)), action.id() + " must be " + expected + ", was " + actual);
    }

    private static void expectMode(
            GameTestHelper helper, ServerPlayer player, LivingEntity target, SocialAction action, RollMode expected) {
        Optional<RollMode> actual = option(player, target, action).map(SocialMenu.Option::mode);
        expect(
                helper,
                actual.equals(Optional.of(expected)),
                action.id() + " must roll " + expected + ", was " + actual);
    }

    private static boolean runningFrom(Villager villager, ServerPlayer player) {
        return villager.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY).orElse(null) == player;
    }
}
