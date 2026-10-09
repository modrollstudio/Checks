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
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.LevelUpEvent;
import studio.modroll.checks.creation.CharacterBuild;
import studio.modroll.checks.creation.CharacterStore;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.creation.PresetGrants;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.level.ImprovementRejection;
import studio.modroll.checks.level.LevelRules;
import studio.modroll.checks.level.LevelStore;
import studio.modroll.checks.level.Levelling;
import studio.modroll.checks.level.PlayerLevel;
import studio.modroll.checks.sheet.StatSheet;
import studio.modroll.checks.sheet.StatSheetPayload;
import studio.modroll.checks.sheet.StatSheets;
import studio.modroll.critfall.api.CombatSuppression;

/**
 * GameTest bodies for levelling. Each runs under the default levelling settings, so the numbers below
 * follow the shipped XP table: level 2 at 30 XP, 4 at 270, 5 at 650, 7 at 2300.
 */
public final class LevelScenarios {

    private static final ScoresConfig.LevellingSettings DEFAULTS = ScoresConfig.DEFAULTS.levelling();
    private static final ResourceLocation STONE_AGE = ResourceLocation.withDefaultNamespace("story/mine_stone");
    private static final ResourceLocation NETHER = ResourceLocation.withDefaultNamespace("story/enter_the_nether");
    private static final ResourceLocation ADVENTURE = ResourceLocation.withDefaultNamespace("adventure/root");
    private static final float LETHAL = 100f;
    private static final ResourceLocation RECIPE =
            ResourceLocation.withDefaultNamespace("recipes/building_blocks/stone_bricks");
    private static final ResourceLocation FIGHTER = ResourceLocation.parse("checks:fighter");
    private static final List<Integer> PLUS_TWO_STR = List.of(2, 0, 0, 0, 0, 0);
    private static final List<Integer> PLUS_ONE_STR_DEX = List.of(1, 1, 0, 0, 0, 0);

    private LevelScenarios() {}

    /** Points count as they are gained; enchanting, anvil costs, negative points or dying take none back. */
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

    /**
     * The first death to a mob earns the shown Adventure advancement, which must give no XP; with or without
     * keepInventory, the level stays and respawning does not announce it again.
     */
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

    /** A survival player past spawn protection, so a mob's blow lands. */
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

    /**
     * A collected orb counts in full, the points Mending spends on repairs included: 10 points repair 20
     * durability, so an orb of 10 on a pickaxe missing 500 never reaches the XP bar, and on one missing 4
     * only 8 do.
     */
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

    /** A shown advancement gives its XP once; a recipe unlock gives none, and neither does a disabled source. */
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

    /** Crossing thresholds levels up one event per level; staying within a level or lowering it fires none. */
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

    /**
     * Players use their level's bonus over a player profile unless a command sets one; mobs keep their
     * profile or the default.
     */
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

    /**
     * With STR 19, +2 STR passes the max and +1 STR alone is no allowed split; +1 STR +1 DEX is accepted.
     * Improvements earned at levels 8 and 12 then wait together until chosen, one at a time.
     */
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

    /** Levels must be 1..20 and XP added must be positive; the commands return the new level and total XP. */
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

    /**
     * With levelling off, players get the flat bonus and no improvements, gain no XP, and the commands and
     * choice are refused; turning it back on restores the stored level.
     */
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

    /** {@code /checks reset} clears the level, XP and chosen improvements along with the character. */
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

    /** A Fighter levelling up records its d10 for each level gained. */
    public static void levelUpRecordsTheClassHitDie(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-lv-hitdie"));
        try {
            CharacterStore.get(player.server).update(player.getUUID(), character -> character.withBuild(fighter()));
            levelled(() -> {
                run(helper, "checks level " + player.getScoreboardName() + " set 3");
                return null;
            });
            expect(
                    helper,
                    storedLevel(player).hitDice().equals(Map.of(2, 10, 3, 10)),
                    "hit dice must be d10 at levels 2 and 3, were "
                            + storedLevel(player).hitDice());
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** The sheet carries the level, XP, both thresholds and pending improvements. */
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

    /** Level, XP and improvements survive logout and a fresh login, and reach the world save. */
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
        PlayerLevel expected = new PlayerLevel(4, 320, List.of(Map.of(Ability.STRENGTH, 2)), Map.of());
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
                DEFAULTS.improvements());
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

    /** Checks the events recorded since the last call, then forgets them. */
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
        return LevelRules.pendingImprovements(storedLevel(player), DEFAULTS);
    }

    private static PlayerLevel storedLevel(ServerPlayer player) {
        return LevelStore.get(player.server).level(player.getUUID());
    }

    private static CharacterBuild fighter() {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            scores.put(ability, 10);
        }
        return new CharacterBuild(
                CreationMethod.STANDARD_ARRAY,
                scores,
                Set.of(),
                new PresetChoices(Optional.empty(), Optional.empty(), Optional.of(FIGHTER), Optional.empty()),
                PresetGrants.NONE);
    }
}
