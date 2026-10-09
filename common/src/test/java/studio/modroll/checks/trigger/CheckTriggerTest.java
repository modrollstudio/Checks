package studio.modroll.checks.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.data.MatchEntry;
import studio.modroll.checks.data.SkillStore;
import studio.modroll.checks.skill.ShippedSkills;
import studio.modroll.critfall.api.dice.RollMode;

class CheckTriggerTest {

    private static final Set<String> KNOWN =
            Set.of("minecraft:cracked_stone_bricks", "minecraft:villager", "minecraft:compass");

    private final List<String> warnings = new ArrayList<>();

    @BeforeEach
    void loadSkills() {
        SkillStore.setSkills(ShippedSkills.load(w -> {}));
    }

    @AfterEach
    void reset() {
        SkillStore.clear();
    }

    private CheckTrigger parse(String json) {
        return CheckTrigger.parse(
                ResourceLocation.parse("pack:test"),
                JsonParser.parseString(json).getAsJsonObject(),
                SkillStore::find,
                on -> id -> KNOWN.contains(id.toString()),
                warnings::add);
    }

    private static String trigger(String on, String target, String extra) {
        return "{\"on\": \"" + on + "\", " + target + ", \"stat\": \"str\", \"dc\": 10, "
                + "\"outcomes\": {\"success\": {\"function\": \"pack:win\"}}" + extra + "}";
    }

    @Test
    void docsExampleIsTheCrackedWall() throws IOException {
        CheckTrigger wall = parse(Files.readString(Path.of("../docs/examples/trigger.json")));

        assertEquals(List.of(), warnings);
        assertEquals(CheckTrigger.Interaction.USE_BLOCK, wall.on());
        assertEquals(new MatchEntry.Exact(ResourceLocation.parse("minecraft:cracked_stone_bricks")), wall.target());
        assertEquals(ResourceLocation.parse("checks:athletics"), ((Skill) wall.stat()).id());
        assertEquals(15, wall.dc());
        assertEquals(RollMode.NORMAL, wall.mode());
        assertEquals(new CheckTrigger.Cooldown(20, 100), wall.cooldown());
        assertEquals(
                Optional.of(new CheckTrigger.Action(
                        Optional.of(ResourceLocation.parse("mypack:cracked_wall/break")), Optional.empty())),
                wall.action(TriggerOutcome.SUCCESS));
        assertEquals(Optional.empty(), wall.action(TriggerOutcome.NATURAL_20));
    }

    @Test
    void readsEntityAndItemTriggersWithAModeAndALootTable() {
        CheckTrigger talk = parse(trigger(
                "use_entity",
                "\"entity\": \"#minecraft:raiders\"",
                ", \"mode\": \"disadvantage\", \"cooldown\": {\"target\": 40}"));
        CheckTrigger compass = parse("{\"on\": \"use_item\", \"item\": \"minecraft:compass\", \"stat\": \"survival\","
                + " \"dc\": 12, \"outcomes\": {\"natural_20\": {\"loot_table\": \"pack:treasure\"}}}");

        assertEquals(RollMode.DISADVANTAGE, talk.mode());
        assertEquals(new CheckTrigger.Cooldown(0, 40), talk.cooldown());
        assertEquals(Ability.STRENGTH, talk.stat());
        assertEquals(CheckTrigger.Interaction.USE_ITEM, compass.on());
        assertEquals(
                Optional.of(ResourceLocation.parse("pack:treasure")),
                compass.action(TriggerOutcome.NATURAL_20).orElseThrow().lootTable());
    }

    @Test
    void naturalBranchesWinOnlyWhenTheTriggerHasThem() {
        Set<TriggerOutcome> plain = Set.of(TriggerOutcome.SUCCESS, TriggerOutcome.FAILURE);
        Set<TriggerOutcome> all = Set.of(TriggerOutcome.values());

        assertEquals(TriggerOutcome.NATURAL_20, TriggerOutcome.of(20, true, all));
        assertEquals(TriggerOutcome.NATURAL_20, TriggerOutcome.of(20, false, all));
        assertEquals(TriggerOutcome.NATURAL_1, TriggerOutcome.of(1, true, all));
        assertEquals(TriggerOutcome.SUCCESS, TriggerOutcome.of(20, true, plain));
        assertEquals(TriggerOutcome.FAILURE, TriggerOutcome.of(20, false, plain));
        assertEquals(TriggerOutcome.FAILURE, TriggerOutcome.of(1, false, plain));
        assertEquals(TriggerOutcome.SUCCESS, TriggerOutcome.of(12, true, all));
        assertEquals(TriggerOutcome.FAILURE, TriggerOutcome.of(12, false, all));
    }

    @Test
    void targetCooldownOnItemUseIsIgnoredWithAWarning() {
        CheckTrigger compass =
                parse(trigger("use_item", "\"item\": \"minecraft:compass\"", ", \"cooldown\": {\"target\": 40}"));

        assertEquals(new CheckTrigger.Cooldown(0, 0), compass.cooldown());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("use_item")));
    }

    @Test
    void badFilesAreRejected() {
        String block = "\"block\": \"minecraft:cracked_stone_bricks\"";
        assertRejected(trigger("use_door", block, ""), "'on'");
        assertRejected(trigger("use_block", "\"entity\": \"minecraft:villager\"", ""), "'block'");
        assertRejected(trigger("use_block", "\"block\": \"minecraft:glass_door\"", ""), "unknown block");
        assertRejected(trigger("use_block", block, ", \"mode\": \"lucky\""), "mode");
        assertRejected(trigger("use_block", block, ", \"cooldown\": {\"player\": -1}"), "negative");
        assertRejected(
                "{\"on\": \"use_block\", " + block + ", \"stat\": \"juggling\", \"dc\": 10,"
                        + " \"outcomes\": {\"success\": {\"function\": \"pack:win\"}}}",
                "juggling");
        assertRejected(
                "{\"on\": \"use_block\", " + block + ", \"stat\": \"str\","
                        + " \"outcomes\": {\"success\": {\"function\": \"pack:win\"}}}",
                "'dc'");
        assertRejected("{\"on\": \"use_block\", " + block + ", \"stat\": \"str\", \"dc\": 10}", "outcomes");
        assertRejected(
                "{\"on\": \"use_block\", " + block + ", \"stat\": \"str\", \"dc\": 10,"
                        + " \"outcomes\": {\"failure\": {}}}",
                "failure");
    }

    private void assertRejected(String json, String messagePart) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parse(json));
        assertTrue(e.getMessage().contains(messagePart), e.getMessage());
    }
}
