package studio.modroll.checks.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import studio.modroll.checks.gametest.BonusSourceScenarios;

/** Fabric registration shim: delegates to the shared {@link BonusSourceScenarios} bodies. */
public class BonusSourceGameTests implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void mainhandItemAddsAFlatBonus(GameTestHelper helper) {
        BonusSourceScenarios.mainhandItemAddsAFlatBonus(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void armorSlotGrantsAdvantage(GameTestHelper helper) {
        BonusSourceScenarios.armorSlotGrantsAdvantage(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void mobEffectBoostsSaves(GameTestHelper helper) {
        BonusSourceScenarios.mobEffectBoostsSaves(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void advantageAndDisadvantageSourcesCancel(GameTestHelper helper) {
        BonusSourceScenarios.advantageAndDisadvantageSourcesCancel(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void disabledBonusSourcesAddNothing(GameTestHelper helper) {
        BonusSourceScenarios.disabledBonusSourcesAddNothing(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void statSheetShowsBonusSources(GameTestHelper helper) {
        BonusSourceScenarios.statSheetShowsBonusSources(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void flatBonusRaisesPassiveScore(GameTestHelper helper) {
        BonusSourceScenarios.flatBonusRaisesPassiveScore(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void advantageRaisesPassiveScoreByFive(GameTestHelper helper) {
        BonusSourceScenarios.advantageRaisesPassiveScoreByFive(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void advantageAndDisadvantageLeavePassiveScore(GameTestHelper helper) {
        BonusSourceScenarios.advantageAndDisadvantageLeavePassiveScore(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void rollLinesShowAdvantageAndBothD20s(GameTestHelper helper) {
        BonusSourceScenarios.rollLinesShowAdvantageAndBothD20s(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void checksGetMatchesTheStatSheet(GameTestHelper helper) {
        BonusSourceScenarios.checksGetMatchesTheStatSheet(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void critfallAttackAndDamageIgnoreBonusSources(GameTestHelper helper) {
        BonusSourceScenarios.critfallAttackAndDamageIgnoreBonusSources(helper);
    }
}
