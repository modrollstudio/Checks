package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.objective;
import static studio.modroll.checks.gametest.ScenarioSupport.perform;
import static studio.modroll.checks.gametest.ScenarioSupport.profiles;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.runExpectingFailure;
import static studio.modroll.checks.gametest.ScenarioSupport.score;
import static studio.modroll.checks.gametest.ScenarioSupport.spawnCalm;
import static studio.modroll.checks.gametest.ScenarioSupport.standardSkill;
import static studio.modroll.checks.gametest.ScenarioSupport.withConfig;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withProfiles;

import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.scores.Objective;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.data.EntityScoreProfile;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.critfall.api.combat.ContestResult;
import studio.modroll.critfall.api.combat.ContestSide;
import studio.modroll.critfall.api.combat.SaveResult;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.critfall.api.dice.RollMode;
import studio.modroll.critfall.api.dice.RollResult;

/**
 * GameTest bodies for the public API and {@code /checks roll}, with every die forced through Critfall's
 * roller seam. The rogue is a zombie with DEX 14 (+2), CON 8 (-1), STR 12 (+1) and stealth
 * bonus 3 (stealth +5); the watcher is a skeleton with WIS 12 (+1, passive perception 11), STR 14 (+2)
 * and athletics bonus 1 (athletics +3).
 */
public final class CheckScenarios {

    private CheckScenarios() {}

    /** The read side of the API reports the profiled scores, modifiers and passive score. */
    public static void apiReadsScoresAndModifiers(GameTestHelper helper) {
        Mob rogue = spawnCalm(helper, EntityType.ZOMBIE);
        Skill stealth = standardSkill("stealth");
        cast(() -> {
            expectEquals(helper, 14, ChecksApi.abilityScore(rogue, Ability.DEXTERITY), "DEX score");
            expectEquals(helper, 2, ChecksApi.abilityModifier(rogue, Ability.DEXTERITY), "DEX modifier");
            expectEquals(helper, 5, ChecksApi.skillModifier(rogue, stealth), "stealth modifier");
            expectEquals(helper, 15, ChecksApi.passiveScore(rogue, stealth), "passive stealth");
            expectEquals(helper, 2, ChecksApi.modifier(rogue, Ability.DEXTERITY), "DEX as a stat");
            expectEquals(helper, 5, ChecksApi.modifier(rogue, stealth), "stealth as a stat");
            return null;
        });
        helper.succeed();
    }

    /** d20 + ability modifier against a DC: exactly meeting the DC passes, one below fails. */
    public static void abilityCheckMeetsBeatsAndMisses(GameTestHelper helper) {
        Mob rogue = spawnCalm(helper, EntityType.ZOMBIE);
        SaveResult exact = cast(() ->
                withD20s(() -> ChecksApi.check(rogue, Ability.DEXTERITY, 15).result(), 13));
        SaveResult under = cast(() ->
                withD20s(() -> ChecksApi.check(rogue, Ability.DEXTERITY, 15).result(), 12));
        SaveResult over = cast(() ->
                withD20s(() -> ChecksApi.check(rogue, Ability.DEXTERITY, 15).result(), 20));

        expect(helper, exact.saved() && exact.saveTotal() == 15, "13 + 2 must exactly meet DC 15, got " + exact);
        expect(helper, !under.saved() && under.saveTotal() == 14, "12 + 2 must miss DC 15, got " + under);
        expect(helper, over.saved() && over.saveTotal() == 22, "20 + 2 must beat DC 15, got " + over);
        expect(helper, exact.dc() == 15 && exact.natural() == 13, "the result must carry DC and natural: " + exact);
        expect(helper, exact.roll().equals(RollDetail.normal(13)), "a normal check rolls one die: " + exact.roll());
        helper.succeed();
    }

    /** A skill check adds the skill modifier (governing ability + bonus), not just the ability. */
    public static void skillCheckAddsSkillModifier(GameTestHelper helper) {
        Mob rogue = spawnCalm(helper, EntityType.ZOMBIE);
        Skill stealth = standardSkill("stealth");
        SaveResult exact =
                cast(() -> withD20s(() -> ChecksApi.check(rogue, stealth, 18).result(), 13));
        SaveResult under =
                cast(() -> withD20s(() -> ChecksApi.check(rogue, stealth, 18).result(), 12));
        expect(helper, exact.saved() && exact.saveTotal() == 18, "13 + 5 must meet DC 18, got " + exact);
        expect(helper, !under.saved() && under.saveTotal() == 17, "12 + 5 must miss DC 18, got " + under);
        helper.succeed();
    }

