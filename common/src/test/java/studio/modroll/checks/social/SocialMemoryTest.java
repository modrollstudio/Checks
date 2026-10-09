package studio.modroll.checks.social;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class SocialMemoryTest {

    private static final UUID PLAYER = UUID.randomUUID();
    private static final UUID OTHER_PLAYER = UUID.randomUUID();
    private static final UUID VILLAGER = UUID.randomUUID();
    private static final UUID OTHER_VILLAGER = UUID.randomUUID();

    private final SocialMemory memory = new SocialMemory();

    @Test
    void aCooldownIsPerActionPlayerAndTargetAndEnds() {
        memory.startCooldown(SocialAction.PERSUADE, PLAYER, VILLAGER, 100, 50);

        assertEquals(OptionalLong.of(30), memory.cooldownLeft(SocialAction.PERSUADE, PLAYER, VILLAGER, 120));
        assertEquals(OptionalLong.empty(), memory.cooldownLeft(SocialAction.PERSUADE, PLAYER, VILLAGER, 150));
        assertEquals(OptionalLong.empty(), memory.cooldownLeft(SocialAction.DECEIVE, PLAYER, VILLAGER, 120));
        assertEquals(OptionalLong.empty(), memory.cooldownLeft(SocialAction.PERSUADE, OTHER_PLAYER, VILLAGER, 120));
        assertEquals(OptionalLong.empty(), memory.cooldownLeft(SocialAction.PERSUADE, PLAYER, OTHER_VILLAGER, 120));
    }

    @Test
    void aZeroCooldownStartsNothing() {
        memory.startCooldown(SocialAction.PERSUADE, PLAYER, VILLAGER, 100, 0);
        assertEquals(OptionalLong.empty(), memory.cooldownLeft(SocialAction.PERSUADE, PLAYER, VILLAGER, 100));
    }

    @Test
    void priceChangesAddUpPerActionAndExpire() {
        memory.changePrices(SocialAction.PERSUADE, PLAYER, VILLAGER, -20, 100, 50);
        memory.changePrices(SocialAction.INTIMIDATE, PLAYER, VILLAGER, 15, 100, 200);

        assertEquals(
                Map.of(SocialAction.PERSUADE, -20.0, SocialAction.INTIMIDATE, 15.0),
                memory.priceChanges(PLAYER, VILLAGER, 149));
        assertEquals(Map.of(SocialAction.INTIMIDATE, 15.0), memory.priceChanges(PLAYER, VILLAGER, 150));
        assertEquals(Map.of(), memory.priceChanges(OTHER_PLAYER, VILLAGER, 120));
        assertEquals(Map.of(), memory.priceChanges(PLAYER, VILLAGER, 300));
    }

    @Test
    void aNewPriceChangeFromTheSameActionReplacesTheOldOne() {
        memory.changePrices(SocialAction.PERSUADE, PLAYER, VILLAGER, -20, 100, 50);
        memory.changePrices(SocialAction.PERSUADE, PLAYER, VILLAGER, 10, 120, 50);

        assertEquals(Map.of(SocialAction.PERSUADE, 10.0), memory.priceChanges(PLAYER, VILLAGER, 160));
    }

    @Test
    void aRefusalLastsItsTicksAndOnlyCountsForTheGivenActions() {
        memory.refuse(SocialAction.PICKPOCKET, PLAYER, VILLAGER, 100, 50);

        assertTrue(memory.refuses(PLAYER, VILLAGER, 149, action -> true));
        assertFalse(memory.refuses(PLAYER, VILLAGER, 150, action -> true));
        assertFalse(memory.refuses(PLAYER, VILLAGER, 120, action -> action != SocialAction.PICKPOCKET));
        assertFalse(memory.refuses(OTHER_PLAYER, VILLAGER, 120, action -> true));
    }

    @Test
    void witnessRefusalsAreKeptApart() {
        memory.refuseAsWitness(SocialAction.PICKPOCKET, PLAYER, VILLAGER, 100, 50);

        assertTrue(memory.refusesAsWitness(PLAYER, VILLAGER, 149, action -> true));
        assertFalse(memory.refusesAsWitness(PLAYER, VILLAGER, 150, action -> true));
        assertFalse(memory.refuses(PLAYER, VILLAGER, 120, action -> true));
    }

    @Test
    void halvingCutsTheTimeLeftOnTheGivenActionsRefusalsOnly() {
        memory.refuse(SocialAction.PICKPOCKET, PLAYER, VILLAGER, 100, 100);
        memory.refuseAsWitness(SocialAction.INTIMIDATE, PLAYER, OTHER_VILLAGER, 100, 100);
        memory.refuse(SocialAction.DECEIVE, PLAYER, OTHER_VILLAGER, 100, 100);
        memory.refuse(SocialAction.PICKPOCKET, OTHER_PLAYER, VILLAGER, 100, 100);

        memory.halveRefusals(PLAYER, 120, action -> action != SocialAction.DECEIVE);

        assertTrue(memory.refuses(PLAYER, VILLAGER, 159, action -> true));
        assertFalse(memory.refuses(PLAYER, VILLAGER, 160, action -> true));
        assertFalse(memory.refusesAsWitness(PLAYER, OTHER_VILLAGER, 160, action -> true));
        assertTrue(memory.refuses(PLAYER, OTHER_VILLAGER, 199, action -> true));
        assertTrue(memory.refuses(OTHER_PLAYER, VILLAGER, 199, action -> true));
    }

    @Test
    void aDisguiseLastsItsTicks() {
        memory.disguise(PLAYER, 100, 50);

        assertTrue(memory.disguised(PLAYER, 149));
        assertFalse(memory.disguised(PLAYER, 150));
        assertFalse(memory.disguised(OTHER_PLAYER, 120));
    }

    @Test
    void everythingSurvivesASaveAndLoad() {
        memory.startCooldown(SocialAction.INTIMIDATE, PLAYER, VILLAGER, 100, 72000);
        memory.changePrices(SocialAction.DECEIVE, PLAYER, VILLAGER, -35, 100, 24000);
        memory.refuse(SocialAction.PICKPOCKET, PLAYER, VILLAGER, 100, 24000);
        memory.refuseAsWitness(SocialAction.INTIMIDATE, PLAYER, OTHER_VILLAGER, 100, 24000);
        memory.disguise(PLAYER, 100, 2400);

        SocialMemory loaded = SocialMemory.load(memory.save(new CompoundTag(), null), null);

        assertEquals(OptionalLong.of(71900), loaded.cooldownLeft(SocialAction.INTIMIDATE, PLAYER, VILLAGER, 200));
        assertEquals(Map.of(SocialAction.DECEIVE, -35.0), loaded.priceChanges(PLAYER, VILLAGER, 200));
        assertTrue(loaded.refuses(PLAYER, VILLAGER, 200, action -> true));
        assertTrue(loaded.refusesAsWitness(PLAYER, OTHER_VILLAGER, 200, action -> true));
        assertTrue(loaded.disguised(PLAYER, 200));
    }

    @Test
    void anUnknownActionIsSkippedOnLoad() {
        memory.startCooldown(SocialAction.PERSUADE, PLAYER, VILLAGER, 100, 50);
        CompoundTag saved = memory.save(new CompoundTag(), null);
        saved.getList("cooldowns", CompoundTag.TAG_COMPOUND).getCompound(0).putString("action", "juggle");

        assertEquals(
                OptionalLong.empty(),
                SocialMemory.load(saved, null).cooldownLeft(SocialAction.PERSUADE, PLAYER, VILLAGER, 120));
    }
}
