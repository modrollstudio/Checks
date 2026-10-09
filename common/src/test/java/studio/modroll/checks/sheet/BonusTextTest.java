package studio.modroll.checks.sheet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import net.minecraft.locale.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.ModLang;
import studio.modroll.checks.api.Ability;

/** The plain-language bonus and suggested-order lines on the creation screen. */
class BonusTextTest {

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
    void theDefaultSplitsReadAsASentence() {
        assertEquals(
                "Raise one of these by 2 and another by 1, or all three by 1.",
                SheetText.bonusSplits(List.of(List.of(2, 1), List.of(1, 1, 1)), 3)
                        .getString());
    }

    @Test
    void equalBonusesAreGrouped() {
        assertEquals(
                "Raise two of these by 2 each and another by 1.",
                SheetText.bonusSplits(List.of(List.of(2, 2, 1)), 4).getString());
        assertEquals(
                "Raise one of these by 3 and two others by 1 each.",
                SheetText.bonusSplits(List.of(List.of(1, 3, 1)), 4).getString());
    }

    @Test
    void bonusesLeftNameWhatIsStillToPlace() {
        assertEquals(
                "Bonuses left: +1", SheetText.bonusesLeft(List.of(List.of(1))).getString());
        assertEquals(
                "Bonuses left: +2 and +1 or +1, +1 and +1",
                SheetText.bonusesLeft(List.of(List.of(2, 1), List.of(1, 1, 1))).getString());
        assertEquals(
                "All bonuses used",
                SheetText.bonusesLeft(List.of(List.of(), List.of(1))).getString());
    }

    @Test
    void theSuggestedOrderReadsFromHighestToLowest() {
        assertEquals(
                "Highest score in CHA, then CON, DEX, WIS, INT, lowest in STR.",
                SheetText.suggestedOrder(List.of(
                                Ability.CHARISMA,
                                Ability.CONSTITUTION,
                                Ability.DEXTERITY,
                                Ability.WISDOM,
                                Ability.INTELLIGENCE,
                                Ability.STRENGTH))
                        .getString());
    }
}
