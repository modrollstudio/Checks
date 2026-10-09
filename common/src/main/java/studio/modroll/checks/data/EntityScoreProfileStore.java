package studio.modroll.checks.data;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/**
 * The loaded profiles as an immutable snapshot behind a volatile field, since {@code /reload} swaps it
 * while the server thread reads. Resolution is cached per entity type and the cache cleared on swap.
 */
public final class EntityScoreProfileStore {

    private static volatile Map<ResourceLocation, EntityScoreProfile> profiles = Map.of();
    private static final Map<ResourceLocation, Optional<EntityScoreProfile>> cache = new ConcurrentHashMap<>();

    private EntityScoreProfileStore() {}

    public static void setProfiles(Map<ResourceLocation, EntityScoreProfile> loaded) {
        profiles = Map.copyOf(loaded);
        cache.clear();
    }

    public static void clear() {
        setProfiles(Map.of());
    }

    public static Map<ResourceLocation, EntityScoreProfile> profiles() {
        return profiles;
    }

    public static Optional<EntityScoreProfile> findEntityProfile(
            ResourceLocation entityTypeId, Predicate<ResourceLocation> tagTest) {
        return cache.computeIfAbsent(entityTypeId, id -> resolve(profiles.values(), id, tagTest));
    }

    /**
     * Highest priority wins; among equal priority the most specific matching entry wins (exact id >
     * tag > namespace); a remaining tie goes to the lexicographically smaller file id so resolution
     * is deterministic across reloads.
     */
    static Optional<EntityScoreProfile> resolve(
            Collection<EntityScoreProfile> candidates, ResourceLocation id, Predicate<ResourceLocation> tagTest) {
        EntityScoreProfile best = null;
        int bestSpecificity = 0;
        for (EntityScoreProfile profile : candidates) {
            int specificity = 0;
            for (MatchEntry entry : profile.matches()) {
                if (entry.specificity() > specificity && entry.matches(id, tagTest)) {
                    specificity = entry.specificity();
                }
            }
            if (specificity == 0) {
                continue;
            }
            if (best == null || wins(profile, specificity, best, bestSpecificity)) {
                best = profile;
                bestSpecificity = specificity;
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean wins(
            EntityScoreProfile candidate, int specificity, EntityScoreProfile best, int bestSpecificity) {
        if (candidate.priority() != best.priority()) {
            return candidate.priority() > best.priority();
        }
        if (specificity != bestSpecificity) {
            return specificity > bestSpecificity;
        }
        return candidate.id().compareTo(best.id()) < 0;
    }
}
