package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.loginWatched;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.waitOutSpawnProtection;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withDice;
import static studio.modroll.checks.gametest.ScenarioSupport.withRest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.body.PlayerBodies;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.level.LevelStore;
import studio.modroll.checks.level.PlayerLevel;
import studio.modroll.checks.rest.Rests;
import studio.modroll.checks.sheet.StatSheet;
import studio.modroll.checks.sheet.StatSheets;
import studio.modroll.checks.trait.TraitUses;

/**
 * GameTest bodies for Hit Dice and rests. The campfire sits well above the test's own structure, so no monster from a
 * neighbouring test is within the rest's monster range; each test takes it away again. A short rest lasts its shipped
 * 1000 ticks unless a test shortens it, and a shortened one is finished by calling the tick under that setting.
 * That tick finishes every player's rest on the server, so each test that calls it runs in a batch of its own.
 */
public final class RestScenarios {

    private static final ScoresConfig.RestSettings DEFAULTS = ScoresConfig.DEFAULTS.rest();
    private static final BlockPos FIRE = new BlockPos(1, 24, 1);
    private static final Vec3 BY_THE_FIRE = new Vec3(1.5, 24, 2.5);
    private static final Vec3 AWAY = new Vec3(1.5, 24, 8.5);
    private static final int LEVEL = 5;
    private static final int PAST_A_SHORT_REST = 3;
    private static final ResourceLocation RELENTLESS = Checks.id("relentless_endurance");

    /** The players a test has put in a fight; the blocker is registered once and never removed. */
    private static final Set<UUID> IN_A_FIGHT = ConcurrentHashMap.newKeySet();

    private static boolean blockerRegistered;

    private RestScenarios() {}

    /**
     * Using a lit campfire with an empty hand starts a short rest. When it is done, Hit Dice are spent one at a time,
     * each rolled through Critfall (a forced 6 on the d8, CON 10), until the player is at full health: from 13 of 24,
     * two dice heal 11. One chat line says so. A rest at full health spends none and says that once.
     */
    public static void aShortRestSpendsHitDiceUntilFullHealth(GameTestHelper helper) {
        List<Packet<?>> sent = new ArrayList<>();
        ServerPlayer player = rester(helper, "rest-short", sent);
        rested(() -> {
            expect(helper, useFire(helper, player).consumesAction(), "using the fire must start a rest");
            expect(helper, Rests.resting(player), "the player must be resting");
            expectEquals(helper, LEVEL, Rests.hitDiceLeft(player), "Hit Dice before the rest is done");
            return null;
        });
        helper.runAfterDelay(PAST_A_SHORT_REST, () -> {
            try {
                withRest(
                        shortRestOf(0),
                        () -> unprofiled(() -> {
                            expectEquals(helper, 24, (int) player.getMaxHealth(), "max health at level 5");
                            player.setHealth(13f);
                            sent.clear();
                            withDice(
                                    () -> {
                                        Rests.tick(player.server);
                                        return null;
                                    },
                                    6,
                                    6);
                            expect(helper, !Rests.resting(player), "the rest must be over");
                            expect(
                                    helper,
                                    player.getHealth() == 24f,
                                    "two 6s must heal to full, health was " + player.getHealth());
                            expectEquals(helper, LEVEL - 2, ChecksApi.hitDiceLeft(player), "Hit Dice after the rest");
                            expectRestLine(helper, sent, "checks.rest.short.spent", 2, 11);
                            StatSheet.HitDiceLine dice = StatSheets.forPlayer(player)
                                    .orElseThrow()
                                    .body()
                                    .hitDice()
                                    .orElseThrow();
                            expect(
                                    helper,
                                    dice.equals(new StatSheet.HitDiceLine(8, LEVEL - 2, LEVEL, 1.0, true)),
                                    "the sheet must show 3 of 5 d8, showed " + dice);

                            expect(helper, useFire(helper, player).consumesAction(), "a second rest must start");
                            sent.clear();
                            Rests.tick(player.server);
                            expectEquals(helper, LEVEL - 2, Rests.hitDiceLeft(player), "Hit Dice at full health");
                            expectRestLine(helper, sent, "checks.rest.short.full_health");
                            return null;
                        }));
            } finally {
                cleanUp(helper, player);
            }
            helper.succeed();
        });
    }

