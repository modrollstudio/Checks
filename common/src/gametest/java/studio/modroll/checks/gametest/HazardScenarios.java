package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.bonusSources;
import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.provider;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.waitOutSpawnProtection;
import static studio.modroll.checks.gametest.ScenarioSupport.withBonusSources;
import static studio.modroll.checks.gametest.ScenarioSupport.withCritfallSettings;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withListeners;
import static studio.modroll.checks.gametest.ScenarioSupport.withoutModifierProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.bonus.BonusTargets;
import studio.modroll.checks.critfall.HazardRolls;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.trait.Trait;
import studio.modroll.checks.trait.TraitEffect;
import studio.modroll.checks.trait.Traits;
import studio.modroll.critfall.api.HazardSave;
import studio.modroll.critfall.api.ModifierProvider;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.SaveResult;
import studio.modroll.critfall.api.dice.DiceExpression;
import studio.modroll.critfall.api.dice.RollMode;

/**
 * GameTest bodies for Critfall hazard saves (a trap from Dungeon Crawl or any other mod): the save key's
 * ability supplies the modifier, and species traits add their advantage. The gametest data tags {@link
 * #POISON_DART} {@code c:is_poison}; cactus is not. A d20 of 2 fails DC 13 with any modifier here, 19 makes
 * it.
 */
public final class HazardScenarios {

    private static final int FAIL = 2;
    private static final int SAVE = 19;
    private static final int DC = 13;
    private static final String TRAP = "Spike trap";
    private static final DiceExpression DAMAGE = DiceExpression.parse("1d4");
    private static final ResourceKey<DamageType> POISON_DART =
            ResourceKey.create(Registries.DAMAGE_TYPE, Checks.id("gametest/poison_dart"));
    private static final String MOD_TRAP_SAVE = "mymod:trap";
    private static final String UNMAPPED_SAVE = "mymod:unmapped";
    private static final float WORLD_DAMAGE = 4f;
    private static final int EFFECT_TICKS = 200;
    private static final int TRAIT_BONUS = 2;
    private static final int SOURCE_BONUS = 3;
    private static final ResourceLocation GNOMISH_CUNNING = Checks.id("gnomish_cunning");

    private static final String WARDED = "{\"source\": {\"effect\": \"minecraft:resistance\"},"
            + " \"applies_to\": {\"saves\": [\"wis\"]}, \"bonus\": " + SOURCE_BONUS + "}";
    private static final String NIMBLE = "{\"source\": {\"effect\": \"minecraft:speed\"},"
            + " \"applies_to\": {\"saves\": [\"dex\"]}, \"mode\": \"advantage\"}";
    private static final String SLOWED = "{\"source\": {\"effect\": \"minecraft:slowness\"},"
            + " \"applies_to\": {\"saves\": [\"dex\"]}, \"mode\": \"disadvantage\"}";

    private HazardScenarios() {}

    /** DEX 16 (+3), proficiency +2: a DEX-proficient player saves at +5, one without at +3. */
    public static void dexSaveUsesModifierAndProficiency(GameTestHelper helper) {
        ServerPlayer proficient = survivor(helper, "hazard-dex-prof");
        ServerPlayer plain = survivor(helper, "hazard-dex-plain");
        try {
            for (ServerPlayer player : new ServerPlayer[] {proficient, plain}) {
                run(helper, "checks set " + player.getScoreboardName() + " dex 16");
                run(helper, "checks set " + player.getScoreboardName() + " proficiency 2");
            }
            run(helper, "checks prof " + proficient.getScoreboardName() + " dex proficient");

            HazardSave spikes = hazard(DamageTypes.CACTUS, ModifierProvider.DEXTERITY_SAVE);
            expectEquals(helper, 5, save(proficient, spikes, FAIL).saveBonus(), "proficient DEX hazard save bonus");
            expectEquals(helper, 3, save(plain, spikes, FAIL).saveBonus(), "plain DEX hazard save bonus");
        } finally {
            logout(helper, proficient);
            logout(helper, plain);
        }
        helper.succeed();
    }

