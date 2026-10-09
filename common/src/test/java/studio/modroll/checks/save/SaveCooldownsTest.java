package studio.modroll.checks.save;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SaveCooldownsTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    private final SaveCooldowns cooldowns = new SaveCooldowns();

    @Test
    void reusesTheLastResultUntilTheCooldownEnds() {
        cooldowns.start(VanillaSave.FIRE, ALICE, 100, 20, true);

        assertEquals(Optional.of(true), cooldowns.recent(VanillaSave.FIRE, ALICE, 100));
        assertEquals(Optional.of(true), cooldowns.recent(VanillaSave.FIRE, ALICE, 119));
        assertEquals(Optional.empty(), cooldowns.recent(VanillaSave.FIRE, ALICE, 120));
    }

    @Test
    void keepsAFailureToo() {
        cooldowns.start(VanillaSave.POISON, ALICE, 0, 20, false);

        assertEquals(Optional.of(false), cooldowns.recent(VanillaSave.POISON, ALICE, 10));
    }

    @Test
    void eachSaveAndEntityCoolsDownSeparately() {
        cooldowns.start(VanillaSave.FIRE, ALICE, 0, 20, true);

        assertEquals(Optional.empty(), cooldowns.recent(VanillaSave.EXPLOSION, ALICE, 10));
        assertEquals(Optional.empty(), cooldowns.recent(VanillaSave.FIRE, BOB, 10));
    }

    @Test
    void noCooldownRollsEveryTime() {
        cooldowns.start(VanillaSave.FIRE, ALICE, 0, 0, true);

        assertEquals(Optional.empty(), cooldowns.recent(VanillaSave.FIRE, ALICE, 0));
    }

    @Test
    void clearForgetsEveryResult() {
        cooldowns.start(VanillaSave.FIRE, ALICE, 0, 20, true);
        cooldowns.clear();

        assertEquals(Optional.empty(), cooldowns.recent(VanillaSave.FIRE, ALICE, 10));
    }
}
