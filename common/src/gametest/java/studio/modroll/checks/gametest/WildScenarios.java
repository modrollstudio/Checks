package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ExplorationScenarios.expectLine;
import static studio.modroll.checks.gametest.ExplorationScenarios.exploring;
import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectClose;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.SocialScenarios.calm;
import static studio.modroll.checks.gametest.SocialScenarios.survivor;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.data.ScoresConfig;

/**
 * GameTest bodies for the wild skills: Animal Handling when taming, Survival on hunger and Medicine on
 * ailments. Players are new survival players with every score 10 and no proficiency, so every check adds +0;
 * a d20 of 15 makes the DC and a d20 of 2 fails it. A test that forces no d20 fails if anything rolls.
 */
public final class WildScenarios {

    private static final int MADE = 15;
    private static final int FAILED = 2;
    private static final int DC = 12;

    private static final ScoresConfig.ExplorationSettings DEFAULTS = ScoresConfig.DEFAULTS.exploration();

    private static final Vec3 STAND = new Vec3(1.5, 2, 1.5);
    private static final BlockPos ANIMAL_SPOT = new BlockPos(1, 2, 3);
    private static final double HORSE_GOAL_SPEED = 1.2;
    /** Far more decisions than a horse with a rider ever needs to make one. */
    private static final int HORSE_TICKS = 5000;

    private static final double PER_POINT = 0.05;
    private static final double MAX_REDUCTION = 0.25;
    /** WIS 16: Survival and Medicine +3. */
    private static final String WISE = "wis 16";
    /** WIS 30: Survival and Medicine +10. */
    private static final String SAGE = "wis 30";

    private static final float EXHAUSTION = 4;
    private static final int TEN_SECONDS = 200;
    private static final int TEN_SECONDS_LESS_15_PERCENT = 170;
    private static final int TEN_SECONDS_LESS_25_PERCENT = 150;

    private WildScenarios() {}

    /** Feeding a wolf, a cat or a parrot rolls Animal Handling, and a made check tames it on that attempt. */
    public static void aMadeAnimalHandlingCheckTamesOnThatAttempt(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "tamer", STAND);
        List<CustomPacketPayload> lines = new ArrayList<>();
        try {
            expectTamed(helper, player, calm(helper, EntityType.WOLF, ANIMAL_SPOT), Items.BONE, lines);
            expectTamed(helper, player, calm(helper, EntityType.CAT, ANIMAL_SPOT), Items.COD, lines);
            expectTamed(helper, player, calm(helper, EntityType.PARROT, ANIMAL_SPOT), Items.WHEAT_SEEDS, lines);
            expectLine(helper, lines, "checks.exploration.taming.success.");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A horse with a player on its back rolls Animal Handling when it decides whether to accept them: a failed
     * check leaves it to its temper, here none, so it throws them; a made one tames it.
     */
    public static void aHorseDecidesOnAnimalHandlingOrItsTemper(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "rider", STAND);
        Horse horse = calm(helper, EntityType.HORSE, ANIMAL_SPOT);
        RunAroundLikeCrazyGoal decides = new RunAroundLikeCrazyGoal(horse, HORSE_GOAL_SPEED);
        List<CustomPacketPayload> lines = new ArrayList<>();
        try {
            ride(helper, player, horse, decides, lines, FAILED);
            expect(helper, !horse.isTamed(), "a failed check must leave the horse to its empty temper");
            expect(helper, !player.isPassenger(), "an untamed horse throws its rider");
            expectLine(helper, lines, "checks.exploration.taming.failure.");

            ride(helper, player, horse, decides, lines, MADE);
            expect(
                    helper,
                    horse.isTamed() && player.getUUID().equals(horse.getOwnerUUID()),
                    "a made check must tame the horse");
            expectLine(helper, lines, "checks.exploration.taming.success.");
        } finally {
            horse.discard();
            logout(helper, player);
        }
        helper.succeed();
    }