    /**
     * CON 14 (+2): a key {@code save_abilities} maps to CON saves with CON; a key it does not map keeps
     * Critfall's own save bonus, as without Checks.
     */
    public static void mappedKeyUsesItsAbilityAndUnknownKeyIsUntouched(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "hazard-mapped");
        try {
            run(helper, "checks set " + player.getScoreboardName() + " con 14");
            ScoresConfig.CritfallSettings live = ScoresRuntime.config().critfall();
            Map<String, Ability> saveAbilities = new HashMap<>(live.saveAbilities());
            saveAbilities.put(MOD_TRAP_SAVE, Ability.CONSTITUTION);
            ScoresConfig.CritfallSettings mapped =
                    new ScoresConfig.CritfallSettings(live.enabled(), live.unprofiledMobs(), saveAbilities);

            withCritfallSettings(mapped, () -> {
                HazardSave modTrap = hazard(DamageTypes.CACTUS, MOD_TRAP_SAVE);
                expectEquals(helper, 2, save(player, modTrap, FAIL).saveBonus(), "mapped key's CON save bonus");

                HazardSave unmapped = hazard(DamageTypes.CACTUS, UNMAPPED_SAVE);
                expect(
                        helper,
                        provider().saveModifier(player, UNMAPPED_SAVE).isEmpty(),
                        "an unmapped save key must get no Checks modifier");
                int withoutChecks = withoutModifierProvider(
                        () -> save(player, unmapped, FAIL).saveBonus());
                expectEquals(
                        helper, withoutChecks, save(player, unmapped, FAIL).saveBonus(), "unmapped key's save bonus");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Dwarven Resilience: a dwarf keeps the better of 2 and 19 against a c:is_poison hazard, any other just the 2. */
    public static void dwarfHasAdvantageOnPoisonHazards(GameTestHelper helper) {
        ServerPlayer dwarf = TraitScenarios.character(helper, "hazard-dwarf", "dwarf");
        ServerPlayer orc = TraitScenarios.character(helper, "hazard-orc", "orc");
        try {
            expect(helper, poisonTagged(helper, POISON_DART), "the gametest poison dart must be tagged c:is_poison");
            expect(helper, !poisonTagged(helper, DamageTypes.CACTUS), "cactus must not be tagged c:is_poison");

            HazardSave poisonDart = hazard(POISON_DART, ModifierProvider.CONSTITUTION_SAVE);
            SaveResult poisoned = save(dwarf, poisonDart, FAIL, SAVE);
            expect(helper, poisoned.saved(), "a dwarf must keep the 19 against poison, got " + poisoned);
            expect(helper, poisoned.roll().mode() == RollMode.ADVANTAGE, "a dwarf's poison save rolls with advantage");

            SaveResult spikes = save(dwarf, hazard(DamageTypes.CACTUS, ModifierProvider.CONSTITUTION_SAVE), FAIL);
            expect(helper, !spikes.saved(), "a dwarf rolls once against an untagged hazard, got " + spikes);
            expect(helper, spikes.roll().mode() == RollMode.NORMAL, "a dwarf's untagged save rolls normally");

            SaveResult orcPoisoned = save(orc, poisonDart, FAIL);
            expect(helper, !orcPoisoned.saved(), "an orc rolls once against poison, got " + orcPoisoned);
        } finally {
            logout(helper, dwarf);
            logout(helper, orc);
        }
        helper.succeed();
    }

    /** Gnomish Cunning: advantage on a WIS hazard save, none on a DEX one. */
    public static void gnomeHasAdvantageOnMentalHazardSaves(GameTestHelper helper) {
        ServerPlayer gnome = TraitScenarios.character(helper, "hazard-gnome", "gnome");
        try {
            SaveResult wisdom = save(gnome, hazard(DamageTypes.MAGIC, ModifierProvider.WISDOM_SAVE), FAIL, SAVE);
            expect(helper, wisdom.saved(), "a gnome must keep the 19 on a WIS hazard save, got " + wisdom);
            SaveResult dexterity = save(gnome, hazard(DamageTypes.CACTUS, ModifierProvider.DEXTERITY_SAVE), FAIL);
            expect(helper, !dexterity.saved(), "a gnome rolls once on a DEX hazard save, got " + dexterity);
        } finally {
            logout(helper, gnome);
        }
        helper.succeed();
    }

    /** Luck: a halfling's natural 1 on a hazard save is rolled again; a dwarf keeps it. */
    public static void halflingRerollsNaturalOneOnHazardSave(GameTestHelper helper) {
        ServerPlayer halfling = TraitScenarios.character(helper, "hazard-halfling", "halfling");
        ServerPlayer dwarf = TraitScenarios.character(helper, "hazard-halfling-dwarf", "dwarf");
        try {
            HazardSave spikes = hazard(DamageTypes.CACTUS, ModifierProvider.DEXTERITY_SAVE);
            SaveResult lucky = save(halfling, spikes, 1, SAVE);
            expectEquals(helper, SAVE, lucky.natural(), "a halfling's rerolled natural 1 on a hazard save");
            expect(helper, lucky.saved(), "a halfling's reroll of 19 must save, got " + lucky);
            expectEquals(helper, 1, save(dwarf, spikes, 1, SAVE).natural(), "a dwarf's natural 1 on a hazard save");
        } finally {
            logout(helper, halfling);
            logout(helper, dwarf);
        }
        helper.succeed();
    }

    /**
     * A gnome's WIS hazard save adds a trait roll_bonus (Gnomish Cunning patched to a flat +2) and a datapack
     * bonus source (+3 while under Resistance) on top of the save modifier.
     */
    public static void traitAndSourceBonusesAddToHazardSave(GameTestHelper helper) {
        ServerPlayer gnome = TraitScenarios.character(helper, "hazard-bonus-gnome", "gnome");
        try {
            HazardSave whispers = hazard(DamageTypes.MAGIC, ModifierProvider.WISDOM_SAVE);
            int modifier =
                    provider().saveModifier(gnome, ModifierProvider.WISDOM_SAVE).getAsInt();
            TraitEffect flat = new TraitEffect.RollBonus(
                    new BonusTargets(Set.of(), Set.of(), Set.of(Ability.WISDOM), false, false),
                    TRAIT_BONUS,
                    RollMode.NORMAL);

            withTrait(new Trait(GNOMISH_CUNNING, List.of(flat)), () -> {
                expectEquals(helper, modifier + TRAIT_BONUS, bonusOf(save(gnome, whispers, FAIL)), "trait bonus");
                gnome.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_TICKS));
                withBonusSources(bonusSources(WARDED), () -> {
                    expectEquals(
                            helper,
                            modifier + TRAIT_BONUS + SOURCE_BONUS,
                            bonusOf(save(gnome, whispers, FAIL)),
                            "trait and bonus source");
                    return null;
                });
                return null;
            });
        } finally {
            logout(helper, gnome);
        }
        helper.succeed();
    }

    /** A datapack source's advantage keeps the 19 of 2 and 19; its disadvantage keeps the 2 of 19 and 2. */
    public static void sourceModesApplyToHazardSave(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "hazard-source-mode");
        try {
            HazardSave spikes = hazard(DamageTypes.CACTUS, ModifierProvider.DEXTERITY_SAVE);
            withBonusSources(bonusSources(NIMBLE, SLOWED), () -> {
                expect(helper, save(player, spikes, FAIL).roll().mode() == RollMode.NORMAL, "no effect, no mode");

                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_TICKS));
                SaveResult nimble = save(player, spikes, FAIL, SAVE);
                expect(helper, nimble.roll().mode() == RollMode.ADVANTAGE, "a source's advantage, got " + nimble);
                expect(helper, nimble.saved(), "advantage must keep the 19, got " + nimble);
                player.removeAllEffects();

                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_TICKS));
                SaveResult slowed = save(player, spikes, SAVE, FAIL);
                expect(helper, slowed.roll().mode() == RollMode.DISADVANTAGE, "a source's disadvantage, got " + slowed);
                expect(helper, !slowed.saved(), "disadvantage must keep the 2, got " + slowed);
                player.removeAllEffects();
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** With Checks' Critfall integration off, a dwarf rolls a poison hazard once, as without Checks. */
    public static void disabledIntegrationGrantsNoAdvantage(GameTestHelper helper) {
        ServerPlayer dwarf = TraitScenarios.character(helper, "hazard-off-dwarf", "dwarf");
        try {
            ScoresConfig.CritfallSettings live = ScoresRuntime.config().critfall();
            ScoresConfig.CritfallSettings off =
                    new ScoresConfig.CritfallSettings(false, live.unprofiledMobs(), live.saveAbilities());
            withCritfallSettings(off, () -> {
                SaveResult poisoned = save(dwarf, hazard(POISON_DART, ModifierProvider.CONSTITUTION_SAVE), FAIL);
                expect(helper, !poisoned.saved(), "a dwarf rolls once with the integration off, got " + poisoned);
                return null;
            });
        } finally {
            logout(helper, dwarf);
        }
        helper.succeed();
    }

