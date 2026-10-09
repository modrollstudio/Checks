package studio.modroll.checks.data;

import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.Skill;

/** The loaded skills, swapped atomically on every reload like {@link EntityScoreProfileStore}. */
public final class SkillStore {

    private static volatile Map<ResourceLocation, Skill> skills = Map.of();

    private SkillStore() {}

    public static void setSkills(Map<ResourceLocation, Skill> loaded) {
        skills = Map.copyOf(loaded);
    }

    public static void clear() {
        setSkills(Map.of());
    }

    public static Map<ResourceLocation, Skill> skills() {
        return skills;
    }

    /**
     * A namespace-less id ({@code "stealth"}) parses as {@code minecraft:}, so without a {@code
     * minecraft:} skill of that name it falls back to the {@code checks:} one.
     */
    public static Optional<Skill> find(ResourceLocation id) {
        Map<ResourceLocation, Skill> current = skills;
        Skill exact = current.get(id);
        if (exact != null || !id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
            return Optional.ofNullable(exact);
        }
        return Optional.ofNullable(current.get(Checks.id(id.getPath())));
    }
}
