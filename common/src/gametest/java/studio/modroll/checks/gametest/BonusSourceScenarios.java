package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.bonusSources;
import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.runCapturingOutput;
import static studio.modroll.checks.gametest.ScenarioSupport.spawnCalm;
import static studio.modroll.checks.gametest.ScenarioSupport.standardSkill;
import static studio.modroll.checks.gametest.ScenarioSupport.withBonusSources;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withDice;
import static studio.modroll.checks.gametest.ScenarioSupport.withExtensions;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.bonus.BonusSource;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.sheet.StatSheet;
import studio.modroll.checks.sheet.StatSheets;
import studio.modroll.critfall.api.AttackDelivery;
import studio.modroll.critfall.api.ModifierProvider;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.SaveResult;
import studio.modroll.critfall.api.dice.RollMode;

public final class BonusSourceScenarios {

    private static final int EFFECT_TICKS = 200;

    private static final String SWORD = "{\"source\": {\"item\": \"minecraft:iron_sword\", \"slot\": \"mainhand\"},"
            + " \"applies_to\": {\"skills\": [\"athletics\"]}, \"bonus\": 2}";
    private static final String BOOTS = "{\"source\": {\"item\": \"minecraft:leather_boots\", \"slot\": \"armor\"},"
            + " \"applies_to\": {\"skills\": [\"stealth\"]}, \"mode\": \"advantage\"}";
    private static final String INVISIBLE = "{\"source\": {\"effect\": \"minecraft:invisibility\"},"
            + " \"applies_to\": {\"saves\": [\"dex\"]}, \"bonus\": 3}";
    private static final String SLOWED = "{\"source\": {\"effect\": \"minecraft:slowness\"},"
            + " \"applies_to\": {\"abilities\": [\"dex\"]}, \"mode\": \"disadvantage\"}";

    private static final String SPYGLASS = "{\"source\": {\"item\": \"minecraft:spyglass\", \"slot\": \"offhand\"},"
            + " \"applies_to\": {\"skills\": [\"perception\"]}, \"bonus\": 2}";
    private static final String NIGHT_VISION = "{\"source\": {\"effect\": \"minecraft:night_vision\"},"
            + " \"applies_to\": {\"skills\": [\"perception\"]}, \"mode\": \"advantage\"}";
    private static final String BLINDED = "{\"source\": {\"effect\": \"minecraft:blindness\"},"
            + " \"applies_to\": {\"abilities\": [\"wis\"]}, \"mode\": \"disadvantage\"}";
    private static final String MIGHTY = "{\"source\": {\"item\": \"minecraft:iron_sword\", \"slot\": \"mainhand\"},"
            + " \"applies_to\": {\"abilities\": [\"str\", \"dex\"], \"all_checks\": true, \"all_saves\": true},"
            + " \"bonus\": 3, \"mode\": \"advantage\"}";

    private BonusSourceScenarios() {}