    /**
     * Each Hit Die heals at least 1, as in the 2024 rules: with CON 1 (-5) a forced 1 heals 1, not 0. Max health is
     * 10 (20, -10 from CON, nothing from the levels), so five dice heal from 5 to full.
     */
    public static void aHitDieHealsAtLeastOne(GameTestHelper helper) {
        List<Packet<?>> sent = new ArrayList<>();
        ServerPlayer player = rester(helper, "rest-min-heal", sent);
        try {
            withRest(
                    shortRestOf(0),
                    () -> unprofiled(() -> {
                        run(helper, "checks set " + player.getScoreboardName() + " con 1");
                        PlayerBodies.sync(player);
                        expectEquals(helper, 10, (int) player.getMaxHealth(), "max health with CON 1");
                        player.setHealth(5f);
                        expect(helper, useFire(helper, player).consumesAction(), "using the fire must start a rest");
                        sent.clear();
                        withDice(
                                () -> {
                                    Rests.tick(player.server);
                                    return null;
                                },
                                1,
                                1,
                                1,
                                1,
                                1);
                        expect(
                                helper,
                                player.getHealth() == 10f,
                                "five 1s must heal 5, health was " + player.getHealth());
                        expectRestLine(helper, sent, "checks.rest.short.spent", 5, 5);
                        return null;
                    }));
        } finally {
            cleanUp(helper, player);
        }
        helper.succeed();
    }

    /** With no Hit Dice left a finished short rest heals nothing and says so. */
    public static void aShortRestWithoutHitDiceHealsNothing(GameTestHelper helper) {
        List<Packet<?>> sent = new ArrayList<>();
        ServerPlayer player = rester(helper, "rest-no-dice", sent);
        try {
            withRest(
                    shortRestOf(0),
                    () -> unprofiled(() -> {
                        spendAll(player);
                        player.setHealth(10f);
                        expect(helper, useFire(helper, player).consumesAction(), "using the fire must start a rest");
                        sent.clear();
                        Rests.tick(player.server);
                        expect(helper, player.getHealth() == 10f, "no Hit Dice must heal nothing");
                        expectRestLine(helper, sent, "checks.rest.short.no_dice");
                        return null;
                    }));
        } finally {
            cleanUp(helper, player);
        }
        helper.succeed();
    }

