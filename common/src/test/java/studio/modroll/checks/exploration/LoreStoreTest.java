package studio.modroll.checks.exploration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class LoreStoreTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final ResourceLocation ZOMBIE = ResourceLocation.withDefaultNamespace("zombie");
    private static final ResourceLocation VILLAGES = ResourceLocation.fromNamespaceAndPath("checks", "history/village");

    @Test
    void loreRoundTripsThroughNbtPerTopic() {
        LoreStore store = new LoreStore();
        store.learn(PLAYER, LoreStore.Topic.MOBS, ZOMBIE);
        store.learn(PLAYER, LoreStore.Topic.STRUCTURES, VILLAGES);

        LoreStore loaded = LoreStore.load(store.save(new CompoundTag(), null), null);

        assertTrue(loaded.knows(PLAYER, LoreStore.Topic.MOBS, ZOMBIE));
        assertTrue(loaded.knows(PLAYER, LoreStore.Topic.STRUCTURES, VILLAGES));
        assertFalse(loaded.knows(PLAYER, LoreStore.Topic.STRUCTURES, ZOMBIE));
    }

    @Test
    void anEmptySaveKnowsNothing() {
        assertFalse(LoreStore.load(new CompoundTag(), null).knows(PLAYER, LoreStore.Topic.MOBS, ZOMBIE));
    }
}
