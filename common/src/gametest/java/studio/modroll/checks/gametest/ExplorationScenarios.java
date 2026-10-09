package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.collecting;
import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectClose;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.profiles;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.waitOutSpawnProtection;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withExploration;
import static studio.modroll.checks.gametest.ScenarioSupport.withProfiles;
import static studio.modroll.checks.gametest.ScenarioSupport.withSender;
import static studio.modroll.checks.gametest.SocialScenarios.TARGET_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.calm;
import static studio.modroll.checks.gametest.SocialScenarios.survivor;
import static studio.modroll.checks.gametest.SocialScenarios.watchedSurvivor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.check.RollLinePayload;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.exploration.Climbing;
import studio.modroll.checks.exploration.ClimbingPayload;
import studio.modroll.checks.exploration.Cobwebs;
import studio.modroll.checks.exploration.Landings;
import studio.modroll.checks.exploration.Tripwires;

/**
 * GameTest bodies for exploration skill uses. Players are new survival players with every score 10 and no
 * skill proficiency, so every check adds +0 and every passive score is 10; a d20 of 15 makes a DC 12 check
 * and a d20 of 2 fails it. A test that forces no d20 fails if anything rolls. Jumps, falls and looks go
 * through each loader's own hooks.
 */
public final class ExplorationScenarios {

    private static final int MADE = 15;
    private static final int FAILED = 2;
    private static final int DC = 12;
    private static final int PASSIVE = 10;
    private static final double BOOST = 0.15;
    private static final double SPRINT_JUMP_IMPULSE = 0.2;

    private static final ScoresConfig.ExplorationSettings DEFAULTS = ScoresConfig.DEFAULTS.exploration();

    private static final Vec3 STAND = new Vec3(1.5, 4, 1.5);
    private static final BlockPos UNDERFOOT = new BlockPos(1, 3, 1);
    /** South of the player, the way they face: the floor a plain jump would land on. */
    private static final BlockPos FLOOR_AHEAD = new BlockPos(1, 3, 2);

    private static final int MIN_DROP = 3;
    private static final int FLAT = 0;
    private static final int SHALLOW = 2;

    private static final float FACING_SOUTH = 0;
    private static final Vec3 GROUND_SPEED = new Vec3(0, 0, 0.28);

    private static final float FALL = 6;
    private static final int BASE_DC = 10;
    /** 10 + half of 6. */
    private static final int FALL_DC = 13;

    private static final float FULL_HEALTH = 20;
    private static final double HALF = 0.5;

    /** Inside the player's body, standing at STAND. */
    private static final BlockPos WEB = new BlockPos(1, 4, 1);

    private static final int NO_RETRY = 0;

    private static final double CLIMB_PER_POINT = 0.1;
    private static final double CLIMB_MAX_BONUS = 0.5;
    private static final float EPSILON_SPEED = 1e-6f;

    /** Passive Perception 7: WIS 4. */
    private static final String DULL_ZOMBIE = "{\"matches\": [\"minecraft:zombie\"], \"abilities\": {\"wis\": 4}}";

    private static final double VANILLA_SNEAK = 0.8;
    private static final double PER_POINT = 0.1;
    private static final double BEATEN_BY_THREE = 1 - 3 * PER_POINT;
    private static final double MIN_VISIBILITY = 0.25;

    private static final Vec3 NEAR_WIRES = new Vec3(3.5, 2, 5.5);
    private static final BlockPos ARMED_WIRE = new BlockPos(2, 2, 3);
    private static final BlockPos LOOSE_WIRE = new BlockPos(4, 2, 3);
    private static final int SPOT_RANGE = 3;
    private static final int EVERY_TICK = 1;

    private static final BlockPos WEST_SUPPORT = new BlockPos(0, 2, 3);
    private static final BlockPos WEST_HOOK = new BlockPos(1, 2, 3);
    private static final BlockPos EAST_WIRE = new BlockPos(3, 2, 3);
    private static final BlockPos EAST_HOOK = new BlockPos(4, 2, 3);
    private static final BlockPos EAST_SUPPORT = new BlockPos(5, 2, 3);
    private static final BlockPos SPARE_STRING = new BlockPos(2, 2, 6);
    private static final double DROP_RADIUS = 1.5;

