package studio.modroll.checks.trait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.CheckKind;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.data.MatchEntry;
import studio.modroll.checks.save.VanillaSave;
import studio.modroll.critfall.api.dice.RollMode;

class TraitParseTest {

    private static final ResourceLocation ID = ResourceLocation.parse("mypack:test");
    private static final ResourceLocation STEALTH = ResourceLocation.parse("checks:stealth");
    private static final ResourceLocation PERCEPTION = ResourceLocation.parse("checks:perception");
    private static final Map<ResourceLocation, Skill> SKILLS =
            Map.of(STEALTH, new Skill(STEALTH, Ability.DEXTERITY), PERCEPTION, new Skill(PERCEPTION, Ability.WISDOM));
    private static final TraitEffect.Lookups LOOKUPS = new TraitEffect.Lookups(
            id -> Optional.ofNullable(SKILLS.get(ResourceLocation.fromNamespaceAndPath("checks", id.getPath()))),
            id -> id.getPath().equals("piglin"),
            id -> id.getPath().equals("generic.movement_speed"));

    private final List<String> warnings = new ArrayList<>();

    private TraitEffect effect(String json) {
        return parse("{\"effects\": [" + json + "]}").effects().getFirst();
    }

    private Trait parse(String json) {
        return Trait.parse(ID, JsonParser.parseString(json).getAsJsonObject(), LOOKUPS, warnings::add);
    }

    @Test
    void readsEveryEffectType() {
        assertEquals(
                new TraitEffect.VanillaSaveAdvantage(Set.of(VanillaSave.POISON, VanillaSave.DARKNESS)),
                effect("{\"type\": \"vanilla_save_advantage\", \"against\": [\"poison\", \"darkness\"]}"));
        TraitEffect.RollBonus bonus = (TraitEffect.RollBonus)
                effect("{\"type\": \"roll_bonus\", \"applies_to\": {\"saves\": [\"int\"]}, \"mode\": \"advantage\"}");
        assertEquals(RollMode.ADVANTAGE, bonus.mode());
        assertTrue(bonus.targets().appliesTo(CheckKind.SAVE, Ability.INTELLIGENCE));
        assertEquals(
                new TraitEffect.DamageResistance(List.of(MatchEntry.parse("#minecraft:is_fire")), 0.5),
                effect("{\"type\": \"damage_resistance\", \"damage_types\": [\"#minecraft:is_fire\"]}"));
        assertEquals(new TraitEffect.ExtraHealth(0.5), effect("{\"type\": \"extra_health\", \"per_level\": 0.5}"));
        assertEquals(new TraitEffect.Darkvision(), effect("{\"type\": \"darkvision\"}"));
        assertEquals(
                new TraitEffect.Reroll(TraitEffect.When.FAILURE, true),
                effect("{\"type\": \"reroll\", \"on\": \"failure\", \"daily\": true}"));
        assertEquals(new TraitEffect.LastStand(), effect("{\"type\": \"last_stand\"}"));
        assertEquals(
                new TraitEffect.IgnoredBy(List.of(MatchEntry.parse("minecraft:piglin"))),
                effect("{\"type\": \"ignored_by\", \"entities\": [\"minecraft:piglin\"]}"));
        assertEquals(
                new TraitEffect.SkillChoices(1, Set.of(STEALTH, PERCEPTION)),
                effect("{\"type\": \"skill_choices\", \"count\": 1, \"from\": [\"stealth\", \"perception\"]}"));
        assertEquals(
                new TraitEffect.Attribute(
                        ResourceLocation.parse("minecraft:generic.movement_speed"),
                        0.05,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                effect("{\"type\": \"attribute\", \"attribute\": \"minecraft:generic.movement_speed\","
                        + " \"amount\": 0.05, \"operation\": \"add_multiplied_base\"}"));
        assertEquals(List.of(), warnings);
    }

    @Test
    void aTraitWithNoEffectsIsFlavorOnly() {
        assertEquals(List.of(), parse("{\"format_version\": 1}").effects());
    }

    @Test
    void badEffectsRejectTheFile() {
        for (String bad : List.of(
                "{\"type\": \"fly\"}",
                "{\"type\": \"vanilla_save_advantage\", \"against\": [\"lava\"]}",
                "{\"type\": \"roll_bonus\", \"applies_to\": {\"saves\": [\"int\"]}}",
                "{\"type\": \"damage_resistance\", \"damage_types\": []}",
                "{\"type\": \"damage_resistance\", \"damage_types\": [\"#minecraft:is_fire\"], \"multiplier\": -1}",
                "{\"type\": \"extra_health\"}",
                "{\"type\": \"reroll\", \"on\": \"natural_20\"}",
                "{\"type\": \"ignored_by\", \"entities\": [\"minecraft:creeper\"]}",
                "{\"type\": \"skill_choices\", \"count\": 0}",
                "{\"type\": \"skill_choices\", \"count\": 1, \"from\": [\"juggling\"]}",
                "{\"type\": \"attribute\", \"attribute\": \"minecraft:generic.flying\", \"amount\": 1}",
                "{\"type\": \"attribute\", \"attribute\": \"minecraft:generic.movement_speed\", \"amount\": 1,"
                        + " \"operation\": \"times\"}")) {
            assertThrows(IllegalArgumentException.class, () -> effect(bad), bad);
        }
    }

    @Test
    void unknownSkillInAListIsSkippedWithWarning() {
        assertEquals(
                new TraitEffect.SkillChoices(1, Set.of(STEALTH)),
                effect("{\"type\": \"skill_choices\", \"count\": 1, \"from\": [\"stealth\", \"juggling\"]}"));
        assertEquals(1, warnings.size());
    }

    @Test
    void docsExampleParsesCleanly() throws Exception {
        String json = java.nio.file.Files.readString(java.nio.file.Path.of("../docs/examples/trait.json"));
        Trait trait = Trait.parse(ID, JsonParser.parseString(json).getAsJsonObject(), LOOKUPS, warnings::add);
        assertEquals(2, trait.effects().size());
        assertEquals(List.of(), warnings);
    }
}
