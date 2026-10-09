package studio.modroll.checks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import studio.modroll.checks.body.PlayerBodies;
import studio.modroll.checks.bonus.BonusSources;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.data.EntityScoreProfileStore;
import studio.modroll.checks.data.SkillStore;
import studio.modroll.checks.death.DeathStories;
import studio.modroll.checks.exploration.ChestSearches;
import studio.modroll.checks.exploration.Climbing;
import studio.modroll.checks.exploration.Cobwebs;
import studio.modroll.checks.exploration.Leaps;
import studio.modroll.checks.exploration.MonsterLore;
import studio.modroll.checks.exploration.StructureLore;
import studio.modroll.checks.exploration.Tripwires;
import studio.modroll.checks.preset.Presets;
import studio.modroll.checks.save.VanillaSaves;
import studio.modroll.checks.social.GolemAlarms;
import studio.modroll.checks.social.Insight;
import studio.modroll.checks.social.Performances;
import studio.modroll.checks.social.SocialFeel;
import studio.modroll.checks.social.SocialMobs;
import studio.modroll.checks.social.SocialPrices;
import studio.modroll.checks.social.Socials;
import studio.modroll.checks.trait.Darkvision;
import studio.modroll.checks.trait.Traits;
import studio.modroll.checks.trigger.CheckTriggers;

/** The server events both loaders hand on unchanged. */
public final class ServerHooks {

    private ServerHooks() {}

    public static void onServerTick(MinecraftServer server) {
        PlayerBodies.tick(server);
        Darkvision.tick(server);
        SocialFeel.tick(server);
        GolemAlarms.tick(server);
        SocialMobs.tick(server);
        Insight.tick(server);
        Tripwires.tick(server);
        Cobwebs.tick(server);
        Climbing.tick(server);
        MonsterLore.tick(server);
        StructureLore.tick(server);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        PlayerBodies.onLogin(player);
        Darkvision.onLogin(player);
        Climbing.onLogin(player);
        CharacterCreation.onJoin(player);
    }

    /** The first use that acts consumes the click: a check trigger, then a tripwire, then a chest search. */
    public static InteractionResult onUseBlock(ServerPlayer player, InteractionHand hand, BlockPos pos) {
        InteractionResult result = CheckTriggers.onUseBlock(player, hand, pos);
        if (!result.consumesAction()) {
            result = Tripwires.onUseBlock(player, hand, pos);
        }
        return result.consumesAction() ? result : ChestSearches.onUseBlock(player, hand, pos);
    }

    /** A check trigger first, then the social menu. */
    public static InteractionResult onUseEntity(ServerPlayer player, InteractionHand hand, Entity entity) {
        InteractionResult result = CheckTriggers.onUseEntity(player, hand, entity);
        return result.consumesAction() ? result : Socials.onUseEntity(player, hand, entity);
    }

    /** The snapshots are static: drop them so one world never sees another's datapacks in a shared JVM. */
    public static void onServerStopping() {
        EntityScoreProfileStore.clear();
        SkillStore.clear();
        Presets.clear();
        BonusSources.clear();
        CheckTriggers.clear();
        VanillaSaves.clear();
        Traits.clear();
        Darkvision.clear();
        PlayerBodies.clear();
        SocialPrices.clear();
        SocialFeel.clear();
        GolemAlarms.clear();
        SocialMobs.clear();
        Insight.clear();
        Performances.clear();
        DeathStories.clear();
        Leaps.clear();
        Tripwires.clear();
        Cobwebs.clear();
        Climbing.clear();
        MonsterLore.clear();
        StructureLore.clear();
    }
}
