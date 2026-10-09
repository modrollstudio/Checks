package studio.modroll.checks.death;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class StoryLogTest {

    private static final UUID PLAYER = UUID.randomUUID();
    private static final int WINDOW = 100;
    private static final Component BREAD = Component.literal("Bread");

    private final StoryLog log = new StoryLog();

    @Test
    void aStoryIsToldInsideTheWindow() {
        log.record(PLAYER, DeathStory.CAUGHT_PICKPOCKETING, BREAD, 1000);
        assertEquals(
                Optional.of(new StoryLog.Told(DeathStory.CAUGHT_PICKPOCKETING, BREAD)),
                log.take(PLAYER, 1000 + WINDOW, WINDOW, story -> true));
    }

    @Test
    void aStoryEndsWithTheWindow() {
        log.record(PLAYER, DeathStory.CAUGHT_PICKPOCKETING, BREAD, 1000);
        assertEquals(Optional.empty(), log.take(PLAYER, 1000 + WINDOW + 1, WINDOW, story -> true));
    }

    @Test
    void onlyAStoryThatFitsTheDeathIsTold() {
        log.record(PLAYER, DeathStory.FAILED_INTIMIDATE, Component.empty(), 1000);
        assertEquals(Optional.empty(), log.take(PLAYER, 1000, WINDOW, story -> false));
    }

    @Test
    void theFirstFittingStoryInOrderWins() {
        log.record(PLAYER, DeathStory.LAST_STAND_SPENT, Component.empty(), 1000);
        log.record(PLAYER, DeathStory.FAILED_INTIMIDATE, Component.empty(), 1010);
        log.record(PLAYER, DeathStory.INTIMIDATE_NATURAL_ONE, Component.empty(), 1020);
        Set<DeathStory> golemStories = Set.of(DeathStory.FAILED_INTIMIDATE, DeathStory.LAST_STAND_SPENT);
        assertEquals(
                Optional.of(DeathStory.FAILED_INTIMIDATE),
                log.take(PLAYER, 1030, WINDOW, golemStories::contains).map(StoryLog.Told::story));
    }

    @Test
    void aDeathForgetsThePlayersStories() {
        log.record(PLAYER, DeathStory.LAST_STAND_SPENT, Component.empty(), 1000);
        log.take(PLAYER, 1000, WINDOW, story -> false);
        assertEquals(Optional.empty(), log.take(PLAYER, 1000, WINDOW, story -> true));
    }

    @Test
    void storiesAreKeptPerPlayer() {
        log.record(PLAYER, DeathStory.LAST_STAND_SPENT, Component.empty(), 1000);
        assertEquals(Optional.empty(), log.take(UUID.randomUUID(), 1000, WINDOW, story -> true));
    }
}
