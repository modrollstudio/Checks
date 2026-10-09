package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.confirm;
import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.profiles;
import static studio.modroll.checks.gametest.ScenarioSupport.provider;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.spawnCalm;
import static studio.modroll.checks.gametest.ScenarioSupport.speciesPicks;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withBody;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withPresetSettings;
import static studio.modroll.checks.gametest.ScenarioSupport.withProfiles;
import static studio.modroll.checks.gametest.ScenarioSupport.withSender;

import com.mojang.authlib.GameProfile;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.Checks;
import studio.modroll.checks.body.Extra;
import studio.modroll.checks.body.PlayerBodies;
import studio.modroll.checks.body.Size;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.CreationSubmission;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.creation.Rejection;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.level.LevelStore;
import studio.modroll.checks.level.PlayerLevel;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.sheet.StatSheet;
import studio.modroll.checks.sheet.StatSheets;
import studio.modroll.critfall.api.AttackContext;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.AttackResult;

/** GameTest bodies for the body: health, armor class, size and the attribute extras, and their switches. */
public final class BodyScenarios {

    private static final double EPSILON = 1e-6;
    private static final float BASE_HEALTH = 20f;
    private static final int HITTING_D20 = 15;
    private static final int DRAW_TICKS = 10;
    private static final int CROSSBOW_TICKS = 25;
    private static final double FASTER = 1.2;
    private static final Vec3 SHOOTING_SPOT = new Vec3(1.5, 2, 1.5);
    private static final ScoresConfig.BodySettings DEFAULTS = ScoresConfig.DEFAULTS.body();
    private static final ResourceLocation HUMAN = ResourceLocation.parse("checks:human");
    private static final ResourceLocation HALFLING = ResourceLocation.parse("checks:halfling");
    private static final ResourceLocation ELF = ResourceLocation.parse("checks:elf");
    private static final List<Holder<Attribute>> BODY_ATTRIBUTES = List.of(
            Attributes.MAX_HEALTH,
            Attributes.SCALE,
            Attributes.ATTACK_KNOCKBACK,
            Attributes.BLOCK_BREAK_SPEED,
            Attributes.OXYGEN_BONUS);
    private static final List<ResourceLocation> MODIFIER_IDS = List.of(
            PlayerBodies.HEALTH,
            PlayerBodies.SIZE,
            PlayerBodies.KNOCKBACK,
            PlayerBodies.MINING_SPEED,
            PlayerBodies.BREATH);

    private static final List<ResourceLocation> PICKS =
            List.of(ResourceLocation.parse("checks:stealth"), ResourceLocation.parse("checks:perception"));

    private BodyScenarios() {}

