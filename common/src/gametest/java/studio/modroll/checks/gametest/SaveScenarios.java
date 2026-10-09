package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.bonusSources;
import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectClose;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.perform;
import static studio.modroll.checks.gametest.ScenarioSupport.profiles;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.spawnCalm;
import static studio.modroll.checks.gametest.ScenarioSupport.waitOutSpawnProtection;
import static studio.modroll.checks.gametest.ScenarioSupport.withBonusSources;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withListeners;
import static studio.modroll.checks.gametest.ScenarioSupport.withProfiles;
import static studio.modroll.checks.gametest.ScenarioSupport.withSaves;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.save.VanillaSave;

/**
 * GameTest bodies for the saves against vanilla hazards. Each test player is new, so no cooldown carries
 * over; most run with no cooldown so every trigger rolls. A d20 of 2 fails and 19 makes every default DC
 * for a player with +0 modifiers.
 */
public final class SaveScenarios {

    private static final int FAIL = 2;
    private static final int SAVE = 19;
    private static final float HIT = 8f;
    private static final float FULL_HEALTH = 20f;
    private static final int POISON_TICKS = 200;
    private static final int DARKNESS_TICKS = 260;
    private static final int FIRE_SECONDS = 8;
    private static final int FIRE_TICKS = 160;
    private static final double BIG_PUSH = 1.0;
    private static final double PLAIN_HIT = 0.4;
    private static final Vec3 STANDING_SPOT = new Vec3(1.5, 2, 1.5);
    private static final Vec3 BLAST_OFFSET = new Vec3(2, 0, 0);
    private static final float BLAST_RADIUS = 2f;
    private static final ScoresConfig.SaveSettings DEFAULTS = ScoresConfig.DEFAULTS.saves();
    private static final String LUCKY_DEX = "{\"source\": {\"effect\": \"minecraft:luck\"},"
            + " \"applies_to\": {\"saves\": [\"dex\"]}, \"mode\": \"advantage\"}";
    private static final String PIG_PROFILE = "{\"matches\": [\"minecraft:pig\"], \"abilities\": {\"con\": 10}}";

    private SaveScenarios() {}

