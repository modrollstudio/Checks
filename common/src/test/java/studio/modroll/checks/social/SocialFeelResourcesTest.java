package studio.modroll.checks.social;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.death.DeathStory;
import studio.modroll.checks.text.FallbackText;

class SocialFeelResourcesTest {

    private static final Path SOUNDS = Path.of("src/main/resources/assets/checks/sounds.json");

    @Test
    void everyReachablePoolHasFourToSevenLines() {
        for (String pool : reachablePools()) {
            int size = FallbackText.poolSize(pool);
            assertTrue(size >= 4 && size <= 7, pool + " has " + size + " lines");
        }
    }

    @Test
    void everyDeathStoryHasThreeToSixLines() {
        for (DeathStory story : DeathStory.values()) {
            int size = FallbackText.poolSize("checks.death." + story.id());
            assertTrue(size >= 3 && size <= 6, story.id() + " has " + size + " lines");
        }
        FallbackText.of("checks.death.killer.unknown");
    }

    @Test
    void everyVoiceIsInSoundsJsonWithASubtitle() throws IOException {
        JsonObject sounds = sounds();
        for (SocialTarget target : SocialTarget.values()) {
            if (target.mob()) {
                continue;
            }
            for (SocialMood mood : SocialMood.values()) {
                expectSoundWithSubtitle(sounds, SocialFeel.voiceId(target, mood).getPath());
            }
        }
    }

    @Test
    void insightsWarningIsInSoundsJsonWithASubtitle() throws IOException {
        expectSoundWithSubtitle(sounds(), Insight.WARNING.getPath());
    }

    @Test
    void mobsNeverSpeak() {
        for (SocialAction action : SocialAction.values()) {
            for (SocialOutcome outcome : SocialOutcome.values()) {
                assertTrue(SpeechPools.of(SocialTarget.HOSTILE, action, outcome).isEmpty());
                assertTrue(SpeechPools.of(SocialTarget.NEUTRAL, action, outcome).isEmpty());
            }
        }
    }

    private static void expectSoundWithSubtitle(JsonObject sounds, String event) {
        assertTrue(sounds.has(event), "sounds.json has no " + event);
        FallbackText.of(sounds.getAsJsonObject(event).get("subtitle").getAsString());
    }

    private static JsonObject sounds() throws IOException {
        try (Reader reader = Files.newBufferedReader(SOUNDS)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Set<String> reachablePools() {
        Set<String> pools = new HashSet<>();
        for (SocialTarget target : SocialTarget.values()) {
            for (SocialAction action : SocialAction.values()) {
                if (!action.worksOn(target)) {
                    continue;
                }
                for (SocialOutcome outcome : SocialOutcome.values()) {
                    SpeechPools.of(target, action, outcome).ifPresent(pools::add);
                }
            }
        }
        return pools;
    }
}
