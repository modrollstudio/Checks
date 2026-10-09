package studio.modroll.checks.preset;

import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.data.LenientJson;

/**
 * A class preset: saving-throw proficiencies, the skills a player picks {@code skillChoices} of, the
 * order its scores are suggested in (all six abilities), and its hit die, kept for levelling.
 */
public record ClassPreset(
        ResourceLocation id,
        List<Ability> saves,
        List<ResourceLocation> skillOptions,
        int skillChoices,
        List<Ability> suggestedOrder,
        int hitDie,
        ResourceLocation icon)
        implements Preset {

    public static final int FORMAT_VERSION = 1;
    private static final String ANY_SKILL = "any";

    private static final StreamCodec<ByteBuf, List<Ability>> ABILITIES =
            Abilities.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<ByteBuf, List<ResourceLocation>> IDS =
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list());

    // Written out by hand: StreamCodec.composite takes at most six fields.
    public static final StreamCodec<ByteBuf, ClassPreset> STREAM_CODEC = StreamCodec.of(
            (buffer, preset) -> {
                ResourceLocation.STREAM_CODEC.encode(buffer, preset.id());
                ABILITIES.encode(buffer, preset.saves());
                IDS.encode(buffer, preset.skillOptions());
                ByteBufCodecs.VAR_INT.encode(buffer, preset.skillChoices());
                ABILITIES.encode(buffer, preset.suggestedOrder());
                ByteBufCodecs.VAR_INT.encode(buffer, preset.hitDie());
                ResourceLocation.STREAM_CODEC.encode(buffer, preset.icon());
            },
            buffer -> new ClassPreset(
                    ResourceLocation.STREAM_CODEC.decode(buffer),
                    ABILITIES.decode(buffer),
                    IDS.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ABILITIES.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ResourceLocation.STREAM_CODEC.decode(buffer)));

    public ClassPreset {
        saves = List.copyOf(saves);
        skillOptions = List.copyOf(skillOptions);
        suggestedOrder = List.copyOf(suggestedOrder);
    }

    /**
     * {@code skills.from} is a list of skill ids, where one naming no loaded skill is skipped with a
     * warning, or {@code "any"} for every skill in {@code allSkills}. {@code suggested_order} may list
     * only the top abilities; the rest follow in STR to CHA order.
     */
    public static ClassPreset parse(
            ResourceLocation id,
            JsonObject json,
            Function<ResourceLocation, Optional<Skill>> skills,
            Collection<ResourceLocation> allSkills,
            Predicate<ResourceLocation> itemExists,
            Consumer<String> warn) {
        LenientJson j = new LenientJson(json, "class " + id, warn);
        j.checkFormatVersion(FORMAT_VERSION);
        int hitDie = j.optionalInt("hit_die").stream()
                .filter(sides -> sides >= 1)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("'hit_die' must be a number of sides, at least 1"));
        List<Ability> saves = PresetJson.abilities(j, "saves");
        LenientJson skillJson = j.object("skills");
        int choices = parseChoices(skillJson.optionalInt("choose"));
        List<ResourceLocation> options = parseOptions(skillJson, skills, allSkills);
        List<Ability> order = completeOrder(PresetJson.abilities(j, "suggested_order"));
        ResourceLocation icon = PresetJson.icon(j, PresetKind.CLASS, itemExists);
        j.finish();
        return new ClassPreset(id, saves, options, choices, order, hitDie, icon);
    }

    private static int parseChoices(OptionalInt choose) {
        if (choose.orElse(0) < 0) {
            throw new IllegalArgumentException("'skills.choose' must not be negative");
        }
        return choose.orElse(0);
    }

    private static List<ResourceLocation> parseOptions(
            LenientJson json, Function<ResourceLocation, Optional<Skill>> skills, Collection<ResourceLocation> all) {
        List<String> from = json.stringList("from");
        if (from.equals(List.of(ANY_SKILL))) {
            return all.stream()
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .toList();
        }
        List<ResourceLocation> options = new ArrayList<>();
        for (String text : from) {
            PresetJson.skill(text, skills)
                    .map(Skill::id)
                    .filter(skill -> !options.contains(skill))
                    .ifPresentOrElse(options::add, () -> json.warn("skipped unknown or repeated skill '" + text + "'"));
        }
        return options;
    }

    private static List<Ability> completeOrder(List<Ability> listed) {
        List<Ability> order = new ArrayList<>(listed);
        for (Ability ability : Ability.values()) {
            if (!order.contains(ability)) {
                order.add(ability);
            }
        }
        return order;
    }
}