    /** CON 16 (+3) adds 6 HP; dropping back to CON 10 lowers the max and caps current health at it. */
    public static void conSetsMaxHealthAndKeepsHealthWithinIt(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-hp"));
        try {
            body(() -> {
                set(helper, player, "con", 16);
                expectHealth(helper, player, 26f, "max health with CON 16");
                player.setHealth(26f);

                set(helper, player, "con", 10);
                expectHealth(helper, player, BASE_HEALTH, "max health with CON 10");
                expect(helper, player.getHealth() <= player.getMaxHealth(), "health must stay within the new max");

                set(helper, player, "con", 6);
                expectHealth(helper, player, 16f, "max health with CON 6 (-2)");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Two levels gained with a d10 are worth 6 each, times 0.5: +6 HP at level 3, +3 back at level 2. */
    public static void levelsAddHitDieHealthWhileOn(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-lv"));
        ScoresConfig.BodySettings perLevel = health(new ScoresConfig.HealthSettings(true, 2.0, true, 0.5));
        try {
            withBody(
                    perLevel,
                    () -> unprofiled(() -> {
                        LevelStore levels = LevelStore.get(player.server);
                        Map<Integer, Integer> hitDice = Map.of(2, 10, 3, 10);
                        levels.set(player.getUUID(), new PlayerLevel(3, 90, List.of(), hitDice));
                        expectHealth(helper, player, 26f, "max health at level 3");
                        player.setHealth(26f);

                        levels.set(player.getUUID(), new PlayerLevel(2, 30, List.of(), hitDice));
                        expectHealth(helper, player, 23f, "max health at level 2");
                        expect(helper, player.getHealth() <= 23f, "health must stay within the new max");
                        return null;
                    }));
            body(() -> unprofiled(() -> {
                expectHealth(helper, player, BASE_HEALTH, "max health with per-level health off (the default)");
                return null;
            }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /**
     * The modifiers are not saved with the player, but Checks keeps the health: a relog comes back at
     * 26 of 26 HP and half size, not capped at 20.
     */
    public static void relogKeepsHealthAboveTheVanillaMax(GameTestHelper helper) {
        GameProfile profile = newProfile("body-relog");
        logout(helper, toughHalfling(helper, login(helper, profile)));

        ServerPlayer second = login(helper, profile);
        try {
            expect(helper, Math.abs(second.getMaxHealth() - 26f) < EPSILON, "max health after the relog");
            expect(helper, Math.abs(second.getHealth() - 26f) < EPSILON, "health above 20 must survive the relog");
            expect(helper, Math.abs(second.getScale() - 0.5f) < EPSILON, "a halfling must log in at half size");
        } finally {
            logout(helper, second);
        }
        helper.succeed();
    }

    /** With every switch off, a relogged player gets no Checks modifier and vanilla max health and size. */
    public static void switchesOffLeaveNoModifiersAfterRelog(GameTestHelper helper) {
        GameProfile profile = newProfile("body-off-relog");
        logout(helper, toughHalfling(helper, login(helper, profile)));

        withBody(allOff(), () -> {
            ServerPlayer second = login(helper, profile);
            try {
                expectNoModifiers(helper, second);
                expect(helper, Math.abs(second.getMaxHealth() - BASE_HEALTH) < EPSILON, "vanilla max health");
                expect(helper, second.getHealth() <= BASE_HEALTH, "health must be within the vanilla max");
                expect(helper, Math.abs(second.getScale() - 1f) < EPSILON, "normal size with size off");
            } finally {
                logout(helper, second);
            }
            return null;
        });
        helper.succeed();
    }

    /**
     * Removing Checks, simulated by loading the player's saved data with no Checks code running: the save
     * holds no Checks modifier, so the player has vanilla max health and scale.
     */
    public static void removedChecksLeavesVanillaAttributes(GameTestHelper helper) {
        GameProfile profile = newProfile("body-removed");
        ServerPlayer player = toughHalfling(helper, login(helper, profile));
        CompoundTag saved;
        try {
            saved = player.saveWithoutId(new CompoundTag());
        } finally {
            logout(helper, player);
        }

        String attributes = saved.getList("attributes", Tag.TAG_COMPOUND).toString();
        expect(helper, !attributes.contains(Checks.MOD_ID + ":"), "no Checks modifier may be saved: " + attributes);
        ServerPlayer withoutChecks =
                new ServerPlayer(player.server, helper.getLevel(), profile, ClientInformation.createDefault());
        withoutChecks.load(saved);
        expect(helper, Math.abs(withoutChecks.getMaxHealth() - BASE_HEALTH) < EPSILON, "vanilla max health");
        expect(helper, withoutChecks.getHealth() <= BASE_HEALTH, "health within the vanilla max");
        expect(helper, Math.abs(withoutChecks.getScale() - 1f) < EPSILON, "vanilla scale");
        expectNoModifiers(helper, withoutChecks);
        helper.succeed();
    }

    /** CON 16 and STR 16 as a halfling, at a full 26 HP. */
    private static ServerPlayer toughHalfling(GameTestHelper helper, ServerPlayer player) {
        body(() -> {
            set(helper, player, "con", 16);
            set(helper, player, "str", 16);
            confirm(player, HALFLING, Optional.empty());
            PlayerBodies.sync(player);
            player.setHealth(26f);
            return null;
        });
        return player;
    }

    /** Vanilla caps a respawned player's health before the modifier is back; a death respawn ends at full. */
    public static void respawnStartsAtFullModifiedHealth(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-respawn"));
        ServerPlayer respawned = player;
        try {
            respawned = body(() -> {
                set(helper, player, "con", 16);
                PlayerBodies.sync(player);
                player.setHealth(5f);
                return player.server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
            });
            expect(helper, Math.abs(respawned.getMaxHealth() - 26f) < EPSILON, "respawned max health with CON 16");
            expect(helper, Math.abs(respawned.getHealth() - 26f) < EPSILON, "respawned at full health");
        } finally {
            logout(helper, respawned);
        }
        helper.succeed();
    }

    /** DEX 18 (+4): all of it unarmored or in leather, +2 in chainmail, none in iron; the heaviest piece decides. */
    public static void armorClassFollowsTheHeaviestArmor(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-ac"));
        Mob attacker = spawnCalm(helper, EntityType.ZOMBIE);
        try {
            body(() -> unprofiled(() -> {
                set(helper, player, "dex", 18);
                expectAc(helper, 4, player, attacker, "unarmored");
                wear(player, EquipmentSlot.FEET, Items.LEATHER_BOOTS);
                wear(player, EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE);
                expectAc(helper, 4, player, attacker, "light armor");
                wear(player, EquipmentSlot.CHEST, Items.CHAINMAIL_CHESTPLATE);
                expectAc(helper, 2, player, attacker, "leather boots with a chainmail chestplate");
                wear(player, EquipmentSlot.HEAD, Items.TURTLE_HELMET);
                expectAc(helper, 2, player, attacker, "a turtle helmet is medium");
                wear(player, EquipmentSlot.LEGS, Items.IRON_LEGGINGS);
                expectAc(helper, 0, player, attacker, "iron leggings with lighter pieces");

                player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
                set(helper, player, "dex", 12);
                expectAc(helper, 1, player, attacker, "DEX 12 under the medium cap");
                set(helper, player, "dex", 8);
                expectAc(helper, -1, player, attacker, "a DEX penalty in medium armor");
                return null;
            }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** The provided DEX reaches Critfall: the roll's AC is Critfall's own plus the bonus. */
    public static void armorClassReachesCritfallRolls(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-roll"));
        Mob attacker = spawnCalm(helper, EntityType.ZOMBIE);
        try {
            body(() -> unprofiled(() -> {
                set(helper, player, "dex", 18);
                int critfallAc = RollService.effectiveEntity(player).armorClass();
                AttackContext claws =
                        AttackContext.melee(attacker.damageSources().mobAttack(attacker), ItemStack.EMPTY);
                AttackResult result = withD20s(() -> RollService.attackRoll(attacker, player, claws), HITTING_D20);
                expectEquals(helper, critfallAc + 4, result.armorClass(), "AC the attack rolled against");
                return null;
            }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Mobs follow the attack rule: no answer without a profile, DEX 14 (+2) with one. */
    public static void onlyProfiledMobsGetArmorClass(GameTestHelper helper) {
        Mob zombie = spawnCalm(helper, EntityType.ZOMBIE);
        Mob attacker = spawnCalm(helper, EntityType.SKELETON);
        zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        body(() -> {
            unprofiled(() -> {
                expect(helper, provider().acModifier(zombie, attacker).isEmpty(), "an unprofiled mob gets no AC");
                return null;
            });
            withProfiles(profiles("{\"matches\": [\"minecraft:zombie\"], \"abilities\": {\"dex\": 14}}"), () -> {
                expectAc(helper, 2, zombie, attacker, "a profiled zombie in leather");
                return null;
            });
            return null;
        });
        helper.succeed();
    }

    /** Species set the size: a halfling is half size, a human Medium by default and 0.6 when Small. */
    public static void speciesAndChoiceSetTheSize(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-size"));
        try {
            body(() -> {
                confirm(player, HALFLING, Optional.empty());
                expectScale(helper, player, 0.5f, "halfling");
                confirm(player, HUMAN, Optional.empty());
                expectScale(helper, player, 1f, "human by default");
                confirm(player, HUMAN, Optional.of(Size.SMALL));
                expectScale(helper, player, 0.6f, "small human");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Creation accepts a Small human and refuses a Small elf; the accepted size applies. */
    public static void creationValidatesTheSizeChoice(GameTestHelper helper) {
        ServerPlayer elf = login(helper, newProfile("body-elf"));
        ServerPlayer human = login(helper, newProfile("body-human"));
        try {
            body(() -> withSender((target, payload) -> false, () -> {
                Optional<Rejection> refused = submit(elf, ELF, Size.SMALL);
                expect(
                        helper,
                        refused.equals(Optional.of(Rejection.SIZE_NOT_OFFERED)),
                        "a Small elf must be refused, was " + refused);
                Optional<Rejection> accepted = submit(human, HUMAN, Size.SMALL);
                expect(helper, accepted.isEmpty(), "a Small human must be accepted, was " + accepted);
                expectScale(helper, human, 0.6f, "the confirmed Small human");
                return null;
            }));
        } finally {
            logout(helper, elf);
            logout(helper, human);
        }
        helper.succeed();
    }

    /** Size off, or presets off, puts a halfling back at normal size. */
    public static void sizeSwitchesOff(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-size-off"));
        try {
            body(() -> {
                confirm(player, HALFLING, Optional.empty());
                expectScale(helper, player, 0.5f, "halfling");
                return null;
            });
            withBody(sizeOff(), () -> {
                expectScale(helper, player, 1f, "halfling with size off");
                return null;
            });
            ScoresConfig.PresetSettings presets =
                    ScoresRuntime.config().creation().presets();
            withPresetSettings(
                    new ScoresConfig.PresetSettings(
                            false, presets.includeShipped(), presets.bonusOptions(), presets.bonusMaxScore()),
                    () -> body(() -> {
                        expectScale(helper, player, 1f, "halfling with presets off");
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** STR 16 (+3): knockback +0.3 and mining speed +15%; each off on its own. */
    public static void strengthExtras(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-str"));
        try {
            body(() -> {
                set(helper, player, "str", 16);
                expectAttribute(helper, player, Attributes.ATTACK_KNOCKBACK, 0.3, "knockback");
                expectAttribute(helper, player, Attributes.BLOCK_BREAK_SPEED, 1.15, "mining speed");
                return null;
            });
            withBody(extraOff(Extra.KNOCKBACK), () -> {
                PlayerBodies.sync(player);
                expectAttribute(helper, player, Attributes.ATTACK_KNOCKBACK, 0.0, "knockback off");
                expectAttribute(helper, player, Attributes.BLOCK_BREAK_SPEED, 1.15, "mining speed still on");
                return null;
            });
            withBody(extraOff(Extra.MINING_SPEED), () -> {
                PlayerBodies.sync(player);
                expectAttribute(helper, player, Attributes.BLOCK_BREAK_SPEED, 1.0, "mining speed off");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** DEX 16 (+3): a bow drawn 10 ticks shoots like 13, a crossbow loads in 19 ticks instead of 25. */
    public static void dexterityExtras(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-dex"));
        Mob zombie = spawnCalm(helper, EntityType.ZOMBIE);
        try {
            player.setGameMode(GameType.SURVIVAL);
            double plainSpeed = body(() -> {
                set(helper, player, "dex", 10);
                return arrowSpeed(helper, player);
            });
            double dexSpeed = body(() -> {
                set(helper, player, "dex", 16);
                expectEquals(helper, 19, crossbowTicks(player), "crossbow load time with DEX 16");
                expectEquals(helper, CROSSBOW_TICKS, crossbowTicks(zombie), "a zombie's crossbow load time");
                return arrowSpeed(helper, player);
            });
            expect(
                    helper,
                    dexSpeed > plainSpeed * FASTER,
                    "DEX 16 must draw faster: arrow speed " + dexSpeed + " vs " + plainSpeed);

            double bowOffSpeed = withBody(extraOff(Extra.BOW_DRAW), () -> arrowSpeed(helper, player));
            expect(helper, bowOffSpeed < dexSpeed / FASTER, "bow draw off must shoot as without DEX");
            withBody(extraOff(Extra.CROSSBOW_RELOAD), () -> {
                expectEquals(helper, CROSSBOW_TICKS, crossbowTicks(player), "crossbow reload off");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** CON 16 (+3): breath +0.6 oxygen bonus and 15% less exhaustion; each off on its own. */
    public static void constitutionExtras(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-con"));
        try {
            player.setGameMode(GameType.SURVIVAL);
            body(() -> {
                set(helper, player, "con", 16);
                expectAttribute(helper, player, Attributes.OXYGEN_BONUS, 0.6, "oxygen bonus");
                expectExhaustion(helper, player, 0.85f, "exhaustion with CON 16");
                return null;
            });
            withBody(extraOff(Extra.BREATH), () -> {
                PlayerBodies.sync(player);
                expectAttribute(helper, player, Attributes.OXYGEN_BONUS, 0.0, "breath off");
                return null;
            });
            withBody(extraOff(Extra.EXHAUSTION), () -> {
                expectExhaustion(helper, player, 1f, "exhaustion off");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Health and armor class off give the vanilla max and no Critfall AC answer. */
    public static void healthAndArmorClassSwitchOff(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-off"));
        Mob attacker = spawnCalm(helper, EntityType.ZOMBIE);
        try {
            body(() -> {
                set(helper, player, "con", 16);
                set(helper, player, "dex", 18);
                return null;
            });
            withBody(health(new ScoresConfig.HealthSettings(false, 2.0, false, 0.2)), () -> {
                expectHealth(helper, player, BASE_HEALTH, "max health with health off");
                expect(
                        helper,
                        player.getAttribute(Attributes.MAX_HEALTH).getModifier(PlayerBodies.HEALTH) == null,
                        "the health modifier must be gone");
                return null;
            });
            withBody(armorOff(), () -> {
                expect(helper, provider().acModifier(player, attacker).isEmpty(), "no AC answer with armor class off");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** The stat sheet carries the body section's values: max health, AC with its DEX part, and size. */
    public static void sheetShowsTheBody(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-sheet"));
        try {
            body(() -> unprofiled(() -> {
                set(helper, player, "con", 16);
                set(helper, player, "dex", 18);
                confirm(player, HALFLING, Optional.empty());
                wear(player, EquipmentSlot.CHEST, Items.CHAINMAIL_CHESTPLATE);
                PlayerBodies.sync(player);
                StatSheet.Body body = StatSheets.forPlayer(player).orElseThrow().body();

                StatSheet.HealthLine health = body.health().orElseThrow();
                expect(helper, Math.abs(health.maxHealth() - 26f) < EPSILON, "sheet max health");
                expectEquals(helper, 3, health.conModifier(), "sheet CON modifier");
                StatSheet.ArmorLine armor = body.armor().orElseThrow();
                expectEquals(helper, 2, armor.dexBonus(), "sheet DEX part of AC");
                expectEquals(helper, armor.baseArmorClass() + 2, armor.armorClass(), "sheet AC");
                expect(helper, body.size().orElseThrow().size() == Size.SMALL, "sheet size");
                expect(helper, body.extras().size() == Extra.values().length, "every extra is on by default");
                return null;
            }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    private static <T> T body(Supplier<T> body) {
        return withBody(DEFAULTS, body);
    }

    private static ScoresConfig.BodySettings health(ScoresConfig.HealthSettings health) {
        return new ScoresConfig.BodySettings(health, DEFAULTS.armorClass(), DEFAULTS.sizeEnabled(), DEFAULTS.extras());
    }

    private static ScoresConfig.BodySettings armorOff() {
        return new ScoresConfig.BodySettings(
                DEFAULTS.health(), new ScoresConfig.ArmorClassSettings(false, 2), true, DEFAULTS.extras());
    }

    private static ScoresConfig.BodySettings sizeOff() {
        return new ScoresConfig.BodySettings(DEFAULTS.health(), DEFAULTS.armorClass(), false, DEFAULTS.extras());
    }

    private static ScoresConfig.BodySettings extraOff(Extra off) {
        Map<Extra, ScoresConfig.ExtraSetting> extras = new EnumMap<>(DEFAULTS.extras());
        extras.put(off, new ScoresConfig.ExtraSetting(false, DEFAULTS.extra(off).perPoint()));
        return new ScoresConfig.BodySettings(DEFAULTS.health(), DEFAULTS.armorClass(), true, extras);
    }

    private static ScoresConfig.BodySettings allOff() {
        Map<Extra, ScoresConfig.ExtraSetting> extras = new EnumMap<>(Extra.class);
        for (Extra extra : Extra.values()) {
            extras.put(
                    extra,
                    new ScoresConfig.ExtraSetting(false, DEFAULTS.extra(extra).perPoint()));
        }
        return new ScoresConfig.BodySettings(
                new ScoresConfig.HealthSettings(false, 2.0, false, 0.2),
                new ScoresConfig.ArmorClassSettings(false, 2),
                false,
                extras);
    }

    private static void set(GameTestHelper helper, ServerPlayer player, String ability, int score) {
        run(helper, "checks set " + player.getScoreboardName() + " " + ability + " " + score);
    }

    /** Stores a confirmed character of the species straight away, the way creation would. */
    private static Optional<Rejection> submit(ServerPlayer player, ResourceLocation species, Size size) {
        PresetChoices choices =
                new PresetChoices(Optional.of(species), Optional.empty(), Optional.empty(), Optional.of(size));
        return CharacterCreation.submit(
                player,
                new CreationSubmission(
                        CreationMethod.STANDARD_ARRAY,
                        List.of(15, 14, 13, 12, 10, 8),
                        List.of(0, 0, 0, 0, 0, 0),
                        PICKS,
                        choices,
                        speciesPicks(player, choices, PICKS)));
    }

    private static void wear(LivingEntity entity, EquipmentSlot slot, Item item) {
        entity.setItemSlot(slot, new ItemStack(item));
    }

    private static void expectAc(
            GameTestHelper helper, int expected, LivingEntity defender, LivingEntity attacker, String what) {
        OptionalInt ac = provider().acModifier(defender, attacker);
        expect(
                helper,
                ac.equals(OptionalInt.of(expected)),
                "AC from DEX, " + what + ": expected " + expected + ", was " + ac);
    }

    private static void expectHealth(GameTestHelper helper, ServerPlayer player, float expected, String what) {
        PlayerBodies.sync(player);
        float actual = player.getMaxHealth();
        expect(helper, Math.abs(actual - expected) < EPSILON, what + " must be " + expected + ", was " + actual);
    }

    private static void expectScale(GameTestHelper helper, ServerPlayer player, float expected, String what) {
        PlayerBodies.sync(player);
        float actual = player.getScale();
        expect(helper, Math.abs(actual - expected) < EPSILON, what + ": scale must be " + expected + ", was " + actual);
    }

    private static void expectAttribute(
            GameTestHelper helper, ServerPlayer player, Holder<Attribute> attribute, double expected, String what) {
        PlayerBodies.sync(player);
        double actual = player.getAttributeValue(attribute);
        expect(helper, Math.abs(actual - expected) < EPSILON, what + " must be " + expected + ", was " + actual);
    }

    private static void expectExhaustion(GameTestHelper helper, ServerPlayer player, float expected, String what) {
        float before = player.getFoodData().getExhaustionLevel();
        player.causeFoodExhaustion(1f);
        float gained = player.getFoodData().getExhaustionLevel() - before;
        expect(helper, Math.abs(gained - expected) < EPSILON, what + " must be " + expected + ", was " + gained);
    }

    private static void expectNoModifiers(GameTestHelper helper, ServerPlayer player) {
        for (Holder<Attribute> attribute : BODY_ATTRIBUTES) {
            AttributeInstance instance = player.getAttribute(attribute);
            for (ResourceLocation id : MODIFIER_IDS) {
                expect(
                        helper,
                        instance.getModifier(id) == null,
                        id + " must be gone from " + attribute.getRegisteredName());
            }
        }
    }

    private static int crossbowTicks(LivingEntity shooter) {
        return CrossbowItem.getChargeDuration(new ItemStack(Items.CROSSBOW), shooter);
    }

    /**
     * Releases a bow drawn for {@link #DRAW_TICKS} and returns the arrow's speed; the arrow is removed. The
     * player shoots from inside the test area, whose chunks are loaded, so the arrow can be found.
     */
    private static double arrowSpeed(GameTestHelper helper, ServerPlayer player) {
        player.moveTo(helper.absoluteVec(SHOOTING_SPOT));
        ItemStack bow = new ItemStack(Items.BOW);
        player.setItemInHand(InteractionHand.MAIN_HAND, bow);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.ARROW));
        bow.releaseUsing(helper.getLevel(), player, bow.getUseDuration(player) - DRAW_TICKS);
        List<Arrow> arrows = helper.getLevel()
                .getEntitiesOfClass(Arrow.class, player.getBoundingBox().inflate(3));
        expect(helper, arrows.size() == 1, "one arrow must be shot, found " + arrows.size());
        Arrow arrow = arrows.getFirst();
        double speed = arrow.getDeltaMovement().length();
        arrow.discard();
        return speed;
    }
}
