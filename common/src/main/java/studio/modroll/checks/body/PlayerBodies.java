package studio.modroll.checks.body;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import studio.modroll.checks.Checks;
import studio.modroll.checks.trait.TraitEffect;
import studio.modroll.checks.trait.Traits;

/**
 * Keeps each player's Checks attribute modifiers in step with their scores, character and the config.
 * The modifiers are transient: never saved with the player, so a world without Checks loads players
 * with vanilla attributes. They are applied on login, respawn and dimension change, and synced every
 * server tick, so any change (a command, an improvement, a {@code /reload}) shows within a tick; a
 * modifier whose switch is off is removed on the next sync. Health above the vanilla max is kept across
 * a relog through {@link HealthStore}.
 */
public final class PlayerBodies {

    public static final ResourceLocation HEALTH = Checks.id("con_health");
    public static final ResourceLocation SIZE = Checks.id("size");
    public static final ResourceLocation KNOCKBACK = Checks.id("str_knockback");
    public static final ResourceLocation MINING_SPEED = Checks.id("str_mining_speed");
    public static final ResourceLocation BREATH = Checks.id("con_breath");
    public static final ResourceLocation TRAIT_HEALTH = Checks.id("trait_health");

    private record Modifier(
            Holder<Attribute> attribute,
            ResourceLocation id,
            AttributeModifier.Operation operation,
            ToDoubleFunction<ServerPlayer> amount) {}

    private static final List<Modifier> MODIFIERS = List.of(
            new Modifier(
                    Attributes.MAX_HEALTH, HEALTH, AttributeModifier.Operation.ADD_VALUE, player -> BodyValues.health(
                                    player)
                            .map(BodyValues.HealthBonus::total)
                            .orElse(0.0)),
            new Modifier(
                    Attributes.SCALE, SIZE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, player -> BodyValues.size(
                                    player)
                            .map(option -> option.scale() - 1.0)
                            .orElse(0.0)),
            new Modifier(
                    Attributes.ATTACK_KNOCKBACK,
                    KNOCKBACK,
                    AttributeModifier.Operation.ADD_VALUE,
                    player -> BodyValues.extra(player, Extra.KNOCKBACK)),
            new Modifier(
                    Attributes.BLOCK_BREAK_SPEED,
                    MINING_SPEED,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    player -> BodyValues.extra(player, Extra.MINING_SPEED)),
            new Modifier(
                    Attributes.OXYGEN_BONUS,
                    BREATH,
                    AttributeModifier.Operation.ADD_VALUE,
                    player -> BodyValues.extra(player, Extra.BREATH)),
            new Modifier(
                    Attributes.MAX_HEALTH,
                    TRAIT_HEALTH,
                    AttributeModifier.Operation.ADD_VALUE,
                    BodyValues::traitHealth));

    /** A trait attribute modifier on a player. */
    private record Applied(Holder<Attribute> attribute, ResourceLocation id) {}

    /** The trait attribute modifiers each player was given, so one whose trait went away is removed. */
    private static final Map<UUID, Set<Applied>> TRAIT_ATTRIBUTES = new ConcurrentHashMap<>();

    private PlayerBodies() {}

    public static void tick(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(PlayerBodies::sync);
    }

    /** Adds, updates or removes each modifier, then keeps current health within the new max. */
    public static void sync(ServerPlayer player) {
        for (Modifier modifier : MODIFIERS) {
            apply(
                    player,
                    modifier.attribute(),
                    modifier.id(),
                    modifier.operation(),
                    modifier.amount().applyAsDouble(player));
        }
        syncTraitAttributes(player);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** Reapplies the modifiers, then gives back the health vanilla capped while loading the player. */
    public static void onLogin(ServerPlayer player) {
        float loaded = player.getHealth();
        float loadedMax = player.getMaxHealth();
        sync(player);
        HealthStore.get(player.server)
                .health(player.getUUID())
                .flatMap(stored -> BodyRules.restoredHealth(stored, loaded, loadedMax, player.getMaxHealth()))
                .ifPresent(player::setHealth);
    }

    /** Called wherever vanilla saves the player (autosave, logout, shutdown), so both saves agree. */
    public static void onSave(ServerPlayer player) {
        HealthStore.get(player.server).set(player.getUUID(), player.getHealth());
    }

    /**
     * Vanilla sets a respawned player's health before any modifier is back, so it would cap at the
     * unmodified max. Synced first, a player respawning from death starts at full health and one returning
     * from the End keeps their health.
     */
    public static void onRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        sync(newPlayer);
        newPlayer.setHealth(alive ? oldPlayer.getHealth() : newPlayer.getMaxHealth());
    }

    /** One modifier per trait attribute effect, under an id naming the trait and the effect's place in it. */
    private static void syncTraitAttributes(ServerPlayer player) {
        Set<Applied> wanted = new HashSet<>();
        for (Traits.Granted<TraitEffect.Attribute> granted : Traits.granted(player, TraitEffect.Attribute.class)) {
            TraitEffect.Attribute effect = granted.effect();
            BuiltInRegistries.ATTRIBUTE.getHolder(effect.attribute()).ifPresent(attribute -> {
                ResourceLocation id = traitModifierId(granted);
                apply(player, attribute, id, effect.operation(), effect.amount());
                wanted.add(new Applied(attribute, id));
            });
        }
        for (Applied stale : TRAIT_ATTRIBUTES.getOrDefault(player.getUUID(), Set.of())) {
            if (!wanted.contains(stale)) {
                apply(player, stale.attribute(), stale.id(), AttributeModifier.Operation.ADD_VALUE, 0.0);
            }
        }
        TRAIT_ATTRIBUTES.put(player.getUUID(), wanted);
    }

    private static ResourceLocation traitModifierId(Traits.Granted<?> granted) {
        ResourceLocation trait = granted.trait();
        return Checks.id("trait/" + trait.getNamespace() + "/" + trait.getPath() + "/" + granted.index());
    }

    /** Forgets which trait modifiers each player had; the modifiers themselves are never saved. */
    public static void clear() {
        TRAIT_ATTRIBUTES.clear();
    }

    private static void apply(
            ServerPlayer player,
            Holder<Attribute> attribute,
            ResourceLocation id,
            AttributeModifier.Operation operation,
            double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        boolean upToDate =
                current == null ? amount == 0.0 : current.amount() == amount && current.operation() == operation;
        if (upToDate) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }
}
