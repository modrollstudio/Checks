package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.confirm;
import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectClose;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.saveWorldData;
import static studio.modroll.checks.gametest.ScenarioSupport.savedCharacterStore;
import static studio.modroll.checks.gametest.ScenarioSupport.savedTraitUses;
import static studio.modroll.checks.gametest.ScenarioSupport.spawnCalm;
import static studio.modroll.checks.gametest.ScenarioSupport.standardSkill;
import static studio.modroll.checks.gametest.ScenarioSupport.waitOutSpawnProtection;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withExploration;
import static studio.modroll.checks.gametest.ScenarioSupport.withSaves;
import static studio.modroll.checks.gametest.ScenarioSupport.withSender;
import static studio.modroll.checks.gametest.ScenarioSupport.withTraits;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.body.PlayerBodies;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.CreationSubmission;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.creation.Rejection;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.level.LevelStore;
import studio.modroll.checks.level.PlayerLevel;
import studio.modroll.checks.trait.Darkvision;
import studio.modroll.checks.trait.DarkvisionPayload;
import studio.modroll.checks.trait.TraitEffectType;
import studio.modroll.checks.trait.TraitUses;
import studio.modroll.critfall.api.combat.ContestResult;
import studio.modroll.critfall.api.combat.SaveResult;

/**
 * GameTest bodies for species traits, each effect type on a player of a shipped species that has it. Every
 * score is 10, so every modifier is +0: a d20 of 2 fails and 19 makes the DCs used here.
 */
public final class TraitScenarios {

    private static final int FAIL = 2;
    private static final int SAVE = 19;
    private static final int DC = 13;
    private static final int POISON_TICKS = 200;
    private static final float HIT = 4f;
    private static final float FULL_HEALTH = 20f;
    private static final float LETHAL = 100f;
    private static final double EPSILON = 1e-4;
    private static final ScoresConfig.TraitSettings DEFAULTS = ScoresConfig.DEFAULTS.traits();

    private TraitScenarios() {}

    /** Dwarven Resilience: the poison save keeps the better of 2 and 19; an orc keeps the 2. */
    public static void vanillaSaveAdvantage(GameTestHelper helper) {
        ServerPlayer dwarf = character(helper, "trait-dwarf", "dwarf");
        ServerPlayer orc = character(helper, "trait-orc-poison", "orc");
        try {
            expectEquals(helper, POISON_TICKS / 2, poisonTicks(dwarf, FAIL, SAVE), "dwarf poison");
            expectEquals(helper, POISON_TICKS, poisonTicks(orc, FAIL), "orc poison");
            withTraits(without(TraitEffectType.VANILLA_SAVE_ADVANTAGE), () -> {
                expectEquals(helper, POISON_TICKS, poisonTicks(dwarf, FAIL), "dwarf poison with the effect off");
                return null;
            });
        } finally {
            logout(helper, dwarf);
            logout(helper, orc);
        }
        helper.succeed();
    }

    /** Gnomish Cunning: advantage on every WIS save through Checks; Naturally Stealthy: on Stealth checks. */
    public static void rollBonusGrantsAdvantage(GameTestHelper helper) {
        ServerPlayer gnome = character(helper, "trait-gnome", "gnome");
        ServerPlayer halfling = character(helper, "trait-halfling", "halfling");
        try {
            SaveResult save = withD20s(
                    () -> ChecksApi.savingThrow(gnome, Ability.WISDOM, DC).result(), FAIL, SAVE);
            expect(helper, save.saved(), "a gnome's WIS save must keep the 19");
            SaveResult stealth = withD20s(
                    () -> ChecksApi.check(halfling, standardSkill("stealth"), DC)
                            .result(),
                    FAIL,
                    SAVE);
            expect(helper, stealth.saved(), "a halfling's Stealth check must keep the 19");
            withTraits(without(TraitEffectType.ROLL_BONUS), () -> {
                SaveResult plain = withD20s(
                        () -> ChecksApi.savingThrow(gnome, Ability.WISDOM, DC).result(), FAIL);
                expect(helper, !plain.saved(), "with roll bonuses off the gnome rolls once");
                return null;
            });
        } finally {
            logout(helper, gnome);
            logout(helper, halfling);
        }
        helper.succeed();
    }

