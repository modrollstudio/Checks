package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ExplorationScenarios.expectLine;
import static studio.modroll.checks.gametest.ExplorationScenarios.exploring;
import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.SocialScenarios.calm;
import static studio.modroll.checks.gametest.SocialScenarios.survivor;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.IglooPieces;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.exploration.ChestSearches;
import studio.modroll.checks.exploration.LoreStore;
import studio.modroll.checks.exploration.MobHints;
import studio.modroll.checks.exploration.MonsterLore;
import studio.modroll.checks.exploration.StructureLore;

/**
 * GameTest bodies for the knowledge skills: Investigation on looted chests, Nature, Arcana and Religion on
 * mobs, History on structures. Players are new survival players with every score 10 and no proficiency, so
 * every check adds +0; a d20 of 15 makes the DC and a d20 of 2 fails it. A test that forces no d20 fails if
 * anything rolls.
 */
public final class LoreScenarios {

    private static final int MADE = 15;
    private static final int FAILED = 2;
    private static final int DC = 12;
    private static final int SEARCH_DC = 13;

    private static final ScoresConfig.ExplorationSettings DEFAULTS = ScoresConfig.DEFAULTS.exploration();

    private static final Vec3 STAND = new Vec3(1.5, 2, 1.5);
    private static final BlockPos CHEST = new BlockPos(3, 2, 1);
    private static final BlockPos CHEST_EAST_HALF = new BlockPos(4, 2, 1);
    private static final ResourceKey<LootTable> DUNGEON = BuiltInLootTables.SIMPLE_DUNGEON;
    private static final long SEED = 0;

    private static final BlockPos MOB_SPOT = new BlockPos(1, 2, 4);
    private static final BlockPos FAR_MOB_SPOT = new BlockPos(1, 2, 6);
    private static final double RANGE = 4;
    private static final int NO_WAIT = 0;
    private static final int LONG_WAIT = Integer.MAX_VALUE / 2;
    private static final int EVERY_TICK = 1;
    private static final List<BlockPos> WALL = List.of(
            new BlockPos(0, 2, 3),
            new BlockPos(1, 2, 3),
            new BlockPos(2, 2, 3),
            new BlockPos(0, 3, 3),
            new BlockPos(1, 3, 3),
            new BlockPos(2, 3, 3),
            new BlockPos(0, 4, 3),
            new BlockPos(1, 4, 3),
            new BlockPos(2, 4, 3));

    /** Vanilla 1.21.1 has 82. */
    private static final int MOB_TYPES_AT_LEAST = 80;

    private static final int MAX_HINT_LENGTH = 70;

    private static final BlockPos IGLOO_CORNER = new BlockPos(0, 1, 0);
    private static final ResourceLocation IGLOO_TOP = ResourceLocation.withDefaultNamespace("igloo/top");
    private static final ResourceLocation IGLOO_KIND = ResourceLocation.fromNamespaceAndPath("checks", "history/igloo");

    private LoreScenarios() {}

