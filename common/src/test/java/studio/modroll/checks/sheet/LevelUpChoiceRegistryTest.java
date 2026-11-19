package studio.modroll.checks.sheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.LevelUpChoice;
import studio.modroll.checks.api.LevelUpChoices;

class LevelUpChoiceRegistryTest {

    private static final LevelUpChoice SPELLS = new LevelUpChoice(Component.literal("Spells"), 3, () -> {});
    private static final LevelUpChoice FEATS = new LevelUpChoice(Component.literal("Feats"), 0, () -> {});

    @AfterEach
    void reset() {
        LevelUpChoiceRegistry.clear();
    }

    private static StatSheet.LevelLine line(boolean modChoices, int improvements) {
        return new StatSheet.LevelLine(
                4,
                270,
                270,
                Optional.of(650),
                new StatSheet.Improvements(improvements, List.of(List.of(2)), 20),
                StatSheet.ExpertiseChoice.NONE,
                List.of(),
                modChoices);
    }

    @Test
    void registeredChoicesShowInRegistrationOrder() {
        LevelUpChoices.register(ResourceLocation.parse("othermod:spells"), () -> Optional.of(SPELLS));
        LevelUpChoices.register(ResourceLocation.parse("othermod:feats"), () -> Optional.of(FEATS));

        assertEquals(List.of(SPELLS, FEATS), LevelUpChoiceRegistry.choicesFor(line(true, 0)));
    }

    @Test
    void anEmptyChoiceIsHiddenAndAThrowingOneSkipped() {
        LevelUpChoices.register(ResourceLocation.parse("othermod:nothing_yet"), Optional::empty);
        LevelUpChoices.register(ResourceLocation.parse("othermod:broken"), () -> {
            throw new IllegalStateException("not synced");
        });
        LevelUpChoices.register(ResourceLocation.parse("othermod:spells"), () -> Optional.of(SPELLS));

        assertEquals(List.of(SPELLS), LevelUpChoiceRegistry.choicesFor(line(true, 0)));
    }

    @Test
    void registeringAnIdAgainReplacesItsChoiceInPlace() {
        LevelUpChoices.register(ResourceLocation.parse("othermod:spells"), () -> Optional.of(FEATS));
        LevelUpChoices.register(ResourceLocation.parse("othermod:feats"), () -> Optional.of(FEATS));
        LevelUpChoices.register(ResourceLocation.parse("othermod:spells"), () -> Optional.of(SPELLS));

        assertEquals(List.of(SPELLS, FEATS), LevelUpChoiceRegistry.choicesFor(line(true, 0)));
    }

    @Test
    void theServerSwitchHidesEveryChoice() {
        LevelUpChoices.register(ResourceLocation.parse("othermod:spells"), () -> Optional.of(SPELLS));

        assertEquals(List.of(), LevelUpChoiceRegistry.choicesFor(line(false, 0)));
        assertFalse(LevelUpChoiceRegistry.pending(line(false, 0)));
    }

    @Test
    void aChoiceWithPicksLeftMakesItALevelUp() {
        LevelUpChoices.register(ResourceLocation.parse("othermod:feats"), () -> Optional.of(FEATS));
        assertFalse(LevelUpChoiceRegistry.pending(line(true, 0)));
        assertTrue(LevelUpChoiceRegistry.pending(line(true, 1)));

        LevelUpChoices.register(ResourceLocation.parse("othermod:spells"), () -> Optional.of(SPELLS));
        assertTrue(LevelUpChoiceRegistry.pending(line(true, 0)));
    }

    @Test
    void theButtonShowsTheCountWhileThereArePicksLeft() {
        Component spells = LevelUpChoiceRegistry.label(SPELLS);
        TranslatableContents contents = (TranslatableContents) spells.getContents();
        assertEquals("screen.checks.level_up.choice", contents.getKey());
        assertEquals(List.of(SPELLS.label(), 3), List.of(contents.getArgs()));
        assertEquals(FEATS.label(), LevelUpChoiceRegistry.label(FEATS));
    }
}
