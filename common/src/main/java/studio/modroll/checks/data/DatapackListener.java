package studio.modroll.checks.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.bonus.BonusSource;
import studio.modroll.checks.bonus.BonusSources;
import studio.modroll.checks.trait.Trait;
import studio.modroll.checks.trait.TraitEffect;
import studio.modroll.checks.trait.Traits;
import studio.modroll.checks.trigger.CheckTrigger;
import studio.modroll.checks.trigger.CheckTriggers;

/**
 * Loads every file of one datapack directory into its store, replacing what the last reload loaded; a file
 * that fails to parse is logged and skipped. Every listener but {@link #skills()} resolves skill keys against
 * the current {@link SkillStore}, so it must run after the skill listener.
 */
public class DatapackListener<T> extends SimpleJsonResourceReloadListener {

    private final String kind;
    private final BiFunction<ResourceLocation, JsonObject, T> parser;
    private final Consumer<Map<ResourceLocation, T>> store;

    public DatapackListener(
            String directory,
            String kind,
            BiFunction<ResourceLocation, JsonObject, T> parser,
            Consumer<Map<ResourceLocation, T>> store) {
        super(new Gson(), directory);
        this.kind = kind;
        this.parser = parser;
        this.store = store;
    }

    public static DatapackListener<Skill> skills() {
        return new DatapackListener<>(
                "checks/skill",
                "skill",
                (id, json) -> SkillParser.parse(id, json, Checks.LOG::warn),
                SkillStore::setSkills);
    }

    public static DatapackListener<EntityScoreProfile> entityProfiles() {
        return new DatapackListener<>(
                "checks/entity_profile",
                "entity score profile",
                (id, json) -> EntityScoreProfile.parse(id, json, SkillStore::find, Checks.LOG::warn),
                EntityScoreProfileStore::setProfiles);
    }

    public static DatapackListener<BonusSource> bonusSources() {
        return new DatapackListener<>(
                "checks/bonus",
                "bonus source",
                (id, json) -> BonusSource.parse(
                        id,
                        json,
                        SkillStore::find,
                        BuiltInRegistries.ITEM::containsKey,
                        BuiltInRegistries.MOB_EFFECT::containsKey,
                        Checks.LOG::warn),
                BonusSources::set);
    }

    public static DatapackListener<CheckTrigger> triggers() {
        return new DatapackListener<>(
                "checks/trigger",
                "trigger",
                (id, json) -> CheckTrigger.parse(
                        id, json, SkillStore::find, DatapackListener::targetExists, Checks.LOG::warn),
                CheckTriggers::set);
    }

    public static DatapackListener<Trait> traits() {
        TraitEffect.Lookups lookups = new TraitEffect.Lookups(
                SkillStore::find, BuiltInRegistries.ENTITY_TYPE::containsKey, BuiltInRegistries.ATTRIBUTE::containsKey);
        return new DatapackListener<>(
                "checks/trait", "trait", (id, json) -> Trait.parse(id, json, lookups, Checks.LOG::warn), Traits::set);
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, T> loaded = new HashMap<>();
        files.forEach((id, element) -> {
            try {
                if (!element.isJsonObject()) {
                    throw new IllegalArgumentException("root must be a JSON object");
                }
                loaded.put(id, parser.apply(id, element.getAsJsonObject()));
            } catch (RuntimeException e) {
                Checks.LOG.error("Skipping bad {} {}: {}", kind, id, e.getMessage());
            }
        });
        Checks.LOG.info("Loaded {} {} files", loaded.size(), kind);
        store.accept(loaded);
    }

    private static Predicate<ResourceLocation> targetExists(CheckTrigger.Interaction on) {
        return switch (on) {
            case USE_BLOCK -> BuiltInRegistries.BLOCK::containsKey;
            case USE_ENTITY -> BuiltInRegistries.ENTITY_TYPE::containsKey;
            case USE_ITEM -> BuiltInRegistries.ITEM::containsKey;
        };
    }
}