    /**
     * A short rest breaks when the player takes damage, attacks, leaves the fire, the fire goes out, or another mod
     * says they are in a fight; it can't start with a monster nearby, or with a full hand.
     */
    public static void aShortRestBreaks(GameTestHelper helper) {
        ServerPlayer player = rester(helper, "rest-breaks");
        try {
            rested(() -> {
                expectBroken(
                        helper,
                        player,
                        "taking damage",
                        () -> player.hurt(player.damageSources().generic(), 2f));

                Mob cow = helper.spawn(EntityType.COW, FIRE.east());
                cow.setNoAi(true);
                expectBroken(
                        helper,
                        player,
                        "attacking",
                        () -> withD20s(
                                () -> {
                                    player.attack(cow);
                                    return null;
                                },
                                20));
                cow.discard();

                expectBroken(helper, player, "leaving the fire", () -> {
                    player.moveTo(helper.absoluteVec(AWAY));
                    Rests.tick(player.server);
                    player.moveTo(helper.absoluteVec(BY_THE_FIRE));
                });

                expectBroken(helper, player, "the fire going out", () -> {
                    helper.setBlock(FIRE, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
                    Rests.tick(player.server);
                    helper.setBlock(FIRE, Blocks.CAMPFIRE.defaultBlockState());
                });

                registerBlocker();
                expectBroken(helper, player, "joining a fight", () -> {
                    IN_A_FIGHT.add(player.getUUID());
                    Rests.tick(player.server);
                });
                expect(helper, useFire(helper, player).consumesAction(), "the fire answers even in a fight");
                expect(helper, !Rests.resting(player), "no rest can start in a fight");
                IN_A_FIGHT.remove(player.getUUID());

                Mob zombie = helper.spawn(EntityType.ZOMBIE, FIRE.west());
                zombie.setNoAi(true);
                useFire(helper, player);
                expect(helper, !Rests.resting(player), "no rest can start with a monster nearby");
                zombie.discard();

                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                expect(helper, !useFire(helper, player).consumesAction(), "a full hand must not start a rest");
                expect(helper, !Rests.resting(player), "no rest with a full hand");
                return null;
            });
        } finally {
            IN_A_FIGHT.remove(player.getUUID());
            cleanUp(helper, player);
        }
        helper.succeed();
    }

    /**
     * Sleeping through the night is the 2024 long rest: it heals fully, gives back every spent Hit Die and readies
     * once-a-day traits, and the tooltip says so. A player who stayed awake gets nothing. With the 2014 rule
     * ({@code regain_fraction} 0.5, no healing) half the level's dice come back, rounded down. The night is not really
     * skipped, which would change the time for every test: the test calls what each loader's ServerLevel mixin calls
     * as vanilla wakes everyone after a skipped night.
     */
    public static void aLongRestHealsAndRegainsHitDiceAndTraits(GameTestHelper helper) {
        ServerPlayer sleeper = rester(helper, "rest-long");
        ServerPlayer awake = rester(helper, "rest-awake");
        try {
            withRest(
                    new ScoresConfig.RestSettings(
                            DEFAULTS.hitDice(),
                            DEFAULTS.shortRest(),
                            new ScoresConfig.LongRestSettings(true, 0.5, 1, false, true)),
                    () -> unprofiled(() -> {
                        spendAll(sleeper);
                        sleeper.setHealth(4f);
                        sleeper.startSleeping(helper.absolutePos(FIRE.north()));
                        Rests.onNightSlept(helper.getLevel());
                        expectEquals(helper, 2, Rests.hitDiceLeft(sleeper), "Hit Dice after a 2014 long rest");
                        expect(
                                helper,
                                sleeper.getHealth() == 4f,
                                "a long rest without healing must not heal, health was " + sleeper.getHealth());
                        return null;
                    }));
            rested(() -> {
                for (ServerPlayer player : List.of(sleeper, awake)) {
                    spendAll(player);
                    TraitUses.get(player.server)
                            .use(player.getUUID(), RELENTLESS, helper.getLevel().getGameTime());
                    player.setHealth(4f);
                }
                Rests.onNightSlept(helper.getLevel());
                expectEquals(helper, LEVEL, Rests.hitDiceLeft(sleeper), "Hit Dice after a long rest");
                expect(
                        helper,
                        sleeper.getHealth() == sleeper.getMaxHealth(),
                        "a long rest must heal fully, health was " + sleeper.getHealth());
                expect(
                        helper,
                        TraitUses.get(sleeper.server)
                                .lastUse(sleeper.getUUID(), RELENTLESS)
                                .isEmpty(),
                        "a long rest must ready the sleeper's traits");
                StatSheet.HitDiceLine dice = StatSheets.forPlayer(sleeper)
                        .orElseThrow()
                        .body()
                        .hitDice()
                        .orElseThrow();
                expect(
                        helper,
                        dice.equals(new StatSheet.HitDiceLine(8, LEVEL, LEVEL, 1.0, true)),
                        "the sheet must show the long rest's settings, showed " + dice);
                expectEquals(helper, 0, Rests.hitDiceLeft(awake), "Hit Dice of a player who stayed awake");
                expect(helper, awake.getHealth() == 4f, "a player who stayed awake must not heal");
                expect(
                        helper,
                        TraitUses.get(awake.server)
                                .lastUse(awake.getUUID(), RELENTLESS)
                                .isPresent(),
                        "a player who stayed awake keeps their used traits");
                return null;
            });
        } finally {
            sleeper.stopSleeping();
            logout(helper, sleeper);
            logout(helper, awake);
        }
        helper.succeed();
    }

    /**
     * With Hit Dice off a campfire is left to vanilla and the sheet shows no pool; with short rests off the same; with
     * long rests off sleeping gives nothing back.
     */
    public static void restsSwitchOff(GameTestHelper helper) {
        ServerPlayer player = rester(helper, "rest-off");
        try {
            withRest(
                    new ScoresConfig.RestSettings(
                            new ScoresConfig.HitDiceSettings(false, 1.0, 1), DEFAULTS.shortRest(), DEFAULTS.longRest()),
                    () -> unprofiled(() -> {
                        expect(helper, !useFire(helper, player).consumesAction(), "the fire with Hit Dice off");
                        expect(
                                helper,
                                StatSheets.forPlayer(player)
                                        .orElseThrow()
                                        .body()
                                        .hitDice()
                                        .isEmpty(),
                                "the sheet must show no Hit Dice while they are off");
                        expectEquals(helper, 0, ChecksApi.hitDiceLeft(player), "Hit Dice while off");
                        return null;
                    }));
            withRest(
                    new ScoresConfig.RestSettings(
                            DEFAULTS.hitDice(),
                            new ScoresConfig.ShortRestSettings(false, 1000, 3, 8, 5),
                            new ScoresConfig.LongRestSettings(false, 1.0, 1, true, true)),
                    () -> unprofiled(() -> {
                        expect(helper, !useFire(helper, player).consumesAction(), "the fire with short rests off");
                        spendAll(player);
                        player.setHealth(4f);
                        Rests.longRest(player);
                        expectEquals(helper, 0, Rests.hitDiceLeft(player), "Hit Dice after sleeping, long rests off");
                        expect(helper, player.getHealth() == 4f, "sleeping must not heal with long rests off");
                        return null;
                    }));
        } finally {
            cleanUp(helper, player);
        }
        helper.succeed();
    }

    /** Starts a rest, runs {@code breaker}, and expects the rest to be gone. */
    private static void expectBroken(GameTestHelper helper, ServerPlayer player, String what, Runnable breaker) {
        expect(helper, useFire(helper, player).consumesAction(), "the rest before " + what + " must start");
        expect(helper, Rests.resting(player), "the player must be resting before " + what);
        breaker.run();
        expect(helper, !Rests.resting(player), what + " must break the rest");
    }

    /** A survivor at level 5 with no class (a d8), by a lit campfire with an empty hand. */
    private static ServerPlayer rester(GameTestHelper helper, String name) {
        return rester(helper, name, new ArrayList<>());
    }

    /** As {@link #rester(GameTestHelper, String)}, with every packet sent to the player added to {@code sent}. */
    private static ServerPlayer rester(GameTestHelper helper, String name, List<Packet<?>> sent) {
        helper.setBlock(FIRE, Blocks.CAMPFIRE.defaultBlockState());
        ServerPlayer player = loginWatched(helper, newProfile(name), sent);
        player.setGameMode(GameType.SURVIVAL);
        waitOutSpawnProtection(player);
        player.moveTo(helper.absoluteVec(BY_THE_FIRE));
        LevelStore.get(player.server).set(player.getUUID(), PlayerLevel.START.withLevelAndXp(LEVEL, 650));
        return player;
    }

    /** Exactly one chat line about rests was sent, the one under {@code key} with {@code args}. */
    private static void expectRestLine(GameTestHelper helper, List<Packet<?>> sent, String key, Object... args) {
        List<TranslatableContents> lines = sent.stream()
                .filter(ClientboundSystemChatPacket.class::isInstance)
                .map(packet -> ((ClientboundSystemChatPacket) packet).content().getContents())
                .filter(TranslatableContents.class::isInstance)
                .map(TranslatableContents.class::cast)
                .filter(line -> line.getKey().startsWith("checks.rest."))
                .toList();
        expectEquals(helper, 1, lines.size(), "rest chat lines");
        expect(
                helper,
                key.equals(lines.getFirst().getKey()),
                "the rest line must be " + key + ", was " + lines.getFirst().getKey());
        expect(
                helper,
                List.of(args).equals(List.of(lines.getFirst().getArgs())),
                "the rest line's values must be " + List.of(args) + ", were "
                        + List.of(lines.getFirst().getArgs()));
    }

    private static void spendAll(ServerPlayer player) {
        LevelStore store = LevelStore.get(player.server);
        store.set(player.getUUID(), store.level(player.getUUID()).withHitDiceSpent(LEVEL));
    }

    private static InteractionResult useFire(GameTestHelper helper, ServerPlayer player) {
        BlockPos pos = helper.absolutePos(FIRE);
        return player.gameMode.useItemOn(
                player,
                helper.getLevel(),
                player.getMainHandItem(),
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static void cleanUp(GameTestHelper helper, ServerPlayer player) {
        logout(helper, player);
        helper.setBlock(FIRE, Blocks.AIR.defaultBlockState());
    }

    private static synchronized void registerBlocker() {
        if (!blockerRegistered) {
            ChecksApi.addRestBlocker(player -> IN_A_FIGHT.contains(player.getUUID()));
            blockerRegistered = true;
        }
    }

    private static ScoresConfig.RestSettings shortRestOf(int ticks) {
        ScoresConfig.ShortRestSettings shipped = DEFAULTS.shortRest();
        return new ScoresConfig.RestSettings(
                DEFAULTS.hitDice(),
                new ScoresConfig.ShortRestSettings(
                        true, ticks, shipped.campfireRadius(), shipped.monsterRange(), shipped.monsterHeight()),
                DEFAULTS.longRest());
    }

    private static <T> T rested(Supplier<T> body) {
        return withRest(DEFAULTS, () -> unprofiled(body));
    }
}
