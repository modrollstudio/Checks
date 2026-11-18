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
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
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
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.body.BodyValues;
import studio.modroll.checks.body.Extra;
import studio.modroll.checks.body.PlayerBodies;
import studio.modroll.checks.body.Size;
import studio.modroll.checks.creation.CharacterBuild;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.creation.CharacterStore;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.CreationSubmission;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.creation.PresetGrants;
import studio.modroll.checks.creation.Rejection;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.level.LevelStore;
import studio.modroll.checks.level.PlayerLevel;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.sheet.SheetTooltips;
import studio.modroll.checks.sheet.StatSheet;
import studio.modroll.checks.sheet.StatSheets;
import studio.modroll.critfall.api.AttackContext;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.combat.AttackResult;

public final class BodyScenarios {

    private static final double EPSILON = 1e-6;

    private static final List<Item> BODY_ARMOR = List.of(
            Items.AIR,
            Items.LEATHER_CHESTPLATE,
            Items.CHAINMAIL_CHESTPLATE,
            Items.GOLDEN_CHESTPLATE,
            Items.IRON_CHESTPLATE,
            Items.DIAMOND_CHESTPLATE,
            Items.NETHERITE_CHESTPLATE);
    private static final Map<EquipmentSlot, List<Item>> PIECES = Map.of(
            EquipmentSlot.HEAD,
            List.of(
                    Items.LEATHER_HELMET,
                    Items.CHAINMAIL_HELMET,
                    Items.GOLDEN_HELMET,
                    Items.TURTLE_HELMET,
                    Items.IRON_HELMET,
                    Items.DIAMOND_HELMET,
                    Items.NETHERITE_HELMET),
            EquipmentSlot.LEGS,
            List.of(
                    Items.LEATHER_LEGGINGS,
                    Items.CHAINMAIL_LEGGINGS,
                    Items.GOLDEN_LEGGINGS,
                    Items.IRON_LEGGINGS,
                    Items.DIAMOND_LEGGINGS,
                    Items.NETHERITE_LEGGINGS),
            EquipmentSlot.FEET,
            List.of(
                    Items.LEATHER_BOOTS,
                    Items.CHAINMAIL_BOOTS,
                    Items.GOLDEN_BOOTS,
                    Items.IRON_BOOTS,
                    Items.DIAMOND_BOOTS,
                    Items.NETHERITE_BOOTS));
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

