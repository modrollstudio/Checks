package studio.modroll.checks.trait;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.api.CheckKind;
import studio.modroll.checks.api.Stat;
import studio.modroll.checks.bonus.BonusPart;
import studio.modroll.checks.data.MatchEntry;
import studio.modroll.checks.death.DeathStories;
import studio.modroll.checks.death.DeathStory;
import studio.modroll.checks.save.VanillaSave;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.text.FallbackText;
import studio.modroll.critfall.api.dice.RollMode;

public final class TraitHooks {

    private static final float LAST_HIT_POINT = 1f;
    private static final int NATURAL_ONE = 1;

    private TraitHooks() {}

    public static float incomingDamage(LivingEntity entity, DamageSource source, float amount) {
        Optional<ResourceLocation> type = source.typeHolder().unwrapKey().map(ResourceKey::location);
        double multiplier = Traits.effects(entity, TraitEffect.DamageResistance.class).stream()
                .filter(resistance -> resistance.damageTypes().stream().anyMatch(match -> matches(match, type, source)))
                .mapToDouble(TraitEffect.DamageResistance::multiplier)
                .min()
                .orElse(1.0);
        return (float) (amount * multiplier);
    }

    private static boolean matches(MatchEntry match, Optional<ResourceLocation> type, DamageSource source) {
        return type.map(id -> match.matches(id, tag -> source.is(TagKey.create(Registries.DAMAGE_TYPE, tag))))
                .orElse(false);
    }

    public static boolean survivesDeath(LivingEntity entity, DamageSource source) {
        Optional<ResourceLocation> trait = readyLastStand(entity, source);
        trait.ifPresent(id -> {
            ServerPlayer player = (ServerPlayer) entity;
            spend(player, id);
            DeathStories.record(player, DeathStory.LAST_STAND_SPENT);
            player.setHealth(LAST_HIT_POINT);
            player.displayClientMessage(FallbackText.of("checks.trait.last_stand", SheetText.traitName(id)), true);
        });
        return trait.isPresent();
    }

    public static boolean lastStandReady(LivingEntity entity, DamageSource source) {
        return readyLastStand(entity, source).isPresent();
    }

    private static Optional<ResourceLocation> readyLastStand(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayer player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return Optional.empty();
        }
        return Traits.granted(player, TraitEffect.LastStand.class).stream()
                .map(Traits.Granted::trait)
                .filter(id -> ready(player, id))
                .findFirst();
    }

    public static boolean canTarget(LivingEntity attacker, LivingEntity target) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType());
        return Traits.effects(target, TraitEffect.IgnoredBy.class).stream()
                .noneMatch(ignored -> ignored.entities().stream()
                        .anyMatch(match -> match.matches(
                                type, tag -> attacker.getType().is(TagKey.create(Registries.ENTITY_TYPE, tag)))));
    }

    public static RollMode vanillaSaveMode(LivingEntity entity, VanillaSave save) {
        boolean advantage = Traits.effects(entity, TraitEffect.VanillaSaveAdvantage.class).stream()
                .anyMatch(effect -> effect.against().contains(save));
        return advantage ? RollMode.ADVANTAGE : RollMode.NORMAL;
    }

    public static List<BonusPart> rollBonuses(LivingEntity entity, CheckKind kind, Stat stat) {
        return Traits.granted(entity, TraitEffect.RollBonus.class).stream()
                .filter(granted -> granted.effect().targets().appliesTo(kind, stat))
                .map(granted -> new BonusPart(
                        granted.trait(),
                        granted.effect().bonus(),
                        granted.effect().mode()))
                .toList();
    }

    public static Optional<ResourceLocation> reroll(LivingEntity entity, int natural, Optional<Boolean> failed) {
        if (!(entity instanceof ServerPlayer player)) {
            return Optional.empty();
        }
        List<Traits.Granted<TraitEffect.Reroll>> rerolls = Traits.granted(player, TraitEffect.Reroll.class).stream()
                .filter(granted -> applies(granted.effect().on(), natural, failed))
                .toList();
        Optional<ResourceLocation> trait = rerolls.stream()
                .filter(granted -> !granted.effect().daily())
                .map(Traits.Granted::trait)
                .findFirst()
                .or(() -> rerolls.stream()
                        .map(Traits.Granted::trait)
                        .filter(id -> ready(player, id))
                        .findFirst()
                        .map(id -> {
                            spend(player, id);
                            return id;
                        }));
        trait.ifPresent(
                id -> player.sendSystemMessage(FallbackText.of("checks.trait.reroll", SheetText.traitName(id))));
        return trait;
    }

    public static boolean rerollsOnes(LivingEntity entity) {
        return Traits.effects(entity, TraitEffect.Reroll.class).stream()
                .anyMatch(reroll -> reroll.on() == TraitEffect.When.NATURAL_1 && !reroll.daily());
    }

    private static boolean applies(TraitEffect.When on, int natural, Optional<Boolean> failed) {
        return switch (on) {
            case NATURAL_1 -> natural == NATURAL_ONE;
            case FAILURE -> failed.orElse(false);
        };
    }

    private static boolean ready(ServerPlayer player, ResourceLocation trait) {
        return TraitUses.ready(
                TraitUses.get(player.server).lastUse(player.getUUID(), trait),
                player.server.overworld().getGameTime(),
                ScoresRuntime.config().traits().rechargeTicks());
    }

    private static void spend(ServerPlayer player, ResourceLocation trait) {
        TraitUses.get(player.server)
                .use(player.getUUID(), trait, player.server.overworld().getGameTime());
    }
}