    private ExplorationScenarios() {}

    /**
     * A sprinting jump toward a drop of three blocks rolls Athletics: a made check speeds the leap along the
     * facing, a failed one leaves it.
     */
    public static void aMadeAthleticsCheckCarriesALeapFurther(GameTestHelper helper) {
        ServerPlayer player = sprinter(helper, "leap-made", MIN_DROP);
        List<CustomPacketPayload> lines = new ArrayList<>();
        try {
            exploring(leap(true, 0), lines, () -> withD20s(() -> jump(player), MADE));
            expect(helper, player.hurtMarked, "a made leap must send the player their new speed");
            expectClose(
                    helper, GROUND_SPEED.z + SPRINT_JUMP_IMPULSE + BOOST, player.getDeltaMovement().z, "leap speed");
            expectLine(helper, lines, "checks.exploration.leap.success.");

            lines.clear();
            exploring(leap(true, 0), lines, () -> withD20s(() -> jump(player), FAILED));
            expect(helper, !player.hurtMarked, "a failed leap must leave the jump alone");
            expectLine(helper, lines, "checks.exploration.leap.failure.");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A jump onto flat ground or toward a drop of only two blocks is a plain jump that rolls nothing; so is a
     * standing jump toward a deep drop, and any jump while leaps are off.
     */
    public static void aLeapRollsOnlyWhenItCouldMatter(GameTestHelper helper) {
        ServerPlayer player = sprinter(helper, "leap-plain", FLAT);
        try {
            expectPlainJump(helper, player, leap(true, 0), "a jump onto flat ground");
            dropAhead(helper, SHALLOW);
            expectPlainJump(helper, player, leap(true, 0), "a jump toward a two-block drop");
            dropAhead(helper, MIN_DROP);
            player.setSprinting(false);
            expectPlainJump(helper, player, leap(true, 0), "a standing jump");
            player.setSprinting(true);
            expectPlainJump(helper, player, leap(false, 0), "a jump while leaps are off");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Within the cooldown a leap reuses the last result without rolling. */
    public static void aLeapWithinTheCooldownReusesTheLastResult(GameTestHelper helper) {
        ServerPlayer player = sprinter(helper, "leap-cooldown", MIN_DROP);
        List<CustomPacketPayload> lines = new ArrayList<>();
        try {
            exploring(leap(true, Integer.MAX_VALUE), lines, () -> withD20s(() -> jump(player), MADE));
            exploring(leap(true, Integer.MAX_VALUE), lines, () -> withD20s(() -> jump(player)));
            expect(helper, player.hurtMarked, "the second leap must reuse the made check");
            expectEquals(helper, 1, lines.size(), "roll lines for two leaps in one cooldown");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * Fall damage rolls Acrobatics against 10 plus half the damage, DC 13 for 6: a made check halves it, a
     * failed one takes it all; other damage, and a fall that does no damage, roll nothing.
     */
    public static void aMadeAcrobaticsCheckSoftensAFall(GameTestHelper helper) {
        ServerPlayer player = faller(helper, "landing-made");
        List<CustomPacketPayload> lines = new ArrayList<>();
        try {
            exploring(landing(true), lines, () -> withD20s(() -> fall(player), FALL_DC));
            expectClose(helper, FULL_HEALTH - FALL * HALF, player.getHealth(), "health after a made landing");
            expectLine(helper, lines, "checks.exploration.landing.success.");

            heal(player);
            lines.clear();
            exploring(landing(true), lines, () -> withD20s(() -> fall(player), FALL_DC - 1));
            expectClose(helper, FULL_HEALTH - FALL, player.getHealth(), "health after a failed landing");
            expectLine(helper, lines, "checks.exploration.landing.failure.");

            heal(player);
            exploring(
                    landing(true),
                    lines,
                    () -> withD20s(() -> {
                        player.hurt(player.damageSources().generic(), FALL);
                        return null;
                    }));
            expectClose(helper, FULL_HEALTH - FALL, player.getHealth(), "health after damage that is no fall");
            float none = exploring(
                    landing(true),
                    lines,
                    () -> withD20s(() ->
                            Landings.fallDamage(player, player.damageSources().fall(), 0)));
            expectClose(helper, 0, none, "a fall that does no damage");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** With landings off a fall rolls nothing and hurts in full. */
    public static void landingsSwitchOff(GameTestHelper helper) {
        ServerPlayer player = faller(helper, "landing-off");
        try {
            exploring(landing(false), new ArrayList<>(), () -> withD20s(() -> fall(player)));
            expectClose(helper, FULL_HEALTH - FALL, player.getHealth(), "health after a fall while off");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A player caught in a cobweb rolls Athletics: a made check tears the web down with nothing dropped, a
     * failed one leaves them stuck and waiting out the retry time before the next roll.
     */
    public static void aMadeAthleticsCheckTearsThroughACobweb(GameTestHelper helper) {
        helper.setBlock(WEB, Blocks.COBWEB);
        ServerPlayer player = survivor(helper, "cobweb-made", STAND);
        List<CustomPacketPayload> lines = new ArrayList<>();
        try {
            exploring(cobwebs(true, NO_RETRY), lines, () -> withD20s(() -> struggle(player), MADE));
            helper.assertBlockNotPresent(Blocks.COBWEB, WEB);
            expectEquals(helper, 0, helper.getEntities(EntityType.ITEM).size(), "items dropped by a torn web");
            expectLine(helper, lines, "checks.exploration.cobweb.success.");

            helper.setBlock(WEB, Blocks.COBWEB);
            lines.clear();
            exploring(cobwebs(true, Integer.MAX_VALUE), lines, () -> withD20s(() -> struggle(player), FAILED));
            helper.assertBlockPresent(Blocks.COBWEB, WEB);
            expectLine(helper, lines, "checks.exploration.cobweb.failure.");
            exploring(cobwebs(true, Integer.MAX_VALUE), lines, () -> withD20s(() -> struggle(player)));
            expectEquals(helper, 1, lines.size(), "rolls before the retry time is up");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Out of a web, or with cobwebs off, nothing rolls and a web stays. */
    public static void aCobwebRollsOnlyWhenCaughtAndSwitchedOn(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "cobweb-none", STAND);
        try {
            exploring(cobwebs(true, NO_RETRY), new ArrayList<>(), () -> withD20s(() -> struggle(player)));
            helper.setBlock(WEB, Blocks.COBWEB);
            exploring(cobwebs(false, NO_RETRY), new ArrayList<>(), () -> withD20s(() -> struggle(player)));
            helper.assertBlockPresent(Blocks.COBWEB, WEB);
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * Climbing speeds up 10% per positive Athletics point, at most 50%, with no roll: STR 16 (+3) climbs 30%
     * faster, STR 30 (+10) 50%, STR 8 (-1) as in vanilla, and so does anyone while climbing is off. The client
     * hears of a new speed only when it changes.
     */
    public static void athleticsSpeedsUpClimbing(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "climber", STAND);
        List<Float> sent = new ArrayList<>();
        try {
            exploring(climbing(true), new ArrayList<>(), () -> {
                expectSpeed(helper, player, "str 16", 1.3f);
                expectSpeed(helper, player, "str 30", 1.5f);
                expectSpeed(helper, player, "str 8", 1f);
                run(helper, "checks set " + player.getScoreboardName() + " str 16");
                syncClimbing(helper, player, sent);
                syncClimbing(helper, player, sent);
                return null;
            });
            expect(helper, sent.equals(List.of(1.3f)), "the client must hear of 1.3 once, heard " + sent);
            exploring(climbing(false), new ArrayList<>(), () -> {
                expect(helper, Climbing.speed(player) == 1f, "climbing speed while off was " + Climbing.speed(player));
                syncClimbing(helper, player, sent);
                return null;
            });
            expect(helper, sent.equals(List.of(1.3f, 1f)), "switching off must send vanilla's speed, sent " + sent);
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A sneaking player whose passive Stealth beats a zombie's passive Perception by 3 is noticed from 30%
     * closer on top of vanilla's sneak, never closer than the minimum; standing, or with sneaking off, only
     * vanilla counts.
     */
    public static void sneakingPastADullMobGetsCloser(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "sneak", STAND);
        Mob zombie = calm(helper, EntityType.ZOMBIE, TARGET_SPOT);
        try {
            withProfiles(profiles(DULL_ZOMBIE), () -> {
                player.setShiftKeyDown(true);
                expectVisibility(
                        helper, player, zombie, sneak(true, PER_POINT), VANILLA_SNEAK * BEATEN_BY_THREE, "sneaking");
                expectVisibility(helper, player, zombie, sneak(true, 1), VANILLA_SNEAK * MIN_VISIBILITY, "the minimum");
                expectVisibility(helper, player, zombie, sneak(false, PER_POINT), VANILLA_SNEAK, "sneaking while off");
                player.setShiftKeyDown(false);
                expectVisibility(helper, player, zombie, sneak(true, PER_POINT), 1, "standing");
                return null;
            });
        } finally {
            zombie.discard();
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * Armed tripwires in range glint for a player whose passive Perception meets the DC, a loose string never
     * does, and a newly spotted wire is pointed out once; below the DC or with spotting off, nothing glints.
     */
    public static void passivePerceptionSpotsArmedTripwires(GameTestHelper helper) {
        List<Packet<?>> packets = new ArrayList<>();
        ServerPlayer player = watchedSurvivor(helper, "spot-wires", packets);
        player.moveTo(helper.absoluteVec(NEAR_WIRES));
        helper.setBlock(ARMED_WIRE, Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.ATTACHED, true));
        helper.setBlock(LOOSE_WIRE, Blocks.TRIPWIRE);
        List<CustomPacketPayload> lines = new ArrayList<>();
        try {
            exploring(spot(true, PASSIVE + 1), lines, () -> lookAround(player));
            expect(helper, glints(helper, packets, ARMED_WIRE) == 0, "below the DC nothing may glint");
            expect(helper, lines.isEmpty(), "below the DC nothing is pointed out");

            exploring(spot(true, PASSIVE), lines, () -> lookAround(player));
            expect(helper, glints(helper, packets, ARMED_WIRE) > 0, "the armed wire must glint");
            expectEquals(helper, 0, glints(helper, packets, LOOSE_WIRE), "glints over a loose string");
            expectLine(helper, lines, "checks.exploration.spot_tripwire.");

            packets.clear();
            exploring(spot(true, PASSIVE), lines, () -> lookAround(player));
            expect(helper, glints(helper, packets, ARMED_WIRE) > 0, "the wire keeps glinting");
            expectEquals(helper, 1, lines.size(), "lines for a wire already spotted");

            packets.clear();
            exploring(spot(false, PASSIVE), lines, () -> lookAround(player));
            expectEquals(helper, 0, glints(helper, packets, ARMED_WIRE), "glints while off");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A made Sleight of Hand check takes the string off quietly: the hooks never fire and one string drops. */
    public static void aMadeSleightOfHandCheckDisarmsATripwire(GameTestHelper helper) {
        stringTrap(helper);
        ServerPlayer player = disarmer(helper, "disarm-made");
        List<CustomPacketPayload> lines = new ArrayList<>();
        try {
            InteractionResult result =
                    exploring(disarm(true), lines, () -> withD20s(() -> use(helper, player, ARMED_WIRE), MADE));
            expect(helper, result.consumesAction(), "a disarm attempt must consume the click");
            helper.assertBlockNotPresent(Blocks.TRIPWIRE, ARMED_WIRE);
            helper.assertBlockProperty(WEST_HOOK, TripWireHookBlock.POWERED, false);
            expectEquals(helper, 1, strings(helper), "strings dropped");
            expectLine(helper, lines, "checks.exploration.disarm.success.");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A failed Sleight of Hand check snaps the string and springs the trap. */
    public static void aFailedSleightOfHandCheckSpringsTheTrap(GameTestHelper helper) {
        stringTrap(helper);
        ServerPlayer player = disarmer(helper, "disarm-failed");
        List<CustomPacketPayload> lines = new ArrayList<>();
        try {
            exploring(disarm(true), lines, () -> withD20s(() -> use(helper, player, ARMED_WIRE), FAILED));
            helper.assertBlockNotPresent(Blocks.TRIPWIRE, ARMED_WIRE);
            helper.assertBlockProperty(WEST_HOOK, TripWireHookBlock.POWERED, true);
            expectLine(helper, lines, "checks.exploration.disarm.failure.");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * Using an armed tripwire rolls nothing without sneaking, with something in hand or with disarming off;
     * nor does using a string that is not armed.
     */
    public static void disarmingRollsOnlyWhenItCouldMatter(GameTestHelper helper) {
        stringTrap(helper);
        helper.setBlock(SPARE_STRING, Blocks.TRIPWIRE);
        ServerPlayer player = disarmer(helper, "disarm-none");
        try {
            exploring(
                    disarm(false),
                    new ArrayList<>(),
                    () -> expectPass(helper, use(helper, player, ARMED_WIRE), "while off"));
            player.setShiftKeyDown(false);
            exploring(
                    disarm(true),
                    new ArrayList<>(),
                    () -> expectPass(helper, use(helper, player, ARMED_WIRE), "standing"));
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            exploring(
                    disarm(true),
                    new ArrayList<>(),
                    () -> expectPass(helper, use(helper, player, ARMED_WIRE), "holding a stick"));
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            exploring(
                    disarm(true),
                    new ArrayList<>(),
                    () -> expectPass(helper, use(helper, player, SPARE_STRING), "on a loose string"));
            helper.assertBlockPresent(Blocks.TRIPWIRE, ARMED_WIRE);
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    private static ServerPlayer sprinter(GameTestHelper helper, String name, int drop) {
        helper.setBlock(UNDERFOOT, Blocks.STONE);
        dropAhead(helper, drop);
        ServerPlayer player = survivor(helper, name, STAND);
        player.setYRot(FACING_SOUTH);
        player.setSprinting(true);
        return player;
    }

    /** Air for {@code drop} blocks down from where a plain jump would land, then stone. */
    private static void dropAhead(GameTestHelper helper, int drop) {
        for (int down = 0; down < MIN_DROP; down++) {
            helper.setBlock(FLOOR_AHEAD.below(down), down < drop ? Blocks.AIR : Blocks.STONE);
        }
    }

    private static void expectPlainJump(
            GameTestHelper helper, ServerPlayer player, ScoresConfig.ExplorationSettings settings, String what) {
        exploring(settings, new ArrayList<>(), () -> withD20s(() -> jump(player)));
        expect(helper, !player.hurtMarked, what + " must be a plain jump");
    }

    private static Void struggle(ServerPlayer player) {
        Cobwebs.struggle(player);
        return null;
    }

    private static void expectSpeed(GameTestHelper helper, ServerPlayer player, String score, float expected) {
        run(helper, "checks set " + player.getScoreboardName() + " " + score);
        float speed = Climbing.speed(player);
        expect(helper, Math.abs(expected - speed) < EPSILON_SPEED, "climbing speed at " + score + " was " + speed);
    }

    /** Runs the server's climbing sync, adding each speed sent to {@code player} to {@code sent}. */
    private static void syncClimbing(GameTestHelper helper, ServerPlayer player, List<Float> sent) {
        withSender(
                (to, payload) ->
                        to == player && payload instanceof ClimbingPayload climbing && sent.add(climbing.speed()),
                () -> {
                    Climbing.tick(helper.getLevel().getServer());
                    return null;
                });
    }

    /** As the server sees a jump: from the speed the client last reported, with nothing marked to send yet. */
    private static Void jump(ServerPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.setKnownMovement(GROUND_SPEED);
        player.hurtMarked = false;
        player.jumpFromGround();
        return null;
    }

    private static ServerPlayer faller(GameTestHelper helper, String name) {
        ServerPlayer player = survivor(helper, name, STAND);
        waitOutSpawnProtection(player);
        heal(player);
        return player;
    }

    private static Void fall(ServerPlayer player) {
        player.hurt(player.damageSources().fall(), FALL);
        return null;
    }

    private static void heal(ServerPlayer player) {
        player.setHealth(FULL_HEALTH);
        player.invulnerableTime = 0;
    }

    private static Void lookAround(ServerPlayer player) {
        Tripwires.lookAround(player);
        return null;
    }

    /** Hooks facing each other on stone, two strings between them, armed by the last string placed. */
    private static void stringTrap(GameTestHelper helper) {
        helper.setBlock(WEST_SUPPORT, Blocks.STONE);
        helper.setBlock(EAST_SUPPORT, Blocks.STONE);
        helper.setBlock(
                WEST_HOOK, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.EAST));
        helper.setBlock(
                EAST_HOOK, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.WEST));
        helper.setBlock(ARMED_WIRE, Blocks.TRIPWIRE);
        helper.setBlock(EAST_WIRE, Blocks.TRIPWIRE);
        helper.assertBlockProperty(ARMED_WIRE, TripWireBlock.ATTACHED, true);
    }

    private static int strings(GameTestHelper helper) {
        return helper.getEntities(EntityType.ITEM, ARMED_WIRE, DROP_RADIUS).stream()
                .mapToInt(
                        item -> item.getItem().is(Items.STRING) ? item.getItem().getCount() : 0)
                .sum();
    }

    private static ServerPlayer disarmer(GameTestHelper helper, String name) {
        ServerPlayer player = survivor(helper, name, NEAR_WIRES);
        player.setShiftKeyDown(true);
        return player;
    }

    private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos wire) {
        return Tripwires.onUseBlock(player, InteractionHand.MAIN_HAND, helper.absolutePos(wire));
    }

    static <T> T exploring(
            ScoresConfig.ExplorationSettings settings, List<CustomPacketPayload> lines, Supplier<T> body) {
        return withExploration(
                settings, () -> unprofiled(() -> withSender(collecting(RollLinePayload.class, lines), body)));
    }

    private static ScoresConfig.ExplorationSettings leap(boolean enabled, int cooldownTicks) {
        return new ScoresConfig.ExplorationSettings(
                new ScoresConfig.LeapSettings(enabled, DC, cooldownTicks, BOOST, MIN_DROP),
                DEFAULTS.landing(),
                DEFAULTS.cobwebs(),
                DEFAULTS.climbing(),
                DEFAULTS.sneak(),
                DEFAULTS.spotTripwires(),
                DEFAULTS.disarmTripwires(),
                DEFAULTS.searchChests(),
                DEFAULTS.monsterLore(),
                DEFAULTS.structureLore(),
                DEFAULTS.taming(),
                DEFAULTS.hunger(),
                DEFAULTS.ailments());
    }

    private static ScoresConfig.ExplorationSettings landing(boolean enabled) {
        return new ScoresConfig.ExplorationSettings(
                DEFAULTS.leap(),
                new ScoresConfig.LandingSettings(enabled, BASE_DC, HALF, HALF),
                DEFAULTS.cobwebs(),
                DEFAULTS.climbing(),
                DEFAULTS.sneak(),
                DEFAULTS.spotTripwires(),
                DEFAULTS.disarmTripwires(),
                DEFAULTS.searchChests(),
                DEFAULTS.monsterLore(),
                DEFAULTS.structureLore(),
                DEFAULTS.taming(),
                DEFAULTS.hunger(),
                DEFAULTS.ailments());
    }

    private static ScoresConfig.ExplorationSettings cobwebs(boolean enabled, int retryTicks) {
        return new ScoresConfig.ExplorationSettings(
                DEFAULTS.leap(),
                DEFAULTS.landing(),
                new ScoresConfig.CobwebSettings(enabled, DC, retryTicks),
                DEFAULTS.climbing(),
                DEFAULTS.sneak(),
                DEFAULTS.spotTripwires(),
                DEFAULTS.disarmTripwires(),
                DEFAULTS.searchChests(),
                DEFAULTS.monsterLore(),
                DEFAULTS.structureLore(),
                DEFAULTS.taming(),
                DEFAULTS.hunger(),
                DEFAULTS.ailments());
    }

    private static ScoresConfig.ExplorationSettings climbing(boolean enabled) {
        return new ScoresConfig.ExplorationSettings(
                DEFAULTS.leap(),
                DEFAULTS.landing(),
                DEFAULTS.cobwebs(),
                new ScoresConfig.ClimbingSettings(enabled, CLIMB_PER_POINT, CLIMB_MAX_BONUS),
                DEFAULTS.sneak(),
                DEFAULTS.spotTripwires(),
                DEFAULTS.disarmTripwires(),
                DEFAULTS.searchChests(),
                DEFAULTS.monsterLore(),
                DEFAULTS.structureLore(),
                DEFAULTS.taming(),
                DEFAULTS.hunger(),
                DEFAULTS.ailments());
    }

    private static ScoresConfig.ExplorationSettings sneak(boolean enabled, double perPoint) {
        return new ScoresConfig.ExplorationSettings(
                DEFAULTS.leap(),
                DEFAULTS.landing(),
                DEFAULTS.cobwebs(),
                DEFAULTS.climbing(),
                new ScoresConfig.SneakSettings(enabled, perPoint, MIN_VISIBILITY),
                DEFAULTS.spotTripwires(),
                DEFAULTS.disarmTripwires(),
                DEFAULTS.searchChests(),
                DEFAULTS.monsterLore(),
                DEFAULTS.structureLore(),
                DEFAULTS.taming(),
                DEFAULTS.hunger(),
                DEFAULTS.ailments());
    }

    private static ScoresConfig.ExplorationSettings spot(boolean enabled, int dc) {
        return new ScoresConfig.ExplorationSettings(
                DEFAULTS.leap(),
                DEFAULTS.landing(),
                DEFAULTS.cobwebs(),
                DEFAULTS.climbing(),
                DEFAULTS.sneak(),
                new ScoresConfig.SpotTripwireSettings(enabled, dc, SPOT_RANGE, EVERY_TICK),
                DEFAULTS.disarmTripwires(),
                DEFAULTS.searchChests(),
                DEFAULTS.monsterLore(),
                DEFAULTS.structureLore(),
                DEFAULTS.taming(),
                DEFAULTS.hunger(),
                DEFAULTS.ailments());
    }

    private static ScoresConfig.ExplorationSettings disarm(boolean enabled) {
        return new ScoresConfig.ExplorationSettings(
                DEFAULTS.leap(),
                DEFAULTS.landing(),
                DEFAULTS.cobwebs(),
                DEFAULTS.climbing(),
                DEFAULTS.sneak(),
                DEFAULTS.spotTripwires(),
                new ScoresConfig.DisarmTripwireSettings(enabled, DC),
                DEFAULTS.searchChests(),
                DEFAULTS.monsterLore(),
                DEFAULTS.structureLore(),
                DEFAULTS.taming(),
                DEFAULTS.hunger(),
                DEFAULTS.ailments());
    }

    private static void expectVisibility(
            GameTestHelper helper,
            ServerPlayer player,
            Mob looker,
            ScoresConfig.ExplorationSettings settings,
            double expected,
            String what) {
        double visibility = withExploration(settings, () -> player.getVisibilityPercent(looker));
        expectClose(helper, expected, visibility, "visibility " + what);
    }

    private static Void expectPass(GameTestHelper helper, InteractionResult result, String what) {
        expect(helper, result == InteractionResult.PASS, "using the wire " + what + " must be left to vanilla");
        return null;
    }

    private static int glints(GameTestHelper helper, List<Packet<?>> packets, BlockPos wire) {
        BlockPos absolute = helper.absolutePos(wire);
        return (int) packets.stream()
                .filter(packet -> packet instanceof ClientboundLevelParticlesPacket particles
                        && Math.floor(particles.getX()) == absolute.getX()
                        && Math.floor(particles.getZ()) == absolute.getZ())
                .count();
    }

    static void expectLine(GameTestHelper helper, List<CustomPacketPayload> lines, String pool) {
        boolean shown = lines.stream()
                .anyMatch(payload -> payload instanceof RollLinePayload line
                        && (line.detail().orElse(line.roll()).getContents() instanceof TranslatableContents text)
                        && text.getKey().startsWith(pool));
        expect(helper, shown, "a line from " + pool + "* must show above the hotbar, sent " + lines);
    }
}