    public static void levelsAddHitDieHealthWhileOn(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-lv"));
        ScoresConfig.BodySettings perLevel =
                health(new ScoresConfig.HealthSettings(true, 2.0, true, 0.5, true, 1, 10, true));
        try {
            withBody(
                    perLevel,
                    () -> unprofiled(() -> {
                        LevelStore levels = LevelStore.get(player.server);
                        levels.set(player.getUUID(), new PlayerLevel(3, 90, List.of(), List.of(), 0));
                        expectHealth(helper, player, 26f, "max health at level 3");
                        player.setHealth(26f);

                        levels.set(player.getUUID(), new PlayerLevel(2, 30, List.of(), List.of(), 0));
                        expectHealth(helper, player, 22f, "max health at level 2, in whole hearts");
                        expect(helper, player.getHealth() <= 22f, "health must stay within the new max");

                        levels.set(player.getUUID(), new PlayerLevel(3, 90, List.of(), List.of(), 0));
                        set(helper, player, "con", 14);
                        expectHealth(helper, player, 32f, "max health at level 3 with CON 14");
                        set(helper, player, "con", 1);
                        expectHealth(helper, player, 10f, "max health at level 3 with CON 1 (-10, +1 per level)");
                        set(helper, player, "con", 10);
                        return null;
                    }));
            withBody(
                    health(new ScoresConfig.HealthSettings(true, 2.0, true, 0.5, true, 1, 10, false)),
                    () -> unprofiled(() -> {
                        LevelStore.get(player.server)
                                .set(player.getUUID(), new PlayerLevel(2, 30, List.of(), List.of(), 0));
                        expectHealth(helper, player, 23f, "max health at level 2 with whole hearts off");
                        return null;
                    }));
            withBody(
                    health(new ScoresConfig.HealthSettings(true, 2.0, false, 0.5, true, 1, 10, true)),
                    () -> unprofiled(() -> {
                        expectHealth(helper, player, BASE_HEALTH, "max health with per level health off");
                        return null;
                    }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void classHitDieCountsForEveryLevel(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-lv-class"));
        try {
            body(() -> unprofiled(() -> {
                LevelStore.get(player.server).set(player.getUUID(), new PlayerLevel(5, 650, List.of(), List.of(), 0));
                expectHealth(helper, player, 24f, "max health at level 5 with no class (d8)");
                CharacterStore.get(player.server)
                        .update(player.getUUID(), character -> character.withBuild(classBuild("checks:fighter")));
                expectHealth(helper, player, 24f, "max health at level 5 as a Fighter (d10)");
                CharacterStore.get(player.server)
                        .update(player.getUUID(), character -> character.withBuild(classBuild("checks:barbarian")));
                expectHealth(helper, player, 24f, "max health at level 5 as a Barbarian (d12)");
                CharacterStore.get(player.server)
                        .update(player.getUUID(), character -> character.withBuild(classBuild("checks:wizard")));
                expectHealth(helper, player, 22f, "max health at level 5 as a Wizard (d6)");
                return null;
            }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    private static CharacterBuild classBuild(String classId) {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            scores.put(ability, 10);
        }
        return new CharacterBuild(
                CreationMethod.STANDARD_ARRAY,
                scores,
                Set.of(),
                new PresetChoices(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(ResourceLocation.parse(classId)),
                        Optional.empty()),
                PresetGrants.NONE);
    }

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

    public static void armorClassFollowsTheBodyArmor(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-ac"));
        Mob attacker = spawnCalm(helper, EntityType.ZOMBIE);
        try {
            body(() -> unprofiled(() -> {
                set(helper, player, "dex", 18);
                expectAc(helper, 4, player, attacker, "unarmored");
                equip(player, EquipmentSlot.HEAD, Items.IRON_HELMET);
                equip(player, EquipmentSlot.LEGS, Items.IRON_LEGGINGS);
                equip(player, EquipmentSlot.FEET, Items.CHAINMAIL_BOOTS);
                expectAc(
                        helper,
                        4,
                        player,
                        attacker,
                        "an iron helmet, iron leggings and chainmail boots, no body armor");
                equip(player, EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE);
                expectAc(helper, 4, player, attacker, "light body armor with heavy leggings");
                equip(player, EquipmentSlot.CHEST, Items.CHAINMAIL_CHESTPLATE);
                expectDexPart(helper, 2, player, "medium body armor with heavy leggings");
                expectAc(helper, 3, player, attacker, "medium body armor, kept from lowering AC");
                equip(player, EquipmentSlot.CHEST, Items.IRON_CHESTPLATE);
                expectDexPart(helper, 0, player, "heavy body armor");
                expectAc(helper, 3, player, attacker, "heavy body armor, kept from lowering AC");

                equip(player, EquipmentSlot.CHEST, Items.CHAINMAIL_CHESTPLATE);
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

    public static void helmetsLeggingsAndBootsNeverLowerArmorClass(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-pieces"));
        Mob attacker = spawnCalm(helper, EntityType.ZOMBIE);
        try {
            body(() -> unprofiled(() -> {
                set(helper, player, "dex", 20);
                expectArmorClass(helper, 15, player, attacker, "unarmored");
                equip(player, EquipmentSlot.HEAD, Items.CHAINMAIL_HELMET);
                expectArmorClass(helper, 15, player, attacker, "with a chainmail helmet");
                equip(player, EquipmentSlot.CHEST, Items.CHAINMAIL_CHESTPLATE);
                expectArmorClass(helper, 15, player, attacker, "with a chainmail chestplate");
                equip(player, EquipmentSlot.LEGS, Items.IRON_LEGGINGS);
                expectArmorClass(helper, 16, player, attacker, "with iron leggings");
                equip(player, EquipmentSlot.FEET, Items.CHAINMAIL_BOOTS);
                expectArmorClass(helper, 17, player, attacker, "with chainmail boots");

                for (Item bodyArmor : BODY_ARMOR) {
                    equip(player, EquipmentSlot.CHEST, bodyArmor);
                    for (Map.Entry<EquipmentSlot, List<Item>> slot : PIECES.entrySet()) {
                        for (Item piece : slot.getValue()) {
                            List.of(EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET)
                                    .forEach(other -> equip(player, other, Items.AIR));
                            int before = armorClass(player, attacker);
                            equip(player, slot.getKey(), piece);
                            int after = armorClass(player, attacker);
                            expect(
                                    helper,
                                    after >= before,
                                    piece + " over " + bodyArmor + " lowered AC from " + before + " to " + after);
                        }
                    }
                }
                return null;
            }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    public static void bodyArmorNeverLowersArmorClass(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-floor"));
        Mob attacker = spawnCalm(helper, EntityType.ZOMBIE);
        try {
            body(() -> unprofiled(() -> {
                set(helper, player, "dex", 20);
                expectArmorClass(helper, 15, player, attacker, "unarmored");
                equip(player, EquipmentSlot.CHEST, Items.CHAINMAIL_CHESTPLATE);
                expectDexPart(helper, 2, player, "in a chainmail chestplate");
                expectArmorClass(helper, 15, player, attacker, "in a chainmail chestplate, not 13");

                StatSheet.Body body = StatSheets.forPlayer(player).orElseThrow().body();
                StatSheet.ArmorLine armor = body.armor().orElseThrow();
                expectEquals(helper, 15, armor.armorClass(), "sheet AC in a chainmail chestplate");
                expectEquals(helper, 2, armor.dexBonus(), "sheet DEX part, capped by the chainmail");
                expectEquals(helper, 2, armor.floorBonus(), "sheet part that keeps the chainmail from lowering AC");
                List<Component> tooltip = SheetTooltips.body(body).orElseThrow().rows().stream()
                        .flatMap(row -> row.tooltip().stream())
                        .toList();
                expect(
                        helper,
                        tooltip.stream()
                                .anyMatch(line -> line.getContents() instanceof TranslatableContents contents
                                        && contents.getKey().equals("screen.checks.tooltip.armor_class.floor")
                                        && List.of(contents.getArgs()).equals(List.of("+2"))),
                        "the tooltip shows the +2 that keeps the chainmail from lowering AC, was " + tooltip);

                equip(player, EquipmentSlot.HEAD, Items.CHAINMAIL_HELMET);
                equip(player, EquipmentSlot.LEGS, Items.CHAINMAIL_LEGGINGS);
                equip(player, EquipmentSlot.FEET, Items.CHAINMAIL_BOOTS);
                expectArmorClass(helper, 16, player, attacker, "in full chainmail");
                return null;
            }));
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

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

    public static void healthAndArmorClassSwitchOff(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-off"));
        Mob attacker = spawnCalm(helper, EntityType.ZOMBIE);
        try {
            body(() -> {
                set(helper, player, "con", 16);
                set(helper, player, "dex", 18);
                return null;
            });
            withBody(health(new ScoresConfig.HealthSettings(false, 2.0, false, 0.2, true, 1, 8, true)), () -> {
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

    public static void sheetShowsTheBody(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("body-sheet"));
        try {
            body(() -> unprofiled(() -> {
                set(helper, player, "con", 16);
                set(helper, player, "dex", 18);
                confirm(player, HALFLING, Optional.empty());
                equip(player, EquipmentSlot.CHEST, Items.CHAINMAIL_CHESTPLATE);
                PlayerBodies.sync(player);
                StatSheet.Body body = StatSheets.forPlayer(player).orElseThrow().body();

                StatSheet.HealthLine health = body.health().orElseThrow();
                expect(helper, Math.abs(health.maxHealth() - 26f) < EPSILON, "sheet max health");
                expectEquals(helper, 3, health.conModifier(), "sheet CON modifier");
                StatSheet.ArmorLine armor = body.armor().orElseThrow();
                expectEquals(helper, 2, armor.dexBonus(), "sheet DEX part of AC");
                expectEquals(helper, 1, armor.floorBonus(), "sheet part that keeps the chainmail from lowering AC");
                expectEquals(helper, armor.baseArmorClass() + 3, armor.armorClass(), "sheet AC");
                expectEquals(
                        helper,
                        RollService.effectiveEntity(player).armorClass(),
                        armor.baseArmorClass(),
                        "sheet Critfall AC is the one attacks roll against");
                expectEquals(helper, 11, armor.baseArmorClass(), "sheet Critfall AC: 10 and a quarter of 5 armor");
                expect(
                        helper,
                        armor.worn().equals(Optional.of(new StatSheet.ArmorPoints(10, 5, 1, 0, 0))),
                        "the sheet carries Critfall's breakdown of its AC, was " + armor.worn());
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
                new ScoresConfig.HealthSettings(false, 2.0, false, 0.2, true, 1, 8, true),
                new ScoresConfig.ArmorClassSettings(false, 2),
                false,
                extras);
    }

    private static void set(GameTestHelper helper, ServerPlayer player, String ability, int score) {
        run(helper, "checks set " + player.getScoreboardName() + " " + ability + " " + score);
    }

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

    private static void equip(LivingEntity entity, EquipmentSlot slot, Item item) {
        entity.getItemBySlot(slot).forEachModifier(slot, (attribute, modifier) -> {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(modifier.id());
            }
        });
        ItemStack piece = new ItemStack(item);
        entity.setItemSlot(slot, piece);
        piece.forEachModifier(slot, (attribute, modifier) -> {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(modifier.id());
                instance.addTransientModifier(modifier);
            }
        });
    }

    private static int armorClass(LivingEntity defender, LivingEntity attacker) {
        return RollService.effectiveEntity(defender).armorClass()
                + provider().acModifier(defender, attacker).orElse(0);
    }

    private static void expectArmorClass(
            GameTestHelper helper, int expected, LivingEntity defender, LivingEntity attacker, String what) {
        expectEquals(helper, expected, armorClass(defender, attacker), "AC " + what);
    }

    private static void expectDexPart(GameTestHelper helper, int expected, LivingEntity entity, String what) {
        expectEquals(
                helper, expected, BodyValues.armorClass(entity).orElseThrow().dexBonus(), "DEX part, " + what);
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
