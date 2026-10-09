package studio.modroll.checks.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EntityScoreProfileStoreTest {

    private static final ResourceLocation ZOMBIE = ResourceLocation.parse("minecraft:zombie");
    private static final Set<ResourceLocation> ZOMBIE_TAGS = Set.of(ResourceLocation.parse("minecraft:undead"));

    @AfterEach
    void reset() {
        EntityScoreProfileStore.clear();
    }

    private static EntityScoreProfile profile(String id, int priority, String... matches) {
        StringBuilder list = new StringBuilder();
        for (String match : matches) {
            list.append(list.isEmpty() ? "" : ", ").append('"').append(match).append('"');
        }
        return EntityScoreProfile.parse(
                ResourceLocation.parse(id),
                JsonParser.parseString("{\"matches\": [" + list + "], \"priority\": " + priority + "}")
                        .getAsJsonObject(),
                skill -> Optional.empty(),
                w -> {});
    }

    private static String resolvedId(EntityScoreProfile... profiles) {
        return EntityScoreProfileStore.resolve(List.of(profiles), ZOMBIE, ZOMBIE_TAGS::contains)
                .map(profile -> profile.id().toString())
                .orElse("none");
    }

    private static String findZombieProfileId() {
        return EntityScoreProfileStore.findEntityProfile(ZOMBIE, ZOMBIE_TAGS::contains)
                .orElseThrow()
                .id()
                .toString();
    }

    @Test
    void higherPriorityWins() {
        assertEquals(
                "pack:override",
                resolvedId(
                        profile("pack:base", 0, "minecraft:zombie"),
                        profile("pack:override", 10, "#minecraft:undead")));
    }

    @Test
    void equalPriorityFallsBackToSpecificity() {
        assertEquals(
                "pack:by_id",
                resolvedId(
                        profile("pack:by_tag", 0, "#minecraft:undead"),
                        profile("pack:by_id", 0, "minecraft:zombie"),
                        profile("pack:by_ns", 0, "minecraft:*")));
    }

    @Test
    void fullTieBreaksOnSmallerFileId() {
        assertEquals(
                "apack:zombie",
                resolvedId(
                        profile("bpack:zombie", 0, "minecraft:zombie"),
                        profile("apack:zombie", 0, "minecraft:zombie")));
    }

    @Test
    void noMatchReturnsEmpty() {
        assertEquals("none", resolvedId(profile("pack:pig", 0, "minecraft:pig")));
    }

    @Test
    void storeSwapInvalidatesCache() {
        EntityScoreProfile first = profile("pack:first", 0, "minecraft:zombie");
        EntityScoreProfileStore.setProfiles(Map.of(first.id(), first));
        assertEquals("pack:first", findZombieProfileId());
        EntityScoreProfile second = profile("pack:second", 0, "minecraft:zombie");
        EntityScoreProfileStore.setProfiles(Map.of(second.id(), second));
        assertEquals("pack:second", findZombieProfileId());
    }
}
