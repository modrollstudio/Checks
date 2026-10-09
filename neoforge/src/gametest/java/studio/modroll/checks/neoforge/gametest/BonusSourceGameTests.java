package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.BonusSourceScenarios;

/** NeoForge registration shim: delegates to the shared {@link BonusSourceScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class BonusSourceGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void mainhandItemAddsAFlatBonus(GameTestHelper helper) {
        BonusSourceScenarios.mainhandItemAddsAFlatBonus(helper);
    }

    @GameTest(template = TEMPLATE)
    public void armorSlotGrantsAdvantage(GameTestHelper helper) {
        BonusSourceScenarios.armorSlotGrantsAdvantage(helper);
    }

    @GameTest(template = TEMPLATE)
    public void mobEffectBoostsSaves(GameTestHelper helper) {
        BonusSourceScenarios.mobEffectBoostsSaves(helper);
    }

    @GameTest(template = TEMPLATE)
    public void advantageAndDisadvantageSourcesCancel(GameTestHelper helper) {
        BonusSourceScenarios.advantageAndDisadvantageSourcesCancel(helper);
    }

    @GameTest(template = TEMPLATE)
    public void disabledBonusSourcesAddNothing(GameTestHelper helper) {
        BonusSourceScenarios.disabledBonusSourcesAddNothing(helper);
    }

    @GameTest(template = TEMPLATE)
    public void statSheetShowsBonusSources(GameTestHelper helper) {
        BonusSourceScenarios.statSheetShowsBonusSources(helper);
    }

    @GameTest(template = TEMPLATE)
    public void flatBonusRaisesPassiveScore(GameTestHelper helper) {
        BonusSourceScenarios.flatBonusRaisesPassiveScore(helper);
    }

    @GameTest(template = TEMPLATE)
    public void advantageRaisesPassiveScoreByFive(GameTestHelper helper) {
        BonusSourceScenarios.advantageRaisesPassiveScoreByFive(helper);
    }

    @GameTest(template = TEMPLATE)
    public void advantageAndDisadvantageLeavePassiveScore(GameTestHelper helper) {
        BonusSourceScenarios.advantageAndDisadvantageLeavePassiveScore(helper);
    }

    @GameTest(template = TEMPLATE)
    public void rollLinesShowAdvantageAndBothD20s(GameTestHelper helper) {
        BonusSourceScenarios.rollLinesShowAdvantageAndBothD20s(helper);
    }

    @GameTest(template = TEMPLATE)
    public void checksGetMatchesTheStatSheet(GameTestHelper helper) {
        BonusSourceScenarios.checksGetMatchesTheStatSheet(helper);
    }

    @GameTest(template = TEMPLATE)
    public void critfallAttackAndDamageIgnoreBonusSources(GameTestHelper helper) {
        BonusSourceScenarios.critfallAttackAndDamageIgnoreBonusSources(helper);
    }
}
