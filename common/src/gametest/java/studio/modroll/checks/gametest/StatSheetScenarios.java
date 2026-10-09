package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withConfig;
import static studio.modroll.checks.gametest.ScenarioSupport.withSkills;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.data.SkillStore;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.sheet.StatSheet;
import studio.modroll.checks.sheet.StatSheets;

/** GameTest bodies for the stat screen's server-built snapshot. */
public final class StatSheetScenarios {

    private static final ResourceLocation STEALTH = ResourceLocation.parse("checks:stealth");
    private static final ResourceLocation PERCEPTION = ResourceLocation.parse("checks:perception");
    private static final ResourceLocation LOCKPICKING = ResourceLocation.parse("checks_gametest:lockpicking");

    private StatSheetScenarios() {}

    /**
     * DEX 16 (+3), WIS 14 (+2), INT 8 (-1), proficiency +3, DEX save proficient, stealth expertise with a
     * flat +1, perception proficient, plus a datapack-added lockpicking skill.
     */
    public static void sheetMatchesSetScoresAndProficiency(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-sh-values"));
        try {
            String name = player.getScoreboardName();
            for (String value :
                    List.of("str 10", "dex 16", "con 10", "int 8", "wis 14", "cha 10", "stealth 1", "proficiency 3")) {
                run(helper, "checks set " + name + " " + value);
            }
            run(helper, "checks prof " + name + " stealth expertise");
            run(helper, "checks prof " + name + " perception proficient");
            run(helper, "checks prof " + name + " dex proficient");
            Map<ResourceLocation, Skill> withLockpicking = new HashMap<>(SkillStore.skills());
            withLockpicking.put(LOCKPICKING, new Skill(LOCKPICKING, Ability.DEXTERITY));

            withSkills(
                    withLockpicking,
                    () -> unprofiled(() -> {
                        StatSheet sheet = StatSheets.forPlayer(player).orElseThrow();
                        expectEquals(helper, 3, sheet.proficiencyBonus(), "proficiency bonus");
                        expectAbilities(helper, sheet);
                        expectSkill(helper, sheet, STEALTH, Ability.DEXTERITY, 10, Proficiency.EXPERTISE, 1);
                        expectSkill(helper, sheet, PERCEPTION, Ability.WISDOM, 5, Proficiency.PROFICIENT, 0);
                        expectSkill(helper, sheet, LOCKPICKING, Ability.DEXTERITY, 3, Proficiency.NONE, 0);
                        expectEquals(
                                helper, withLockpicking.size(), sheet.skills().size(), "skill count");
                        expectMatchesApi(helper, player, sheet);
                        expect(
                                helper,
                                sheet.passives()
                                        .equals(List.of(
                                                new StatSheet.PassiveLine(PERCEPTION, 15),
                                                new StatSheet.PassiveLine(
                                                        ResourceLocation.parse("checks:investigation"), 9),
                                                new StatSheet.PassiveLine(
                                                        ResourceLocation.parse("checks:insight"), 12))),
                                "passives must be perception 15, investigation 9, insight 12, were "
                                        + sheet.passives());
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** The snapshot follows the skills and proficiency toggles, and the screen toggle withholds it. */
    public static void sheetFollowsToggles(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-sh-toggle"));
        try {
            String name = player.getScoreboardName();
            for (String value : List.of("dex 16", "stealth 2", "proficiency 3")) {
                run(helper, "checks set " + name + " " + value);
            }
            run(helper, "checks prof " + name + " stealth expertise");
            run(helper, "checks prof " + name + " dex proficient");
            ScoresConfig live = ScoresRuntime.config();

            StatSheet skillsOff = sheetWith(player, configWith(live, false, live.proficiency(), true));
            expectSkill(helper, skillsOff, STEALTH, Ability.DEXTERITY, 9, Proficiency.EXPERTISE, 0);

            ScoresConfig.ProficiencySettings off = new ScoresConfig.ProficiencySettings(
                    false, live.proficiency().defaultBonus());
            StatSheet proficiencyOff = sheetWith(player, configWith(live, true, off, true));
            expectEquals(helper, 0, proficiencyOff.proficiencyBonus(), "proficiency bonus with proficiency off");
            expectSkill(helper, proficiencyOff, STEALTH, Ability.DEXTERITY, 5, Proficiency.NONE, 2);
            StatSheet.AbilityLine dex = proficiencyOff.abilities().get(Ability.DEXTERITY.ordinal());
            expectEquals(helper, 3, dex.saveModifier(), "DEX save with proficiency off");
            expect(helper, dex.saveProficiency() == Proficiency.NONE, "DEX save proficiency with proficiency off");

            boolean withheld =
                    withConfig(configWith(live, true, live.proficiency(), false), () -> StatSheets.forPlayer(player)
                            .isEmpty());
            expect(helper, withheld, "no sheet may be built while the stat screen is disabled");

            StatSheet backOn = unprofiled(() -> StatSheets.forPlayer(player).orElseThrow());
            expectSkill(helper, backOn, STEALTH, Ability.DEXTERITY, 11, Proficiency.EXPERTISE, 2);
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    private static StatSheet sheetWith(ServerPlayer player, ScoresConfig config) {
        return withConfig(
                config, () -> unprofiled(() -> StatSheets.forPlayer(player).orElseThrow()));
    }

    private static ScoresConfig configWith(
            ScoresConfig live,
            boolean skillsEnabled,
            ScoresConfig.ProficiencySettings proficiency,
            boolean statScreenEnabled) {
        return new ScoresConfig(
                live.playerDefaults(),
                live.derivation(),
                live.profilesEnabled(),
                skillsEnabled,
                proficiency,
                live.critfall(),
                statScreenEnabled,
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
    }

    private static void expectAbilities(GameTestHelper helper, StatSheet sheet) {
        List<StatSheet.AbilityLine> expected = List.of(
                new StatSheet.AbilityLine(Ability.STRENGTH, 10, 0, 0, Proficiency.NONE, StatSheet.AbilityBonuses.NONE),
                new StatSheet.AbilityLine(
                        Ability.DEXTERITY, 16, 3, 6, Proficiency.PROFICIENT, StatSheet.AbilityBonuses.NONE),
                new StatSheet.AbilityLine(
                        Ability.CONSTITUTION, 10, 0, 0, Proficiency.NONE, StatSheet.AbilityBonuses.NONE),
                new StatSheet.AbilityLine(
                        Ability.INTELLIGENCE, 8, -1, -1, Proficiency.NONE, StatSheet.AbilityBonuses.NONE),
                new StatSheet.AbilityLine(Ability.WISDOM, 14, 2, 2, Proficiency.NONE, StatSheet.AbilityBonuses.NONE),
                new StatSheet.AbilityLine(Ability.CHARISMA, 10, 0, 0, Proficiency.NONE, StatSheet.AbilityBonuses.NONE));
        expect(
                helper,
                sheet.abilities().equals(expected),
                "abilities must be " + expected + ", were " + sheet.abilities());
    }

    private static void expectSkill(
            GameTestHelper helper,
            StatSheet sheet,
            ResourceLocation id,
            Ability ability,
            int modifier,
            Proficiency proficiency,
            int bonus) {
        StatSheet.SkillLine expected = new StatSheet.SkillLine(id, ability, modifier, proficiency, bonus, List.of());
        expect(
                helper,
                sheet.skills().contains(expected),
                "skills must contain " + expected + ", were " + sheet.skills());
    }

    private static void expectMatchesApi(GameTestHelper helper, ServerPlayer player, StatSheet sheet) {
        for (StatSheet.SkillLine line : sheet.skills()) {
            Skill skill = ChecksApi.skill(line.id()).orElseThrow();
            expectEquals(helper, ChecksApi.skillModifier(player, skill), line.modifier(), line.id() + " modifier");
            expectEquals(helper, ChecksApi.skillBonus(player, skill), line.bonus(), line.id() + " bonus");
        }
    }
}
