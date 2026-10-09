package studio.modroll.checks.score;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.BiFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.creation.CharacterBuild;
import studio.modroll.checks.creation.CharacterStore;
import studio.modroll.checks.data.EntityScoreProfile;
import studio.modroll.checks.data.EntityScoreProfileStore;
import studio.modroll.checks.level.LevelStore;
import studio.modroll.checks.level.PlayerLevel;
import studio.modroll.checks.proficiency.Proficiencies;
import studio.modroll.checks.skill.Skills;

/** Resolves an entity's live values for {@link studio.modroll.checks.api.ChecksApi}; server-side entities only. */
public final class ScoreService {

    private ScoreService() {}

    public static int abilityScore(LivingEntity entity, Ability ability) {
        return ScoreResolver.resolveScore(
                ability,
                profileFor(entity),
                entity instanceof Player,
                stored(entity, (store, uuid) -> store.getScore(uuid, ability), OptionalInt.empty()),
                characterOf(entity),
                levelOf(entity),
                attributesOf(entity),
                ScoresRuntime.config());
    }

    public static int modifier(LivingEntity entity, Ability ability) {
        return Abilities.modifier(abilityScore(entity, ability));
    }

    public static int skillBonus(LivingEntity entity, Skill skill) {
        return ScoreResolver.resolveSkillBonus(
                skill,
                profileFor(entity),
                stored(entity, (store, uuid) -> store.getSkillBonus(uuid, skill.id()), OptionalInt.empty()),
                ScoresRuntime.config());
    }

    public static int proficiencyBonus(LivingEntity entity) {
        return ScoreResolver.resolveProficiencyBonus(
                profileFor(entity),
                stored(entity, (store, uuid) -> store.getProficiencyBonus(uuid), OptionalInt.empty()),
                levelOf(entity),
                ScoresRuntime.config());
    }

    public static Proficiency skillProficiency(LivingEntity entity, Skill skill) {
        return ScoreResolver.resolveSkillProficiency(
                skill,
                profileFor(entity),
                stored(entity, (store, uuid) -> store.getSkillProficiency(uuid, skill.id()), Optional.empty()),
                characterOf(entity),
                ScoresRuntime.config());
    }

    public static Proficiency saveProficiency(LivingEntity entity, Ability ability) {
        return ScoreResolver.resolveSaveProficiency(
                ability,
                profileFor(entity),
                stored(entity, (store, uuid) -> store.getSaveProficiency(uuid, ability), Optional.empty()),
                characterOf(entity),
                ScoresRuntime.config());
    }

    public static int skillModifier(LivingEntity entity, Skill skill) {
        return Skills.modifier(
                modifier(entity, skill.ability()),
                skillProficiency(entity, skill),
                proficiencyBonus(entity),
                skillBonus(entity, skill));
    }

    public static int saveModifier(LivingEntity entity, Ability ability) {
        return Proficiencies.saveModifier(
                modifier(entity, ability), saveProficiency(entity, ability), proficiencyBonus(entity));
    }

    public static int passiveScore(LivingEntity entity, Skill skill) {
        return Skills.passive(skillModifier(entity, skill));
    }

    /** Whether a datapack profile applies to the entity; never while profiles are disabled. */
    public static boolean hasProfile(LivingEntity entity) {
        return ScoresRuntime.config().profilesEnabled() && profileFor(entity).isPresent();
    }

    private static Optional<EntityScoreProfile> profileFor(LivingEntity entity) {
        return EntityScoreProfileStore.findEntityProfile(
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()),
                tagId -> entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, tagId)));
    }

    private static <T> T stored(LivingEntity entity, BiFunction<PlayerScoreStore, UUID, T> read, T absent) {
        return playerServer(entity)
                .map(server -> read.apply(PlayerScoreStore.get(server), entity.getUUID()))
                .orElse(absent);
    }

    private static Optional<CharacterBuild> characterOf(LivingEntity entity) {
        return playerServer(entity)
                .flatMap(server ->
                        CharacterStore.get(server).character(entity.getUUID()).build());
    }

    private static Optional<PlayerLevel> levelOf(LivingEntity entity) {
        return playerServer(entity).map(server -> LevelStore.get(server).level(entity.getUUID()));
    }

    /** Empty for non-players and entities with no server. */
    private static Optional<MinecraftServer> playerServer(LivingEntity entity) {
        if (!(entity instanceof Player) || entity.getServer() == null) {
            return Optional.empty();
        }
        return Optional.of(entity.getServer());
    }

    private static AttributeValues attributesOf(LivingEntity entity) {
        return new AttributeValues(
                attribute(entity, Attributes.ATTACK_DAMAGE),
                attribute(entity, Attributes.MOVEMENT_SPEED),
                attribute(entity, Attributes.MAX_HEALTH));
    }

    private static double attribute(LivingEntity entity, Holder<Attribute> attribute) {
        return entity.getAttributes().hasAttribute(attribute) ? entity.getAttributeValue(attribute) : 0.0;
    }
}