    public static void flatBonusRaisesPassiveScore(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SPYGLASS));
        expectPassiveChange(helper, mob, bonusSources(SPYGLASS), 2, "a +2 perception item");
        helper.succeed();
    }

    public static void advantageRaisesPassiveScoreByFive(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        mob.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_TICKS));
        expectPassiveChange(helper, mob, bonusSources(NIGHT_VISION), 5, "advantage on perception");
        helper.succeed();
    }

    public static void advantageAndDisadvantageLeavePassiveScore(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        mob.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_TICKS));
        mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EFFECT_TICKS));
        expectPassiveChange(helper, mob, bonusSources(NIGHT_VISION, BLINDED), 0, "advantage and disadvantage");
        helper.succeed();
    }

    public static void rollLinesShowAdvantageAndBothD20s(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        String target = mob.getStringUUID();
        for (String command : List.of("checks roll " + target + " stealth", "checks roll " + target + " stealth 15")) {
            String line = withBonusSources(
                            bonusSources(BOOTS), () -> withD20s(() -> runCapturingOutput(helper, command), 4, 17))
                    .getFirst();
            expect(helper, line.contains(": advantage, d20 17 and 4, keeps 17 ("), command + " printed " + line);
        }
        helper.succeed();
    }

    public static void checksGetMatchesTheStatSheet(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("BonusGet"));
        String target = player.getStringUUID();
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SPYGLASS));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_TICKS));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, EFFECT_TICKS));
        Skill athletics = standardSkill("athletics");
        Skill perception = standardSkill("perception");
        int basePassive = ChecksApi.passiveScore(player, perception);
        withBonusSources(bonusSources(SWORD, SPYGLASS, NIGHT_VISION, INVISIBLE), () -> {
            StatSheet sheet = StatSheets.forPlayer(player).orElseThrow();
            StatSheet.SkillLine athleticsLine = skillLine(sheet, athletics);
            StatSheet.SkillLine perceptionLine = skillLine(sheet, perception);
            int sheetPassive = sheet.passives().stream()
                    .filter(line -> line.skill().equals(perception.id()))
                    .findFirst()
                    .orElseThrow()
                    .score();
            int sheetDexSave = sheet.ability(Ability.DEXTERITY).saveModifier();
            expectEquals(helper, basePassive + 2 + 5, sheetPassive, "sheet passive perception");

            expectEquals(
                    helper, athleticsLine.modifier(), run(helper, "checks get " + target + " athletics"), "get result");
            List<String> lines = runCapturingOutput(helper, "checks get " + target);
            String all = String.join("\n", lines);
            for (String expected : List.of(
                    "athletics " + SheetText.signed(athleticsLine.modifier()) + " (str",
                    "perception " + SheetText.signed(perceptionLine.modifier()) + " (wis, passive " + sheetPassive
                            + ")",
                    "dex " + SheetText.signed(sheetDexSave))) {
                expect(helper, all.contains(expected), "/checks get must show '" + expected + "', was " + all);
            }
            return null;
        });
        logout(helper, player);
        helper.succeed();
    }

    public static void critfallAttackAndDamageIgnoreBonusSources(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("BonusFighter"));
        Mob target = spawnCalm(helper, EntityType.ZOMBIE);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        ModifierProvider provider = RollService.modifierProvider().orElseThrow();
        List<OptionalInt> before = critfallModifiers(provider, player, target);
        List<OptionalInt> with =
                withBonusSources(bonusSources(MIGHTY), () -> critfallModifiers(provider, player, target));
        expect(helper, before.get(0).isPresent(), "Checks must supply the player's attack modifier");
        expect(helper, with.equals(before), "attack and damage modifiers must stay " + before + ", were " + with);
        logout(helper, player);
        helper.succeed();
    }

    private static List<OptionalInt> critfallModifiers(ModifierProvider provider, ServerPlayer player, Mob target) {
        return List.of(
                provider.attackModifier(player, target, AttackDelivery.MELEE),
                provider.attackModifier(player, target, AttackDelivery.PROJECTILE),
                provider.damageModifier(player, AttackDelivery.MELEE),
                provider.damageModifier(player, AttackDelivery.PROJECTILE));
    }

    private static void expectPassiveChange(
            GameTestHelper helper, Mob mob, Map<ResourceLocation, BonusSource> sources, int change, String what) {
        Skill perception = standardSkill("perception");
        int base = withBonusSources(Map.of(), () -> ChecksApi.passiveScore(mob, perception));
        int with = withBonusSources(sources, () -> ChecksApi.passiveScore(mob, perception));
        expectEquals(helper, base + change, with, "passive perception with " + what);
    }

    private static StatSheet.SkillLine skillLine(StatSheet sheet, Skill skill) {
        return sheet.skills().stream()
                .filter(line -> line.id().equals(skill.id()))
                .findFirst()
                .orElseThrow();
    }

    public static void mainhandItemAddsAFlatBonus(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        Skill athletics = standardSkill("athletics");
        int modifier = ChecksApi.modifier(mob, athletics);
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        withBonusSources(bonusSources(SWORD), () -> {
            SaveResult held = withDice(() -> ChecksApi.check(mob, athletics, 10).result(), 9);
            expectEquals(helper, 9 + modifier + 2, held.saveTotal(), "athletics with the sword in hand");
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_SWORD));
            SaveResult offhand =
                    withDice(() -> ChecksApi.check(mob, athletics, 10).result(), 9);
            expectEquals(helper, 9 + modifier, offhand.saveTotal(), "athletics with the sword in the off hand");
            return null;
        });
        helper.succeed();
    }

    public static void armorSlotGrantsAdvantage(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        withBonusSources(bonusSources(BOOTS), () -> {
            SaveResult stealth = withDice(
                    () -> ChecksApi.check(mob, standardSkill("stealth"), 10).result(), 4, 17);
            expect(helper, stealth.roll().mode() == RollMode.ADVANTAGE, "stealth must roll with advantage");
            expectEquals(helper, 17, stealth.natural(), "advantage keeps the higher face");
            SaveResult acrobatics = withDice(
                    () -> ChecksApi.check(mob, standardSkill("acrobatics"), 10).result(), 4);
            expect(helper, acrobatics.roll().mode() == RollMode.NORMAL, "acrobatics must roll normally");
            return null;
        });
        helper.succeed();
    }

    public static void mobEffectBoostsSaves(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        int saveModifier = ChecksApi.saveModifier(mob, Ability.DEXTERITY);
        int checkModifier = ChecksApi.abilityModifier(mob, Ability.DEXTERITY);
        mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, EFFECT_TICKS));
        withBonusSources(bonusSources(INVISIBLE), () -> {
            SaveResult save = withDice(
                    () -> ChecksApi.savingThrow(mob, Ability.DEXTERITY, 10).result(), 8);
            expectEquals(helper, 8 + saveModifier + 3, save.saveTotal(), "DEX save while invisible");
            SaveResult check =
                    withDice(() -> ChecksApi.check(mob, Ability.DEXTERITY, 10).result(), 8);
            expectEquals(helper, 8 + checkModifier, check.saveTotal(), "DEX check while invisible");
            return null;
        });
        helper.succeed();
    }

    public static void advantageAndDisadvantageSourcesCancel(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_TICKS));
        SaveResult stealth = withBonusSources(
                bonusSources(BOOTS, SLOWED),
                () -> withDice(
                        () -> ChecksApi.check(mob, standardSkill("stealth"), 10).result(), 7));
        expect(helper, stealth.roll().mode() == RollMode.NORMAL, "both modes must cancel, was " + stealth.roll());
        expectEquals(helper, 7, stealth.natural(), "the single die");
        helper.succeed();
    }

    public static void disabledBonusSourcesAddNothing(GameTestHelper helper) {
        Mob mob = spawnCalm(helper, EntityType.ZOMBIE);
        Skill athletics = standardSkill("athletics");
        int modifier = ChecksApi.modifier(mob, athletics);
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        SaveResult result = withExtensions(
                new ScoresConfig.ExtensionSettings(
                        true, new ScoresConfig.BonusSourceSettings(false, 5), true, true, true),
                () -> withBonusSources(
                        bonusSources(SWORD),
                        () -> withDice(() -> ChecksApi.check(mob, athletics, 10).result(), 9)));
        expectEquals(helper, 9 + modifier, result.saveTotal(), "athletics with bonus sources off");
        helper.succeed();
    }

    public static void statSheetShowsBonusSources(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("BonusSheet"));
        Skill athletics = standardSkill("athletics");
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        Map<ResourceLocation, BonusSource> sources = bonusSources(SWORD);
        StatSheet sheet =
                withBonusSources(sources, () -> StatSheets.forPlayer(player).orElseThrow());
        StatSheet.SkillLine line = skillLine(sheet, athletics);
        expectEquals(helper, ChecksApi.skillModifier(player, athletics) + 2, line.modifier(), "sheet athletics");
        expect(
                helper,
                line.situational()
                        .equals(List.of(sources.values().iterator().next().part())),
                "the sword must be listed, was " + line.situational());
        logout(helper, player);
        helper.succeed();
    }
}
