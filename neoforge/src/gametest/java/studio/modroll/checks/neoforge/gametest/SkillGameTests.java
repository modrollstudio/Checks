package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.SkillScenarios;

/** NeoForge registration shim: delegates to the shared {@link SkillScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class SkillGameTests {

    private static final String TEMPLATE = "empty";
    // A reload finishes off-thread; the GameTest server ticks fast enough to outrun it.
    private static final int ASYNC_TIMEOUT_TICKS = 2000;

    @GameTest(template = TEMPLATE)
    public void standardSkillsLoadFromBuiltInDatapack(GameTestHelper helper) {
        SkillScenarios.standardSkillsLoadFromBuiltInDatapack(helper);
    }

    @GameTest(template = TEMPLATE)
    public void skillUsesGoverningAbility(GameTestHelper helper) {
        SkillScenarios.skillUsesGoverningAbility(helper);
    }

    @GameTest(template = TEMPLATE)
    public void playerSkillBonusResolutionOrder(GameTestHelper helper) {
        SkillScenarios.playerSkillBonusResolutionOrder(helper);
    }

    @GameTest(template = TEMPLATE)
    public void profiledMobGetsSkillBonus(GameTestHelper helper) {
        SkillScenarios.profiledMobGetsSkillBonus(helper);
    }

    @GameTest(template = TEMPLATE)
    public void datapackAddedSkillWorks(GameTestHelper helper) {
        SkillScenarios.datapackAddedSkillWorks(helper);
    }

    @GameTest(template = TEMPLATE)
    public void playerSkillBonusSurvivesRelogAndSave(GameTestHelper helper) {
        SkillScenarios.playerSkillBonusSurvivesRelogAndSave(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = ASYNC_TIMEOUT_TICKS)
    public void reloadReplacesSkills(GameTestHelper helper) {
        SkillScenarios.reloadReplacesSkills(helper);
    }
}
