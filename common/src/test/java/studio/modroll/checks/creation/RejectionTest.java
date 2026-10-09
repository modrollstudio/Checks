package studio.modroll.checks.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Optional;
import net.minecraft.locale.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.ModLang;

class RejectionTest {

    private final Language before = Language.getInstance();

    @BeforeEach
    void loadModLang() throws IOException {
        ModLang.inject();
    }

    @AfterEach
    void restoreLang() {
        Language.inject(before);
    }

    @Test
    void skillCountNamesTheRequiredNumber() {
        assertEquals(
                "Pick 2 skills.",
                Rejection.SKILL_COUNT
                        .message(Offers.unrolled(), PresetChoices.CUSTOM)
                        .getString());
    }

    @Test
    void anIncompleteAssignmentAsksForAllSixScores() {
        assertEquals(
                "Assign all six scores.",
                Rejection.SCORE_COUNT
                        .message(Offers.unrolled(), PresetChoices.CUSTOM)
                        .getString());
    }

    @Test
    void everyRejectionHasALangLine() {
        for (Rejection rejection : Rejection.values()) {
            assertTrue(
                    Language.getInstance().has("checks.creation.rejected." + rejection.id()),
                    rejection.id() + " has no lang line");
        }
    }

    @Test
    void bonusMessagesNameTheSplitsAndTheCap() {
        PresetChoices soldier =
                new PresetChoices(Optional.empty(), Optional.of(Offers.SOLDIER), Optional.empty(), Optional.empty());
        assertEquals(
                "Your background bonuses don't fit. Raise one of these by 2 and another by 1, or all three by 1.",
                Rejection.BONUS_PATTERN.message(Offers.withPresets(), soldier).getString());
        assertEquals(
                "A background bonus can't raise a score above 20.",
                Rejection.BONUS_OVER_MAX.message(Offers.withPresets(), soldier).getString());
    }

    @Test
    void skillCountFollowsTheChosenClass() {
        PresetChoices sageFighter = new PresetChoices(
                Optional.empty(), Optional.of(Offers.SAGE), Optional.of(Offers.FIGHTER), Optional.empty());
        assertEquals(
                "Pick 2 skills.",
                Rejection.SKILL_COUNT.message(Offers.withPresets(), sageFighter).getString());
    }
}
