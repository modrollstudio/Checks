package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.collecting;
import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.levellingEnabled;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.profiles;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.runExpectingFailure;
import static studio.modroll.checks.gametest.ScenarioSupport.saveWorldData;
import static studio.modroll.checks.gametest.ScenarioSupport.savedLevelStore;
import static studio.modroll.checks.gametest.ScenarioSupport.spawnCalm;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.waitOutSpawnProtection;
import static studio.modroll.checks.gametest.ScenarioSupport.withLevelling;
import static studio.modroll.checks.gametest.ScenarioSupport.withProfiles;
import static studio.modroll.checks.gametest.ScenarioSupport.withSender;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.LevelUpEvent;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.body.PlayerBodies;
import studio.modroll.checks.creation.CharacterBuild;
import studio.modroll.checks.creation.CharacterStore;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.creation.PresetGrants;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.level.ImprovementRejection;
import studio.modroll.checks.level.LevelGain;
import studio.modroll.checks.level.LevelRules;
import studio.modroll.checks.level.LevelStore;
import studio.modroll.checks.level.Levelling;
import studio.modroll.checks.level.PlayerLevel;
import studio.modroll.checks.sheet.StatSheet;
import studio.modroll.checks.sheet.StatSheetPayload;
import studio.modroll.checks.sheet.StatSheets;
import studio.modroll.critfall.api.CombatSuppression;

public final class LevelScenarios {

    private static final ScoresConfig.LevellingSettings DEFAULTS = ScoresConfig.DEFAULTS.levelling();
    private static final ResourceLocation STONE_AGE = ResourceLocation.withDefaultNamespace("story/mine_stone");
    private static final ResourceLocation NETHER = ResourceLocation.withDefaultNamespace("story/enter_the_nether");
    private static final ResourceLocation ADVENTURE = ResourceLocation.withDefaultNamespace("adventure/root");
    private static final float LETHAL = 100f;
    private static final ResourceLocation RECIPE =
            ResourceLocation.withDefaultNamespace("recipes/building_blocks/stone_bricks");
    private static final Optional<ResourceLocation> FIGHTER = Optional.of(ResourceLocation.parse("checks:fighter"));
    private static final Optional<ResourceLocation> ROGUE = Optional.of(ResourceLocation.parse("checks:rogue"));
    private static final ResourceLocation STEALTH = ResourceLocation.parse("checks:stealth");
    private static final ResourceLocation PERCEPTION = ResourceLocation.parse("checks:perception");
    private static final ResourceLocation ATHLETICS = ResourceLocation.parse("checks:athletics");
    private static final ResourceLocation ARCANA = ResourceLocation.parse("checks:arcana");
    private static final List<Integer> PLUS_TWO_STR = List.of(2, 0, 0, 0, 0, 0);
    private static final List<Integer> PLUS_ONE_STR_DEX = List.of(1, 1, 0, 0, 0, 0);

    private LevelScenarios() {}