    /** A saving throw is d20 + ability modifier against a DC, including a negative modifier. */
    public static void savingThrowUsesAbilityModifier(GameTestHelper helper) {
        Mob rogue = spawnCalm(helper, EntityType.ZOMBIE);
        SaveResult saved = cast(() -> withD20s(
                () -> ChecksApi.savingThrow(rogue, Ability.CONSTITUTION, 12).result(), 13));
        SaveResult failed = cast(() -> withD20s(
                () -> ChecksApi.savingThrow(rogue, Ability.CONSTITUTION, 12).result(), 12));
        expect(helper, saved.saved() && saved.saveTotal() == 12, "13 - 1 must meet DC 12, got " + saved);
        expect(helper, !failed.saved() && failed.saveTotal() == 11, "12 - 1 must miss DC 12, got " + failed);
        helper.succeed();
    }

    /** Each side adds its own stat; either side can win. Initiator's die is drawn first. */
    public static void contestGoesEitherWay(GameTestHelper helper) {
        Mob rogue = spawnCalm(helper, EntityType.ZOMBIE);
        Mob watcher = spawnCalm(helper, EntityType.SKELETON);
        Skill stealth = standardSkill("stealth");
        Skill perception = standardSkill("perception");

        ContestResult rogueWins = cast(() -> withD20s(
                () -> ChecksApi.contest(rogue, stealth, watcher, perception).result(), 10, 12));
        expect(
                helper,
                rogueWins.initiatorTotal() == 15 && rogueWins.opponentTotal() == 13,
                "10 + 5 vs 12 + 1 must total 15 vs 13, got " + rogueWins);
        expect(helper, rogueWins.winner() == ContestSide.INITIATOR, "15 vs 13 must go to the initiator");

        ContestResult watcherWins = cast(() -> withD20s(
                () -> ChecksApi.contest(rogue, stealth, watcher, perception).result(), 5, 12));
        expect(
                helper,
                watcherWins.initiatorTotal() == 10 && watcherWins.opponentTotal() == 13,
                "5 + 5 vs 12 + 1 must total 10 vs 13, got " + watcherWins);
        expect(helper, watcherWins.winner() == ContestSide.OPPONENT, "10 vs 13 must go to the opponent");
        helper.succeed();
    }

    /** Critfall's contest rule: a tie goes to the opponent. Also mixes an ability against a skill. */
    public static void contestTieGoesToOpponent(GameTestHelper helper) {
        Mob rogue = spawnCalm(helper, EntityType.ZOMBIE);
        Mob watcher = spawnCalm(helper, EntityType.SKELETON);
        Skill athletics = standardSkill("athletics");
        ContestResult tie = cast(() -> withD20s(
                () -> ChecksApi.contest(rogue, Ability.STRENGTH, watcher, athletics)
                        .result(),
                12,
                10));
        expect(
                helper,
                tie.initiatorTotal() == 13 && tie.opponentTotal() == 13,
                "STR 12 + 1 vs athletics 10 + 3 must tie at 13, got " + tie);
        expect(
                helper,
                !tie.initiatorWins() && tie.winner() == ContestSide.OPPONENT,
                "a tie must go to the opponent, winner was " + tie.winner());
        helper.succeed();
    }

    /** The target's passive score is the DC: meeting it succeeds, and the target rolls no die. */
    public static void checkAgainstPassiveScore(GameTestHelper helper) {
        Mob rogue = spawnCalm(helper, EntityType.ZOMBIE);
        Mob watcher = spawnCalm(helper, EntityType.SKELETON);
        Skill stealth = standardSkill("stealth");
        Skill perception = standardSkill("perception");

        SaveResult meets = cast(() -> withD20s(
                () -> ChecksApi.checkAgainstPassive(rogue, stealth, watcher, perception)
                        .result(),
                6));
        SaveResult misses = cast(() -> withD20s(
                () -> ChecksApi.checkAgainstPassive(rogue, stealth, watcher, perception)
                        .result(),
                5));
        expect(helper, meets.dc() == 11, "passive perception 10 + 1 must be the DC, was " + meets.dc());
        expect(helper, meets.saved() && meets.saveTotal() == 11, "6 + 5 must meet passive 11, got " + meets);
        expect(helper, !misses.saved() && misses.saveTotal() == 10, "5 + 5 must miss passive 11, got " + misses);
        helper.succeed();
    }

