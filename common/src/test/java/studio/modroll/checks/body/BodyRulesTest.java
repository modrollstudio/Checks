package studio.modroll.checks.body;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.data.ScoresConfig;

class BodyRulesTest {

    private static final int MEDIUM_MAX = 2;
    private static final ScoresConfig.HealthSettings HEALTH = new ScoresConfig.HealthSettings(true, 2.0, true, 0.5);

    @Test
    void unarmoredAndLightArmorAddAllOfDex() {
        assertEquals(4, BodyRules.armorClassBonus(4, ArmorCategory.NONE, MEDIUM_MAX));
        assertEquals(4, BodyRules.armorClassBonus(4, ArmorCategory.LIGHT, MEDIUM_MAX));
        assertEquals(-1, BodyRules.armorClassBonus(-1, ArmorCategory.LIGHT, MEDIUM_MAX));
    }

    @Test
    void mediumArmorCapsDexButKeepsAPenalty() {
        assertEquals(2, BodyRules.armorClassBonus(4, ArmorCategory.MEDIUM, MEDIUM_MAX));
        assertEquals(1, BodyRules.armorClassBonus(1, ArmorCategory.MEDIUM, MEDIUM_MAX));
        assertEquals(-2, BodyRules.armorClassBonus(-2, ArmorCategory.MEDIUM, MEDIUM_MAX));
        assertEquals(3, BodyRules.armorClassBonus(4, ArmorCategory.MEDIUM, 3));
    }

    @Test
    void heavyArmorIgnoresDex() {
        assertEquals(0, BodyRules.armorClassBonus(4, ArmorCategory.HEAVY, MEDIUM_MAX));
        assertEquals(0, BodyRules.armorClassBonus(-3, ArmorCategory.HEAVY, MEDIUM_MAX));
    }

    @Test
    void conHealthIsTheModifierTimesHitPointsPerPoint() {
        assertEquals(6.0, BodyRules.conHealth(3, HEALTH));
        assertEquals(-2.0, BodyRules.conHealth(-1, HEALTH));
        assertEquals(0.0, BodyRules.conHealth(0, HEALTH));
    }

    /** d10 is worth 6 and d6 4, times 0.5: 3 + 3 + 2 = 8. */
    @Test
    void levelHealthAddsEachGainedLevelsHitDie() {
        assertEquals(8.0, BodyRules.levelHealth(4, Map.of(2, 10, 3, 10, 4, 6), HEALTH));
    }

    /** Levels 2 and 3 count 6 × 0.2 each, 2.4 in all; the level 9 die does not count yet. */
    @Test
    void levelHealthSkipsHitDiceAboveTheCurrentLevelAndRoundsDown() {
        ScoresConfig.HealthSettings oneFifth = new ScoresConfig.HealthSettings(true, 2.0, true, 0.2);
        assertEquals(2.0, BodyRules.levelHealth(3, Map.of(2, 10, 3, 10, 9, 12), oneFifth));
        assertEquals(0.0, BodyRules.levelHealth(1, Map.of(2, 10), HEALTH));
    }

    @Test
    void extraAmountIsTheModifierTimesItsPerPoint() {
        assertEquals(0.3, BodyRules.extraAmount(3, new ScoresConfig.ExtraSetting(true, 0.1)), 1e-9);
        assertEquals(-0.05, BodyRules.extraAmount(-1, new ScoresConfig.ExtraSetting(true, 0.05)), 1e-9);
    }

    @Test
    void bowChargeCountsFasterWithAPositiveAmount() {
        assertEquals(13, BodyRules.bowCharge(10, 0.3));
        assertEquals(9, BodyRules.bowCharge(10, -0.1));
        assertEquals(10, BodyRules.bowCharge(10, 0.0));
    }

    @Test
    void crossbowLoadsFasterButNeverInstantly() {
        assertEquals(19, BodyRules.crossbowChargeDuration(25, 0.3));
        assertEquals(28, BodyRules.crossbowChargeDuration(25, -0.1));
        assertEquals(1, BodyRules.crossbowChargeDuration(1, 5.0));
    }

    @Test
    void aVeryLowModifierSlowsButNeverStopsABowOrCrossbow() {
        assertEquals(2, BodyRules.bowCharge(20, -5.0));
        assertEquals(250, BodyRules.crossbowChargeDuration(25, -5.0));
    }

    @Test
    void exhaustionShrinksByTheAmountButNeverBelowZero() {
        assertEquals(0.85f, BodyRules.exhaustion(1.0f, 0.15), 1e-6);
        assertEquals(1.05f, BodyRules.exhaustion(1.0f, -0.05), 1e-6);
        assertEquals(0.0f, BodyRules.exhaustion(1.0f, 2.0), 1e-6);
    }

    @Test
    void healthVanillaCappedOnLoadIsRestoredUpToTheNewMax() {
        assertEquals(Optional.of(26f), BodyRules.restoredHealth(26f, 20f, 20f, 26f));
        assertEquals(Optional.of(24f), BodyRules.restoredHealth(26f, 20f, 20f, 24f));
        assertEquals(Optional.of(26f), BodyRules.restoredHealth(26f, 24f, 24f, 30f));
    }

    @Test
    void healthIsLeftAloneWhenNothingWasCapped() {
        assertEquals(Optional.empty(), BodyRules.restoredHealth(18f, 18f, 20f, 26f));
        assertEquals(Optional.empty(), BodyRules.restoredHealth(0f, 0f, 20f, 26f));
    }

    /** The player's own save says 5 HP: a higher stored health from an earlier save must not heal them. */
    @Test
    void aStoredHealthOlderThanThePlayersSaveNeverHeals() {
        assertEquals(Optional.empty(), BodyRules.restoredHealth(26f, 5f, 20f, 26f));
    }

    @Test
    void traitLevelHealthRoundsDown() {
        assertEquals(0.0, BodyRules.levelHealth(1, 0.5));
        assertEquals(2.0, BodyRules.levelHealth(5, 0.5));
        assertEquals(10.0, BodyRules.levelHealth(20, 0.5));
    }
}
