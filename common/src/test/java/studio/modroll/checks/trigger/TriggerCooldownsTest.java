package studio.modroll.checks.trigger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.data.MatchEntry;
import studio.modroll.critfall.api.dice.RollMode;

class TriggerCooldownsTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final Optional<String> WALL = Optional.of("minecraft:overworld@1, 2, 3");
    private static final Optional<String> OTHER_WALL = Optional.of("minecraft:overworld@4, 5, 6");

    private final TriggerCooldowns cooldowns = new TriggerCooldowns();

    private static CheckTrigger trigger(String id, int playerTicks, int targetTicks) {
        return new CheckTrigger(
                ResourceLocation.parse(id),
                CheckTrigger.Interaction.USE_BLOCK,
                new MatchEntry.Exact(ResourceLocation.parse("minecraft:cracked_stone_bricks")),
                Ability.STRENGTH,
                15,
                RollMode.NORMAL,
                new CheckTrigger.Cooldown(playerTicks, targetTicks),
                Map.of(
                        TriggerOutcome.SUCCESS,
                        new CheckTrigger.Action(Optional.of(ResourceLocation.parse("pack:win")), Optional.empty())));
    }

    @Test
    void playerCooldownBlocksThatPlayerEverywhereUntilItEnds() {
        CheckTrigger wall = trigger("pack:wall", 20, 0);
        cooldowns.start(wall, ALICE, WALL, 100);

        assertFalse(cooldowns.ready(wall.id(), ALICE, OTHER_WALL, 119));
        assertTrue(cooldowns.ready(wall.id(), BOB, WALL, 119));
        assertTrue(cooldowns.ready(wall.id(), ALICE, WALL, 120));
    }

    @Test
    void targetCooldownBlocksEveryoneOnThatTargetOnly() {
        CheckTrigger wall = trigger("pack:wall", 0, 50);
        cooldowns.start(wall, ALICE, WALL, 100);

        assertFalse(cooldowns.ready(wall.id(), BOB, WALL, 149));
        assertTrue(cooldowns.ready(wall.id(), BOB, OTHER_WALL, 101));
        assertTrue(cooldowns.ready(wall.id(), ALICE, WALL, 150));
    }

    @Test
    void cooldownsAreKeptPerTrigger() {
        CheckTrigger wall = trigger("pack:wall", 20, 20);
        cooldowns.start(wall, ALICE, WALL, 100);

        assertTrue(cooldowns.ready(ResourceLocation.parse("pack:door"), ALICE, WALL, 101));
    }

    @Test
    void noCooldownAndItemUsesWithoutATargetAreAlwaysReady() {
        CheckTrigger free = trigger("pack:free", 0, 0);
        cooldowns.start(free, ALICE, WALL, 100);

        assertTrue(cooldowns.ready(free.id(), ALICE, WALL, 100));
        assertTrue(cooldowns.ready(free.id(), ALICE, Optional.empty(), 100));
    }

    @Test
    void clearForgetsEveryCooldown() {
        CheckTrigger wall = trigger("pack:wall", 20, 20);
        cooldowns.start(wall, ALICE, WALL, 100);
        cooldowns.clear();

        assertTrue(cooldowns.ready(wall.id(), ALICE, WALL, 101));
    }
}
