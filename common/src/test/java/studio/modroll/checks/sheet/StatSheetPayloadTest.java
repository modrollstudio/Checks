package studio.modroll.checks.sheet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.body.ArmorCategory;
import studio.modroll.checks.body.Extra;
import studio.modroll.checks.body.Size;
import studio.modroll.checks.body.SizeOption;
import studio.modroll.checks.bonus.BonusPart;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.critfall.api.dice.RollMode;

class StatSheetPayloadTest {

    @Test
    void sheetSurvivesTheWire() {
        StatSheet sheet = new StatSheet(
                3,
                List.of(
                        new StatSheet.AbilityLine(
                                Ability.STRENGTH, 8, -1, -1, Proficiency.NONE, StatSheet.AbilityBonuses.NONE),
                        new StatSheet.AbilityLine(
                                Ability.DEXTERITY,
                                30,
                                10,
                                15,
                                Proficiency.PROFICIENT,
                                new StatSheet.AbilityBonuses(
                                        List.of(new BonusPart(
                                                ResourceLocation.parse("pack:gloves"), 0, RollMode.ADVANTAGE)),
                                        List.of(new BonusPart(
                                                ResourceLocation.parse("pack:cloak"), 2, RollMode.NORMAL))))),
                List.of(
                        new StatSheet.SkillLine(
                                ResourceLocation.parse("checks:stealth"),
                                Ability.DEXTERITY,
                                16,
                                Proficiency.EXPERTISE,
                                2,
                                List.of(new BonusPart(
                                        ResourceLocation.parse("pack:elven_boots"), 0, RollMode.DISADVANTAGE))),
                        new StatSheet.SkillLine(
                                ResourceLocation.parse("mypack:lore/old_runes"),
                                Ability.INTELLIGENCE,
                                -5,
                                Proficiency.NONE,
                                -30,
                                List.of())),
                List.of(new StatSheet.PassiveLine(ResourceLocation.parse("checks:perception"), 13)),
                true,
                Optional.of(new StatSheet.Identity(
                        new PresetChoices(
                                Optional.of(ResourceLocation.parse("checks:elf")),
                                Optional.empty(),
                                Optional.of(ResourceLocation.parse("checks:wizard")),
                                Optional.empty()),
                        List.of(ResourceLocation.parse("checks:darkvision")))),
                Optional.of(new StatSheet.LevelLine(
                        5,
                        700,
                        650,
                        Optional.of(1400),
                        new StatSheet.Improvements(1, List.of(List.of(2), List.of(1, 1)), 20))),
                true,
                new StatSheet.Body(
                        Optional.of(new StatSheet.HealthLine(27.5f, 3, 6.0, 1.5)),
                        Optional.of(new StatSheet.ArmorLine(14, 12, 3, ArmorCategory.MEDIUM, 2, 2)),
                        Optional.of(new SizeOption(Size.SMALL, 0.6)),
                        List.of(
                                new StatSheet.ExtraLine(Extra.KNOCKBACK, -0.1),
                                new StatSheet.ExtraLine(Extra.EXHAUSTION, 0.15))));
        ByteBuf buffer = Unpooled.buffer();

        StatSheetPayload.STREAM_CODEC.encode(buffer, new StatSheetPayload(sheet));

        assertEquals(sheet, StatSheetPayload.STREAM_CODEC.decode(buffer).sheet());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void sheetWithoutLevelSurvivesTheWire() {
        StatSheet sheet = new StatSheet(
                2,
                List.of(),
                List.of(),
                List.of(),
                false,
                Optional.empty(),
                Optional.empty(),
                false,
                StatSheet.Body.NONE);
        ByteBuf buffer = Unpooled.buffer();

        StatSheetPayload.STREAM_CODEC.encode(buffer, new StatSheetPayload(sheet));

        assertEquals(sheet, StatSheetPayload.STREAM_CODEC.decode(buffer).sheet());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void requestCarriesNoBytes() {
        ByteBuf buffer = Unpooled.buffer();

        StatSheetRequestPayload.STREAM_CODEC.encode(buffer, StatSheetRequestPayload.INSTANCE);

        assertEquals(0, buffer.readableBytes());
        assertEquals(StatSheetRequestPayload.INSTANCE, StatSheetRequestPayload.STREAM_CODEC.decode(buffer));
    }
}