    /**
     * The first sneaking use of an opened loot chest rolls Investigation: a made check finds one item from its
     * loot table. Using it before it was opened rolls nothing, and once searched it never rolls again.
     */
    public static void aMadeInvestigationCheckFindsOneMoreItem(GameTestHelper helper) {
        RandomizableContainerBlockEntity chest = lootChest(helper, CHEST, Blocks.CHEST.defaultBlockState());
        ServerPlayer player = searcher(helper, "search-made");
        List<CustomPacketPayload> lines = new ArrayList<>();
        try {
            expectPass(helper, searching(true, lines, () -> withD20s(() -> use(helper, player, CHEST))), "unopened");
            chest.unpackLootTable(player);

            InteractionResult result = searching(true, lines, () -> withD20s(() -> use(helper, player, CHEST), MADE));
            expect(helper, result.consumesAction(), "a search must consume the click");
            expectEquals(helper, 1, carried(player), "items found");
            expectLine(helper, lines, "checks.exploration.search.success.");

            expectPass(helper, searching(true, lines, () -> withD20s(() -> use(helper, player, CHEST))), "searched");
            expectEquals(helper, 1, lines.size(), "roll lines for one chest");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A failed Investigation check finds nothing else, and searches both halves of a double chest for good. */
    public static void aFailedInvestigationCheckFindsNothingElse(GameTestHelper helper) {
        RandomizableContainerBlockEntity west = lootChest(helper, CHEST, doubleChestHalf(ChestType.LEFT));
        RandomizableContainerBlockEntity east = lootChest(helper, CHEST_EAST_HALF, doubleChestHalf(ChestType.RIGHT));
        ServerPlayer player = searcher(helper, "search-failed");
        List<CustomPacketPayload> lines = new ArrayList<>();
        try {
            expectPass(helper, searching(true, lines, () -> withD20s(() -> use(helper, player, CHEST))), "unopened");
            west.unpackLootTable(player);
            east.unpackLootTable(player);

            searching(true, lines, () -> withD20s(() -> use(helper, player, CHEST), FAILED));
            expectEquals(helper, 0, carried(player), "items found");
            expectLine(helper, lines, "checks.exploration.search.failure.");
            expectPass(
                    helper,
                    searching(true, lines, () -> withD20s(() -> use(helper, player, CHEST_EAST_HALF))),
                    "the other half");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A chest no loot table filled, one used without sneaking, by a creative player or with searching off
     * rolls nothing and is left to vanilla.
     */
    public static void searchingRollsOnlyWhenItCouldMatter(GameTestHelper helper) {
        helper.setBlock(CHEST_EAST_HALF, Blocks.CHEST);
        RandomizableContainerBlockEntity chest = lootChest(helper, CHEST, Blocks.CHEST.defaultBlockState());
        ServerPlayer player = searcher(helper, "search-none");
        try {
            searching(true, new ArrayList<>(), () -> withD20s(() -> use(helper, player, CHEST)));
            chest.unpackLootTable(player);
            expectPass(
                    helper,
                    searching(true, new ArrayList<>(), () -> withD20s(() -> use(helper, player, CHEST_EAST_HALF))),
                    "on a chest no loot table filled");
            expectPass(
                    helper,
                    searching(false, new ArrayList<>(), () -> withD20s(() -> use(helper, player, CHEST))),
                    "while off");
            player.setShiftKeyDown(false);
            expectPass(
                    helper,
                    searching(true, new ArrayList<>(), () -> withD20s(() -> use(helper, player, CHEST))),
                    "standing");
            player.setShiftKeyDown(true);
            player.setGameMode(GameType.CREATIVE);
            expectPass(
                    helper,
                    searching(true, new ArrayList<>(), () -> withD20s(() -> use(helper, player, CHEST))),
                    "in creative");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * Seeing a zombie up close for the first time rolls Religion: a made check shows a hint about zombies and
     * is saved, so zombies never roll again for that player.
     */
    public static void aMadeLoreCheckRecallsAMobTypeForGood(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "lore-made", STAND);
        Mob zombie = calm(helper, EntityType.ZOMBIE, MOB_SPOT);
        List<CustomPacketPayload> lines = new ArrayList<>();
        try {
            exploring(monsterLore(true, NO_WAIT), lines, () -> withD20s(() -> lookForMobs(player), MADE));
            expectLine(helper, lines, "checks.lore.minecraft.zombie");
            expect(
                    helper,
                    LoreStore.get(player.server)
                            .knows(
                                    player.getUUID(),
                                    LoreStore.Topic.MOBS,
                                    BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ZOMBIE)),
                    "a recalled mob type must be saved");

            exploring(monsterLore(true, NO_WAIT), lines, () -> withD20s(() -> lookForMobs(player)));
            expectEquals(helper, 1, lines.size(), "roll lines for a mob type already recalled");
        } finally {
            zombie.discard();
            logout(helper, player);
        }
        helper.succeed();
    }

    /** After a failed lore check the mob type waits out the retry time before it rolls again. */
    public static void aFailedLoreCheckWaitsBeforeTheNextTry(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "lore-failed", STAND);
        Mob skeleton = calm(helper, EntityType.SKELETON, MOB_SPOT);
        List<CustomPacketPayload> lines = new ArrayList<>();
        try {
            exploring(monsterLore(true, NO_WAIT), lines, () -> withD20s(() -> lookForMobs(player), FAILED));
            expectLine(helper, lines, "checks.exploration.monster_lore.failure.");
            exploring(monsterLore(true, NO_WAIT), lines, () -> withD20s(() -> lookForMobs(player), FAILED));
            expectEquals(helper, 2, lines.size(), "rolls with no retry time");

            exploring(monsterLore(true, LONG_WAIT), lines, () -> withD20s(() -> lookForMobs(player), FAILED));
            exploring(monsterLore(true, LONG_WAIT), lines, () -> withD20s(() -> lookForMobs(player)));
            expectEquals(helper, 3, lines.size(), "rolls before the retry time is up");
        } finally {
            skeleton.discard();
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A mob out of range or behind a wall rolls nothing, nor does an armor stand, which is no mob; neither
     * does anything for a creative player or with monster lore off.
     */
    public static void loreRollsOnlyForMobsInSightAndInRange(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "lore-none", STAND);
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, MOB_SPOT);
        Mob far = calm(helper, EntityType.ZOMBIE, FAR_MOB_SPOT);
        try {
            noLore(player, monsterLore(true, NO_WAIT));
            stand.discard();
            Mob hidden = calm(helper, EntityType.ZOMBIE, MOB_SPOT);
            WALL.forEach(pos -> helper.setBlock(pos, Blocks.STONE));
            noLore(player, monsterLore(true, NO_WAIT));
            WALL.forEach(pos -> helper.setBlock(pos, Blocks.AIR));
            noLore(player, monsterLore(false, NO_WAIT));
            player.setGameMode(GameType.CREATIVE);
            noLore(player, monsterLore(true, NO_WAIT));
            hidden.discard();
        } finally {
            far.discard();
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Every vanilla mob has a hint line of its own, and the hint it would get without one stays short. */
    public static void everyVanillaMobHasAHintOfItsOwn(GameTestHelper helper) {
        List<Mob> mobs = vanillaMobs(helper);
        expect(helper, mobs.size() > MOB_TYPES_AT_LEAST, "only " + mobs.size() + " vanilla mobs found");
        for (Mob mob : mobs) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            String key = "checks.lore." + id.getNamespace() + "." + id.getPath();
            expect(
                    helper,
                    MobHints.hint(mob).getContents() instanceof TranslatableContents hint
                            && hint.getKey().equals(key)
                            && !hint.getFallback()
                                    .equals(MobHints.generated(mob).getString()),
                    id + " has no hint line " + key);
            String generated = MobHints.generated(mob).getString();
            expect(helper, generated.length() < MAX_HINT_LENGTH, id + "'s built hint is too long: " + generated);
        }
        helper.succeed();
    }

    /**
     * A lore tag decides the skill; a mob in none falls back to Religion if undead, Nature if an animal, water
     * animal, arthropod or aquatic, and Arcana otherwise.
     */
    public static void loreSkillsComeFromTagsThenTheFallback(GameTestHelper helper) {
        expectSkill(helper, EntityType.ZOMBIE, "religion");
        expectSkill(helper, EntityType.GIANT, "religion");
        expectSkill(helper, EntityType.BLAZE, "arcana");
        expectSkill(helper, EntityType.CREEPER, "nature");
        expectSkill(helper, EntityType.BAT, "nature");
        expectSkill(helper, EntityType.VILLAGER, "history");
        expectSkill(helper, EntityType.WANDERING_TRADER, "history");
        expectSkill(helper, EntityType.COW, "nature");
        expectSkill(helper, EntityType.COD, "nature");
        expectSkill(helper, EntityType.AXOLOTL, "nature");
        expectSkill(helper, EntityType.ALLAY, "arcana");
        expectSkill(helper, EntityType.SNOW_GOLEM, "arcana");
        helper.succeed();
    }

    /** A hint built from the mob: its hearts, its hit if it attacks, its weakness and fire immunity. */
    public static void aHintIsBuiltFromTheMobsData(GameTestHelper helper) {
        expectBuiltHint(helper, EntityType.ZOMBIE, "About 10 hearts, hits for 1.5. Weak to Smite.");
        expectBuiltHint(helper, EntityType.SPIDER, "About 8 hearts, hits for 1. Weak to Bane of Arthropods.");
        expectBuiltHint(helper, EntityType.BLAZE, "About 10 hearts, hits for 3. Fireproof.");
        expectBuiltHint(
                helper, EntityType.ZOMBIFIED_PIGLIN, "About 10 hearts, hits for 2.5. Weak to Smite. Fireproof.");
        expectBuiltHint(helper, EntityType.COW, "About 5 hearts.");
        expectBuiltHint(helper, EntityType.COD, "About 1.5 hearts. Weak to Impaling.");
        helper.succeed();
    }

    /**
     * Standing in an igloo for the first time rolls History: a made check shows the igloo hint and is saved,
     * so igloos never roll again for that player.
     */
    public static void aMadeHistoryCheckRecallsAStructureKindForGood(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "history-made", STAND);
        List<CustomPacketPayload> lines = new ArrayList<>();
        inIgloo(helper, player, () -> {
            exploring(structureLore(true, NO_WAIT), lines, () -> withD20s(() -> lookForStructures(player), MADE));
            expectLine(helper, lines, "checks.lore.structure.igloo");
            expect(
                    helper,
                    LoreStore.get(player.server).knows(player.getUUID(), LoreStore.Topic.STRUCTURES, IGLOO_KIND),
                    "a recalled structure kind must be saved");
            exploring(structureLore(true, NO_WAIT), lines, () -> withD20s(() -> lookForStructures(player)));
            expectEquals(helper, 1, lines.size(), "roll lines for a structure kind already recalled");
        });
        helper.succeed();
    }

    /**
     * After a failed History check the kind waits out the retry time; outside any structure, or with
     * structure lore off, nothing rolls.
     */
    public static void historyRollsOnlyInsideAStructureAndWaitsAfterAFailure(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "history-failed", STAND);
        List<CustomPacketPayload> lines = new ArrayList<>();
        try {
            exploring(structureLore(true, NO_WAIT), lines, () -> withD20s(() -> lookForStructures(player)));
        } finally {
            logout(helper, player);
        }
        ServerPlayer visitor = survivor(helper, "history-wait", STAND);
        inIgloo(helper, visitor, () -> {
            exploring(structureLore(false, NO_WAIT), lines, () -> withD20s(() -> lookForStructures(visitor)));
            expect(helper, lines.isEmpty(), "nothing may roll outside a structure or while off");
            exploring(structureLore(true, LONG_WAIT), lines, () -> withD20s(() -> lookForStructures(visitor), FAILED));
            expectLine(helper, lines, "checks.exploration.structure_lore.failure.");
            exploring(structureLore(true, LONG_WAIT), lines, () -> withD20s(() -> lookForStructures(visitor)));
            expectEquals(helper, 1, lines.size(), "rolls before the retry time is up");
        });
        helper.succeed();
    }

    /** A mob of every vanilla entity type that makes one, none of them added to the world. */
    private static List<Mob> vanillaMobs(GameTestHelper helper) {
        return BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(type -> BuiltInRegistries.ENTITY_TYPE
                        .getKey(type)
                        .getNamespace()
                        .equals("minecraft"))
                .map(type -> type.create(helper.getLevel()))
                .filter(Mob.class::isInstance)
                .map(Mob.class::cast)
                .toList();
    }

    private static Mob unspawned(GameTestHelper helper, EntityType<? extends Mob> type) {
        return type.create(helper.getLevel());
    }

    private static void expectSkill(GameTestHelper helper, EntityType<? extends Mob> type, String skill) {
        ResourceLocation actual = MonsterLore.skill(unspawned(helper, type));
        expect(
                helper,
                actual.equals(ResourceLocation.fromNamespaceAndPath("checks", skill)),
                type + " rolls " + actual);
    }

    private static void expectBuiltHint(GameTestHelper helper, EntityType<? extends Mob> type, String expected) {
        String actual = MobHints.generated(unspawned(helper, type)).getString();
        expect(
                helper,
                actual.equals(expected),
                type + "'s built hint must be '" + expected + "', was '" + actual + "'");
    }

    private static RandomizableContainerBlockEntity lootChest(GameTestHelper helper, BlockPos pos, BlockState state) {
        helper.setBlock(pos, state);
        RandomizableContainerBlockEntity chest = helper.getBlockEntity(pos);
        chest.setLootTable(DUNGEON, SEED);
        return chest;
    }

    /** One half of a double chest facing north: the left half is the west one. */
    private static BlockState doubleChestHalf(ChestType type) {
        return Blocks.CHEST
                .defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.NORTH)
                .setValue(ChestBlock.TYPE, type);
    }

    private static ServerPlayer searcher(GameTestHelper helper, String name) {
        ServerPlayer player = survivor(helper, name, STAND);
        player.setShiftKeyDown(true);
        return player;
    }

    private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos chest) {
        return ChestSearches.onUseBlock(player, InteractionHand.MAIN_HAND, helper.absolutePos(chest));
    }

