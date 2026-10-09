package studio.modroll.checks.neoforge.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.TraitScenarios;

/** NeoForge registration shim: delegates to the shared {@link TraitScenarios} bodies. */
@GameTestHolder(Checks.MOD_ID)
@PrefixGameTestTemplate(false)
public class TraitGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public void vanillaSaveAdvantage(GameTestHelper helper) {
        TraitScenarios.vanillaSaveAdvantage(helper);
    }

    @GameTest(template = TEMPLATE)
    public void rollBonusGrantsAdvantage(GameTestHelper helper) {
        TraitScenarios.rollBonusGrantsAdvantage(helper);
    }

    @GameTest(template = TEMPLATE)
    public void damageResistanceHalvesMatchingDamage(GameTestHelper helper) {
        TraitScenarios.damageResistanceHalvesMatchingDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public void extraHealthFollowsTheLevel(GameTestHelper helper) {
        TraitScenarios.extraHealthFollowsTheLevel(helper);
    }

    @GameTest(template = TEMPLATE)
    public void darkvisionIsSentToTheClient(GameTestHelper helper) {
        TraitScenarios.darkvisionIsSentToTheClient(helper);
    }

    @GameTest(template = TEMPLATE)
    public void luckRerollsNaturalOnes(GameTestHelper helper) {
        TraitScenarios.luckRerollsNaturalOnes(helper);
    }

    @GameTest(template = TEMPLATE)
    public void resourcefulRerollsOnceADay(GameTestHelper helper) {
        TraitScenarios.resourcefulRerollsOnceADay(helper);
    }

    @GameTest(template = TEMPLATE)
    public void lastStandOnceADay(GameTestHelper helper) {
        TraitScenarios.lastStandOnceADay(helper);
    }

    @GameTest(template = TEMPLATE)
    public void lastStandBeforeTheTotem(GameTestHelper helper) {
        TraitScenarios.lastStandBeforeTheTotem(helper);
    }

    @GameTest(template = TEMPLATE)
    public void totemWhenLastStandIsOff(GameTestHelper helper) {
        TraitScenarios.totemWhenLastStandIsOff(helper);
    }

    @GameTest(template = TEMPLATE)
    public void ignoredByMobs(GameTestHelper helper) {
        TraitScenarios.ignoredByMobs(helper);
    }

    @GameTest(template = TEMPLATE)
    public void skillChoicesAtCreation(GameTestHelper helper) {
        TraitScenarios.skillChoicesAtCreation(helper);
    }

    @GameTest(template = TEMPLATE)
    public void attributeModifiers(GameTestHelper helper) {
        TraitScenarios.attributeModifiers(helper);
    }

    @GameTest(template = TEMPLATE)
    public void masterSwitchTurnsEveryTraitOff(GameTestHelper helper) {
        TraitScenarios.masterSwitchTurnsEveryTraitOff(helper);
    }
}
