package studio.modroll.checks.exploration;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.Checks;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * Investigation: a loot chest, barrel or other container a loot table fills remembers that table when a
 * player first uses it, in the container's own {@code minecraft:custom_data}. Once it has been opened,
 * a player in survival or adventure who sneaks and uses it rolls Investigation through the API, once per
 * container for everyone: a made check finds one more item from its loot table, a failed one finds nothing
 * else. Both halves of a double chest count as one. A container no loot table ever filled rolls nothing.
 */
public final class ChestSearches {

    private static final ResourceLocation INVESTIGATION = Checks.id("investigation");
    private static final String SEARCH_KEY = Checks.MOD_ID + ":search";
    private static final String LOOT_TABLE_KEY = "loot_table";
    private static final String SEARCHED_KEY = "searched";
    private static final int ONE_ITEM = 1;

    private ChestSearches() {}

    public static InteractionResult onUseBlock(ServerPlayer player, InteractionHand hand, BlockPos pos) {
        ScoresConfig.SearchSettings settings =
                ScoresRuntime.config().exploration().searchChests();
        Level level = player.level();
        List<RandomizableContainerBlockEntity> container = halves(level, pos);
        if (!settings.enabled() || hand != InteractionHand.MAIN_HAND || container.isEmpty()) {
            return InteractionResult.PASS;
        }
        container.forEach(ChestSearches::rememberLootTable);
        Optional<ResourceKey<LootTable>> lootTable = unsearchedLootTable(container.getFirst());
        if (lootTable.isEmpty()
                || !player.isShiftKeyDown()
                || !ExplorationChecks.rollsFor(player)
                || !level.mayInteract(player, pos)
                || !container.getFirst().canOpen(player)) {
            return InteractionResult.PASS;
        }
        Optional<Boolean> made = ExplorationChecks.made(player, INVESTIGATION, settings.dc(), "search");
        if (made.isEmpty()) {
            return InteractionResult.PASS;
        }
        container.forEach(half -> updateSearch(half, search -> search.putBoolean(SEARCHED_KEY, true)));
        if (made.get()) {
            findOneMore(player, lootTable.get(), pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** The used container first, then the other half of a double chest. */
    private static List<RandomizableContainerBlockEntity> halves(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Stream<BlockPos> positions =
                state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                        ? Stream.of(pos, pos.relative(ChestBlock.getConnectedDirection(state)))
                        : Stream.of(pos);
        return positions
                .map(level::getBlockEntity)
                .filter(RandomizableContainerBlockEntity.class::isInstance)
                .map(RandomizableContainerBlockEntity.class::cast)
                .toList();
    }

    /** A container not yet opened still has its loot table; a new one, as from a command, starts a new search. */
    private static void rememberLootTable(RandomizableContainerBlockEntity container) {
        ResourceKey<LootTable> lootTable = container.getLootTable();
        if (lootTable != null) {
            updateSearch(container, search -> {
                search.putString(LOOT_TABLE_KEY, lootTable.location().toString());
                search.remove(SEARCHED_KEY);
            });
        }
    }

    /** The remembered loot table of a container that has been opened and not yet searched. */
    private static Optional<ResourceKey<LootTable>> unsearchedLootTable(RandomizableContainerBlockEntity container) {
        CompoundTag search = search(container);
        if (container.getLootTable() != null || search.getBoolean(SEARCHED_KEY) || !search.contains(LOOT_TABLE_KEY)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(search.getString(LOOT_TABLE_KEY)))
                .map(id -> ResourceKey.create(Registries.LOOT_TABLE, id));
    }

    private static CompoundTag search(RandomizableContainerBlockEntity container) {
        return container
                .components()
                .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getCompound(SEARCH_KEY);
    }

    private static void updateSearch(RandomizableContainerBlockEntity container, Consumer<CompoundTag> change) {
        CompoundTag search = search(container);
        change.accept(search);
        CustomData data = container
                .components()
                .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .update(tag -> tag.put(SEARCH_KEY, search));
        container.setComponents(DataComponentMap.builder()
                .addAll(container.components())
                .set(DataComponents.CUSTOM_DATA, data)
                .build());
        container.setChanged();
    }

    /**
     * The loot table rolled again as for a chest, and one item of one of its stacks handed to the player.
     * Which stack is loot randomness, as vanilla's own filling of the chest, never a check.
     */
    private static void findOneMore(ServerPlayer player, ResourceKey<LootTable> key, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.CHEST);
        List<ItemStack> loot =
                level.getServer().reloadableRegistries().getLootTable(key).getRandomItems(params);
        if (!loot.isEmpty()) {
            ItemStack found = loot.get(level.getRandom().nextInt(loot.size())).copyWithCount(ONE_ITEM);
            player.getInventory().placeItemBackInInventory(found);
        }
    }
}