    /** Advantage and disadvantage reach Critfall: two dice, the right one kept, the mode recorded. */
    public static void rollModesPassThroughToCritfall(GameTestHelper helper) {
        Mob rogue = spawnCalm(helper, EntityType.ZOMBIE);
        Mob watcher = spawnCalm(helper, EntityType.SKELETON);
        Skill stealth = standardSkill("stealth");
        Skill perception = standardSkill("perception");

        SaveResult advantage = cast(() -> withD20s(
                () -> ChecksApi.check(rogue, Ability.DEXTERITY, 15, RollMode.ADVANTAGE)
                        .result(),
                4,
                17));
        expectTwoDice(helper, advantage.roll(), RollMode.ADVANTAGE, 17, 4);
        expect(helper, advantage.saveTotal() == 19, "advantage must keep 17 for 19, got " + advantage);

        SaveResult disadvantage = cast(() -> withD20s(
                () -> ChecksApi.savingThrow(rogue, Ability.DEXTERITY, 15, RollMode.DISADVANTAGE)
                        .result(),
                4,
                17));
        expectTwoDice(helper, disadvantage.roll(), RollMode.DISADVANTAGE, 4, 17);
        expect(helper, disadvantage.saveTotal() == 6, "disadvantage must keep 4 for 6, got " + disadvantage);

        ContestResult contest = cast(() -> withD20s(
                () -> ChecksApi.contest(rogue, stealth, RollMode.ADVANTAGE, watcher, perception, RollMode.DISADVANTAGE)
                        .result(),
                3,
                18,
                9,
                2));
        expectTwoDice(helper, contest.initiatorRoll(), RollMode.ADVANTAGE, 18, 3);
        expectTwoDice(helper, contest.opponentRoll(), RollMode.DISADVANTAGE, 2, 9);
        expect(
                helper,
                contest.initiatorTotal() == 23 && contest.opponentTotal() == 3,
                "contest must total 18 + 5 vs 2 + 1, got " + contest);

        SaveResult passive = cast(() -> withD20s(
                () -> ChecksApi.checkAgainstPassive(rogue, stealth, watcher, perception, RollMode.DISADVANTAGE)
                        .result(),
                15,
                6));
        expectTwoDice(helper, passive.roll(), RollMode.DISADVANTAGE, 6, 15);

        RollResult open = cast(() -> withD20s(
                () -> ChecksApi.roll(rogue, Ability.DEXTERITY, RollMode.ADVANTAGE)
                        .result(),
                4,
                17));
        expectTwoDice(helper, RollDetail.of(RollMode.ADVANTAGE, open), RollMode.ADVANTAGE, 17, 4);
        expect(helper, open.total() == 19, "an open advantage roll must keep 17 for 19, got " + open.total());
        helper.succeed();
    }

    /** With skills disabled in the config, a skill check falls back to the plain ability modifier. */
    public static void skillsDisabledUseAbilityModifier(GameTestHelper helper) {
        Mob rogue = spawnCalm(helper, EntityType.ZOMBIE);
        Skill stealth = standardSkill("stealth");
        ScoresConfig live = ScoresRuntime.config();
        ScoresConfig skillsOff = new ScoresConfig(
                live.playerDefaults(),
                live.derivation(),
                live.profilesEnabled(),
                false,
                live.proficiency(),
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
                skillsOff,
                () -> cast(() -> {
                    expectEquals(helper, 2, ChecksApi.skillModifier(rogue, stealth), "stealth with skills off");
                    expectEquals(helper, 12, ChecksApi.passiveScore(rogue, stealth), "passive stealth with skills off");
                    SaveResult check =
                            withD20s(() -> ChecksApi.check(rogue, stealth, 12).result(), 10);
                    expect(helper, check.saved() && check.saveTotal() == 12, "10 + 2 must meet DC 12, got " + check);
                    return null;
                }));
        helper.succeed();
    }

