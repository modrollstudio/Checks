package studio.modroll.checks.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.ModLang;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.preset.PresetKind;

class CharacterStoryTest {

    private static final Component NAME = Component.literal("Dev1");
    private static final ResourceLocation ATHLETICS = ResourceLocation.parse("checks:athletics");
    private static final ResourceLocation INSIGHT = ResourceLocation.parse("checks:insight");

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
    void tellsWhoTheCharacterIsTheirBestAbilitiesAndSkills() {
        Map<PresetKind, Optional<ResourceLocation>> presets = Map.of(
                PresetKind.SPECIES, Optional.of(ResourceLocation.parse("checks:dwarf")),
                PresetKind.BACKGROUND, Optional.of(ResourceLocation.parse("checks:soldier")),
                PresetKind.CLASS, Optional.of(ResourceLocation.parse("checks:fighter")));

        String story = CharacterStory.of(NAME, presets, scores(15, 13, 14, 8, 12, 10), List.of(ATHLETICS, INSIGHT))
                .getString();

        assertEquals(
                "You are Dev1, Dwarf by birth, Soldier by upbringing and Fighter by calling."
                        + " Your greatest strengths are Strength (15) and Constitution (14)."
                        + " You are skilled in Athletics and Insight.",
                story);
    }

    @Test
    void customPresetsAndNoSkillsAreToldPlainly() {
        Map<PresetKind, Optional<ResourceLocation>> presets = Map.of(PresetKind.SPECIES, Optional.empty());

        String story = CharacterStory.of(NAME, presets, scores(10, 10, 10, 10, 10, 10), List.of())
                .getString();

        assertEquals(
                "You are Dev1. Your greatest strengths are Strength (10) and Dexterity (10)."
                        + " You have no skill proficiencies.",
                story);
    }

    @Test
    void bestAbilitiesBreakTiesInAbilityOrder() {
        assertEquals(
                List.of(Ability.WISDOM, Ability.DEXTERITY),
                CharacterStory.bestAbilities(scores(10, 14, 12, 8, 15, 14)));
    }

    private static Map<Ability, Integer> scores(int... values) {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            scores.put(ability, values[ability.ordinal()]);
        }
        return scores;
    }
}
