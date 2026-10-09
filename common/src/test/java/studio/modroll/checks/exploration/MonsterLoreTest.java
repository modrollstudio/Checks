package studio.modroll.checks.exploration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class MonsterLoreTest {

    @Test
    void anUndeadMobInNoLoreTagCallsForReligionEvenIfItIsAlsoWild() {
        assertEquals(skill("religion"), MonsterLore.fallbackSkill(true, false));
        assertEquals(skill("religion"), MonsterLore.fallbackSkill(true, true));
    }

    @Test
    void aCreatureOfTheWildInNoLoreTagCallsForNature() {
        assertEquals(skill("nature"), MonsterLore.fallbackSkill(false, true));
    }

    @Test
    void anythingElseInNoLoreTagCallsForArcana() {
        assertEquals(skill("arcana"), MonsterLore.fallbackSkill(false, false));
    }

    private static ResourceLocation skill(String path) {
        return ResourceLocation.fromNamespaceAndPath("checks", path);
    }
}