    /** Fiendish Legacy halves fire, Draconic Flight halves falls; a human takes it in full. */
    public static void damageResistanceHalvesMatchingDamage(GameTestHelper helper) {
        ServerPlayer tiefling = character(helper, "trait-tiefling", "tiefling");
        ServerPlayer dragonborn = character(helper, "trait-dragon", "dragonborn");
        ServerPlayer human = character(helper, "trait-human2", "human");
        try {
            expectClose(
                    helper, HIT / 2, damage(tiefling, tiefling.damageSources().inFire()), "tiefling in fire");
            expectClose(helper, HIT, damage(tiefling, tiefling.damageSources().fall()), "tiefling falling");
            expectClose(
                    helper,
                    HIT / 2,
                    damage(dragonborn, dragonborn.damageSources().fall()),
                    "dragonborn falling");
            expectClose(helper, HIT, damage(human, human.damageSources().inFire()), "human in fire");
            withTraits(without(TraitEffectType.DAMAGE_RESISTANCE), () -> {
                expectClose(
                        helper, HIT, damage(tiefling, tiefling.damageSources().inFire()), "resistance off");
                return null;
            });
        } finally {
            logout(helper, tiefling);
            logout(helper, dragonborn);
            logout(helper, human);
        }
        helper.succeed();
    }

    /** Dwarven Toughness: half a hit point per level, rounded down, so +2 at level 4. */
    public static void extraHealthFollowsTheLevel(GameTestHelper helper) {
        ServerPlayer dwarf = character(helper, "trait-tough", "dwarf");
        try {
            LevelStore.get(dwarf.server).set(dwarf.getUUID(), new PlayerLevel(4, 270, List.of(), Map.of()));
            PlayerBodies.sync(dwarf);
            expectClose(helper, FULL_HEALTH + 2, dwarf.getMaxHealth(), "a level 4 dwarf's max health");
            withTraits(without(TraitEffectType.EXTRA_HEALTH), () -> {
                PlayerBodies.sync(dwarf);
                expectClose(helper, FULL_HEALTH, dwarf.getMaxHealth(), "max health with extra health off");
                return null;
            });
        } finally {
            logout(helper, dwarf);
        }
        helper.succeed();
    }

    /** The server sends an elf the configured strength and a human none; strength 0 turns it off. */
    public static void darkvisionIsSentToTheClient(GameTestHelper helper) {
        ServerPlayer elf = character(helper, "trait-elf", "elf");
        ServerPlayer human = character(helper, "trait-human3", "human");
        try {
            List<CustomPacketPayload> sent = new ArrayList<>();
            withSender((player, payload) -> player == elf && sent.add(payload), () -> {
                Darkvision.onLogin(elf);
                Darkvision.tick(elf.server);
                return null;
            });
            expect(
                    helper,
                    sent.equals(List.of(new DarkvisionPayload((float) DEFAULTS.darkvisionStrength()))),
                    "the elf must be sent its strength once, was " + sent);
            expectClose(helper, 0, Darkvision.strength(human), "a human's darkvision");
            ScoresConfig.TraitSettings dark =
                    new ScoresConfig.TraitSettings(true, DEFAULTS.rechargeTicks(), 0, DEFAULTS.effects());
            withTraits(dark, () -> {
                expectClose(helper, 0, Darkvision.strength(elf), "an elf's darkvision at strength 0");
                return null;
            });
        } finally {
            logout(helper, elf);
            logout(helper, human);
        }
        helper.succeed();
    }

    /** Luck rerolls a natural 1 on checks and saves through Checks, but never a contest side. */
    public static void luckRerollsNaturalOnes(GameTestHelper helper) {
        ServerPlayer halfling = character(helper, "trait-lucky", "halfling");
        ServerPlayer dwarf = character(helper, "trait-unlucky", "dwarf");
        try {
            SaveResult rerolled = withD20s(
                    () -> ChecksApi.savingThrow(halfling, Ability.STRENGTH, DC).result(), 1, SAVE);
            expectEquals(helper, SAVE, rerolled.natural(), "a halfling's rerolled natural 1");
            SaveResult kept = withD20s(
                    () -> ChecksApi.savingThrow(dwarf, Ability.STRENGTH, DC).result(), 1);
            expectEquals(helper, 1, kept.natural(), "a dwarf's natural 1");
            ContestResult contest = withD20s(
                    () -> ChecksApi.contest(halfling, Ability.STRENGTH, dwarf, Ability.STRENGTH)
                            .result(),
                    1,
                    SAVE);
            expectEquals(helper, 1, contest.initiatorRoll().kept(), "a contest side is never rerolled");
        } finally {
            logout(helper, halfling);
            logout(helper, dwarf);
        }
        helper.succeed();
    }

