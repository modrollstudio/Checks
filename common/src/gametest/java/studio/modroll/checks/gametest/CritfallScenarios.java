package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.profiles;
import static studio.modroll.checks.gametest.ScenarioSupport.provider;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.spawnCalm;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withCritfallSettings;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withOtherModifierProvider;
import static studio.modroll.checks.gametest.ScenarioSupport.withProfiles;
import static studio.modroll.checks.gametest.ScenarioSupport.withoutModifierProvider;

import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.critfall.ChecksModifierProvider;
import studio.modroll.checks.data.EntityScoreProfile;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.critfall.api.AttackContext;
import studio.modroll.critfall.api.AttackDelivery;
import studio.modroll.critfall.api.ModifierProvider;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.AttackOutcome;
import studio.modroll.critfall.api.combat.AttackResult;

/** GameTest bodies for the Critfall modifier provider: who it covers, what it supplies, and its toggle. */
public final class CritfallScenarios {

    private static final int HITTING_D20 = 15;
    private static final int PROFILED_ZOMBIE_STR_MODIFIER = 3;
    private static final int SHARPNESS_V = 5;
    private static final int EFFECT_TICKS = 600;
    private static final String PROFILED_ZOMBIE = "{\"matches\": [\"minecraft:zombie\"],"
            + " \"abilities\": {\"str\": 16, \"dex\": 14},"
            + " \"proficiency\": {\"bonus\": 3, \"saves\": {\"dex\": \"proficient\"}}}";

    private CritfallScenarios() {}

