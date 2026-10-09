package studio.modroll.checks.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.ModLang;
import studio.modroll.critfall.api.dice.DieRoll;
import studio.modroll.critfall.api.dice.RollResult;

class RollAnnouncementTest {

    private static final RollResult LOW_ROLL = new RollResult(
            4,
            List.of(
                    new DieRoll(6, 1, true),
                    new DieRoll(6, 1, true),
                    new DieRoll(6, 2, true),
                    new DieRoll(6, 1, false)),
            0);

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
    void hardcoreLinesNameTheAbilityAndListEveryDie() {
        List<Component> lines =
                RollAnnouncement.lines(Component.literal("Steve"), CreationMethod.HARDCORE, List.of(LOW_ROLL));
        assertEquals("Steve rolled STR: 1, 1, 2, 1 = 4", lines.getFirst().getString());
    }

    @Test
    void aClientWithoutChecksSeesEnglish() {
        ModLang.injectWithoutChecks();

        assertEquals(
                "Steve rolled STR: 1, 1, 2, 1 = 4",
                RollAnnouncement.lines(Component.literal("Steve"), CreationMethod.HARDCORE, List.of(LOW_ROLL))
                        .getFirst()
                        .getString());
        assertEquals(
                "Steve rolled #1: 1, 1, 2, 1 = 4",
                RollAnnouncement.lines(Component.literal("Steve"), CreationMethod.ROLL, List.of(LOW_ROLL))
                        .getFirst()
                        .getString());
    }

    @Test
    void freeRollsAreNumbered() {
        List<Component> lines =
                RollAnnouncement.lines(Component.literal("Steve"), CreationMethod.ROLL, List.of(LOW_ROLL, LOW_ROLL));
        assertEquals("Steve rolled #2: 1, 1, 2, 1 = 4", lines.get(1).getString());
    }

    @Test
    void onlyTheDroppedDieIsStruckThrough() {
        List<Boolean> struck = new ArrayList<>();
        RollAnnouncement.dice(LOW_ROLL.dice())
                .visit(
                        (style, text) -> {
                            if (Character.isDigit(text.charAt(0))) {
                                struck.add(style.isStrikethrough());
                            }
                            return Optional.empty();
                        },
                        Style.EMPTY);
        assertEquals(List.of(false, false, false, true), struck);
    }
}