    /** Resourceful rerolls one failed check a day, and the spent use survives a relog and the world save. */
    public static void resourcefulRerollsOnceADay(GameTestHelper helper) {
        GameProfile profile = newProfile("trait-resourceful");
        ServerPlayer human = character(helper, profile, "human");
        try {
            SaveResult first =
                    withD20s(() -> ChecksApi.check(human, Ability.STRENGTH, DC).result(), FAIL, SAVE);
            expect(helper, first.saved(), "the first failed check of the day must be rerolled");
        } finally {
            logout(helper, human);
        }
        ServerPlayer back = character(helper, profile, "human");
        try {
            SaveResult second =
                    withD20s(() -> ChecksApi.check(back, Ability.STRENGTH, DC).result(), FAIL);
            expect(helper, !second.saved(), "the second failed check of the day stands");
        } finally {
            logout(helper, back);
        }
        saveWorldData(helper);
        expect(
                helper,
                savedTraitUses(helper)
                        .lastUse(profile.getId(), ResourceLocation.parse("checks:resourceful"))
                        .isPresent(),
                "the spent use must be saved with the world");
        helper.succeed();
    }

    /** Relentless Endurance: an orc survives one killing blow a day on 1 HP; the void always kills. */
    public static void lastStandOnceADay(GameTestHelper helper) {
        ServerPlayer orc = character(helper, "trait-orc", "orc");
        ServerPlayer voidOrc = character(helper, "trait-orc-void", "orc");
        try {
            orc.hurt(orc.damageSources().generic(), LETHAL);
            expect(helper, orc.isAlive() && Math.abs(orc.getHealth() - 1f) < EPSILON, "the orc must stand on 1 HP");
            orc.invulnerableTime = 0;
            orc.hurt(orc.damageSources().generic(), LETHAL);
            expect(helper, orc.isDeadOrDying(), "the second killing blow of the day kills");
            voidOrc.hurt(voidOrc.damageSources().fellOutOfWorld(), LETHAL);
            expect(helper, voidOrc.isDeadOrDying(), "the void is never survived");
        } finally {
            logout(helper, orc);
            logout(helper, voidOrc);
        }
        helper.succeed();
    }

    /** A ready last stand comes before a Totem of Undying; once it is spent, the totem saves the orc. */
    public static void lastStandBeforeTheTotem(GameTestHelper helper) {
        ServerPlayer orc = character(helper, "trait-orc-totem", "orc");
        orc.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        try {
            orc.hurt(orc.damageSources().generic(), LETHAL);
            expect(helper, orc.isAlive(), "the last stand must keep the orc alive");
            expect(helper, orc.getOffhandItem().is(Items.TOTEM_OF_UNDYING), "the totem must be kept");

            orc.invulnerableTime = 0;
            orc.hurt(orc.damageSources().generic(), LETHAL);
            expect(helper, orc.isAlive(), "with the last stand spent, the totem must save the orc");
            expect(helper, orc.getOffhandItem().isEmpty(), "the totem must be used once the last stand is spent");
        } finally {
            logout(helper, orc);
        }
        helper.succeed();
    }

    /** With last stand off, the totem saves the orc as in vanilla. */
    public static void totemWhenLastStandIsOff(GameTestHelper helper) {
        ServerPlayer orc = character(helper, "trait-orc-off-totem", "orc");
        orc.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        try {
            withTraits(without(TraitEffectType.LAST_STAND), () -> {
                orc.hurt(orc.damageSources().generic(), LETHAL);
                return null;
            });
            expect(helper, orc.isAlive(), "the totem must save the orc");
            expect(helper, orc.getOffhandItem().isEmpty(), "the totem must be used with last stand off");
            expect(
                    helper,
                    TraitUses.get(orc.server)
                            .lastUse(orc.getUUID(), ResourceLocation.parse("checks:relentless_endurance"))
                            .isEmpty(),
                    "the last stand must not be spent while off");
        } finally {
            logout(helper, orc);
        }
        helper.succeed();
    }

