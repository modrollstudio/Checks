package studio.modroll.checks.body;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.data.ScoresConfig;

class BodyRulesTest {

    private static final int MEDIUM_MAX = 2;
    private static final ScoresConfig.HealthSettings HEALTH =
            new ScoresConfig.HealthSettings(true, 2.0, true, 0.5, true, 1, 8, true);

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
    void bodyArmorNeverLowersArmorClass() {
        assertEquals(
                2,
                BodyRules.bodyArmorFloor(11, 2, 10, 5),
                "DEX +5 in a chainmail chestplate: 11 + 2 = 13 would be below the 15 without it");
        assertEquals(4, BodyRules.bodyArmorFloor(11, 0, 10, 5), "DEX +5 in an iron chestplate: 15, not 11");
        assertEquals(0, BodyRules.bodyArmorFloor(13, 2, 11, 3), "DEX +3 in medium armor: 15 beats 14 without it");
        assertEquals(0, BodyRules.bodyArmorFloor(12, 2, 10, 4), "a tie needs nothing");
        assertEquals(0, BodyRules.bodyArmorFloor(11, 0, 10, -1), "a DEX penalty heavy armor ignores");
    }

    @Test
    void conHealthIsTheModifierTimesHitPointsPerPoint() {
        assertEquals(6.0, BodyRules.conHealth(3, HEALTH));
        assertEquals(-2.0, BodyRules.conHealth(-1, HEALTH));
        assertEquals(0.0, BodyRules.conHealth(0, HEALTH));
    }

    /** A d10 is worth 6, plus CON +2: 8 for each of levels 2 to 4, times 0.5. */
    @Test
    void levelHealthCountsEveryLevelAfterTheFirst() {
        assertEquals(12.0, BodyRules.levelHealth(4, 10, 2, HEALTH));
        assertEquals(0.0, BodyRules.levelHealth(1, 10, 2, HEALTH));
    }

    /** At the shipped 0.2: a d8 with CON 10 adds 5 × 0.2 = 1 a level, and 4 levels of a d6 add 3.2, rounded down. */
    @Test
    void levelHealthRoundsDownTheTotal() {
        ScoresConfig.HealthSettings shipped = ScoresConfig.DEFAULTS.body().health();
        assertEquals(19.0, BodyRules.levelHealth(20, 8, 0, shipped));
        assertEquals(3.0, BodyRules.levelHealth(5, 6, 0, shipped));
        assertEquals(38.0, BodyRules.levelHealth(20, 12, 3, shipped));
    }

    @Test
    void eachLevelAddsAtLeastTheMinimum() {
        assertEquals(1, BodyRules.perLevel(6, -5, HEALTH));
        assertEquals(4, BodyRules.perLevel(6, 0, HEALTH));
        ScoresConfig.HealthSettings noCon = new ScoresConfig.HealthSettings(true, 2.0, true, 0.5, false, 1, 8, true);
        assertEquals(4, BodyRules.perLevel(6, 3, noCon));
    }

    @Test
    void wholeHeartsRoundsDownToAnEvenNumber() {
        assertEquals(6.0, BodyRules.wholeHearts(7.0));
        assertEquals(6.0, BodyRules.wholeHearts(6.0));
        assertEquals(0.0, BodyRules.wholeHearts(1.5));
        assertEquals(-4.0, BodyRules.wholeHearts(-3.0));
    }

    @Test
    void aHitDieHealsItsRollPlusConAtLeastTheMinimum() {
        assertEquals(7f, BodyRules.hitDieHealing(5, 2, 1, 1.0));
        assertEquals(1f, BodyRules.hitDieHealing(1, -3, 1, 1.0));
        assertEquals(0f, BodyRules.hitDieHealing(1, -3, 0, 1.0));
        assertEquals(3.5f, BodyRules.hitDieHealing(5, 2, 1, 0.5));
    }

    @Test
    void aLongRestGivesBackHalfTheLevelAtLeastOne() {
        assertEquals(1, BodyRules.hitDiceRegained(1, 0.5, 1));
        assertEquals(1, BodyRules.hitDiceRegained(3, 0.5, 1));
        assertEquals(10, BodyRules.hitDiceRegained(20, 0.5, 1));
        assertEquals(5, BodyRules.hitDiceRegained(5, 1.0, 1));
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
