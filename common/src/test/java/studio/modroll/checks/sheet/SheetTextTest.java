package studio.modroll.checks.sheet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class SheetTextTest {

    @Test
    void untranslatedSkillFallsBackToItsLastPathSegmentInTitleCase() {
        assertEquals("Old Runes", SheetText.readableName(ResourceLocation.parse("mypack:lore/old_runes")));
        assertEquals("Lockpicking", SheetText.readableName(ResourceLocation.parse("mypack:lockpicking")));
    }

    @Test
    void modifiersAlwaysCarryASign() {
        assertEquals("+0", SheetText.signed(0));
        assertEquals("+3", SheetText.signed(3));
        assertEquals("-1", SheetText.signed(-1));
    }
}
