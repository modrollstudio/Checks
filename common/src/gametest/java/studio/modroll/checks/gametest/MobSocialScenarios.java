package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withSocial;
import static studio.modroll.checks.gametest.SocialScenarios.IN_FRONT;
import static studio.modroll.checks.gametest.SocialScenarios.TARGET_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.act;
import static studio.modroll.checks.gametest.SocialScenarios.calm;
import static studio.modroll.checks.gametest.SocialScenarios.expectAvailability;
import static studio.modroll.checks.gametest.SocialScenarios.replacing;
import static studio.modroll.checks.gametest.SocialScenarios.rolls;
import static studio.modroll.checks.gametest.SocialScenarios.settings;
import static studio.modroll.checks.gametest.SocialScenarios.survivor;

import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.social.SocialAction;
import studio.modroll.checks.social.SocialMenu;
import studio.modroll.checks.social.SocialMobs;
import studio.modroll.checks.social.Socials;

/**
 * GameTest bodies for Intimidate and Calm on mobs. Players are new survival players with every score 10, so
 * a d20 of 18 makes Intimidate's DC 12 and Calm's DC 13, and a 5 makes neither.
 */
public final class MobSocialScenarios {

    private static final int NAT_ONE = 1;
    private static final int FAIL = 5;
    private static final int GOOD = 18;
    private static final int SHORT_FLEE_TICKS = 5;
    private static final int PAST_THE_FLIGHT = 10;
    private static final int LONG_COOLDOWN = 1200;
    private static final double TOUGH_ENOUGH = 1000;
    /** A vanilla tag holding the skeletons, standing in for a pack's own. */
    private static final ResourceLocation SKELETONS = ResourceLocation.withDefaultNamespace("skeletons");

    private static final ScoresConfig.SocialSettings DEFAULTS = ScoresConfig.DEFAULTS.social();

    private MobSocialScenarios() {}

