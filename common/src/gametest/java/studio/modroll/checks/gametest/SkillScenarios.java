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
import static studio.modroll.checks.gametest.ScenarioSupport.standardSkill;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withProfiles;
import static studio.modroll.checks.gametest.ScenarioSupport.withSkills;

import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.data.EntityScoreProfile;
import studio.modroll.checks.data.SkillStore;
import studio.modroll.checks.score.ScoreService;

/** GameTest bodies for skills, skill bonuses and passive scores on live entities. */
public final class SkillScenarios {

    private static final int STANDARD_SKILL_COUNT = 18;
    private static final ResourceLocation SAILING = ResourceLocation.fromNamespaceAndPath("gametest", "sailing");

    private SkillScenarios() {}

    /** The 18 standard skills load from Checks' built-in datapack on this loader. */
    public static void standardSkillsLoadFromBuiltInDatapack(GameTestHelper helper) {
        long standard = SkillStore.skills().keySet().stream()
                .filter(id -> id.getNamespace().equals("checks"))
                .count();
        expect(helper, standard == STANDARD_SKILL_COUNT, "expected 18 standard skills, loaded " + standard);
        expectGovernedBy(helper, "athletics", Ability.STRENGTH);
        expectGovernedBy(helper, "stealth", Ability.DEXTERITY);
        expectGovernedBy(helper, "perception", Ability.WISDOM);
        expectGovernedBy(helper, "intimidation", Ability.CHARISMA);
        helper.succeed();
    }

