package studio.modroll.checks.data;

import com.google.gson.JsonObject;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;

/** Parses one {@code data/<ns>/checks/skill/*.json} file into a {@link Skill}. */
public final class SkillParser {

    public static final int FORMAT_VERSION = 1;

    private SkillParser() {}

    public static Skill parse(ResourceLocation id, JsonObject json, Consumer<String> warn) {
        LenientJson j = new LenientJson(json, "skill " + id, warn);
        j.checkFormatVersion(FORMAT_VERSION);
        String abilityId = j.optionalString("ability")
                .orElseThrow(
                        () -> new IllegalArgumentException("'ability' must name one of str, dex, con, int, wis, cha"));
        Ability ability = Ability.byId(abilityId)
                .orElseThrow(() -> new IllegalArgumentException("unknown ability '" + abilityId + "'"));
        j.finish();
        return new Skill(id, ability);
    }
}
