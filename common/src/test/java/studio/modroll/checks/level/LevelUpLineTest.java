package studio.modroll.checks.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.ModLang;

class LevelUpLineTest {

    private final Language before = Language.getInstance();

    @AfterEach
    void restoreLang() {
        Language.inject(before);
    }

    @Test
    void aClientWithoutChecksSeesTheLevelUpInEnglish() {
        ModLang.injectWithoutChecks();

        assertEquals(
                "Steve reached level 5!",
                Levelling.levelUpLine(Component.literal("Steve"), 5).getString());
    }
}