    /** Otherworldly Presence: piglins never target a tiefling; Trance: phantoms never target an elf. */
    public static void ignoredByMobs(GameTestHelper helper) {
        ServerPlayer tiefling = character(helper, "trait-kin", "tiefling");
        ServerPlayer elf = character(helper, "trait-trance", "elf");
        ServerPlayer human = character(helper, "trait-target", "human");
        Mob piglin = spawnCalm(helper, EntityType.PIGLIN);
        Mob phantom = spawnCalm(helper, EntityType.PHANTOM);
        Mob bee = spawnCalm(helper, EntityType.BEE);
        ServerPlayer gnome = character(helper, "trait-beekeeper", "gnome");
        try {
            expect(helper, !piglin.canAttack(tiefling), "a piglin must ignore a tiefling");
            expect(helper, !phantom.canAttack(elf), "a phantom must ignore an elf");
            expect(helper, !bee.canAttack(gnome), "a bee must ignore a gnome");
            expect(helper, piglin.canAttack(human), "a piglin may attack a human");
            withTraits(without(TraitEffectType.IGNORED_BY), () -> {
                expect(helper, piglin.canAttack(tiefling), "with ignored_by off a piglin may attack a tiefling");
                return null;
            });
        } finally {
            logout(helper, tiefling);
            logout(helper, elf);
            logout(helper, human);
            logout(helper, gnome);
        }
        helper.succeed();
    }

    /** Keen Senses: an elf picks one of Insight, Perception or Survival; it is saved and switches off. */
    public static void skillChoicesAtCreation(GameTestHelper helper) {
        ServerPlayer elf = login(helper, newProfile("trait-keen"));
        ResourceLocation perception = standardSkill("perception").id();
        try {
            expect(
                    helper,
                    submitElf(elf, List.of(standardSkill("athletics").id()))
                            .equals(Optional.of(Rejection.SKILL_NOT_FROM_SPECIES)),
                    "an elf may not pick Athletics with Keen Senses");
            expect(helper, submitElf(elf, List.of(perception)).isEmpty(), "an elf may pick Perception");
            expect(
                    helper,
                    ChecksApi.skillProficiency(elf, standardSkill("perception")) == Proficiency.PROFICIENT,
                    "the picked skill must be proficient");
            withTraits(without(TraitEffectType.SKILL_CHOICES), () -> {
                expect(
                        helper,
                        ChecksApi.skillProficiency(elf, standardSkill("perception")) == Proficiency.NONE,
                        "with skill choices off the species pick no longer counts");
                return null;
            });
        } finally {
            logout(helper, elf);
        }
        saveWorldData(helper);
        expect(
                helper,
                savedCharacterStore(helper)
                        .character(elf.getUUID())
                        .build()
                        .map(build -> build.grants().traitSkills().contains(perception))
                        .orElse(false),
                "the species pick must be saved with the world");
        helper.succeed();
    }

    /** Large Form and Giant Ancestry: a goliath reaches further and hits harder; the modifiers go when off. */
    public static void attributeModifiers(GameTestHelper helper) {
        ServerPlayer goliath = character(helper, "trait-goliath", "goliath");
        ServerPlayer human = character(helper, "trait-human4", "human");
        try {
            double reach = human.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
            expectClose(helper, reach + 0.5, attribute(goliath, Attributes.ENTITY_INTERACTION_RANGE), "goliath reach");
            expectClose(
                    helper,
                    human.getAttributeValue(Attributes.ATTACK_KNOCKBACK) + 0.5,
                    attribute(goliath, Attributes.ATTACK_KNOCKBACK),
                    "goliath knockback");
            withTraits(without(TraitEffectType.ATTRIBUTE), () -> {
                expectClose(helper, reach, attribute(goliath, Attributes.ENTITY_INTERACTION_RANGE), "reach when off");
                return null;
            });
        } finally {
            logout(helper, goliath);
            logout(helper, human);
        }
        helper.succeed();
    }