    /** An open roll is d20 + modifier through Critfall, with the modifier reported on the result. */
    public static void openRollAddsModifier(GameTestHelper helper) {
        Mob rogue = spawnCalm(helper, EntityType.ZOMBIE);
        RollResult stealthRoll = cast(() ->
                withD20s(() -> ChecksApi.roll(rogue, standardSkill("stealth")).result(), 9));
        RollResult conRoll = cast(
                () -> withD20s(() -> ChecksApi.roll(rogue, Ability.CONSTITUTION).result(), 9));
        expect(
                helper,
                stealthRoll.total() == 14 && stealthRoll.modifier() == 5,
                "9 + 5 must total 14 with modifier 5, got " + stealthRoll);
        expect(
                helper,
                conRoll.total() == 8 && conRoll.modifier() == -1,
                "9 - 1 must total 8 with modifier -1, got " + conRoll);
        helper.succeed();
    }

    /** {@code /checks roll} rolls through the API and returns the total, with or without a DC. */
    public static void rollCommandReturnsTotal(GameTestHelper helper) {
        Mob rogue = spawnCalm(helper, EntityType.ZOMBIE);
        String target = rogue.getStringUUID();
        int againstDc = cast(() -> withD20s(() -> run(helper, "checks roll " + target + " dex 15"), 13));
        int open = cast(() -> withD20s(() -> run(helper, "checks roll " + target + " stealth"), 9));
        expect(helper, againstDc == 15, "/checks roll dex 15 must return 13 + 2, returned " + againstDc);
        expect(helper, open == 14, "/checks roll stealth must return 9 + 5, returned " + open);
        helper.succeed();
    }

    /**
     * {@code execute store result} records the roll total; {@code /checks check} returns 1 on success
     * and fails the command otherwise, so {@code store success} and {@code store result} both record 1 or 0.
     */
    public static void commandResultsWorkWithExecuteStore(GameTestHelper helper) {
        Mob rogue = spawnCalm(helper, EntityType.ZOMBIE);
        String target = rogue.getStringUUID();
        Objective objective = objective(helper, "checks_gametest");
        String store = "execute store %s score %s " + objective.getName() + " run checks %s " + target + " dex%s";
        cast(() -> {
            performWithD20(helper, 13, store.formatted("result", "#roll", "roll", ""));
            performWithD20(helper, 13, store.formatted("success", "#met", "check", " 15"));
            performWithD20(helper, 12, store.formatted("success", "#missed", "check", " 15"));
            performWithD20(helper, 13, store.formatted("result", "#result", "check", " 15"));
            performWithD20(helper, 12, store.formatted("result", "#noresult", "check", " 15"));
            String check = "checks check " + target + " dex 15";
            expectEquals(helper, 1, withD20s(() -> run(helper, check), 13), "/checks check on a success");
            withD20s(
                    () -> {
                        runExpectingFailure(helper, check);
                        return null;
                    },
                    12);
            return null;
        });
        expectEquals(helper, 15, score(helper, objective, "#roll"), "stored roll total, 13 + 2");
        expectEquals(helper, 1, score(helper, objective, "#met"), "store success when 13 + 2 meets 15");
        expectEquals(helper, 0, score(helper, objective, "#missed"), "store success when 12 + 2 misses 15");
        expectEquals(helper, 1, score(helper, objective, "#result"), "store result on a success");
        expectEquals(helper, 0, score(helper, objective, "#noresult"), "store result on a failure");
        helper.succeed();
    }

    private static void performWithD20(GameTestHelper helper, int face, String command) {
        withD20s(
                () -> {
                    perform(helper, command);
                    return null;
                },
                face);
    }

    private static <T> T cast(Supplier<T> body) {
        Map<ResourceLocation, EntityScoreProfile> cast = profiles(
                "{\"matches\": [\"minecraft:zombie\"],"
                        + " \"abilities\": {\"dex\": 14, \"con\": 8, \"str\": 12}, \"skills\": {\"stealth\": 3}}",
                "{\"matches\": [\"minecraft:skeleton\"],"
                        + " \"abilities\": {\"wis\": 12, \"str\": 14}, \"skills\": {\"athletics\": 1}}");
        return withProfiles(cast, body);
    }

    private static void expectTwoDice(GameTestHelper helper, RollDetail roll, RollMode mode, int kept, int dropped) {
        expect(
                helper,
                roll.mode() == mode && roll.kept() == kept && roll.dropped().equals(OptionalInt.of(dropped)),
                mode + " must keep " + kept + " and drop " + dropped + ", got " + roll);
    }
}