    public static void vanillaXpCountsWhenGained(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-xp"));
        try {
            levelled(() -> {
                expectProgress(helper, player, 1, 0, "a new player");
                player.giveExperiencePoints(30);
                expectProgress(helper, player, 2, 30, "after 30 vanilla points");
                player.onEnchantmentPerformed(ItemStack.EMPTY, 1);
                player.giveExperienceLevels(-1);
                player.giveExperiencePoints(-10);
                player.kill();
                expectProgress(helper, player, 2, 30, "after enchanting, an anvil cost, losing points and dying");
                return null;
            });
            withLevelling(sources(new ScoresConfig.XpSources(true, 3, true, 25)), () -> {
                player.giveExperiencePoints(10);
                return null;
            });
            withLevelling(sources(new ScoresConfig.XpSources(false, 1, true, 25)), () -> {
                player.giveExperiencePoints(100);
                return null;
            });
            levelled(() -> {
                expectProgress(helper, player, 2, 60, "after 10 points at 3 each, and 100 with the source off");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void dyingToAMobGivesNoXp(GameTestHelper helper) {
        expectNoXpFromDeath(helper, "checks-lv-death", false);
        expectNoXpFromDeath(helper, "checks-lv-death-keep", true);
        helper.succeed();
    }

    private static void expectNoXpFromDeath(GameTestHelper helper, String name, boolean keepInventory) {
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
        boolean previousRule = rule.get();
        rule.set(keepInventory, helper.getLevel().getServer());
        ServerPlayer player = survivor(helper, name);
        ServerPlayer respawned = player;
        List<String> events = new ArrayList<>();
        ChecksApi.onLevelUp(event -> recordFor(player, event, events));
        String when = keepInventory ? " with keepInventory" : " without keepInventory";
        try {
            respawned = levelled(() -> {
                Levelling.setLevel(player, 2);
                events.clear();
                killedByZoglin(helper, player);
                expect(helper, earned(player, ADVENTURE), "dying to a mob must earn the Adventure advancement" + when);
                expectProgress(helper, player, 2, 30, "after dying" + when);
                ServerPlayer next = player.server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
                expectProgress(helper, next, 2, 30, "after respawning" + when);
                expectEvents(helper, events, List.of(), "dying and respawning" + when);
                return next;
            });
        } finally {
            logout(helper, respawned);
            rule.set(previousRule, helper.getLevel().getServer());
        }
    }

    private static ServerPlayer survivor(GameTestHelper helper, String name) {
        ServerPlayer player = login(helper, newProfile(name));
        player.setGameMode(GameType.SURVIVAL);
        waitOutSpawnProtection(player);
        return player;
    }

    private static void killedByZoglin(GameTestHelper helper, ServerPlayer player) {
        Mob zoglin = spawnCalm(helper, EntityType.ZOGLIN);
        List<UUID> fighters = List.of(player.getUUID(), zoglin.getUUID());
        fighters.forEach(CombatSuppression::suppress);
        try {
            player.hurt(helper.getLevel().damageSources().mobAttack(zoglin), LETHAL);
        } finally {
            fighters.forEach(CombatSuppression::release);
            zoglin.discard();
        }
    }

    public static void orbCountsInFullWithMending(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-mending"));
        try {
            levelled(() -> {
                ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                pickaxe.enchant(
                        helper.getLevel()
                                .registryAccess()
                                .registryOrThrow(Registries.ENCHANTMENT)
                                .getHolderOrThrow(Enchantments.MENDING),
                        1);
                pickaxe.setDamageValue(500);
                player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);

                collectOrb(helper, player, 10);
                expectOrbOutcome(helper, player, 480, 0, 10, "an orb spent on Mending");
                player.getMainHandItem().setDamageValue(4);
                collectOrb(helper, player, 10);
                expectOrbOutcome(helper, player, 0, 8, 20, "an orb partly spent on Mending");
                collectOrb(helper, player, 10);
                expectOrbOutcome(helper, player, 0, 18, 30, "an orb with nothing to repair");
                expectEquals(helper, 2, ChecksApi.level(player), "level after three orbs");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void advancementsGiveXpOnce(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-adv"));
        try {
            levelled(() -> {
                award(helper, player, STONE_AGE);
                expectEquals(helper, 25, ChecksApi.experience(player), "XP after Stone Age");
                award(helper, player, STONE_AGE);
                award(helper, player, RECIPE);
                expectEquals(helper, 25, ChecksApi.experience(player), "XP after repeats and a recipe unlock");
                return null;
            });
            withLevelling(sources(new ScoresConfig.XpSources(true, 1, false, 25)), () -> {
                award(helper, player, NETHER);
                return null;
            });
            levelled(() -> {
                expectEquals(helper, 25, ChecksApi.experience(player), "XP after an advancement with the source off");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void levelUpFiresOncePerLevel(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-events"));
        List<String> events = new ArrayList<>();
        ChecksApi.onLevelUp(event -> recordFor(player, event, events));
        try {
            levelled(() -> {
                String name = player.getScoreboardName();
                run(helper, "checks xp " + name + " add 300");
                expectEvents(helper, events, List.of("1>2", "2>3", "3>4"), "300 XP");
                run(helper, "checks xp " + name + " add 10");
                run(helper, "checks level " + name + " set 4");
                run(helper, "checks level " + name + " set 2");
                expectEvents(helper, events, List.of(), "staying at level 4, then lowering to 2");
                run(helper, "checks level " + name + " set 5");
                expectEvents(helper, events, List.of("2>3", "3>4", "4>5"), "setting level 5 from 2");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void proficiencyFollowsLevel(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-prof"));
        try {
            String name = player.getScoreboardName();
            levelled(() -> unprofiled(() -> {
                for (int level = 1; level <= 20; level++) {
                    run(helper, "checks level " + name + " set " + level);
                    expectEquals(
                            helper,
                            LevelRules.proficiencyBonus(level, DEFAULTS),
                            ChecksApi.proficiencyBonus(player),
                            "proficiency bonus at level " + level);
                }
                int overProfile = withProfiles(
                        profiles("{\"matches\": [\"minecraft:player\"], \"proficiency\": {\"bonus\": 4}}"),
                        () -> ChecksApi.proficiencyBonus(player));
                expectEquals(helper, 6, overProfile, "the level 20 bonus over a player profile");
                run(helper, "checks set " + name + " proficiency 9");
                expectEquals(helper, 9, ChecksApi.proficiencyBonus(player), "a command bonus at level 20");
                Mob zombie = ScenarioSupport.spawnCalm(helper, EntityType.ZOMBIE);
                expectEquals(helper, 2, ChecksApi.proficiencyBonus(zombie), "an unprofiled zombie");
                return null;
            }));
            Mob profiled = ScenarioSupport.spawnCalm(helper, EntityType.ZOMBIE);
            int bonus = levelled(() -> withProfiles(
                    profiles("{\"matches\": [\"minecraft:zombie\"], \"proficiency\": {\"bonus\": 4}}"),
                    () -> ChecksApi.proficiencyBonus(profiled)));
            expectEquals(helper, 4, bonus, "a profiled zombie");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void abilityScoreImprovements(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-asi"));
        List<CustomPacketPayload> sent = new ArrayList<>();
        try {
            String name = player.getScoreboardName();
            levelled(() -> withSender(
                    collecting(StatSheetPayload.class, sent),
                    () -> withProfiles(
                            profiles("{\"matches\": [\"minecraft:player\"], \"abilities\": {\"str\": 19}}"), () -> {
                                expectRejected(helper, player, PLUS_TWO_STR, ImprovementRejection.NONE_PENDING);
                                run(helper, "checks level " + name + " set 4");
                                expectEquals(helper, 1, pending(player), "pending at level 4");
                                expectRejected(helper, player, PLUS_TWO_STR, ImprovementRejection.OVER_MAX);
                                expectRejected(
                                        helper, player, List.of(1, 0, 0, 0, 0, 0), ImprovementRejection.WRONG_SPLIT);
                                expect(
                                        helper,
                                        Levelling.improve(player, PLUS_ONE_STR_DEX)
                                                .isEmpty(),
                                        "+1 STR +1 DEX must be accepted");
                                expectEquals(helper, 20, ChecksApi.abilityScore(player, Ability.STRENGTH), "STR");
                                expectEquals(helper, 11, ChecksApi.abilityScore(player, Ability.DEXTERITY), "DEX");
                                expectEquals(helper, 0, pending(player), "pending once chosen");
                                run(helper, "checks level " + name + " set 12");
                                expectEquals(helper, 2, pending(player), "pending at level 12");
                                expect(
                                        helper,
                                        Levelling.improve(player, List.of(0, 0, 2, 0, 0, 0))
                                                .isEmpty(),
                                        "+2 CON must be accepted");
                                expectEquals(helper, 1, pending(player), "pending after one of two");
                                expectEquals(helper, 12, ChecksApi.abilityScore(player, Ability.CONSTITUTION), "CON");
                                return null;
                            })));
            long sheets =
                    sent.stream().filter(StatSheetPayload.class::isInstance).count();
            expectEquals(helper, 5, (int) sheets, "every improvement, accepted or not, must resend the sheet");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void levelCommands(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-cmds"));
        try {
            String name = player.getScoreboardName();
            levelled(() -> {
                runExpectingFailure(helper, "checks level " + name + " set 0");
                runExpectingFailure(helper, "checks level " + name + " set 21");
                runExpectingFailure(helper, "checks xp " + name + " add 0");
                expectEquals(helper, 650, run(helper, "checks xp " + name + " add 650"), "xp add result");
                expectEquals(helper, 5, ChecksApi.level(player), "level after 650 XP");
                expectEquals(helper, 7, run(helper, "checks level " + name + " set 7"), "level set result");
                expectProgress(helper, player, 7, 2300, "after level set 7");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void levellingOffRestoresPreviousRules(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-toggle"));
        try {
            String name = player.getScoreboardName();
            levelled(() -> unprofiled(() -> {
                run(helper, "checks level " + name + " set 4");
                Levelling.improve(player, PLUS_TWO_STR);
                run(helper, "checks level " + name + " set 17");
                return null;
            }));
            withLevelling(
                    levellingEnabled(false),
                    () -> unprofiled(() -> {
                        expectEquals(helper, 2, ChecksApi.proficiencyBonus(player), "flat proficiency bonus");
                        expectEquals(
                                helper,
                                10,
                                ChecksApi.abilityScore(player, Ability.STRENGTH),
                                "STR without improvements");
                        expectProgress(helper, player, 1, 0, "API reads while off");
                        player.giveExperiencePoints(500);
                        runExpectingFailure(helper, "checks level " + name + " set 5");
                        runExpectingFailure(helper, "checks xp " + name + " add 5");
                        expect(
                                helper,
                                Levelling.improve(player, PLUS_ONE_STR_DEX)
                                        .equals(Optional.of(ImprovementRejection.LEVELLING_DISABLED)),
                                "an improvement must be refused while levelling is off");
                        expect(
                                helper,
                                StatSheets.forPlayer(player)
                                        .orElseThrow()
                                        .level()
                                        .isEmpty(),
                                "the sheet must have no level line while levelling is off");
                        return null;
                    }));
            levelled(() -> unprofiled(() -> {
                expectProgress(helper, player, 17, LevelRules.threshold(17, DEFAULTS), "after turning it back on");
                expectEquals(helper, 6, ChecksApi.proficiencyBonus(player), "level 17 bonus");
                expectEquals(helper, 12, ChecksApi.abilityScore(player, Ability.STRENGTH), "STR with +2");
                return null;
            }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void resetClearsLevel(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-reset"));
        try {
            String name = player.getScoreboardName();
            levelled(() -> unprofiled(() -> {
                run(helper, "checks level " + name + " set 8");
                Levelling.improve(player, PLUS_TWO_STR);
                run(helper, "checks reset " + name);
                expectProgress(helper, player, 1, 0, "after reset");
                expectEquals(helper, 10, ChecksApi.abilityScore(player, Ability.STRENGTH), "STR after reset");
                expectEquals(helper, 0, pending(player), "pending after reset");
                return null;
            }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void classImprovementsAreCappedAtTheNumberEarned(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-class-asi"));
        try {
            levelled(() -> unprofiled(() -> {
                CharacterStore.get(player.server)
                        .update(player.getUUID(), character -> character.withBuild(build(FIGHTER, Set.of())));
                Levelling.setLevel(player, 6);
                expectEquals(helper, 2, pending(player), "a level 6 Fighter's pending improvements");
                expect(helper, Levelling.improve(player, PLUS_TWO_STR).isEmpty(), "the first improvement");
                expect(helper, Levelling.improve(player, PLUS_TWO_STR).isEmpty(), "the Fighter's own improvement");
                expectRejected(helper, player, PLUS_TWO_STR, ImprovementRejection.NONE_PENDING);
                expectEquals(helper, 14, ChecksApi.abilityScore(player, Ability.STRENGTH), "STR with both");

                CharacterStore.get(player.server)
                        .update(player.getUUID(), character -> character.withBuild(build(Optional.empty(), Set.of())));
                expectEquals(helper, 12, ChecksApi.abilityScore(player, Ability.STRENGTH), "STR as a Custom class");
                expectEquals(helper, 0, pending(player), "pending as a Custom class");

                CharacterStore.get(player.server)
                        .update(player.getUUID(), character -> character.withBuild(build(FIGHTER, Set.of())));
                expectEquals(helper, 14, ChecksApi.abilityScore(player, Ability.STRENGTH), "STR as a Fighter again");
                return null;
            }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void expertiseDoublesTheProficiencyBonus(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-expertise"));
        Skill stealth = ChecksApi.skill(STEALTH).orElseThrow();
        try {
            levelled(() -> unprofiled(() -> {
                CharacterStore.get(player.server)
                        .update(
                                player.getUUID(),
                                character -> character.withBuild(build(ROGUE, Set.of(STEALTH, PERCEPTION, ATHLETICS))));
                StatSheet.ExpertiseChoice choice = sheetLevel(player).expertise();
                expectEquals(helper, 2, choice.pending(), "a level 1 Rogue's Expertise to choose");
                expect(
                        helper,
                        choice.options().equals(List.of(ATHLETICS, PERCEPTION, STEALTH)),
                        "the sheet must offer the proficient skills, offered " + choice.options());
                expectEquals(helper, 2, ChecksApi.skillModifier(player, stealth), "Stealth before Expertise");

                expectExpertiseRejected(helper, player, List.of(ARCANA), ImprovementRejection.NOT_PROFICIENT);
                expectExpertiseRejected(
                        helper, player, List.of(STEALTH, STEALTH), ImprovementRejection.WRONG_EXPERTISE);
                expectExpertiseRejected(
                        helper, player, List.of(STEALTH, PERCEPTION, ATHLETICS), ImprovementRejection.WRONG_EXPERTISE);
                expect(
                        helper,
                        Levelling.chooseExpertise(player, List.of(STEALTH)).isEmpty(),
                        "Expertise in Stealth");
                expect(
                        helper,
                        ChecksApi.skillProficiency(player, stealth) == Proficiency.EXPERTISE,
                        "Stealth must have Expertise");
                expectEquals(helper, 4, ChecksApi.skillModifier(player, stealth), "Stealth with Expertise");
                expectExpertiseRejected(helper, player, List.of(STEALTH), ImprovementRejection.NOT_PROFICIENT);
                expect(
                        helper,
                        Levelling.chooseExpertise(player, List.of(PERCEPTION)).isEmpty(),
                        "the second pick");
                expectExpertiseRejected(helper, player, List.of(ATHLETICS), ImprovementRejection.NO_EXPERTISE_PENDING);

                Levelling.setLevel(player, 6);
                expectEquals(helper, 2, sheetLevel(player).expertise().pending(), "a level 6 Rogue's new Expertise");

                CharacterStore.get(player.server)
                        .update(
                                player.getUUID(),
                                character ->
                                        character.withBuild(build(FIGHTER, Set.of(STEALTH, PERCEPTION, ATHLETICS))));
                expect(
                        helper,
                        ChecksApi.skillProficiency(player, stealth) == Proficiency.PROFICIENT,
                        "a Fighter's Stealth must fall back to proficient");
                expectEquals(helper, 0, sheetLevel(player).expertise().pending(), "a Fighter's Expertise to choose");
                return null;
            }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void levelUpPanelListsWhatEachLevelGave(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-gains"));
        try {
            List<LevelGain> gains = levelled(() -> unprofiled(() -> {
                CharacterStore.get(player.server)
                        .update(player.getUUID(), character -> character.withBuild(build(ROGUE, Set.of(STEALTH))));
                Levelling.setLevel(player, 6);
                PlayerBodies.sync(player);
                return sheetLevel(player).gains();
            }));
            expectEquals(helper, 6, gains.size(), "levels listed");
            expect(
                    helper,
                    gains.get(0).equals(new LevelGain(1, 20, Optional.of(2), 0, 2, Optional.empty())),
                    "level 1 must give 20 max health, +2 and two Expertise, gave " + gains.get(0));
            expect(helper, gains.get(1).health() == 0.0, "level 2 must add no whole heart, added " + gains.get(1));
            expect(helper, gains.get(2).health() == 2.0, "level 3 must add a heart, added " + gains.get(2));
            expectEquals(helper, 1, gains.get(3).improvements(), "level 4 improvements");
            expect(helper, gains.get(4).proficiencyBonus().equals(Optional.of(3)), "level 5 bonus " + gains.get(4));
            expectEquals(helper, 2, gains.get(5).expertise(), "level 6 Expertise");
            double total = gains.stream().mapToDouble(LevelGain::health).sum();
            expect(
                    helper,
                    total == player.getMaxHealth(),
                    "the gains must add up to " + player.getMaxHealth() + ", were " + total);
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void extraAttackFollowsClassAndLevel(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-extra-attack"));
        try {
            levelled(() -> unprofiled(() -> {
                setClass(player, FIGHTER);
                Levelling.setLevel(player, 4);
                expectAttacks(helper, player, 1, Optional.of(5), "a level 4 Fighter");
                Levelling.setLevel(player, 5);
                expectAttacks(helper, player, 2, Optional.of(11), "a level 5 Fighter");
                Levelling.setLevel(player, 11);
                expectAttacks(helper, player, 3, Optional.of(20), "a level 11 Fighter");
                Levelling.setLevel(player, 20);
                expectAttacks(helper, player, 4, Optional.empty(), "a level 20 Fighter");
                List<Optional<Integer>> raised = sheetLevel(player).gains().stream()
                        .map(LevelGain::attacks)
                        .toList();
                expect(
                        helper,
                        raised.get(4).equals(Optional.of(2))
                                && raised.get(10).equals(Optional.of(3))
                                && raised.get(19).equals(Optional.of(4))
                                && raised.stream().filter(Optional::isPresent).count() == 3,
                        "the Level up! panel must list Extra Attack at 5, 11 and 20, listed " + raised);

                for (String martial : List.of("barbarian", "monk", "paladin", "ranger")) {
                    setClass(player, Optional.of(Checks.id(martial)));
                    Levelling.setLevel(player, 4);
                    expectAttacks(helper, player, 1, Optional.of(5), "a level 4 " + martial);
                    Levelling.setLevel(player, 5);
                    expectAttacks(helper, player, 2, Optional.empty(), "a level 5 " + martial);
                }
                Levelling.setLevel(player, 20);
                setClass(player, Optional.of(Checks.id("wizard")));
                expectAttacks(helper, player, 1, Optional.empty(), "a level 20 Wizard");
                setClass(player, Optional.empty());
                expectAttacks(helper, player, 1, Optional.empty(), "a level 20 Custom class");
                return null;
            }));
            Mob zombie = spawnCalm(helper, EntityType.ZOMBIE);
            expectEquals(helper, 1, ChecksApi.attacksPerAction(zombie), "a zombie's attacks");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void extraAttackOffLeavesOneAttack(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-extra-attack-off"));
        try {
            levelled(() -> {
                setClass(player, FIGHTER);
                Levelling.setLevel(player, 20);
                return null;
            });
            withLevelling(
                    extraAttackOff(),
                    () -> unprofiled(() -> {
                        expectEquals(helper, 1, ChecksApi.attacksPerAction(player), "attacks with Extra Attack off");
                        expect(
                                helper,
                                StatSheets.forPlayer(player)
                                        .orElseThrow()
                                        .body()
                                        .attacks()
                                        .isEmpty(),
                                "the sheet must have no attacks row with Extra Attack off");
                        expect(
                                helper,
                                sheetLevel(player).gains().stream()
                                        .noneMatch(gain -> gain.attacks().isPresent()),
                                "the Level up! panel must list no Extra Attack with it off");
                        return null;
                    }));
            withLevelling(
                    levellingEnabled(false),
                    () -> unprofiled(() -> {
                        expectAttacks(helper, player, 1, Optional.empty(), "a Fighter with levelling off");
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    private static void expectAttacks(
            GameTestHelper helper, ServerPlayer player, int attacks, Optional<Integer> nextLevel, String who) {
        expectEquals(helper, attacks, ChecksApi.attacksPerAction(player), who + "'s attacks");
        Optional<StatSheet.AttacksLine> shown =
                StatSheets.forPlayer(player).orElseThrow().body().attacks();
        Optional<StatSheet.AttacksLine> expected = Optional.of(new StatSheet.AttacksLine(attacks, nextLevel));
        expect(helper, shown.equals(expected), "the sheet must show " + expected + " for " + who + ", showed " + shown);
    }

    private static void setClass(ServerPlayer player, Optional<ResourceLocation> classId) {
        CharacterStore.get(player.server)
                .update(player.getUUID(), character -> character.withBuild(build(classId, Set.of())));
    }

    private static ScoresConfig.LevellingSettings extraAttackOff() {
        return new ScoresConfig.LevellingSettings(
                DEFAULTS.enabled(),
                DEFAULTS.xpThresholds(),
                DEFAULTS.proficiencyBonuses(),
                DEFAULTS.xpSources(),
                DEFAULTS.improvements(),
                DEFAULTS.expertise(),
                new ScoresConfig.ExtraAttackSettings(
                        false, DEFAULTS.extraAttack().classLevels()));
    }

    private static void expectExpertiseRejected(
            GameTestHelper helper, ServerPlayer player, List<ResourceLocation> skills, ImprovementRejection expected) {
        Optional<ImprovementRejection> rejection = Levelling.chooseExpertise(player, skills);
        expect(
                helper,
                rejection.equals(Optional.of(expected)),
                skills + " must be rejected as " + expected + ", was " + rejection);
    }

    private static StatSheet.LevelLine sheetLevel(ServerPlayer player) {
        return StatSheets.forPlayer(player).orElseThrow().level().orElseThrow();
    }

    public static void sheetShowsLevel(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-sheet"));
        try {
            String name = player.getScoreboardName();
            StatSheet.LevelLine line = levelled(() -> {
                run(helper, "checks level " + name + " set 4");
                run(helper, "checks xp " + name + " add 100");
                return StatSheets.forPlayer(player).orElseThrow().level().orElseThrow();
            });
            expectEquals(helper, 4, line.level(), "sheet level");
            expectEquals(helper, 370, line.xp(), "sheet XP");
            expectEquals(helper, 270, line.levelXp(), "sheet level threshold");
            expect(helper, line.nextLevelXp().equals(Optional.of(650)), "sheet next threshold " + line.nextLevelXp());
            expectEquals(helper, 1, line.improvements().pending(), "sheet pending improvements");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void levelSurvivesRelogAndSave(GameTestHelper helper) {
        GameProfile profile = newProfile("checks-lv-relog");
        ServerPlayer firstSession = login(helper, profile);
        String name = firstSession.getScoreboardName();
        levelled(() -> {
            run(helper, "checks level " + name + " set 4");
            run(helper, "checks xp " + name + " add 50");
            Levelling.improve(firstSession, PLUS_TWO_STR);
            return null;
        });
        logout(helper, firstSession);

        ServerPlayer secondSession = login(helper, profile);
        try {
            levelled(() -> unprofiled(() -> {
                expectProgress(helper, secondSession, 4, 320, "after relog");
                expectEquals(helper, 12, ChecksApi.abilityScore(secondSession, Ability.STRENGTH), "STR after relog");
                return null;
            }));
        } finally {
            logout(helper, secondSession);
        }

        saveWorldData(helper);
        PlayerLevel expected = new PlayerLevel(4, 320, List.of(Map.of(Ability.STRENGTH, 2)), List.of(), 0);
        PlayerLevel fromDisk = savedLevelStore(helper).level(profile.getId());
        expect(helper, fromDisk.equals(expected), "the world save must hold " + expected + ", was " + fromDisk);

        helper.succeed();
    }

    private static <T> T levelled(Supplier<T> body) {
        return withLevelling(DEFAULTS, body);
    }

    private static ScoresConfig.LevellingSettings sources(ScoresConfig.XpSources sources) {
        return new ScoresConfig.LevellingSettings(
                DEFAULTS.enabled(),
                DEFAULTS.xpThresholds(),
                DEFAULTS.proficiencyBonuses(),
                sources,
                DEFAULTS.improvements(),
                DEFAULTS.expertise(),
                DEFAULTS.extraAttack());
    }

    private static void collectOrb(GameTestHelper helper, ServerPlayer player, int value) {
        ExperienceOrb orb = new ExperienceOrb(helper.getLevel(), player.getX(), player.getY(), player.getZ(), value);
        helper.getLevel().addFreshEntity(orb);
        player.takeXpDelay = 0;
        orb.playerTouch(player);
    }

    private static void expectOrbOutcome(
            GameTestHelper helper, ServerPlayer player, int damage, int vanillaXp, int characterXp, String after) {
        expectEquals(helper, damage, player.getMainHandItem().getDamageValue(), "pickaxe damage after " + after);
        expectEquals(helper, vanillaXp, player.totalExperience, "vanilla XP after " + after);
        expectEquals(helper, characterXp, ChecksApi.experience(player), "character XP after " + after);
    }

    private static void award(GameTestHelper helper, ServerPlayer player, ResourceLocation id) {
        AdvancementHolder advancement = player.server.getAdvancements().get(id);
        if (advancement == null) {
            helper.fail("advancement " + id + " is not loaded");
            return;
        }
        for (String criterion : advancement.value().criteria().keySet()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private static boolean earned(ServerPlayer player, ResourceLocation id) {
        AdvancementHolder advancement = player.server.getAdvancements().get(id);
        return advancement != null
                && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void recordFor(ServerPlayer player, LevelUpEvent event, List<String> events) {
        if (event.player().getUUID().equals(player.getUUID())) {
            events.add(event.oldLevel() + ">" + event.newLevel());
        }
    }

    private static void expectEvents(GameTestHelper helper, List<String> events, List<String> expected, String when) {
        expect(
                helper,
                events.equals(expected),
                "level-up events after " + when + " must be " + expected + ", were " + events);
        events.clear();
    }

    private static void expectRejected(
            GameTestHelper helper, ServerPlayer player, List<Integer> increases, ImprovementRejection expected) {
        Optional<ImprovementRejection> rejection = Levelling.improve(player, increases);
        expect(
                helper,
                rejection.equals(Optional.of(expected)),
                increases + " must be rejected as " + expected + ", was " + rejection);
    }

    private static void expectProgress(GameTestHelper helper, ServerPlayer player, int level, int xp, String when) {
        expectEquals(helper, level, ChecksApi.level(player), "level " + when);
        expectEquals(helper, xp, ChecksApi.experience(player), "XP " + when);
    }

    private static int pending(ServerPlayer player) {
        return LevelRules.pendingImprovements(storedLevel(player), Levelling.classId(player), DEFAULTS);
    }

    private static PlayerLevel storedLevel(ServerPlayer player) {
        return LevelStore.get(player.server).level(player.getUUID());
    }

    private static CharacterBuild build(Optional<ResourceLocation> classId, Set<ResourceLocation> skills) {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            scores.put(ability, 10);
        }
        return new CharacterBuild(
                CreationMethod.STANDARD_ARRAY,
                scores,
                skills,
                new PresetChoices(Optional.empty(), Optional.empty(), classId, Optional.empty()),
                PresetGrants.NONE);
    }
}
