package studio.modroll.checks.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;

class LevelStoreTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void levelsRoundTripThroughNbt() {
        PlayerLevel level = new PlayerLevel(
                8,
                3500,
                List.of(Map.of(Ability.STRENGTH, 2), Map.of(Ability.DEXTERITY, 1, Ability.WISDOM, 1)),
                Map.of(2, 10, 3, 10, 4, 8));
        LevelStore store = new LevelStore();
        store.set(PLAYER, level);

        LevelStore loaded = LevelStore.load(store.save(new CompoundTag(), null), null);

        assertEquals(level, loaded.level(PLAYER));
    }

    @Test
    void anEmptySaveStartsAtLevelOne() {
        assertEquals(PlayerLevel.START, LevelStore.load(new CompoundTag(), null).level(PLAYER));
    }

    @Test
    void anEntryMissingKeysLoadsAsLevelOne() {
        CompoundTag players = new CompoundTag();
        players.put(PLAYER.toString(), new CompoundTag());
        CompoundTag tag = new CompoundTag();
        tag.put("players", players);

        assertEquals(PlayerLevel.START, LevelStore.load(tag, null).level(PLAYER));
    }

    @Test
    void anOutOfRangeLevelIsClamped() {
        CompoundTag entry = new CompoundTag();
        entry.putInt("level", 99);
        CompoundTag players = new CompoundTag();
        players.put(PLAYER.toString(), entry);
        CompoundTag tag = new CompoundTag();
        tag.put("players", players);

        assertEquals(20, LevelStore.load(tag, null).level(PLAYER).level());
    }

    @Test
    void resetForgetsThePlayer() {
        LevelStore store = new LevelStore();
        store.set(PLAYER, PlayerLevel.START.withLevelAndXp(5, 650));

        store.reset(PLAYER);

        assertEquals(PlayerLevel.START, store.level(PLAYER));
    }
}