    /** A real explosion: DEX halves its damage, STR halves its push. */
    public static void explosionCallsForDexAndStrSaves(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "save-blast");
        try {
            Blast failed = noCooldown(() -> withD20s(() -> blast(helper, player), FAIL, FAIL));
            Blast saved = noCooldown(() -> withD20s(() -> blast(helper, player), SAVE, SAVE));
            expect(helper, failed.damage() > 0, "the explosion must hurt the player");
            expect(helper, failed.push() > 0, "the explosion must push the player");
            expectClose(helper, failed.damage() / 2, saved.damage(), "damage after a made DEX save");
            expectClose(helper, failed.push() / 2, saved.push(), "push after a made STR save");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** CON halves poison, wither and hunger; any other effect never calls for a save. */
    public static void poisonSaveHalvesTheDuration(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "save-poison");
        try {
            noCooldown(() -> {
                expectEquals(helper, POISON_TICKS, effectTicks(player, MobEffects.POISON, FAIL), "failed poison");
                expectEquals(helper, POISON_TICKS / 2, effectTicks(player, MobEffects.POISON, SAVE), "saved poison");
                expectEquals(helper, POISON_TICKS / 2, effectTicks(player, MobEffects.WITHER, SAVE), "saved wither");
                expectEquals(helper, POISON_TICKS / 2, effectTicks(player, MobEffects.HUNGER, SAVE), "saved hunger");
                expectEquals(
                        helper, 0, rolls(() -> player.addEffect(effect(MobEffects.MOVEMENT_SPEED))), "speed rolls");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** {@code /effect give}, from chat or a function, never calls for a save. */
    public static void commandEffectsNeverCallForASave(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "save-command");
        try {
            noCooldown(() -> {
                String command = "effect give " + player.getStringUUID() + " minecraft:poison 10";
                expectEquals(helper, 0, rolls(() -> run(helper, command)), "rolls for /effect give");
                expectEquals(
                        helper,
                        POISON_TICKS,
                        player.getEffect(MobEffects.POISON).getDuration(),
                        "poison");
                player.removeAllEffects();
                expectEquals(helper, 0, rolls(() -> perform(helper, command)), "rolls for a function's /effect");
                expectEquals(helper, POISON_TICKS / 2, effectTicks(player, MobEffects.POISON, SAVE), "mob poison");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** WIS halves the Warden's darkness. */
    public static void darknessSaveHalvesTheDuration(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "save-dark");
        try {
            noCooldown(() -> {
                expectEquals(helper, DARKNESS_TICKS, darknessTicks(player, FAIL), "failed darkness");
                expectEquals(helper, DARKNESS_TICKS / 2, darknessTicks(player, SAVE), "saved darkness");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** STR halves a big push; a plain hit's knockback never calls for a save. */
    public static void knockbackSaveOnlyForBigPushes(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "save-push");
        try {
            noCooldown(() -> {
                expectClose(helper, BIG_PUSH, withD20s(() -> push(player, BIG_PUSH), FAIL), "failed STR save push");
                expectClose(helper, BIG_PUSH / 2, withD20s(() -> push(player, BIG_PUSH), SAVE), "saved STR save push");
                expectEquals(helper, 0, rolls(() -> push(player, PLAIN_HIT)), "rolls for a plain hit");
                expectClose(helper, PLAIN_HIT, push(player, PLAIN_HIT), "plain hit push");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** DEX keeps a fire block from setting the player alight; lava never calls for a save. */
    public static void fireSaveKeepsThePlayerFromCatchingFire(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "save-fire");
        try {
            noCooldown(() -> {
                expectEquals(
                        helper, FIRE_TICKS, withD20s(() -> ignite(player), FAIL), "burn ticks after a failed save");
                expectEquals(helper, 0, withD20s(() -> ignite(player), SAVE), "burn ticks after a made save");

                BlockPos at = player.blockPosition();
                player.clearFire();
                withD20s(
                        () -> {
                            Blocks.FIRE.defaultBlockState().entityInside(helper.getLevel(), at, player);
                            return null;
                        },
                        SAVE);
                expect(helper, player.getRemainingFireTicks() < FIRE_TICKS, "a made save must keep the fire block off");

                helper.getLevel().setBlockAndUpdate(at, Blocks.LAVA.defaultBlockState());
                try {
                    expectEquals(helper, 0, rolls(() -> ignite(player)), "rolls in lava");
                    expectEquals(helper, FIRE_TICKS, ignite(player), "burn ticks in lava");
                } finally {
                    helper.getLevel().setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                }
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A bonus source's advantage on DEX saves keeps the better of 2 and 19. */
    public static void bonusSourceAdvantageApplies(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "save-lucky");
        player.addEffect(effect(MobEffects.LUCK));
        try {
            int ticks = noCooldown(
                    () -> withBonusSources(bonusSources(LUCKY_DEX), () -> withD20s(() -> ignite(player), FAIL, SAVE)));
            expectEquals(helper, 0, ticks, "burn ticks with advantage on the DEX save");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Inside the cooldown a second fire reuses the made save: one roll, never alight. */
    public static void cooldownReusesTheLastResult(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "save-cool");
        try {
            withSaves(DEFAULTS, () -> {
                AtomicInteger burns = new AtomicInteger();
                int rolled = withD20s(
                        () -> rolls(() -> {
                            burns.addAndGet(ignite(player));
                            burns.addAndGet(ignite(player));
                        }),
                        SAVE);
                expectEquals(helper, 1, rolled, "rolls inside the cooldown");
                expectEquals(helper, 0, burns.get(), "burn ticks inside the cooldown");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Each save switched off alone rolls nothing and leaves vanilla's outcome. */
    public static void eachSaveSwitchesOff(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, "save-off");
        try {
            for (VanillaSave save : VanillaSave.values()) {
                withSaves(without(save), () -> {
                    expectEquals(helper, 0, rolls(() -> trigger(helper, player, save)), save.id() + " rolls when off");
                    return null;
                });
            }
            withSaves(without(VanillaSave.EXPLOSION), () -> {
                player.hurt(helper.getLevel().damageSources().explosion(null, null), HIT);
                expectClose(helper, FULL_HEALTH - HIT, player.getHealth(), "health after an explosion with saves off");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Mobs never save by default; with profiled mobs on, a profiled pig saves and an unprofiled cow does not. */
    public static void mobsSaveOnlyWhenProfiledAndSwitchedOn(GameTestHelper helper) {
        Mob pig = spawnCalm(helper, EntityType.PIG);
        Mob cow = spawnCalm(helper, EntityType.COW);
        withProfiles(profiles(PIG_PROFILE), () -> {
            noCooldown(() -> {
                expectEquals(helper, 0, rolls(() -> pig.addEffect(effect(MobEffects.POISON))), "pig rolls by default");
                return null;
            });
            withSaves(profiledMobs(), () -> {
                pig.removeAllEffects();
                expectEquals(helper, POISON_TICKS / 2, effectTicks(pig, MobEffects.POISON, SAVE), "profiled pig");
                expectEquals(helper, 0, rolls(() -> cow.addEffect(effect(MobEffects.POISON))), "unprofiled cow rolls");
                return null;
            });
            return null;
        });
        helper.succeed();
    }

    private record Blast(float damage, double push) {}

    private static Blast blast(GameTestHelper helper, ServerPlayer player) {
        reset(player);
        Vec3 at = player.position().add(BLAST_OFFSET);
        helper.getLevel().explode(null, at.x, at.y, at.z, BLAST_RADIUS, Level.ExplosionInteraction.NONE);
        return new Blast(
                FULL_HEALTH - player.getHealth(), player.getDeltaMovement().horizontalDistance());
    }

    private static int effectTicks(LivingEntity entity, Holder<MobEffect> effect, int face) {
        entity.removeAllEffects();
        withD20s(() -> entity.addEffect(effect(effect)), face);
        return entity.getEffect(effect).getDuration();
    }

    private static int darknessTicks(ServerPlayer player, int face) {
        player.removeAllEffects();
        withD20s(() -> player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_TICKS)), face);
        return player.getEffect(MobEffects.DARKNESS).getDuration();
    }

    private static MobEffectInstance effect(Holder<MobEffect> effect) {
        return new MobEffectInstance(effect, POISON_TICKS);
    }

    private static double push(ServerPlayer player, double strength) {
        player.setDeltaMovement(Vec3.ZERO);
        player.knockback(strength, 1, 0);
        return player.getDeltaMovement().horizontalDistance();
    }

    private static int ignite(ServerPlayer player) {
        player.clearFire();
        player.igniteForSeconds(FIRE_SECONDS);
        return Math.max(0, player.getRemainingFireTicks());
    }

    private static void trigger(GameTestHelper helper, ServerPlayer player, VanillaSave save) {
        reset(player);
        player.removeAllEffects();
        switch (save) {
            case EXPLOSION -> player.hurt(helper.getLevel().damageSources().explosion(null, null), HIT);
            case POISON -> player.addEffect(effect(MobEffects.POISON));
            case KNOCKBACK -> push(player, BIG_PUSH);
            case FIRE -> ignite(player);
            case DARKNESS -> player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_TICKS));
        }
        reset(player);
    }

    /** How many rolls {@code body} made. */
    private static int rolls(Runnable body) {
        AtomicInteger count = new AtomicInteger();
        withListeners(before -> {}, after -> count.incrementAndGet(), () -> {
            body.run();
            return null;
        });
        return count.get();
    }

    private static ServerPlayer survivor(GameTestHelper helper, String name) {
        ServerPlayer player = login(helper, newProfile(name));
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(STANDING_SPOT));
        waitOutSpawnProtection(player);
        reset(player);
        return player;
    }

    private static void reset(ServerPlayer player) {
        player.setHealth(FULL_HEALTH);
        player.invulnerableTime = 0;
        player.setDeltaMovement(Vec3.ZERO);
        player.clearFire();
    }

    private static <T> T noCooldown(Supplier<T> body) {
        return withSaves(settings(DEFAULTS.profiledMobs(), DEFAULTS.saves()), body);
    }

    private static ScoresConfig.SaveSettings profiledMobs() {
        return settings(true, DEFAULTS.saves());
    }

    private static ScoresConfig.SaveSettings without(VanillaSave off) {
        Map<VanillaSave, ScoresConfig.SaveSetting> saves = new EnumMap<>(DEFAULTS.saves());
        ScoresConfig.SaveSetting setting = saves.get(off);
        saves.put(off, new ScoresConfig.SaveSetting(false, setting.dc(), setting.successMultiplier()));
        return settings(DEFAULTS.profiledMobs(), saves);
    }

    private static ScoresConfig.SaveSettings settings(
            boolean profiledMobs, Map<VanillaSave, ScoresConfig.SaveSetting> saves) {
        return new ScoresConfig.SaveSettings(profiledMobs, 0, DEFAULTS.knockbackMinStrength(), saves);
    }
}
