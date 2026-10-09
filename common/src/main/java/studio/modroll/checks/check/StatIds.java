package studio.modroll.checks.check;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.api.Stat;

/** Looks up a stat by the id commands and datapacks use: an ability id ({@code dex}) or a skill id. */
public final class StatIds {

    private StatIds() {}

    /** Ability ids carry no namespace, so they parse as {@code minecraft:<id>}. */
    public static Optional<Stat> find(ResourceLocation id, Function<ResourceLocation, Optional<Skill>> skills) {
        if (id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
            Optional<Ability> ability = Ability.byId(id.getPath());
            if (ability.isPresent()) {
                return Optional.of(ability.get());
            }
        }
        return skills.apply(id).map(Stat.class::cast);
    }
}
