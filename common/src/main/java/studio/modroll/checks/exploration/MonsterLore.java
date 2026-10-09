package studio.modroll.checks.exploration;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import studio.modroll.checks.Checks;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;

/**
 * Nature, Arcana, Religion and History: the first time a player in survival or adventure sees a mob type
 * within range, in line of sight, they roll the skill that covers it. A made check shows a hint about that
 * mob type (see {@link MobHints}) and is never rolled for again; a failed one may be tried again after the
 * retry time. Each look rolls for the nearest such mob only.
 */
public final class MonsterLore {

    private record Lore(ResourceLocation skill, TagKey<EntityType<?>> mobs) {}

    private static final ResourceLocation RELIGION = Checks.id("religion");
    private static final ResourceLocation ARCANA = Checks.id("arcana");
    private static final ResourceLocation NATURE = Checks.id("nature");
    private static final ResourceLocation HISTORY = Checks.id("history");
    private static final List<Lore> LORE = List.of(lore(RELIGION), lore(ARCANA), lore(NATURE), lore(HISTORY));

    private static final Recollection RECOLLECTION = new Recollection(LoreStore.Topic.MOBS, "monster_lore");

    private MonsterLore() {}

    /** Called once per server tick; looks around for every player once per interval. */
    public static void tick(MinecraftServer server) {
        ScoresConfig.MonsterLoreSettings settings =
                ScoresRuntime.config().exploration().monsterLore();
        if (server.getTickCount() % settings.intervalTicks() != 0) {
            return;
        }
        server.getPlayerList().getPlayers().forEach(MonsterLore::lookAround);
        RECOLLECTION.forgetWaited(server.getTickCount());
    }

    /** Rolls for the nearest mob {@code player} sees whose type they could roll for now. */
    public static void lookAround(ServerPlayer player) {
        ScoresConfig.MonsterLoreSettings settings =
                ScoresRuntime.config().exploration().monsterLore();
        if (!settings.enabled() || !ExplorationChecks.rollsFor(player)) {
            return;
        }
        nearestNew(player, settings.range())
                .ifPresent(mob -> RECOLLECTION.recall(
                        player, id(mob), skill(mob), settings.dc(), settings.retryTicks(), MobHints.hint(mob)));
    }

    /**
     * The skill whose {@code checks:lore/<skill>} entity type tag holds the mob's type, the first of Religion,
     * Arcana, Nature and History; for a type in none, {@link #fallbackSkill}.
     */
    public static ResourceLocation skill(Mob mob) {
        EntityType<?> type = mob.getType();
        return LORE.stream()
                .filter(lore -> type.is(lore.mobs()))
                .map(Lore::skill)
                .findFirst()
                .orElseGet(() -> fallbackSkill(type.is(EntityTypeTags.UNDEAD), wild(mob)));
    }

    /** Religion for the undead, Nature for creatures of the wild, Arcana for everything else. */
    static ResourceLocation fallbackSkill(boolean undead, boolean wild) {
        if (undead) {
            return RELIGION;
        }
        return wild ? NATURE : ARCANA;
    }

    public static void clear() {
        RECOLLECTION.clear();
    }

    /** Animals, water animals, arthropods and aquatic mobs. */
    private static boolean wild(Mob mob) {
        return mob instanceof Animal
                || mob instanceof WaterAnimal
                || mob.getType().is(EntityTypeTags.ARTHROPOD)
                || mob.getType().is(EntityTypeTags.AQUATIC);
    }

    private static Optional<Mob> nearestNew(ServerPlayer player, double range) {
        return player
                .serverLevel()
                .getEntitiesOfClass(
                        Mob.class,
                        player.getBoundingBox().inflate(range),
                        mob -> mob.isAlive()
                                && !mob.isInvisible()
                                && player.distanceToSqr(mob) <= range * range
                                && RECOLLECTION.ready(player, id(mob))
                                && player.hasLineOfSight(mob))
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr));
    }

    private static ResourceLocation id(Mob mob) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
    }

    private static Lore lore(ResourceLocation skill) {
        return new Lore(skill, TagKey.create(Registries.ENTITY_TYPE, skill.withPrefix("lore/")));
    }
}
