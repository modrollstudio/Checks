package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.profiles;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.saveWorldData;
import static studio.modroll.checks.gametest.ScenarioSupport.savedPlayerStore;
import static studio.modroll.checks.gametest.ScenarioSupport.spawnCalm;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withProfiles;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.data.EntityScoreProfile;
import studio.modroll.checks.data.EntityScoreProfileStore;
import studio.modroll.checks.score.ScoreService;
import studio.modroll.checks.score.ScoresRuntime;

/** GameTest bodies for ability scores on live entities. */
public final class ScoreScenarios {

    private ScoreScenarios() {}

    /** A datapack value beats derivation for that ability only; the rest stay derived. */
    public static void profiledEntityBeatsDerivedScores(GameTestHelper helper) {
        Mob zombie = spawnCalm(helper, EntityType.ZOMBIE);
        int derivedStr = unprofiledScore(zombie, Ability.STRENGTH);
        int derivedDex = unprofiledScore(zombie, Ability.DEXTERITY);
        int pinnedStr = derivedStr + 5;

        Map<ResourceLocation, EntityScoreProfile> zombieProfile =
                profiles("{\"matches\": [\"minecraft:zombie\"], \"abilities\": {\"str\": " + pinnedStr + "}}");
        int profiledStr = withProfiles(zombieProfile, () -> ScoreService.abilityScore(zombie, Ability.STRENGTH));
        int profiledDex = withProfiles(zombieProfile, () -> ScoreService.abilityScore(zombie, Ability.DEXTERITY));

        expect(helper, profiledStr == pinnedStr, "profiled zombie STR must be " + pinnedStr + ", was " + profiledStr);
        expect(
                helper,
                profiledDex == derivedDex,
                "unpinned DEX must stay derived " + derivedDex + ", was " + profiledDex);
        helper.succeed();
    }

    /** A tag-matched profile applies to every member; an entity outside the tag stays derived. */
    public static void tagProfileAppliesOnlyToTaggedEntities(GameTestHelper helper) {
        Mob skeleton = spawnCalm(helper, EntityType.SKELETON);
        Mob zombie = spawnCalm(helper, EntityType.ZOMBIE);
        int zombieDex = unprofiledScore(zombie, Ability.DEXTERITY);

        Map<ResourceLocation, EntityScoreProfile> skeletons =
                profiles("{\"matches\": [\"#minecraft:skeletons\"], \"abilities\": {\"dex\": 23}}");
        int skeletonDex = withProfiles(skeletons, () -> ScoreService.abilityScore(skeleton, Ability.DEXTERITY));
        int unprofiledDex = withProfiles(skeletons, () -> ScoreService.abilityScore(zombie, Ability.DEXTERITY));

        expect(helper, skeletonDex == 23, "tag-profiled skeleton DEX must be 23, was " + skeletonDex);
        expect(helper, unprofiledDex == zombieDex, "unprofiled zombie DEX must stay derived, was " + unprofiledDex);
        helper.succeed();
    }

    /** Unprofiled mobs get in-range scores that follow their attributes (golem stronger than chicken). */
    public static void unprofiledMobsGetVariedDerivedScores(GameTestHelper helper) {
        Mob chicken = spawnCalm(helper, EntityType.CHICKEN);
        Mob golem = spawnCalm(helper, EntityType.IRON_GOLEM);
        for (Ability ability : Ability.values()) {
            expectInRange(helper, "chicken", ability, unprofiledScore(chicken, ability));
            expectInRange(helper, "iron golem", ability, unprofiledScore(golem, ability));
        }
        expect(
                helper,
                unprofiledScore(golem, Ability.STRENGTH) > unprofiledScore(chicken, Ability.STRENGTH),
                "iron golem must derive a higher STR than a chicken");
        expect(
                helper,
                unprofiledScore(golem, Ability.CONSTITUTION) > unprofiledScore(chicken, Ability.CONSTITUTION),
                "iron golem must derive a higher CON than a chicken");
        helper.succeed();
    }

    /** Player order: config default, then a datapack profile, then {@code /checks set} wins. */
    public static void playerScoreResolutionOrder(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-order"));
        try {
            int configDefault = ScoresRuntime.config().playerDefault(Ability.STRENGTH);
            int defaulted = unprofiledScore(player, Ability.STRENGTH);
            expect(helper, defaulted == configDefault, "player STR must start at the config default, was " + defaulted);

            Map<ResourceLocation, EntityScoreProfile> playerProfile =
                    profiles("{\"matches\": [\"minecraft:player\"], \"abilities\": {\"str\": 14}}");
            int profiled = withProfiles(playerProfile, () -> ScoreService.abilityScore(player, Ability.STRENGTH));
            expect(helper, profiled == 14, "a player profile must override the default, was " + profiled);

            run(helper, "checks set " + player.getScoreboardName() + " str 17");
            int commanded = withProfiles(playerProfile, () -> ScoreService.abilityScore(player, Ability.STRENGTH));
            expect(helper, commanded == 17, "/checks set must beat the profile, was " + commanded);
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A set score survives logout and a fresh login, and reaches the world save on disk. NeoForge
     * writes SavedData on an IO worker, so the disk read is polled.
     */
    public static void playerScoreSurvivesRelogAndSave(GameTestHelper helper) {
        GameProfile profile = newProfile("checks-relog");
        ServerPlayer firstSession = login(helper, profile);
        run(helper, "checks set " + firstSession.getScoreboardName() + " wis 19");
        logout(helper, firstSession);

        ServerPlayer secondSession = login(helper, profile);
        try {
            int afterRelog = ScoreService.abilityScore(secondSession, Ability.WISDOM);
            expect(helper, afterRelog == 19, "WIS must still be 19 after relog, was " + afterRelog);
            int reported = run(helper, "checks get " + secondSession.getScoreboardName() + " wis");
            expect(helper, reported == 19, "/checks get must report WIS 19, reported " + reported);
        } finally {
            logout(helper, secondSession);
        }

        saveWorldData(helper);
        OptionalInt fromDisk = savedPlayerStore(helper).getScore(profile.getId(), Ability.WISDOM);
        expect(helper, fromDisk.equals(OptionalInt.of(19)), "the world save must hold WIS 19, held " + fromDisk);

        helper.succeed();
    }

    /** A real {@code /reload} re-runs this loader's profile listener and swaps the store. */
    public static void reloadReplacesProfiles(GameTestHelper helper) {
        Map<ResourceLocation, EntityScoreProfile> sentinel =
                profiles("{\"matches\": [\"minecraft:zombie\"], \"abilities\": {\"str\": 30}}");
        EntityScoreProfileStore.setProfiles(sentinel);
        MinecraftServer server = helper.getLevel().getServer();
        server.reloadResources(server.getPackRepository().getSelectedIds());
        helper.succeedWhen(() -> expect(
                helper,
                !EntityScoreProfileStore.profiles().keySet().containsAll(sentinel.keySet()),
                "/reload must replace the patched profiles"));
    }

    private static int unprofiledScore(LivingEntity entity, Ability ability) {
        return unprofiled(() -> ScoreService.abilityScore(entity, ability));
    }

    private static void expectInRange(GameTestHelper helper, String name, Ability ability, int score) {
        expect(
                helper,
                score >= Abilities.MIN_SCORE && score <= Abilities.MAX_SCORE,
                name + " " + ability.id() + " out of range: " + score);
    }
}
