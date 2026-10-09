package studio.modroll.checks.preset;

import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
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
 * A background: the abilities its creation bonuses may go to, the skills it makes proficient, and a
 * starting kit handed out once when the character is confirmed.
 */
public record Background(
        ResourceLocation id,
        List<Ability> abilities,
        List<ResourceLocation> skills,
        List<KitItem> kit,
        ResourceLocation icon)
        implements Preset {

    public static final int FORMAT_VERSION = 1;

    public static final StreamCodec<ByteBuf, Background> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            Background::id,
            Abilities.STREAM_CODEC.apply(ByteBufCodecs.list()),
            Background::abilities,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
            Background::skills,
            KitItem.STREAM_CODEC.apply(ByteBufCodecs.list()),
            Background::kit,
            ResourceLocation.STREAM_CODEC,
            Background::icon,
            Background::new);

    public Background {
        abilities = List.copyOf(abilities);
        skills = List.copyOf(skills);
        kit = List.copyOf(kit);
    }

    /**
     * Abilities and skills must all be known, or the file is rejected; a kit entry naming no registered
     * item, or with a count below 1, is skipped with a warning.
     */
    public static Background parse(
            ResourceLocation id,
            JsonObject json,
            Function<ResourceLocation, Optional<Skill>> skills,
            Predicate<ResourceLocation> itemExists,
            Consumer<String> warn) {
        LenientJson j = new LenientJson(json, "background " + id, warn);
        j.checkFormatVersion(FORMAT_VERSION);
        List<Ability> abilities = PresetJson.abilities(j, "abilities");
        if (abilities.isEmpty()) {
            throw new IllegalArgumentException("'abilities' must list at least one ability");
        }
        List<ResourceLocation> skillIds = parseSkills(j, skills);
        List<KitItem> kit = parseKit(j, itemExists);
        ResourceLocation icon = PresetJson.icon(j, PresetKind.BACKGROUND, itemExists);
        j.finish();
        return new Background(id, abilities, skillIds, kit, icon);
    }

    private static List<ResourceLocation> parseSkills(
            LenientJson json, Function<ResourceLocation, Optional<Skill>> skills) {
        return json.stringList("skills").stream()
                .map(text -> PresetJson.skill(text, skills)
                        .orElseThrow(() -> new IllegalArgumentException("'skills' names unknown skill '" + text + "'")))
                .map(Skill::id)
                .distinct()
                .toList();
    }

    private static List<KitItem> parseKit(LenientJson json, Predicate<ResourceLocation> itemExists) {
        List<KitItem> kit = new ArrayList<>();
        for (LenientJson entry : json.objectList("kit")) {
            Optional<ResourceLocation> item =
                    entry.optionalString("item").map(ResourceLocation::tryParse).filter(itemExists);
            OptionalInt count = entry.optionalInt("count");
            if (item.isEmpty() || count.orElse(1) < 1) {
                entry.warn("skipped: needs a registered 'item' and a 'count' of at least 1");
                continue;
            }
            kit.add(new KitItem(item.get(), count.orElse(1)));
        }
        return kit;
    }
}