    /**
     * One roll per hit: a hazard's explosion or fall damage, already saved against through Critfall, calls
     * for no DEX save or Acrobatics landing; the same damage from the world still does.
     */
    public static void hazardDamageCallsForNoSecondRoll(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "hazard-one-roll");
        try {
            HazardSave blast = hazard(DamageTypes.EXPLOSION, ModifierProvider.DEXTERITY_SAVE);
            expectEquals(
                    helper, 0, checksRolls(() -> save(player, blast, FAIL)), "Checks rolls for a hazard's explosion");
            expectEquals(
                    helper,
                    1,
                    checksRolls(() -> hurt(player, player.damageSources().explosion(null, null))),
                    "Checks rolls for a vanilla explosion");

            HazardSave pit = hazard(DamageTypes.FALL, ModifierProvider.DEXTERITY_SAVE);
            expectEquals(helper, 0, checksRolls(() -> save(player, pit, FAIL)), "Checks rolls for a hazard's fall");
            expectEquals(
                    helper,
                    1,
                    checksRolls(() -> hurt(player, player.damageSources().fall())),
                    "Checks rolls for a vanilla fall");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    private static HazardSave hazard(ResourceKey<DamageType> damageType, String saveKey) {
        return HazardSave.of(TRAP, damageType, saveKey, DC, DAMAGE);
    }

    /** The hazard save on a player at full health, with the d20s forced; the damage rolls its max. */
    private static SaveResult save(ServerPlayer player, HazardSave hazard, int... faces) {
        player.setHealth(player.getMaxHealth());
        return withD20s(() -> RollService.performHazardSave(player, hazard), faces)
                .save();
    }

    /** What the save added to its d20: the modifier and every bonus. */
    private static int bonusOf(SaveResult result) {
        return result.saveTotal() - result.natural();
    }

    /** Runs {@code body} with {@code trait} in place of the loaded trait of the same id. */
    private static <T> T withTrait(Trait trait, Supplier<T> body) {
        Map<ResourceLocation, Trait> before = Traits.all();
        Map<ResourceLocation, Trait> patched = new HashMap<>(before);
        patched.put(trait.id(), trait);
        Traits.set(patched);
        try {
            return body.get();
        } finally {
            Traits.set(before);
        }
    }

    private static boolean poisonTagged(GameTestHelper helper, ResourceKey<DamageType> type) {
        return helper.getLevel()
                .registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type)
                .is(HazardRolls.POISON);
    }

    /** Past spawn protection, so the world's damage reaches the saves. */
    private static ServerPlayer survivor(GameTestHelper helper, String name) {
        ServerPlayer player = login(helper, newProfile(name));
        player.setGameMode(GameType.SURVIVAL);
        waitOutSpawnProtection(player);
        return player;
    }

    private static void hurt(ServerPlayer player, DamageSource source) {
        player.setHealth(player.getMaxHealth());
        player.invulnerableTime = 0;
        player.hurt(source, WORLD_DAMAGE);
    }

    /** The checks and saves Checks rolls during {@code body}; Critfall's hazard save is not one. */
    private static int checksRolls(Runnable body) {
        AtomicInteger count = new AtomicInteger();
        withListeners(before -> {}, after -> count.incrementAndGet(), () -> {
            body.run();
            return null;
        });
        return count.get();
    }
}