    /** STR 16 (+3), DEX 18 (+4), proficiency +2: melee +5 to hit and +3 damage, ranged +6 and +4. */
    public static void playerAttackAndDamageUseAbilityAndProficiency(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-cf-player"));
        try {
            String name = player.getScoreboardName();
            run(helper, "checks set " + name + " str 16");
            run(helper, "checks set " + name + " dex 18");
            run(helper, "checks set " + name + " proficiency 2");
            Mob target = spawnCalm(helper, EntityType.ZOMBIE);

            unprofiled(() -> {
                ModifierProvider provider = provider();
                expectModifier(helper, 5, provider.attackModifier(player, target, AttackDelivery.MELEE), "melee hit");
                expectModifier(helper, 6, provider.attackModifier(player, target, AttackDelivery.PROJECTILE), "bow");
                expectModifier(helper, 6, provider.attackModifier(player, target, AttackDelivery.THROWN), "thrown");
                expectModifier(helper, 3, provider.damageModifier(player, AttackDelivery.MELEE), "melee damage");
                expectModifier(helper, 4, provider.damageModifier(player, AttackDelivery.PROJECTILE), "bow damage");
                expectEmpty(helper, provider.attackModifier(player, target, AttackDelivery.SPELL), "spell attack");
                expectEmpty(helper, provider.damageModifier(player, AttackDelivery.SPELL), "spell damage");

                expectEquals(helper, 5, meleeHit(helper, player, target).attackBonus(), "Critfall melee bonus");
                expectEquals(helper, 6, rangedHit(helper, player, target).attackBonus(), "Critfall ranged bonus");
                return null;
            });

            int strongDamage = unprofiled(() -> meleeHit(helper, player, target).damage());
            run(helper, "checks set " + name + " str 10");
            int plainDamage = unprofiled(() -> meleeHit(helper, player, target).damage());
            expectEquals(helper, 3, strongDamage - plainDamage, "Critfall damage gained from STR 16 over STR 10");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A mob with no Checks profile gets no answers, so Critfall rolls exactly as without Checks. */
    public static void unprofiledMobKeepsCritfallBonus(GameTestHelper helper) {
        Mob zombie = spawnCalm(helper, EntityType.ZOMBIE);
        Mob target = spawnCalm(helper, EntityType.ZOMBIE);
        unprofiled(() -> {
            expectNoAnswers(helper, zombie, target);
            expectSameAsWithoutChecks(helper, () -> meleeHit(helper, zombie, target));
            return null;
        });
        helper.succeed();
    }

    /** STR 16 (+3), DEX 14 (+2), proficiency +3, DEX save proficient. */
    public static void profiledMobGetsChecksValues(GameTestHelper helper) {
        Mob zombie = spawnCalm(helper, EntityType.ZOMBIE);
        Mob target = spawnCalm(helper, EntityType.SKELETON);
        withProfiles(profiledZombie(), () -> {
            ModifierProvider provider = provider();
            expectModifier(helper, 6, provider.attackModifier(zombie, target, AttackDelivery.MELEE), "melee hit");
            expectModifier(helper, 5, provider.attackModifier(zombie, target, AttackDelivery.PROJECTILE), "ranged");
            expectModifier(helper, 3, provider.damageModifier(zombie, AttackDelivery.MELEE), "melee damage");
            expectModifier(helper, 2, provider.damageModifier(zombie, AttackDelivery.PROJECTILE), "ranged damage");
            expectModifier(helper, 5, provider.saveModifier(zombie, ModifierProvider.SPELL_SAVE), "spell save");
            expectEquals(helper, 6, meleeHit(helper, zombie, target).attackBonus(), "Critfall melee bonus");
            return null;
        });
        helper.succeed();
    }

    /** With {@code unprofiled_mobs} on, an unprofiled mob gets its derived Checks values too. */
    public static void unprofiledMobsOptionCoversEveryMob(GameTestHelper helper) {
        Mob zombie = spawnCalm(helper, EntityType.ZOMBIE);
        Mob target = spawnCalm(helper, EntityType.SKELETON);
        ScoresConfig.CritfallSettings live = ScoresRuntime.config().critfall();
        ScoresConfig.CritfallSettings allMobs = new ScoresConfig.CritfallSettings(true, true, live.saveAbilities());
        withCritfallSettings(
                allMobs,
                () -> unprofiled(() -> {
                    ModifierProvider provider = provider();
                    int str = ChecksApi.abilityModifier(zombie, Ability.STRENGTH);
                    int dex = ChecksApi.abilityModifier(zombie, Ability.DEXTERITY);
                    int proficiency = ChecksApi.proficiencyBonus(zombie);
                    expectModifier(
                            helper,
                            str + proficiency,
                            provider.attackModifier(zombie, target, AttackDelivery.MELEE),
                            "melee hit");
                    expectModifier(
                            helper,
                            dex + proficiency,
                            provider.attackModifier(zombie, target, AttackDelivery.PROJECTILE),
                            "ranged");
                    expectModifier(helper, str, provider.damageModifier(zombie, AttackDelivery.MELEE), "damage");
                    expectModifier(
                            helper,
                            ChecksApi.saveModifier(zombie, Ability.DEXTERITY),
                            provider.saveModifier(zombie, ModifierProvider.SPELL_SAVE),
                            "spell save");
                    expectEquals(
                            helper,
                            str + proficiency,
                            meleeHit(helper, zombie, target).attackBonus(),
                            "Critfall melee bonus");
                    return null;
                }));
        helper.succeed();
    }

    /** DEX 16 (+3) with DEX save proficiency, WIS 14 (+2), proficiency +2. */
    public static void spellSaveUsesMappedAbility(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-cf-save"));
        try {
            String name = player.getScoreboardName();
            run(helper, "checks set " + name + " dex 16");
            run(helper, "checks set " + name + " wis 14");
            run(helper, "checks set " + name + " proficiency 2");
            run(helper, "checks prof " + name + " dex proficient");
            ScoresConfig.CritfallSettings live = ScoresRuntime.config().critfall();
            ScoresConfig.CritfallSettings wisdomSave = new ScoresConfig.CritfallSettings(
                    live.enabled(), live.unprofiledMobs(), Map.of(ModifierProvider.SPELL_SAVE, Ability.WISDOM));

            unprofiled(() -> {
                expectModifier(helper, 5, provider().saveModifier(player, ModifierProvider.SPELL_SAVE), "DEX save");
                expectEmpty(helper, provider().saveModifier(player, "critfall:unknown"), "an unknown save key");
                return null;
            });
            withCritfallSettings(
                    wisdomSave,
                    () -> unprofiled(() -> {
                        expectModifier(
                                helper, 2, provider().saveModifier(player, ModifierProvider.SPELL_SAVE), "WIS save");
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Disabled, Checks leaves Critfall's slot empty, so every attack rolls exactly as with no provider at all. */
    public static void disabledMatchesNoChecks(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-cf-off"));
        try {
            String name = player.getScoreboardName();
            run(helper, "checks set " + name + " str 16");
            run(helper, "checks set " + name + " dex 18");
            Mob zombie = spawnCalm(helper, EntityType.ZOMBIE);
            Mob target = spawnCalm(helper, EntityType.SKELETON);
            ScoresConfig.CritfallSettings live = ScoresRuntime.config().critfall();
            ScoresConfig.CritfallSettings off = new ScoresConfig.CritfallSettings(false, true, live.saveAbilities());

            withCritfallSettings(
                    off,
                    () -> withProfiles(profiledZombie(), () -> {
                        expect(helper, RollService.modifierProvider().isEmpty(), "disabled Checks must free the slot");
                        expectSameAsWithoutChecks(helper, () -> meleeHit(helper, player, target));
                        expectSameAsWithoutChecks(helper, () -> rangedHit(helper, player, target));
                        expectSameAsWithoutChecks(helper, () -> meleeHit(helper, zombie, target));
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Turning the provider off frees Critfall's slot; turning it back on (as {@code /reload} does) refills it. */
    public static void toggleFreesAndRefillsTheSlot(GameTestHelper helper) {
        expect(helper, holdsChecks(), "Checks must hold Critfall's slot at startup");
        withCritfallSettings(disabled(), () -> {
            expect(helper, RollService.modifierProvider().isEmpty(), "disabled Checks must free the slot");
            return null;
        });
        expect(helper, holdsChecks(), "re-enabled Checks must register again");
        helper.succeed();
    }

    /** Neither turning Checks off nor back on touches a provider another mod registered. */
    public static void toggleKeepsAnotherModsProvider(GameTestHelper helper) {
        ModifierProvider other = new ModifierProvider() {};
        withOtherModifierProvider(other, () -> {
            withCritfallSettings(disabled(), () -> {
                expect(helper, holdsOnly(other), "disabled Checks must keep another mod's provider");
                return null;
            });
            expect(helper, holdsOnly(other), "re-enabled Checks must not replace another mod's provider");
            return null;
        });
        expect(helper, holdsChecks(), "Checks must hold the slot again once the other provider is gone");
        helper.succeed();
    }

    /**
     * Critfall still counts weapon material, the Strength effect and Sharpness in its own damage, and
     * Checks adds the profiled zombie's STR modifier (+3) on top of each.
     */
    public static void damageFactorsStackWithAbilityModifier(GameTestHelper helper) {
        Mob wooden = armedZombie(helper, new ItemStack(Items.WOODEN_SWORD));
        Mob stone = armedZombie(helper, new ItemStack(Items.STONE_SWORD));
        Mob iron = armedZombie(helper, new ItemStack(Items.IRON_SWORD));
        Mob strong = armedZombie(helper, new ItemStack(Items.IRON_SWORD));
        strong.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_TICKS, 1));
        Mob sharp = armedZombie(helper, sharpnessV(helper));
        Mob target = spawnCalm(helper, EntityType.PIG);

        // A held weapon's attribute modifiers land on the wielder's next tick.
        helper.runAfterDelay(1, () -> {
            withProfiles(profiledZombie(), () -> {
                int woodenDamage = damageOnTopOfCritfall(helper, wooden, target, "wooden sword");
                int stoneDamage = damageOnTopOfCritfall(helper, stone, target, "stone sword");
                int ironDamage = damageOnTopOfCritfall(helper, iron, target, "iron sword");
                int strongDamage = damageOnTopOfCritfall(helper, strong, target, "Strength II");
                int sharpDamage = damageOnTopOfCritfall(helper, sharp, target, "Sharpness V");
                expect(helper, stoneDamage > woodenDamage, "stone must out-damage wooden with Checks");
                expect(helper, strongDamage > ironDamage, "Strength II must add damage with Checks");
                expect(helper, sharpDamage > ironDamage, "Sharpness V must add damage with Checks");
                return null;
            });
            helper.succeed();
        });
    }

    /** The armed zombie's hit damage, checked to be Critfall's own plus the STR modifier. */
    private static int damageOnTopOfCritfall(GameTestHelper helper, Mob attacker, LivingEntity target, String what) {
        int withChecks = heldWeaponHit(helper, attacker, target).damage();
        int withoutChecks = withoutModifierProvider(
                () -> heldWeaponHit(helper, attacker, target).damage());
        expectEquals(helper, PROFILED_ZOMBIE_STR_MODIFIER, withChecks - withoutChecks, what + ": damage Checks adds");
        return withChecks;
    }

    private static Mob armedZombie(GameTestHelper helper, ItemStack weapon) {
        Mob zombie = spawnCalm(helper, EntityType.ZOMBIE);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        return zombie;
    }

    private static ItemStack sharpnessV(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(
                helper.getLevel()
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.SHARPNESS),
                SHARPNESS_V);
        return sword;
    }

    private static ScoresConfig.CritfallSettings disabled() {
        ScoresConfig.CritfallSettings live = ScoresRuntime.config().critfall();
        return new ScoresConfig.CritfallSettings(false, live.unprofiledMobs(), live.saveAbilities());
    }

    private static boolean holdsChecks() {
        return RollService.modifierProvider().orElse(null) instanceof ChecksModifierProvider;
    }

    private static boolean holdsOnly(ModifierProvider provider) {
        return RollService.modifierProvider().orElse(null) == provider;
    }

    private static Map<ResourceLocation, EntityScoreProfile> profiledZombie() {
        return profiles(PROFILED_ZOMBIE);
    }

    private static AttackResult meleeHit(GameTestHelper helper, LivingEntity attacker, LivingEntity target) {
        AttackContext sword =
                AttackContext.melee(attacker.damageSources().mobAttack(attacker), new ItemStack(Items.IRON_SWORD));
        return hit(helper, attacker, target, sword);
    }

    private static AttackResult rangedHit(GameTestHelper helper, LivingEntity attacker, LivingEntity target) {
        AttackContext bow =
                AttackContext.projectile(attacker.damageSources().mobAttack(attacker), new ItemStack(Items.BOW));
        return hit(helper, attacker, target, bow);
    }

    private static AttackResult heldWeaponHit(GameTestHelper helper, LivingEntity attacker, LivingEntity target) {
        AttackContext held =
                AttackContext.melee(attacker.damageSources().mobAttack(attacker), attacker.getMainHandItem());
        return hit(helper, attacker, target, held);
    }

    /** A plain hit: non-d20 dice roll their max, so the damage is deterministic. */
    private static AttackResult hit(
            GameTestHelper helper, LivingEntity attacker, LivingEntity target, AttackContext context) {
        AttackResult result = withD20s(() -> RollService.attackRoll(attacker, target, context), HITTING_D20);
        expect(helper, result.outcome() == AttackOutcome.HIT, "a d20 of " + HITTING_D20 + " must hit, got " + result);
        return result;
    }

    private static void expectSameAsWithoutChecks(GameTestHelper helper, Supplier<AttackResult> attack) {
        AttackResult withChecks = attack.get();
        AttackResult withoutChecks = withoutModifierProvider(attack);
        expect(
                helper,
                withChecks.equals(withoutChecks),
                "Critfall must roll as without Checks: " + withChecks + " vs " + withoutChecks);
    }

    private static void expectNoAnswers(GameTestHelper helper, LivingEntity entity, LivingEntity target) {
        ModifierProvider provider = provider();
        for (AttackDelivery delivery : AttackDelivery.values()) {
            expectEmpty(helper, provider.attackModifier(entity, target, delivery), delivery + " attack");
            expectEmpty(helper, provider.damageModifier(entity, delivery), delivery + " damage");
        }
        expectEmpty(helper, provider.saveModifier(entity, ModifierProvider.SPELL_SAVE), "spell save");
    }

    private static void expectModifier(GameTestHelper helper, int expected, OptionalInt actual, String what) {
        expect(helper, actual.equals(OptionalInt.of(expected)), what + " must be " + expected + ", was " + actual);
    }

    private static void expectEmpty(GameTestHelper helper, OptionalInt actual, String what) {
        expect(helper, actual.isEmpty(), what + " must keep Critfall's own bonus, was " + actual);
    }
}
