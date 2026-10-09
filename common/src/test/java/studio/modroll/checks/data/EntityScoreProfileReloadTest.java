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

class EntityScoreProfileReloadTest {

    private static final ResourceLocation ZOMBIE = ResourceLocation.parse("minecraft:zombie");
    private static final ResourceLocation PROFILE_ID = ResourceLocation.parse("pack:zombie");

    private final DatapackListener<EntityScoreProfile> listener = DatapackListener.entityProfiles();

    @AfterEach
    void reset() {
        EntityScoreProfileStore.clear();
    }

    private static JsonElement json(String text) {
        return JsonParser.parseString(text);
    }

    private static JsonElement zombieProfile(int strength) {
        return json("{\"format_version\": 1, \"matches\": [\"minecraft:zombie\"], \"abilities\": {\"str\": " + strength
                + "}}");
    }

    private void reload(Map<ResourceLocation, JsonElement> files) {
        listener.apply(files, null, InactiveProfiler.INSTANCE);
    }

    private static OptionalInt zombieStrength() {
        return EntityScoreProfileStore.findEntityProfile(ZOMBIE, tag -> false)
                .map(profile -> profile.score(Ability.STRENGTH))
                .orElse(OptionalInt.empty());
    }

    @Test
    void reloadPicksUpAChangedProfile() {
        reload(Map.of(PROFILE_ID, zombieProfile(14)));
        assertEquals(OptionalInt.of(14), zombieStrength());

        reload(Map.of(PROFILE_ID, zombieProfile(18)));
        assertEquals(OptionalInt.of(18), zombieStrength());
    }

    @Test
    void reloadDropsARemovedProfile() {
        reload(Map.of(PROFILE_ID, zombieProfile(14)));
        reload(Map.of());
        assertTrue(zombieStrength().isEmpty());
    }

    @Test
    void malformedFilesAreSkippedWhileTheRestLoad() {
        reload(Map.of(
                PROFILE_ID,
                zombieProfile(14),
                ResourceLocation.parse("pack:out_of_range"),
                json("{\"matches\": [\"minecraft:skeleton\"], \"abilities\": {\"str\": 99}}"),
                ResourceLocation.parse("pack:no_matches"),
                json("{\"abilities\": {\"str\": 12}}"),
                ResourceLocation.parse("pack:wrong_type"),
                json("{\"matches\": [\"minecraft:husk\"], \"abilities\": {\"str\": \"high\"}}"),
                ResourceLocation.parse("pack:not_an_object"),
                json("[1, 2, 3]")));

        assertEquals(Set.of(PROFILE_ID), EntityScoreProfileStore.profiles().keySet());
        assertEquals(OptionalInt.of(14), zombieStrength());
    }
}