    /** A skill modifier follows its governing ability and only that ability; passive = 10 + modifier. */
    public static void skillUsesGoverningAbility(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-governing"));
        try {
            Skill stealth = standardSkill("stealth");
            Skill athletics = standardSkill("athletics");
            run(helper, "checks set " + player.getScoreboardName() + " dex 16");
            run(helper, "checks set " + player.getScoreboardName() + " str 8");

            int stealthModifier = unprofiled(() -> ScoreService.skillModifier(player, stealth));
            int athleticsModifier = unprofiled(() -> ScoreService.skillModifier(player, athletics));
            int passiveStealth = unprofiled(() -> ScoreService.passiveScore(player, stealth));
            expect(helper, stealthModifier == 3, "DEX 16 stealth must be +3, was " + stealthModifier);
            expect(helper, athleticsModifier == -1, "STR 8 athletics must be -1, was " + athleticsModifier);
            expect(helper, passiveStealth == 13, "passive stealth must be 10 + 3, was " + passiveStealth);

            int reported = unprofiled(() -> run(helper, "checks get " + player.getScoreboardName() + " stealth"));
            expect(helper, reported == 3, "/checks get must report stealth +3, reported " + reported);
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Bonus order for players: 0 by default, then a player profile, then {@code /checks set} wins. */
    public static void playerSkillBonusResolutionOrder(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-sk-order"));
        try {
            Skill perception = standardSkill("perception");
            int defaulted = unprofiled(() -> ScoreService.skillBonus(player, perception));
            expect(helper, defaulted == 0, "a player's skill bonus must default to 0, was " + defaulted);

            Map<ResourceLocation, EntityScoreProfile> playerProfile =
                    profiles("{\"matches\": [\"minecraft:player\"], \"skills\": {\"perception\": 2}}");
            int profiled = withProfiles(playerProfile, () -> ScoreService.skillBonus(player, perception));
            expect(helper, profiled == 2, "a player profile must set the bonus to 2, was " + profiled);

            run(helper, "checks set " + player.getScoreboardName() + " perception 5");
            int commanded = withProfiles(playerProfile, () -> ScoreService.skillBonus(player, perception));
            expect(helper, commanded == 5, "/checks set must beat the profile, was " + commanded);
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A mob takes its skill bonus from a profile; unprofiled skills stay at the bare ability modifier. */
    public static void profiledMobGetsSkillBonus(GameTestHelper helper) {
        Mob zombie = spawnCalm(helper, EntityType.ZOMBIE);
        Skill athletics = standardSkill("athletics");
        Skill stealth = standardSkill("stealth");
        Map<ResourceLocation, EntityScoreProfile> zombieProfile = profiles(
                "{\"matches\": [\"minecraft:zombie\"], \"abilities\": {\"str\": 14}, \"skills\": {\"athletics\": 3}}");

        int athleticsModifier = withProfiles(zombieProfile, () -> ScoreService.skillModifier(zombie, athletics));
        int stealthBonus = withProfiles(zombieProfile, () -> ScoreService.skillBonus(zombie, stealth));
        expect(helper, athleticsModifier == 5, "STR 14 (+2) + bonus 3 must be +5, was " + athleticsModifier);
        expect(helper, stealthBonus == 0, "an unlisted skill must have bonus 0, was " + stealthBonus);
        helper.succeed();
    }

    /** A datapack-added skill resolves for mobs (profile bonus) and players (command) alike. */
    public static void datapackAddedSkillWorks(GameTestHelper helper) {
        Map<ResourceLocation, Skill> withSailing = new HashMap<>(SkillStore.skills());
        Skill sailing = new Skill(SAILING, Ability.WISDOM);
        withSailing.put(SAILING, sailing);
        Mob zombie = spawnCalm(helper, EntityType.ZOMBIE);
        ServerPlayer player = login(helper, newProfile("checks-sailing"));
        try {
            withSkills(withSailing, () -> {
                Map<ResourceLocation, EntityScoreProfile> sailorZombie = profiles(
                        "{\"matches\": [\"minecraft:zombie\"], \"abilities\": {\"wis\": 12}, \"skills\": {\"gametest:sailing\": 4}}");
                int zombieModifier = withProfiles(sailorZombie, () -> ScoreService.skillModifier(zombie, sailing));
                int zombiePassive = withProfiles(sailorZombie, () -> ScoreService.passiveScore(zombie, sailing));
                expect(helper, zombieModifier == 5, "WIS 12 (+1) + bonus 4 must be +5, was " + zombieModifier);
                expect(helper, zombiePassive == 15, "passive sailing must be 15, was " + zombiePassive);

                run(helper, "checks set " + player.getScoreboardName() + " gametest:sailing 2");
                int playerModifier = unprofiled(() -> ScoreService.skillModifier(player, sailing));
                expect(helper, playerModifier == 2, "WIS 10 (+0) + bonus 2 must be +2, was " + playerModifier);
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A set skill bonus survives logout and a fresh login, and reaches the world save on disk. */
    public static void playerSkillBonusSurvivesRelogAndSave(GameTestHelper helper) {
        GameProfile profile = newProfile("checks-sk-relog");
        Skill insight = standardSkill("insight");
        ServerPlayer firstSession = login(helper, profile);
        run(helper, "checks set " + firstSession.getScoreboardName() + " insight 6");
        logout(helper, firstSession);

        ServerPlayer secondSession = login(helper, profile);
        try {
            int afterRelog = unprofiled(() -> ScoreService.skillBonus(secondSession, insight));
            expect(helper, afterRelog == 6, "insight bonus must still be 6 after relog, was " + afterRelog);
        } finally {
            logout(helper, secondSession);
        }

        saveWorldData(helper);
        OptionalInt fromDisk = savedPlayerStore(helper).getSkillBonus(profile.getId(), insight.id());
        expect(
                helper,
                fromDisk.equals(OptionalInt.of(6)),
                "the world save must hold insight bonus 6, held " + fromDisk);

        helper.succeed();
    }

    /**
     * A real {@code /reload} re-runs this loader's skill listener: a skill missing from the datapacks
     * is dropped and the standard skills are back. The sentinel only adds a skill, so scenarios
     * running alongside still see the standard ones.
     */
    public static void reloadReplacesSkills(GameTestHelper helper) {
        ResourceLocation sentinel = ResourceLocation.fromNamespaceAndPath("gametest", "reload_sentinel");
        Map<ResourceLocation, Skill> patched = new HashMap<>(SkillStore.skills());
        patched.put(sentinel, new Skill(sentinel, Ability.CHARISMA));
        SkillStore.setSkills(patched);
        MinecraftServer server = helper.getLevel().getServer();
        server.reloadResources(server.getPackRepository().getSelectedIds());
        helper.succeedWhen(() -> {
            expect(helper, SkillStore.find(sentinel).isEmpty(), "/reload must drop the patched skill");
            expectGovernedBy(helper, "stealth", Ability.DEXTERITY);
        });
    }

    private static void expectGovernedBy(GameTestHelper helper, String path, Ability ability) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("checks", path);
        expect(
                helper,
                SkillStore.find(id).map(Skill::ability).orElse(null) == ability,
                path + " must be governed by " + ability.id() + ", was " + SkillStore.find(id));
    }
}