    /** A made check sends a zombie running for the flee time, dropping its target; then it stops. */
    public static void intimidateSendsAHostileMobRunning(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "mob-scare", IN_FRONT);
        Mob zombie = calm(helper, EntityType.ZOMBIE, TARGET_SPOT);
        zombie.setTarget(player);
        mobs(intimidate(true, DEFAULTS.intimidateMob().maxHealth(), SHORT_FLEE_TICKS), calming(true, 0), () -> {
            expectAvailability(helper, player, zombie, SocialAction.INTIMIDATE_MOB, SocialMenu.Availability.AVAILABLE);
            act(player, zombie, SocialAction.INTIMIDATE_MOB, GOOD);
            expect(helper, SocialMobs.fleeing(zombie), "a scared zombie must run");
            expect(helper, zombie.getTarget() == null, "a scared zombie must drop its target");
        });
        helper.runAfterDelay(PAST_THE_FLIGHT, () -> {
            try {
                expect(helper, !SocialMobs.fleeing(zombie), "the zombie must stop running after the flee time");
            } finally {
                logout(helper, player);
            }
            helper.succeed();
        });
    }

    /** A failed check changes nothing; a natural 1 sets the mob on the player with a burst of speed. */
    public static void aFailedThreatDoesNothingAndANaturalOneEnrages(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "mob-rush", IN_FRONT);
        Mob zombie = calm(helper, EntityType.ZOMBIE, TARGET_SPOT);
        try {
            mobs(intimidate(true, DEFAULTS.intimidateMob().maxHealth(), SHORT_FLEE_TICKS), calming(true, 0), () -> {
                act(player, zombie, SocialAction.INTIMIDATE_MOB, FAIL);
                expect(helper, !SocialMobs.fleeing(zombie), "a failed threat must not scare the zombie");
                expect(helper, zombie.getTarget() == null, "a failed threat must not set the zombie on the player");
                act(player, zombie, SocialAction.INTIMIDATE_MOB, NAT_ONE);
                expect(helper, zombie.getTarget() == player, "a natural 1 must set the zombie on the player");
                expect(helper, zombie.hasEffect(MobEffects.MOVEMENT_SPEED), "a natural 1 must speed the zombie up");
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A witch has more than 20 max health and won't be scared, until the limit is raised; a wither never is,
     * however high it goes; nor is a mob in the exclude tag.
     */
    public static void strongBossAndExcludedMobsAreFearless(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "mob-fearless", IN_FRONT);
        Mob witch = calm(helper, EntityType.WITCH, TARGET_SPOT);
        Mob skeleton = calm(helper, EntityType.SKELETON, SocialScenarios.SECOND_SPOT);
        try {
            mobs(intimidate(true, DEFAULTS.intimidateMob().maxHealth(), SHORT_FLEE_TICKS), calming(true, 0), () -> {
                expectAvailability(
                        helper, player, witch, SocialAction.INTIMIDATE_MOB, SocialMenu.Availability.FEARLESS);
                expect(helper, rolls(() -> act(player, witch, SocialAction.INTIMIDATE_MOB, GOOD)) == 0, "no roll");
            });
            mobs(
                    intimidate(true, TOUGH_ENOUGH, SHORT_FLEE_TICKS),
                    calming(true, 0),
                    () -> expectAvailability(
                            helper, player, witch, SocialAction.INTIMIDATE_MOB, SocialMenu.Availability.AVAILABLE));
            ScoresConfig.IntimidateMobSettings tough = intimidate(true, TOUGH_ENOUGH, SHORT_FLEE_TICKS);
            ScoresConfig.IntimidateMobSettings excluding = new ScoresConfig.IntimidateMobSettings(
                    true,
                    tough.dc(),
                    tough.cooldownTicks(),
                    tough.maxHealth(),
                    SKELETONS,
                    tough.fleeTicks(),
                    tough.fleeDistance(),
                    tough.fleeSpeed(),
                    tough.speedBoostTicks(),
                    tough.speedBoostAmplifier());
            mobs(
                    excluding,
                    calming(true, 0),
                    () -> expectAvailability(
                            helper, player, skeleton, SocialAction.INTIMIDATE_MOB, SocialMenu.Availability.FEARLESS));
            witch.discard();
            Mob wither = calm(helper, EntityType.WITHER, TARGET_SPOT);
            mobs(
                    tough,
                    calming(true, 0),
                    () -> expectAvailability(
                            helper, player, wither, SocialAction.INTIMIDATE_MOB, SocialMenu.Availability.FEARLESS));
            wither.discard();
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A made Calm check clears the anger of every listed mob, and of a mob in the configured tag. */
    public static void calmClearsAngerForEachListedMob(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "mob-calm", IN_FRONT);
        try {
            for (EntityType<? extends Mob> type : List.of(
                    EntityType.WOLF,
                    EntityType.BEE,
                    EntityType.ENDERMAN,
                    EntityType.ZOMBIFIED_PIGLIN,
                    EntityType.IRON_GOLEM,
                    EntityType.SKELETON)) {
                Mob mob = calm(helper, type, TARGET_SPOT);
                angerAt(mob, player);
                mobs(intimidate(true, DEFAULTS.intimidateMob().maxHealth(), SHORT_FLEE_TICKS), calmTagged(), () -> {
                    expectAvailability(helper, player, mob, SocialAction.CALM, SocialMenu.Availability.AVAILABLE);
                    act(player, mob, SocialAction.CALM, GOOD);
                    expect(helper, !angry(mob, player), type.toShortString() + " must calm down");
                });
                mob.discard();
            }
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A failed Calm check leaves the wolf angry. A calm wolf greys Calm out; after a calming the wolf cools
     * down per target, and trying again rolls nothing.
     */
    public static void calmFailsLeavesAngerAndCoolsDown(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "mob-calm-cd", IN_FRONT);
        Mob wolf = calm(helper, EntityType.WOLF, TARGET_SPOT);
        try {
            mobs(
                    intimidate(true, DEFAULTS.intimidateMob().maxHealth(), SHORT_FLEE_TICKS),
                    calming(true, LONG_COOLDOWN),
                    () -> {
                        expectAvailability(helper, player, wolf, SocialAction.CALM, SocialMenu.Availability.NOT_ANGRY);
                        angerAt(wolf, player);
                        act(player, wolf, SocialAction.CALM, FAIL);
                        expect(helper, angry(wolf, player), "a failed Calm check must leave the wolf angry");
                        expectAvailability(helper, player, wolf, SocialAction.CALM, SocialMenu.Availability.COOLDOWN);
                        expect(
                                helper,
                                rolls(() -> act(player, wolf, SocialAction.CALM, GOOD)) == 0,
                                "no roll on cooldown");
                        expect(helper, angry(wolf, player), "trying again on cooldown must change nothing");
                    });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * With Intimidate on mobs off, a zombie has nothing to offer and a running one stops; with Calm off, an
     * angry wolf has nothing either. Villagers never offer the mob actions.
     */
    public static void mobActionsSwitchOff(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "mob-off", IN_FRONT);
        Mob zombie = calm(helper, EntityType.ZOMBIE, TARGET_SPOT);
        Mob wolf = calm(helper, EntityType.WOLF, SocialScenarios.SECOND_SPOT);
        angerAt(wolf, player);
        try {
            mobs(intimidate(true, DEFAULTS.intimidateMob().maxHealth(), LONG_COOLDOWN), calming(true, 0), () -> {
                act(player, zombie, SocialAction.INTIMIDATE_MOB, GOOD);
                expect(helper, SocialMobs.fleeing(zombie), "the zombie must run");
            });
            mobs(intimidate(false, DEFAULTS.intimidateMob().maxHealth(), LONG_COOLDOWN), calming(false, 0), () -> {
                expect(helper, Socials.menu(player, zombie.getId()).isEmpty(), "no menu for a zombie with both off");
                expect(helper, Socials.menu(player, wolf.getId()).isEmpty(), "no menu for a wolf with both off");
                expect(helper, rolls(() -> act(player, wolf, SocialAction.CALM, GOOD)) == 0, "Calm off rolls nothing");
                SocialMobs.tick(helper.getLevel().getServer());
                expect(helper, !SocialMobs.fleeing(zombie), "switching Intimidate on mobs off must stop the run");
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    static void angerAt(Mob mob, ServerPlayer player) {
        if (mob instanceof NeutralMob neutral) {
            neutral.setPersistentAngerTarget(player.getUUID());
            neutral.startPersistentAngerTimer();
        }
        mob.setTarget(player);
    }

    static boolean angry(Mob mob, ServerPlayer player) {
        return mob.getTarget() == player
                || (mob instanceof NeutralMob neutral && player.getUUID().equals(neutral.getPersistentAngerTarget()));
    }

    static ScoresConfig.IntimidateMobSettings intimidate(boolean enabled, double maxHealth, int fleeTicks) {
        ScoresConfig.IntimidateMobSettings defaults = DEFAULTS.intimidateMob();
        return new ScoresConfig.IntimidateMobSettings(
                enabled,
                defaults.dc(),
                0,
                maxHealth,
                defaults.excludeTag(),
                fleeTicks,
                defaults.fleeDistance(),
                defaults.fleeSpeed(),
                defaults.speedBoostTicks(),
                defaults.speedBoostAmplifier());
    }

    static ScoresConfig.CalmSettings calming(boolean enabled, int cooldown) {
        ScoresConfig.CalmSettings defaults = DEFAULTS.calm();
        return new ScoresConfig.CalmSettings(enabled, defaults.dc(), cooldown, defaults.tag());
    }

    /** Calm with no cooldown that also works on skeletons, through the tag. */
    private static ScoresConfig.CalmSettings calmTagged() {
        return new ScoresConfig.CalmSettings(true, DEFAULTS.calm().dc(), 0, SKELETONS);
    }

    /** Test settings with these mob actions, unprofiled. */
    static void mobs(ScoresConfig.IntimidateMobSettings intimidate, ScoresConfig.CalmSettings calm, Runnable body) {
        ScoresConfig.SocialSettings base = settings(action -> true, false, 0);
        withSocial(
                replacing(base, intimidate, calm, base.insight(), base.performance()),
                () -> unprofiled(() -> {
                    body.run();
                    return null;
                }));
    }
}
