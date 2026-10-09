package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.spawnCalm;
import static studio.modroll.checks.gametest.ScenarioSupport.standardSkill;
import static studio.modroll.checks.gametest.ScenarioSupport.trigger;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withExtensions;
import static studio.modroll.checks.gametest.ScenarioSupport.withListeners;
import static studio.modroll.checks.gametest.ScenarioSupport.withTriggers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.trigger.CheckTrigger;
import studio.modroll.checks.trigger.CheckTriggers;

/**
 * GameTest bodies for datapack check triggers. Block and item uses go through the player's game mode, so
 * the loader's own interaction hook fires the trigger; entity use calls the handler both loaders' hooks
 * call, since Fabric only hooks it in the network handler. The outcomes are the gametest datapack's
 * functions, which tag the player (success also breaks the block), and a loot table holding one diamond.
 */
public final class TriggerScenarios {

    private static final String TAG_PREFIX = "checks_trigger_";
    private static final BlockPos WALL = new BlockPos(1, 2, 1);
    private static final BlockPos OTHER_WALL = new BlockPos(2, 2, 1);
    private static final int SUCCESS_FACE = 15;
    private static final int FAILURE_FACE = 5;
    private static final int DC_ABOVE_MODIFIER = 10;

    private TriggerScenarios() {}