    /** With taming off, or for a creative player, feeding a wolf rolls nothing and is left to vanilla's chance. */
    public static void tamingRollsNothingWhileOffOrInCreative(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "tamer-none", STAND);
        try {
            feed(player, calm(helper, EntityType.WOLF, ANIMAL_SPOT), Items.BONE, taming(false), new ArrayList<>());
            player.setGameMode(GameType.CREATIVE);
            feed(player, calm(helper, EntityType.WOLF, ANIMAL_SPOT), Items.BONE, taming(true), new ArrayList<>());
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * Hunger drains 5% slower per positive Survival point, at most 25%, with no roll: +3 gains 85% of the
     * exhaustion and +10 75%; with hunger off, all of it.
     */
    public static void survivalSlowsHunger(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "forager", STAND);
        try {
            run(helper, "checks set " + player.getScoreboardName() + " " + WISE);
            expectExhaustion(helper, player, true, EXHAUSTION * 0.85f, "at +3");
            expectExhaustion(helper, player, false, EXHAUSTION, "while off");
            run(helper, "checks set " + player.getScoreboardName() + " " + SAGE);
            expectExhaustion(helper, player, true, EXHAUSTION * 0.75f, "at +10");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * Poison, wither, hunger and nausea last 5% shorter per positive Medicine point, at most 25%, with no roll,
     * on top of a failed CON save; other effects, effects a command gives, and anything while ailments are off,
     * last as long as given.
     */
    public static void medicineShortensAilments(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "medic", STAND);
        try {
            run(helper, "checks set " + player.getScoreboardName() + " " + WISE);
            expectDuration(helper, player, true, MobEffects.CONFUSION, TEN_SECONDS_LESS_15_PERCENT);
            expectDuration(helper, player, true, MobEffects.HUNGER, TEN_SECONDS_LESS_15_PERCENT);
            expectDuration(helper, player, true, MobEffects.MOVEMENT_SPEED, TEN_SECONDS);
            expectDuration(helper, player, false, MobEffects.CONFUSION, TEN_SECONDS);
            run(helper, "effect give " + player.getScoreboardName() + " minecraft:nausea 10");
            expectEquals(helper, TEN_SECONDS, duration(player, MobEffects.CONFUSION), "nausea from a command");
            player.removeAllEffects();
            run(helper, "checks set " + player.getScoreboardName() + " " + SAGE);
            expectDuration(helper, player, true, MobEffects.CONFUSION, TEN_SECONDS_LESS_25_PERCENT);
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    private static void expectTamed(
            GameTestHelper helper,
            ServerPlayer player,
            TamableAnimal animal,
            Item food,
            List<CustomPacketPayload> lines) {
        feed(player, animal, food, taming(true), lines, MADE);
        expect(
                helper,
                animal.isTame() && animal.isOwnedBy(player),
                animal.getType() + " must be tamed by a made check");
    }

    private static void feed(
            ServerPlayer player,
            TamableAnimal animal,
            Item food,
            ScoresConfig.ExplorationSettings settings,
            List<CustomPacketPayload> lines,
            int... d20s) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(food));
        exploring(settings, lines, () -> withD20s(() -> animal.mobInteract(player, InteractionHand.MAIN_HAND), d20s));
        animal.discard();
    }

    /** Mounts the horse and lets it decide, once, whether to accept the player. */
    private static void ride(
            GameTestHelper helper,
            ServerPlayer player,
            Horse horse,
            RunAroundLikeCrazyGoal decides,
            List<CustomPacketPayload> lines,
            int d20) {
        player.startRiding(horse, true);
        int before = lines.size();
        exploring(
                taming(true),
                lines,
                () -> withD20s(
                        () -> {
                            for (int tick = 0; tick < HORSE_TICKS && lines.size() == before; tick++) {
                                decides.tick();
                            }
                            return null;
                        },
                        d20));
        expectEquals(helper, before + 1, lines.size(), "rolls for one decision");
    }

    private static void expectExhaustion(
            GameTestHelper helper, ServerPlayer player, boolean enabled, float expected, String what) {
        player.getFoodData().setExhaustion(0);
        exploring(hunger(enabled), new ArrayList<>(), () -> {
            player.causeFoodExhaustion(EXHAUSTION);
            return null;
        });
        expectClose(helper, expected, player.getFoodData().getExhaustionLevel(), "exhaustion " + what);
    }

    /** Adds the effect for ten seconds, failing any CON save it calls for. */
    private static void expectDuration(
            GameTestHelper helper, ServerPlayer player, boolean enabled, Holder<MobEffect> effect, int expected) {
        player.removeAllEffects();
        exploring(
                ailments(enabled),
                new ArrayList<>(),
                () -> withD20s(() -> player.addEffect(new MobEffectInstance(effect, TEN_SECONDS)), FAILED));
        expectEquals(helper, expected, duration(player, effect), effect.value().getDescriptionId() + " duration");
        player.removeAllEffects();
    }

    private static int duration(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance instance = player.getEffect(effect);
        return instance == null ? 0 : instance.getDuration();
    }

    private static ScoresConfig.ExplorationSettings taming(boolean enabled) {
        return new ScoresConfig.ExplorationSettings(
                DEFAULTS.leap(),
                DEFAULTS.landing(),
                DEFAULTS.cobwebs(),
                DEFAULTS.climbing(),
                DEFAULTS.sneak(),
                DEFAULTS.spotTripwires(),
                DEFAULTS.disarmTripwires(),
                DEFAULTS.searchChests(),
                DEFAULTS.monsterLore(),
                DEFAULTS.structureLore(),
                new ScoresConfig.TamingSettings(enabled, DC),
                DEFAULTS.hunger(),
                DEFAULTS.ailments());
    }

    private static ScoresConfig.ExplorationSettings hunger(boolean enabled) {
        return new ScoresConfig.ExplorationSettings(
                DEFAULTS.leap(),
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
                new ScoresConfig.ReductionSettings(enabled, PER_POINT, MAX_REDUCTION),
                DEFAULTS.ailments());
    }

    private static ScoresConfig.ExplorationSettings ailments(boolean enabled) {
        return new ScoresConfig.ExplorationSettings(
                DEFAULTS.leap(),
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
                new ScoresConfig.ReductionSettings(enabled, PER_POINT, MAX_REDUCTION));
    }
}