    private static int carried(ServerPlayer player) {
        return player.getInventory().items.stream()
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static void expectPass(GameTestHelper helper, InteractionResult result, String what) {
        expect(helper, result == InteractionResult.PASS, "using the chest " + what + " must be left to vanilla");
    }

    private static <T> T searching(boolean enabled, List<CustomPacketPayload> lines, Supplier<T> body) {
        return exploring(withSearch(new ScoresConfig.SearchSettings(enabled, SEARCH_DC)), lines, body);
    }

    private static Void lookForMobs(ServerPlayer player) {
        MonsterLore.lookAround(player);
        return null;
    }

    private static Void lookForStructures(ServerPlayer player) {
        StructureLore.lookAround(player);
        return null;
    }

    private static void noLore(ServerPlayer player, ScoresConfig.ExplorationSettings settings) {
        exploring(settings, new ArrayList<>(), () -> withD20s(() -> lookForMobs(player)));
    }

    /**
     * Runs {@code body} with an igloo's top piece around the player, its start and reference in the player's
     * chunk, as world generation leaves them; then puts the chunk back and logs the player out.
     */
    private static void inIgloo(GameTestHelper helper, ServerPlayer player, Runnable body) {
        Structure igloo = helper.getLevel()
                .registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .getOrThrow(BuiltinStructures.IGLOO);
        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        ChunkAccess chunk = helper.getLevel().getChunk(chunkPos.x, chunkPos.z);
        Map<Structure, StructureStart> starts = new HashMap<>(chunk.getAllStarts());
        Map<Structure, LongSet> references = new HashMap<>(chunk.getAllReferences());
        IglooPieces.IglooPiece top = new IglooPieces.IglooPiece(
                helper.getLevel().getStructureManager(), IGLOO_TOP, helper.absolutePos(IGLOO_CORNER), Rotation.NONE, 0);
        chunk.setStartForStructure(igloo, new StructureStart(igloo, chunkPos, 0, new PiecesContainer(List.of(top))));
        chunk.addReferenceForStructure(igloo, chunkPos.toLong());
        try {
            body.run();
        } finally {
            chunk.setAllStarts(starts);
            chunk.setAllReferences(references);
            logout(helper, player);
        }
    }

    private static ScoresConfig.ExplorationSettings monsterLore(boolean enabled, int retryTicks) {
        return new ScoresConfig.ExplorationSettings(
                DEFAULTS.leap(),
                DEFAULTS.landing(),
                DEFAULTS.cobwebs(),
                DEFAULTS.climbing(),
                DEFAULTS.sneak(),
                DEFAULTS.spotTripwires(),
                DEFAULTS.disarmTripwires(),
                DEFAULTS.searchChests(),
                new ScoresConfig.MonsterLoreSettings(enabled, DC, RANGE, retryTicks, EVERY_TICK),
                DEFAULTS.structureLore(),
                DEFAULTS.taming(),
                DEFAULTS.hunger(),
                DEFAULTS.ailments());
    }

    private static ScoresConfig.ExplorationSettings structureLore(boolean enabled, int retryTicks) {
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
                new ScoresConfig.StructureLoreSettings(enabled, DC, retryTicks, EVERY_TICK),
                DEFAULTS.taming(),
                DEFAULTS.hunger(),
                DEFAULTS.ailments());
    }

    private static ScoresConfig.ExplorationSettings withSearch(ScoresConfig.SearchSettings search) {
        return new ScoresConfig.ExplorationSettings(
                DEFAULTS.leap(),
                DEFAULTS.landing(),
                DEFAULTS.cobwebs(),
                DEFAULTS.climbing(),
                DEFAULTS.sneak(),
                DEFAULTS.spotTripwires(),
                DEFAULTS.disarmTripwires(),
                search,
                DEFAULTS.monsterLore(),
                DEFAULTS.structureLore(),
                DEFAULTS.taming(),
                DEFAULTS.hunger(),
                DEFAULTS.ailments());
    }
}