    /** One trigger on cracked stone bricks takes each branch with the face it is forced to roll. */
    public static void blockTriggerTakesEachBranch(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("WallBreaker"));
        CheckTrigger wall = trigger(
                "gametest_wall",
                "{\"on\": \"use_block\", \"block\": \"minecraft:cracked_stone_bricks\","
                        + " \"stat\": \"athletics\", \"dc\": " + dc(player, "athletics") + ", \"outcomes\": {"
                        + outcome("success", "function", "checks:gametest/trigger_success") + ", "
                        + outcome("failure", "function", "checks:gametest/trigger_failure") + ", "
                        + outcome("natural_1", "function", "checks:gametest/trigger_natural_1") + ", "
                        + outcome("natural_20", "loot_table", "checks:gametest/trigger_reward") + "}}");
        helper.setBlock(WALL, Blocks.CRACKED_STONE_BRICKS);
        withTriggers(List.of(wall), () -> {
            expectFired(helper, withD20s(() -> useBlock(helper, player, WALL), FAILURE_FACE), "failure");
            expectTags(helper, player, Set.of("failure"));
            helper.assertBlockPresent(Blocks.CRACKED_STONE_BRICKS, WALL);

            expectFired(helper, withD20s(() -> useBlock(helper, player, WALL), 1), "natural 1");
            expectTags(helper, player, Set.of("natural_1"));

            expectFired(helper, withD20s(() -> useBlock(helper, player, WALL), 20), "natural 20");
            expectTags(helper, player, Set.of());
            expect(helper, player.getInventory().contains(new ItemStack(Items.DIAMOND)), "natural 20 gives a diamond");

            expectFired(helper, withD20s(() -> useBlock(helper, player, WALL), SUCCESS_FACE), "success");
            expectTags(helper, player, Set.of("success"));
            helper.assertBlockPresent(Blocks.AIR, WALL);
            return null;
        });
        logout(helper, player);
        helper.succeed();
    }

    /** Using a matching entity rolls; the off hand never does. */
    public static void entityTriggerFires(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("Persuader"));
        Mob villager = spawnCalm(helper, EntityType.VILLAGER);
        CheckTrigger talk = trigger(
                "gametest_talk",
                "{\"on\": \"use_entity\", \"entity\": \"minecraft:villager\","
                        + " \"stat\": \"persuasion\", \"dc\": " + dc(player, "persuasion") + ", \"outcomes\": {"
                        + outcome("success", "function", "checks:gametest/trigger_failure") + "}}");
        withTriggers(List.of(talk), () -> {
            InteractionResult offhand = CheckTriggers.onUseEntity(player, InteractionHand.OFF_HAND, villager);
            expect(helper, offhand == InteractionResult.PASS, "the off hand must not fire, was " + offhand);
            InteractionResult mainhand = withD20s(
                    () -> CheckTriggers.onUseEntity(player, InteractionHand.MAIN_HAND, villager), SUCCESS_FACE);
            expectFired(helper, mainhand, "entity use");
            expectTags(helper, player, Set.of("failure"));
            return null;
        });
        logout(helper, player);
        helper.succeed();
    }

    /** Using a compass rolls survival and hands out the loot table. */
    public static void itemTriggerFires(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("Navigator"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COMPASS));
        CheckTrigger compass = trigger(
                "gametest_compass",
                "{\"on\": \"use_item\", \"item\": \"minecraft:compass\","
                        + " \"stat\": \"survival\", \"dc\": " + dc(player, "survival") + ", \"outcomes\": {"
                        + outcome("success", "loot_table", "checks:gametest/trigger_reward") + "}}");
        withTriggers(List.of(compass), () -> {
            InteractionResult result = withD20s(
                    () -> player.gameMode.useItem(
                            player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND),
                    SUCCESS_FACE);
            expectFired(helper, result, "item use");
            expect(helper, player.getInventory().contains(new ItemStack(Items.DIAMOND)), "success gives a diamond");
            return null;
        });
        logout(helper, player);
        helper.succeed();
    }

    /**
     * A player cooldown stops the same player on any wall; a target cooldown stops anyone on the same
     * wall but not on another. A blocked use rolls nothing: only one face is scripted per fired use.
     */
    public static void cooldownsBlockPlayersAndTargets(GameTestHelper helper) {
        ServerPlayer alice = login(helper, newProfile("Alice"));
        ServerPlayer bob = login(helper, newProfile("Bob"));
        helper.setBlock(WALL, Blocks.CRACKED_STONE_BRICKS);
        helper.setBlock(OTHER_WALL, Blocks.CRACKED_STONE_BRICKS);
        CheckTrigger perPlayer = wallTrigger("gametest_player_cooldown", alice, "{\"player\": 100}");
        withTriggers(List.of(perPlayer), () -> {
            expectFired(helper, withD20s(() -> useBlock(helper, alice, WALL), FAILURE_FACE), "first use");
            expectPassed(helper, useBlock(helper, alice, OTHER_WALL), "the same player on another wall");
            expectFired(helper, withD20s(() -> useBlock(helper, bob, WALL), FAILURE_FACE), "another player");
            return null;
        });
        CheckTrigger perTarget = wallTrigger("gametest_target_cooldown", alice, "{\"target\": 100}");
        withTriggers(List.of(perTarget), () -> {
            expectFired(helper, withD20s(() -> useBlock(helper, alice, WALL), FAILURE_FACE), "first use");
            expectPassed(helper, useBlock(helper, bob, WALL), "another player on the same wall");
            expectFired(helper, withD20s(() -> useBlock(helper, bob, OTHER_WALL), FAILURE_FACE), "another wall");
            return null;
        });
        logout(helper, alice);
        logout(helper, bob);
        helper.succeed();
    }

    /** Switched off, or with the roll canceled by a listener, the use is left to vanilla and nothing runs. */
    public static void disabledOrCanceledTriggersDoNothing(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("Bystander"));
        helper.setBlock(WALL, Blocks.CRACKED_STONE_BRICKS);
        CheckTrigger wall = wallTrigger("gametest_quiet", player, "{}");
        withTriggers(List.of(wall), () -> {
            expectPassed(
                    helper,
                    withExtensions(
                            new ScoresConfig.ExtensionSettings(
                                    true, ScoresConfig.DEFAULTS.extensions().bonusSources(), false, true, true),
                            () -> useBlock(helper, player, WALL)),
                    "triggers off");
            expectPassed(
                    helper,
                    withListeners(event -> event.cancel(), event -> {}, () -> useBlock(helper, player, WALL)),
                    "a canceled roll");
            expectTags(helper, player, Set.of());
            return null;
        });
        logout(helper, player);
        helper.succeed();
    }

    private static CheckTrigger wallTrigger(String path, ServerPlayer player, String cooldown) {
        return trigger(
                path,
                "{\"on\": \"use_block\", \"block\": \"minecraft:cracked_stone_bricks\","
                        + " \"stat\": \"athletics\", \"dc\": " + dc(player, "athletics") + ", \"cooldown\": " + cooldown
                        + ", \"outcomes\": {"
                        + outcome("failure", "function", "checks:gametest/trigger_failure") + "}}");
    }

    /** The success face meets it and the failure face misses it, whatever the player's modifiers. */
    private static int dc(ServerPlayer player, String skill) {
        return DC_ABOVE_MODIFIER + ChecksApi.modifier(player, standardSkill(skill));
    }

    private static String outcome(String branch, String kind, String id) {
        return "\"" + branch + "\": {\"" + kind + "\": \"" + id + "\"}";
    }

    private static InteractionResult useBlock(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        return player.gameMode.useItemOn(
                player,
                helper.getLevel(),
                player.getMainHandItem(),
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static void expectFired(GameTestHelper helper, InteractionResult result, String what) {
        expect(helper, result.consumesAction(), what + " must fire the trigger, was " + result);
    }

    private static void expectPassed(GameTestHelper helper, InteractionResult result, String what) {
        expect(helper, !result.consumesAction(), what + " must not fire the trigger, was " + result);
    }

    /** The branch tags the outcome functions added since the last call, which are then removed. */
    private static void expectTags(GameTestHelper helper, ServerPlayer player, Set<String> branches) {
        Set<String> tags = player.getTags().stream()
                .filter(tag -> tag.startsWith(TAG_PREFIX))
                .collect(Collectors.toSet());
        tags.forEach(player::removeTag);
        Set<String> found =
                tags.stream().map(tag -> tag.substring(TAG_PREFIX.length())).collect(Collectors.toSet());
        expect(helper, found.equals(branches), "branches run must be " + branches + ", were " + found);
    }
}
