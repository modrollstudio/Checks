package studio.modroll.checks.sheet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.SheetRow;
import studio.modroll.checks.api.SheetSection;
import studio.modroll.checks.api.StatSheetSections;

/** The data the stat screen draws for sections other mods register; the drawing itself is not tested. */
class SheetSectionRegistryTest {

    private static final SheetSection REPUTATION = new SheetSection(
            Component.literal("Reputation"),
            List.of(
                    new SheetRow(
                            Component.literal("Villagers"),
                            Component.literal("Friendly"),
                            List.of(Component.literal("Trade prices are lower."))),
                    new SheetRow(Component.literal("Pillagers"), Component.literal("Hostile"))));
    private static final SheetSection SPELLS = new SheetSection(
            Component.literal("Spell slots"),
            List.of(new SheetRow(Component.literal("1st"), Component.literal("2/4"))));

    @AfterEach
    void reset() {
        SheetSectionRegistry.clear();
    }

    private static StatSheet sheet(boolean modSections) {
        return new StatSheet(
                2,
                List.of(),
                List.of(),
                List.of(),
                false,
                Optional.empty(),
                Optional.empty(),
                modSections,
                StatSheet.Body.NONE);
    }

    @Test
    void registeredSectionsAreDrawnInRegistrationOrder() {
        StatSheetSections.register(ResourceLocation.parse("othermod:reputation"), () -> Optional.of(REPUTATION));
        StatSheetSections.register(ResourceLocation.parse("othermod:spells"), () -> Optional.of(SPELLS));

        List<SheetSection> sections = SheetSectionRegistry.sectionsFor(sheet(true));

        assertEquals(List.of(REPUTATION, SPELLS), sections);
        assertEquals(
                "Trade prices are lower.",
                sections.getFirst().rows().getFirst().tooltip().getFirst().getString());
        assertEquals(List.of(), sections.getFirst().rows().get(1).tooltip());
    }

    @Test
    void anEmptySectionIsHiddenAndAThrowingOneSkipped() {
        StatSheetSections.register(ResourceLocation.parse("othermod:nothing_yet"), Optional::empty);
        StatSheetSections.register(ResourceLocation.parse("othermod:broken"), () -> {
            throw new IllegalStateException("not synced");
        });
        StatSheetSections.register(ResourceLocation.parse("othermod:spells"), () -> Optional.of(SPELLS));

        assertEquals(List.of(SPELLS), SheetSectionRegistry.sectionsFor(sheet(true)));
    }

    @Test
    void registeringAnIdAgainReplacesItsSectionInPlace() {
        StatSheetSections.register(ResourceLocation.parse("othermod:reputation"), () -> Optional.of(SPELLS));
        StatSheetSections.register(ResourceLocation.parse("othermod:spells"), () -> Optional.of(SPELLS));
        StatSheetSections.register(ResourceLocation.parse("othermod:reputation"), () -> Optional.of(REPUTATION));

        assertEquals(List.of(REPUTATION, SPELLS), SheetSectionRegistry.sectionsFor(sheet(true)));
    }

    @Test
    void theServerSwitchHidesEverySection() {
        StatSheetSections.register(ResourceLocation.parse("othermod:reputation"), () -> Optional.of(REPUTATION));

        assertEquals(List.of(), SheetSectionRegistry.sectionsFor(sheet(false)));
    }
}
