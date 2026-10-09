package studio.modroll.checks.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.InactiveProfiler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.preset.Background;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.checks.preset.Presets;

class PresetReloadListenerTest {

    private static final ResourceLocation SOLDIER = ResourceLocation.parse("checks:soldier");
    private static final ResourceLocation KNIGHT = ResourceLocation.parse("pack:knight");

    private final DatapackListener<Skill> skills = DatapackListener.skills();
    private final PresetReloadListener<Background> backgrounds = new PresetReloadListener<>(
            PresetKind.BACKGROUND,
            Presets.BACKGROUNDS,
            (id, json) -> Background.parse(id, json, SkillStore::find, item -> true, warning -> {}));

    @AfterEach
    void reset() {
        SkillStore.clear();
        Presets.clear();
    }

    @Test
    void aPackAddsAnEntryAndReplacesAShippedOne() {
        loadAthletics();
        reloadBackgrounds(Map.of(SOLDIER, background("str")));
        assertEquals(List.of(Ability.STRENGTH), abilitiesOf(SOLDIER));

        // A pack file at the shipped path wins the merge, as the resource manager does it.
        reloadBackgrounds(Map.of(SOLDIER, background("dex"), KNIGHT, background("cha")));

        assertEquals(List.of(Ability.DEXTERITY), abilitiesOf(SOLDIER));
        assertEquals(List.of(Ability.CHARISMA), abilitiesOf(KNIGHT));
    }

    @Test
    void anInvalidFileIsSkippedWhileTheRestLoad() {
        loadAthletics();
        reloadBackgrounds(Map.of(
                SOLDIER,
                background("str"),
                ResourceLocation.parse("pack:bad_ability"),
                background("luck"),
                ResourceLocation.parse("pack:unknown_skill"),
                json("{\"abilities\": [\"str\"], \"skills\": [\"sailing\"]}"),
                ResourceLocation.parse("pack:not_an_object"),
                json("[1]")));
        assertEquals(Set.of(SOLDIER), Presets.BACKGROUNDS.all().keySet());
    }

    @Test
    void aRemovedFlagDropsTheEntry() {
        loadAthletics();
        reloadBackgrounds(Map.of(SOLDIER, json("{\"removed\": true}"), KNIGHT, background("cha")));
        assertEquals(Set.of(KNIGHT), Presets.BACKGROUNDS.all().keySet());
    }

    @Test
    void shippedEntriesCanBeHidden() {
        loadAthletics();
        reloadBackgrounds(Map.of(SOLDIER, background("str"), KNIGHT, background("cha")));
        assertEquals(List.of(KNIGHT, SOLDIER), ids(Presets.BACKGROUNDS.offered(true)));
        assertEquals(List.of(KNIGHT), ids(Presets.BACKGROUNDS.offered(false)));
    }

    @Test
    void backgroundsReloadedAfterSkillsPickUpNewSkills() {
        Map<ResourceLocation, JsonElement> files =
                Map.of(KNIGHT, json("{\"abilities\": [\"cha\"], \"skills\": [\"pack:jousting\"]}"));
        reloadBackgrounds(files);
        assertEquals(Set.of(), Presets.BACKGROUNDS.all().keySet());

        skills.apply(
                Map.of(ResourceLocation.parse("pack:jousting"), json("{\"ability\": \"str\"}")),
                null,
                InactiveProfiler.INSTANCE);
        reloadBackgrounds(files);
        assertEquals(Set.of(KNIGHT), Presets.BACKGROUNDS.all().keySet());
    }

    private void loadAthletics() {
        skills.apply(
                Map.of(ResourceLocation.parse("checks:athletics"), json("{\"ability\": \"str\"}")),
                null,
                InactiveProfiler.INSTANCE);
    }

    private void reloadBackgrounds(Map<ResourceLocation, JsonElement> files) {
        backgrounds.apply(new HashMap<>(files), null, InactiveProfiler.INSTANCE);
    }

    private static List<Ability> abilitiesOf(ResourceLocation id) {
        return Presets.BACKGROUNDS.find(id).orElseThrow().abilities();
    }

    private static List<ResourceLocation> ids(List<Background> backgrounds) {
        return backgrounds.stream().map(Background::id).toList();
    }

    private static JsonElement background(String ability) {
        return json("{\"format_version\": 1, \"abilities\": [\"" + ability + "\"], \"skills\": [\"athletics\"]}");
    }

    private static JsonElement json(String text) {
        return JsonParser.parseString(text);
    }
}
