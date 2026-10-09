package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.levellingEnabled;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.profiles;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.runCapturingOutput;
import static studio.modroll.checks.gametest.ScenarioSupport.runExpectingFailure;
import static studio.modroll.checks.gametest.ScenarioSupport.saveWorldData;
import static studio.modroll.checks.gametest.ScenarioSupport.savedPlayerStore;
import static studio.modroll.checks.gametest.ScenarioSupport.spawnCalm;
import static studio.modroll.checks.gametest.ScenarioSupport.standardSkill;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withConfig;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withLevelling;
import static studio.modroll.checks.gametest.ScenarioSupport.withProfiles;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.data.EntityScoreProfile;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.PlayerScoreStore;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.critfall.api.combat.SaveResult;

/** GameTest bodies for proficiency: bonus, skill and save proficiency, commands and persistence. */
public final class ProficiencyScenarios {

    private ProficiencyScenarios() {}

    /** DEX 16 (+3), flat stealth bonus 1, proficiency +2: none +4, proficient +6, expertise +8. */
    public static void skillModifierForEachProficiencyLevel(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-pf-levels"));
        try {
            String name = player.getScoreboardName();
            Skill stealth = standardSkill("stealth");
            run(helper, "checks set " + name + " dex 16");
            run(helper, "checks set " + name + " stealth 1");
            run(helper, "checks set " + name + " proficiency 2");

            expectStealth(helper, player, stealth, Proficiency.NONE, 4);
            run(helper, "checks prof " + name + " stealth proficient");
            expectStealth(helper, player, stealth, Proficiency.PROFICIENT, 6);
            run(helper, "checks prof " + name + " stealth expertise");
            expectStealth(helper, player, stealth, Proficiency.EXPERTISE, 8);

            int passive = unprofiled(() -> ChecksApi.passiveScore(player, stealth));
            expect(helper, passive == 18, "passive stealth with expertise must be 10 + 8, was " + passive);
            int reported = unprofiled(() -> run(helper, "checks get " + name + " stealth"));
            expect(helper, reported == 8, "/checks get must report stealth +8, reported " + reported);

            run(helper, "checks prof " + name + " stealth none");
            expectStealth(helper, player, stealth, Proficiency.NONE, 4);
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * A saving throw adds the proficiency bonus only when proficient in that save. A CON 8 (-1) zombie
     * with proficiency +3 and CON save proficiency saves at +2; its DEX 14 (+2) save stays +2; a CON
     * ability check stays at the plain -1.
     */
    public static void saveAddsProficiencyOnlyWhenProficient(GameTestHelper helper) {
        Mob zombie = spawnCalm(helper, EntityType.ZOMBIE);
        Map<ResourceLocation, EntityScoreProfile> profile = profiles("{\"matches\": [\"minecraft:zombie\"],"
                + " \"abilities\": {\"con\": 8, \"dex\": 14},"
                + " \"proficiency\": {\"bonus\": 3, \"saves\": {\"con\": \"proficient\"}}}");
        withProfiles(profile, () -> {
            expectEquals(helper, 2, ChecksApi.saveModifier(zombie, Ability.CONSTITUTION), "proficient CON save");
            expectEquals(helper, 2, ChecksApi.saveModifier(zombie, Ability.DEXTERITY), "non-proficient DEX save");
            expectLevel(
                    helper,
                    Proficiency.PROFICIENT,
                    ChecksApi.saveProficiency(zombie, Ability.CONSTITUTION),
                    "CON save");
            expectLevel(helper, Proficiency.NONE, ChecksApi.saveProficiency(zombie, Ability.DEXTERITY), "DEX save");

            SaveResult saved = withD20s(
                    () -> ChecksApi.savingThrow(zombie, Ability.CONSTITUTION, 12)
                            .result(),
                    10);
            SaveResult failed = withD20s(
                    () -> ChecksApi.savingThrow(zombie, Ability.CONSTITUTION, 12)
                            .result(),
                    9);
            SaveResult dexSave = withD20s(
                    () -> ChecksApi.savingThrow(zombie, Ability.DEXTERITY, 12).result(), 10);
            SaveResult conCheck = withD20s(
                    () -> ChecksApi.check(zombie, Ability.CONSTITUTION, 12).result(), 10);
            expect(helper, saved.saved() && saved.saveTotal() == 12, "10 - 1 + 3 must meet DC 12, got " + saved);
            expect(helper, !failed.saved() && failed.saveTotal() == 11, "9 - 1 + 3 must miss DC 12, got " + failed);
            expect(helper, dexSave.saveTotal() == 12, "a non-proficient DEX save must be 10 + 2, got " + dexSave);
            expect(helper, conCheck.saveTotal() == 9, "a CON ability check must add no proficiency, got " + conCheck);
            return null;
        });

        ServerPlayer player = login(helper, newProfile("checks-pf-save"));
        try {
            String name = player.getScoreboardName();
            run(helper, "checks set " + name + " con 10");
            run(helper, "checks set " + name + " wis 10");
            run(helper, "checks set " + name + " proficiency 2");
            run(helper, "checks prof " + name + " con proficient");
            unprofiled(() -> {
                expectEquals(helper, 2, ChecksApi.saveModifier(player, Ability.CONSTITUTION), "player CON save");
                expectEquals(helper, 0, ChecksApi.saveModifier(player, Ability.WISDOM), "player WIS save");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Players: config default, then a profile, then commands win. Mobs: config default, then a profile. */
    public static void proficiencyResolutionOrder(GameTestHelper helper) {
        int configDefault = ScoresRuntime.config().proficiency().defaultBonus();
        Mob zombie = spawnCalm(helper, EntityType.ZOMBIE);
        int mobDefault = unprofiled(() -> ChecksApi.proficiencyBonus(zombie));
        int mobProfiled = withProfiles(
                profiles("{\"matches\": [\"minecraft:zombie\"], \"proficiency\": {\"bonus\": 5}}"),
                () -> ChecksApi.proficiencyBonus(zombie));
        expectEquals(helper, configDefault, mobDefault, "an unprofiled mob's proficiency bonus");
        expectEquals(helper, 5, mobProfiled, "a profiled mob's proficiency bonus");

        ServerPlayer player = login(helper, newProfile("checks-pf-order"));
        try {
            String name = player.getScoreboardName();
            Skill perception = standardSkill("perception");
            Map<ResourceLocation, EntityScoreProfile> playerProfile =
                    profiles("{\"matches\": [\"minecraft:player\"], \"proficiency\": {\"bonus\": 4,"
                            + " \"skills\": {\"perception\": \"proficient\"}, \"saves\": {\"wis\": \"proficient\"}}}");

            withLevelling(levellingEnabled(false), () -> {
                expectEquals(
                        helper, configDefault, unprofiled(() -> ChecksApi.proficiencyBonus(player)), "player default");
                return withProfiles(playerProfile, () -> {
                    expectEquals(helper, 4, ChecksApi.proficiencyBonus(player), "player profile bonus");
                    expectLevel(
                            helper, Proficiency.PROFICIENT, ChecksApi.skillProficiency(player, perception), "profile");
                    expectEquals(
                            helper, 4, ChecksApi.saveModifier(player, Ability.WISDOM) - wisModifier(player), "WIS");
                    return null;
                });
            });
            withLevelling(
                    levellingEnabled(true),
                    () -> withProfiles(playerProfile, () -> {
                        expectEquals(
                                helper,
                                2,
                                ChecksApi.proficiencyBonus(player),
                                "the level 1 bonus over the player profile");
                        expectLevel(
                                helper,
                                Proficiency.PROFICIENT,
                                ChecksApi.skillProficiency(player, perception),
                                "levelled");
                        return null;
                    }));

            run(helper, "checks set " + name + " proficiency 6");
            run(helper, "checks prof " + name + " perception expertise");
            run(helper, "checks prof " + name + " wis none");
            withProfiles(playerProfile, () -> {
                expectEquals(helper, 6, ChecksApi.proficiencyBonus(player), "/checks set over the profile");
                expectLevel(helper, Proficiency.EXPERTISE, ChecksApi.skillProficiency(player, perception), "command");
                expectEquals(
                        helper,
                        wisModifier(player),
                        ChecksApi.saveModifier(player, Ability.WISDOM),
                        "WIS save after /checks prof none");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Expertise on a save, unknown levels and out-of-range bonuses are rejected and change nothing. */
    public static void invalidProficiencyCommandsAreRejected(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-pf-reject"));
        try {
            String name = player.getScoreboardName();
            PlayerScoreStore store = PlayerScoreStore.get(helper.getLevel().getServer());
            runExpectingFailure(helper, "checks prof " + name + " dex expertise");
            runExpectingFailure(helper, "checks prof " + name + " stealth master");
            runExpectingFailure(helper, "checks set " + name + " proficiency 11");
            runExpectingFailure(helper, "checks set " + name + " proficiency -1");
            expect(
                    helper,
                    store.getSaveProficiency(player.getUUID(), Ability.DEXTERITY)
                            .isEmpty(),
                    "a rejected save expertise must store nothing");
            expect(
                    helper,
                    store.getProficiencyBonus(player.getUUID()).isEmpty(),
                    "a rejected proficiency bonus must store nothing");

            run(helper, "checks prof " + name + " stealth expertise");
            run(helper, "checks prof " + name + " dex proficient");
            expect(
                    helper,
                    store.getSaveProficiency(player.getUUID(), Ability.DEXTERITY)
                            .equals(Optional.of(Proficiency.PROFICIENT)),
                    "a proficient save must be accepted");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A set proficiency bonus and levels survive logout and a fresh login, and reach the world save. */
    public static void proficiencySurvivesRelogAndSave(GameTestHelper helper) {
        GameProfile profile = newProfile("checks-pf-relog");
        Skill stealth = standardSkill("stealth");
        ServerPlayer firstSession = login(helper, profile);
        String name = firstSession.getScoreboardName();
        run(helper, "checks set " + name + " proficiency 5");
        run(helper, "checks prof " + name + " stealth expertise");
        run(helper, "checks prof " + name + " wis proficient");
        logout(helper, firstSession);

        ServerPlayer secondSession = login(helper, profile);
        try {
            unprofiled(() -> {
                expectEquals(helper, 5, ChecksApi.proficiencyBonus(secondSession), "bonus after relog");
                expectLevel(helper, Proficiency.EXPERTISE, ChecksApi.skillProficiency(secondSession, stealth), "relog");
                expectEquals(
                        helper,
                        wisModifier(secondSession) + 5,
                        ChecksApi.saveModifier(secondSession, Ability.WISDOM),
                        "WIS save after relog");
                return null;
            });
        } finally {
            logout(helper, secondSession);
        }

        saveWorldData(helper);
        PlayerScoreStore fromDisk = savedPlayerStore(helper);
        expect(
                helper,
                fromDisk.getProficiencyBonus(profile.getId()).equals(OptionalInt.of(5)),
                "the world save must hold proficiency 5");
        expect(
                helper,
                fromDisk.getSkillProficiency(profile.getId(), stealth.id()).equals(Optional.of(Proficiency.EXPERTISE)),
                "the world save must hold stealth expertise");
        expect(
                helper,
                fromDisk.getSaveProficiency(profile.getId(), Ability.WISDOM)
                        .equals(Optional.of(Proficiency.PROFICIENT)),
                "the world save must hold the WIS save proficiency");

        helper.succeed();
    }

    /** With proficiency disabled, skills and saves fall back to the pre-proficiency rules; stored values come back after. */
    public static void proficiencyDisabledRestoresPreviousRules(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-pf-toggle"));
        try {
            String name = player.getScoreboardName();
            Skill stealth = standardSkill("stealth");
            run(helper, "checks set " + name + " dex 16");
            run(helper, "checks set " + name + " con 10");
            run(helper, "checks set " + name + " stealth 1");
            run(helper, "checks set " + name + " proficiency 4");
            run(helper, "checks prof " + name + " stealth expertise");
            run(helper, "checks prof " + name + " con proficient");
            ScoresConfig live = ScoresRuntime.config();
            ScoresConfig proficiencyOff = new ScoresConfig(
                    live.playerDefaults(),
                    live.derivation(),
                    live.profilesEnabled(),
                    live.skillsEnabled(),
                    new ScoresConfig.ProficiencySettings(
                            false, live.proficiency().defaultBonus()),
                    live.critfall(),
                    live.statScreenEnabled(),
                    live.creation(),
                    live.levelling(),
                    live.extensions(),
                    live.body(),
                    live.saves(),
                    live.traits(),
                    live.social(),
                    live.rollMessages(),
                    live.deathMessages(),
                    live.exploration());

            withConfig(
                    proficiencyOff,
                    () -> unprofiled(() -> {
                        expectEquals(helper, 0, ChecksApi.proficiencyBonus(player), "bonus with proficiency off");
                        expectLevel(helper, Proficiency.NONE, ChecksApi.skillProficiency(player, stealth), "off");
                        expectEquals(
                                helper, 4, ChecksApi.skillModifier(player, stealth), "stealth with proficiency off");
                        expectEquals(helper, 0, ChecksApi.saveModifier(player, Ability.CONSTITUTION), "CON save off");
                        SaveResult save = withD20s(
                                () -> ChecksApi.savingThrow(player, Ability.CONSTITUTION, 10)
                                        .result(),
                                10);
                        expect(
                                helper,
                                save.saveTotal() == 10,
                                "a save with proficiency off must be d20 + 0, got " + save);
                        return null;
                    }));

            unprofiled(() -> {
                expectEquals(helper, 12, ChecksApi.skillModifier(player, stealth), "stealth back on: 3 + 8 + 1");
                expectEquals(helper, 4, ChecksApi.saveModifier(player, Ability.CONSTITUTION), "CON save back on");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** {@code /checks get} prints the proficiency bonus, save modifiers and proficiency markers. */
    public static void getShowsProficiencyMarkersAndSaveModifiers(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-pf-get"));
        try {
            String name = player.getScoreboardName();
            for (String ability : List.of("str 10", "dex 16", "con 12", "wis 10")) {
                run(helper, "checks set " + name + " " + ability);
            }
            run(helper, "checks set " + name + " proficiency 3");
            run(helper, "checks prof " + name + " stealth expertise");
            run(helper, "checks prof " + name + " perception proficient");
            run(helper, "checks prof " + name + " con proficient");

            List<String> all = unprofiled(() -> runCapturingOutput(helper, "checks get " + name));
            String output = String.join("\n", all);
            for (String expected : List.of(
                    "Proficiency +3, saves: str +0, dex +3, con +4 (proficient)",
                    "stealth +9 (dex, expertise, passive 19)",
                    "perception +3 (wis, proficient, passive 13)",
                    "athletics +0 (str, passive 10)")) {
                expect(
                        helper,
                        output.contains(expected),
                        "/checks get must show \"" + expected + "\", was:\n" + output);
            }

            List<String> con = unprofiled(() -> runCapturingOutput(helper, "checks get " + name + " con"));
            List<String> stealth = unprofiled(() -> runCapturingOutput(helper, "checks get " + name + " stealth"));
            expect(
                    helper,
                    con.equals(List.of(name + ": con 12 (+1), save +4 (proficient)")),
                    "/checks get con must show the save, was " + con);
            expect(
                    helper,
                    stealth.equals(List.of(name + ": stealth +9 (dex, expertise, passive 19)")),
                    "/checks get stealth must show the marker, was " + stealth);
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    private static void expectStealth(
            GameTestHelper helper, ServerPlayer player, Skill stealth, Proficiency level, int modifier) {
        unprofiled(() -> {
            expectLevel(helper, level, ChecksApi.skillProficiency(player, stealth), "stealth");
            expectEquals(helper, modifier, ChecksApi.skillModifier(player, stealth), "stealth at " + level.id());
            return null;
        });
    }

    private static int wisModifier(ServerPlayer player) {
        return ChecksApi.abilityModifier(player, Ability.WISDOM);
    }

    private static void expectLevel(GameTestHelper helper, Proficiency expected, Proficiency actual, String what) {
        expect(helper, actual == expected, what + " proficiency must be " + expected.id() + ", was " + actual.id());
    }
}
