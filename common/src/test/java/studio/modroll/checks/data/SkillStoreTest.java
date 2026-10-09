package studio.modroll.checks.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;

class SkillStoreTest {

    private static final Skill STEALTH = new Skill(ResourceLocation.parse("checks:stealth"), Ability.DEXTERITY);
    private static final Skill SAILING = new Skill(ResourceLocation.parse("pack:sailing"), Ability.WISDOM);

    @AfterEach
    void reset() {
        SkillStore.clear();
    }

    @Test
    void findsBySkillId() {
        SkillStore.setSkills(Map.of(STEALTH.id(), STEALTH, SAILING.id(), SAILING));
        assertEquals(Optional.of(SAILING), SkillStore.find(SAILING.id()));
        assertEquals(Optional.of(STEALTH), SkillStore.find(STEALTH.id()));
    }

    @Test
    void namespacelessIdFallsBackToChecksNamespace() {
        SkillStore.setSkills(Map.of(STEALTH.id(), STEALTH, SAILING.id(), SAILING));
        assertEquals(Optional.of(STEALTH), SkillStore.find(ResourceLocation.parse("stealth")));
        assertEquals(Optional.empty(), SkillStore.find(ResourceLocation.parse("sailing")));
    }

    @Test
    void exactMinecraftSkillWinsOverFallback() {
        Skill minecraftStealth = new Skill(ResourceLocation.parse("minecraft:stealth"), Ability.WISDOM);
        SkillStore.setSkills(Map.of(STEALTH.id(), STEALTH, minecraftStealth.id(), minecraftStealth));
        assertEquals(Optional.of(minecraftStealth), SkillStore.find(ResourceLocation.parse("stealth")));
    }
}