    /** With traits off, a dwarf, a tiefling and an orc are plain players again. */
    public static void masterSwitchTurnsEveryTraitOff(GameTestHelper helper) {
        ServerPlayer dwarf = character(helper, "trait-off-dwarf", "dwarf");
        ServerPlayer tiefling = character(helper, "trait-off-tiefling", "tiefling");
        ServerPlayer orc = character(helper, "trait-off-orc", "orc");
        ScoresConfig.TraitSettings off = new ScoresConfig.TraitSettings(
                false, DEFAULTS.rechargeTicks(), DEFAULTS.darkvisionStrength(), DEFAULTS.effects());
        try {
            withTraits(off, () -> {
                expectEquals(helper, POISON_TICKS, poisonTicks(dwarf, FAIL), "dwarf poison");
                expectClose(
                        helper, HIT, damage(tiefling, tiefling.damageSources().inFire()), "tiefling in fire");
                orc.hurt(orc.damageSources().generic(), LETHAL);
                expect(helper, orc.isDeadOrDying(), "an orc dies with traits off");
                return null;
            });
        } finally {
            logout(helper, dwarf);
            logout(helper, tiefling);
            logout(helper, orc);
        }
        helper.succeed();
    }

    private static Optional<Rejection> submitElf(ServerPlayer elf, List<ResourceLocation> speciesSkills) {
        return CharacterCreation.submit(
                elf,
                new CreationSubmission(
                        CreationMethod.STANDARD_ARRAY,
                        List.of(15, 14, 13, 12, 10, 8),
                        List.of(0, 0, 0, 0, 0, 0),
                        List.of(
                                standardSkill("stealth").id(),
                                standardSkill("arcana").id()),
                        new PresetChoices(
                                Optional.of(ResourceLocation.parse("checks:elf")),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()),
                        speciesSkills));
    }

    private static double attribute(ServerPlayer player, Holder<Attribute> attribute) {
        PlayerBodies.sync(player);
        return player.getAttributeValue(attribute);
    }

    private static int poisonTicks(ServerPlayer player, int... faces) {
        player.removeAllEffects();
        noCooldown(
                () -> withD20s(() -> player.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS)), faces));
        return player.getEffect(MobEffects.POISON).getDuration();
    }

    /** With Acrobatics landings off, so a fall is never softened by a roll and the damage is the trait's alone. */
    private static float damage(ServerPlayer player, DamageSource source) {
        return withExploration(withoutLandings(), () -> {
            player.setHealth(FULL_HEALTH);
            player.invulnerableTime = 0;
            player.hurt(source, HIT);
            return FULL_HEALTH - player.getHealth();
        });
    }

    private static ScoresConfig.ExplorationSettings withoutLandings() {
        ScoresConfig.ExplorationSettings defaults = ScoresConfig.DEFAULTS.exploration();
        ScoresConfig.LandingSettings landing = defaults.landing();
        return new ScoresConfig.ExplorationSettings(
                defaults.leap(),
                new ScoresConfig.LandingSettings(
                        false, landing.baseDc(), landing.dcPerDamage(), landing.successMultiplier()),
                defaults.cobwebs(),
                defaults.climbing(),
                defaults.sneak(),
                defaults.spotTripwires(),
                defaults.disarmTripwires(),
                defaults.searchChests(),
                defaults.monsterLore(),
                defaults.structureLore(),
                defaults.taming(),
                defaults.hunger(),
                defaults.ailments());
    }

    private static <T> T noCooldown(Supplier<T> body) {
        ScoresConfig.SaveSettings saves = ScoresConfig.DEFAULTS.saves();
        return withSaves(new ScoresConfig.SaveSettings(false, 0, saves.knockbackMinStrength(), saves.saves()), body);
    }

    private static ScoresConfig.TraitSettings without(TraitEffectType off) {
        Map<TraitEffectType, Boolean> effects = new EnumMap<>(DEFAULTS.effects());
        effects.put(off, false);
        return new ScoresConfig.TraitSettings(
                DEFAULTS.enabled(), DEFAULTS.rechargeTicks(), DEFAULTS.darkvisionStrength(), effects);
    }

    private static ServerPlayer character(GameTestHelper helper, String name, String species) {
        return character(helper, newProfile(name), species);
    }

    /** A survival player of a shipped species, past spawn protection, at full health. */
    private static ServerPlayer character(GameTestHelper helper, GameProfile profile, String species) {
        ServerPlayer player = login(helper, profile);
        player.setGameMode(GameType.SURVIVAL);
        waitOutSpawnProtection(player);
        confirm(player, ResourceLocation.fromNamespaceAndPath("checks", species), Optional.empty());
        PlayerBodies.sync(player);
        player.setHealth(player.getMaxHealth());
        return player;
    }
}
