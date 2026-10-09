package studio.modroll.checks.check;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.OptionalInt;
import net.minecraft.locale.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.ModLang;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.critfall.api.dice.RollMode;

class RollTextTest {

    private final Language before = Language.getInstance();

    @BeforeEach
    void useALanguageWithoutChecks() {
        ModLang.injectWithoutChecks();
    }

    @AfterEach
    void restoreLang() {
        Language.inject(before);
    }

    @Test
    void aNormalRollShowsOneD20() {
        assertEquals("d20 13", RollText.d20(RollDetail.normal(13)).getString());
    }

    @Test
    void advantageNamesTheModeAndShowsBothD20s() {
        RollDetail roll = new RollDetail(RollMode.ADVANTAGE, 18, OptionalInt.of(4));

        assertEquals("advantage, d20 18 and 4, keeps 18", RollText.d20(roll).getString());
    }

    @Test
    void disadvantageNamesTheModeAndShowsBothD20s() {
        RollDetail roll = new RollDetail(RollMode.DISADVANTAGE, 4, OptionalInt.of(18));

        assertEquals("disadvantage, d20 4 and 18, keeps 4", RollText.d20(roll).getString());
    }
}
