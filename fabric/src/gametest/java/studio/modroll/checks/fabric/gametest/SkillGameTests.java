package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.SkillScenarios;

/** Fabric registration shim: delegates to the shared {@link SkillScenarios} bodies. */
public class SkillGameTests implements FabricGameTest {

    // A reload finishes off-thread; the GameTest server ticks fast enough to outrun it.
    private static final int ASYNC_TIMEOUT_TICKS = 2000;

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void standardSkillsLoadFromBuiltInDatapack(GameTestHelper helper) {
        SkillScenarios.standardSkillsLoadFromBuiltInDatapack(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void skillUsesGoverningAbility(GameTestHelper helper) {
        SkillScenarios.skillUsesGoverningAbility(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void playerSkillBonusResolutionOrder(GameTestHelper helper) {
        SkillScenarios.playerSkillBonusResolutionOrder(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void profiledMobGetsSkillBonus(GameTestHelper helper) {
        SkillScenarios.profiledMobGetsSkillBonus(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void datapackAddedSkillWorks(GameTestHelper helper) {
        SkillScenarios.datapackAddedSkillWorks(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void playerSkillBonusSurvivesRelogAndSave(GameTestHelper helper) {
        SkillScenarios.playerSkillBonusSurvivesRelogAndSave(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = ASYNC_TIMEOUT_TICKS)
    public void reloadReplacesSkills(GameTestHelper helper) {
        SkillScenarios.reloadReplacesSkills(helper);
    }
}
