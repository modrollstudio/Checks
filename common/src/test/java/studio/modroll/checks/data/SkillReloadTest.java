package studio.modroll.checks.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.InactiveProfiler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;

class SkillReloadTest {

    private static final ResourceLocation SAILING = ResourceLocation.parse("pack:sailing");
    private static final ResourceLocation ZOMBIE = ResourceLocation.parse("minecraft:zombie");

    private final DatapackListener<Skill> skills = DatapackListener.skills();
    private final DatapackListener<EntityScoreProfile> profiles = DatapackListener.entityProfiles();

    @AfterEach
    void reset() {
        SkillStore.clear();
        EntityScoreProfileStore.clear();
    }

    private static JsonElement json(String text) {
        return JsonParser.parseString(text);
    }

    private static JsonElement skill(String ability) {
        return json("{\"format_version\": 1, \"ability\": \"" + ability + "\"}");
    }

    private void reloadSkills(Map<ResourceLocation, JsonElement> files) {
        skills.apply(files, null, InactiveProfiler.INSTANCE);
    }

    private void reloadProfiles(Map<ResourceLocation, JsonElement> files) {
        profiles.apply(files, null, InactiveProfiler.INSTANCE);
    }

    @Test
    void reloadAddsAndChangesSkills() {
        reloadSkills(Map.of(SAILING, skill("wis")));
        assertEquals(Ability.WISDOM, SkillStore.find(SAILING).orElseThrow().ability());

        reloadSkills(Map.of(SAILING, skill("dex")));
        assertEquals(Ability.DEXTERITY, SkillStore.find(SAILING).orElseThrow().ability());
    }

    @Test
    void reloadDropsARemovedSkill() {
        reloadSkills(Map.of(SAILING, skill("wis")));
        reloadSkills(Map.of());
        assertTrue(SkillStore.find(SAILING).isEmpty());
    }

    @Test
    void malformedSkillFilesAreSkippedWhileTheRestLoad() {
        reloadSkills(Map.of(
                SAILING,
                skill("wis"),
                ResourceLocation.parse("pack:no_ability"),
                json("{\"format_version\": 1}"),
                ResourceLocation.parse("pack:bad_ability"),
                skill("luck"),
                ResourceLocation.parse("pack:not_an_object"),
                json("[1]")));
        assertEquals(Set.of(SAILING), SkillStore.skills().keySet());
    }

    @Test
    void profilesReloadedAfterSkillsPickUpNewSkillBonuses() {
        Map<ResourceLocation, JsonElement> profileFiles = Map.of(
                ResourceLocation.parse("pack:zombie"),
                json("{\"matches\": [\"minecraft:zombie\"], \"skills\": {\"pack:sailing\": 4}}"));

        reloadSkills(Map.of());
        reloadProfiles(profileFiles);
        assertEquals(OptionalInt.empty(), zombieBonus(SAILING));

        reloadSkills(Map.of(SAILING, skill("wis")));
        reloadProfiles(profileFiles);
        assertEquals(OptionalInt.of(4), zombieBonus(SAILING));
    }

    private static OptionalInt zombieBonus(ResourceLocation skillId) {
        Skill skill = new Skill(skillId, Ability.WISDOM);
        return EntityScoreProfileStore.findEntityProfile(ZOMBIE, tag -> false)
                .map(profile -> profile.skillBonus(skill))
                .orElse(OptionalInt.empty());
    }
}
