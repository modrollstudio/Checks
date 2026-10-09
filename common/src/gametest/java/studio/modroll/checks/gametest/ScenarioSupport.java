package studio.modroll.checks.gametest;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import studio.modroll.checks.ClientPayloads;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.AfterCheckEvent;
import studio.modroll.checks.api.BeforeCheckEvent;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.body.Size;
import studio.modroll.checks.bonus.BonusSource;
import studio.modroll.checks.bonus.BonusSources;
import studio.modroll.checks.check.CheckEvents;
import studio.modroll.checks.creation.CharacterBuild;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.creation.CharacterStore;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.creation.PresetGrants;
import studio.modroll.checks.creation.SkillPlan;
import studio.modroll.checks.data.EntityScoreProfile;
import studio.modroll.checks.data.EntityScoreProfileStore;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.data.SkillStore;
import studio.modroll.checks.level.LevelStore;
import studio.modroll.checks.preset.Background;
import studio.modroll.checks.preset.Presets;
import studio.modroll.checks.score.PlayerScoreStore;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.social.SocialFeel;
import studio.modroll.checks.trait.TraitUses;
import studio.modroll.checks.trigger.CheckTrigger;
import studio.modroll.checks.trigger.CheckTriggers;
import studio.modroll.critfall.api.ModifierProvider;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.dice.DiceRoller;

/**
 * The {@code with*} helpers patch a store, the config or Critfall's roller for one synchronous body and
 * restore it afterwards, so no test fixture ships in the mod jar.
 */
final class ScenarioSupport {

    private static final int D20_SIDES = 20;
    private static final double CLOSE_ENOUGH = 1e-4;
    private static final int SPAWN_PROTECTION_TICKS = 60;

    private ScenarioSupport() {}

    static Map<ResourceLocation, EntityScoreProfile> profiles(String... jsons) {
        Map<ResourceLocation, EntityScoreProfile> profiles = new HashMap<>();
        for (int i = 0; i < jsons.length; i++) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("checks", "gametest_profile_" + i);
            profiles.put(
                    id,
                    EntityScoreProfile.parse(
                            id, JsonParser.parseString(jsons[i]).getAsJsonObject(), SkillStore::find, w -> {}));
        }
        return profiles;
    }

    static <T> T unprofiled(Supplier<T> body) {
        return withProfiles(Map.of(), body);
    }

    static <T> T withProfiles(Map<ResourceLocation, EntityScoreProfile> patched, Supplier<T> body) {
        Map<ResourceLocation, EntityScoreProfile> before = EntityScoreProfileStore.profiles();
        EntityScoreProfileStore.setProfiles(patched);
        try {
            return body.get();
        } finally {
            EntityScoreProfileStore.setProfiles(before);
        }
    }

    static <T> T withSkills(Map<ResourceLocation, Skill> patched, Supplier<T> body) {
        Map<ResourceLocation, Skill> before = SkillStore.skills();
        SkillStore.setSkills(patched);
        try {
            return body.get();
        } finally {
            SkillStore.setSkills(before);
        }
    }

    static <T> T withConfig(ScoresConfig patched, Supplier<T> body) {
        ScoresConfig before = ScoresRuntime.config();
        ScoresRuntime.setConfig(patched);
        try {
            return body.get();
        } finally {
            ScoresRuntime.setConfig(before);
        }
    }

    static <T> T withCreation(ScoresConfig.CreationSettings patched, Supplier<T> body) {
        ScoresConfig live = ScoresRuntime.config();
        return withConfig(
                new ScoresConfig(
                        live.playerDefaults(),
                        live.derivation(),
                        live.profilesEnabled(),
                        live.skillsEnabled(),
                        live.proficiency(),
                        live.critfall(),
                        live.statScreenEnabled(),
                        patched,
                        live.levelling(),
                        live.extensions(),
                        live.body(),
                        live.saves(),
                        live.traits(),
                        live.social(),
                        live.rollMessages(),
                        live.deathMessages(),
                        live.exploration()),
                body);
    }

    static <T> T withPresetSettings(ScoresConfig.PresetSettings patched, Supplier<T> body) {
        ScoresConfig.CreationSettings live = ScoresRuntime.config().creation();
        return withCreation(
                new ScoresConfig.CreationSettings(
                        live.enabled(),
                        live.methods(),
                        live.skillChoices(),
                        live.standardArray(),
                        live.pointBuy(),
                        live.rollDice(),
                        live.hardcoreDice(),
                        patched),
                body);
    }

    static <T> T withLevelling(ScoresConfig.LevellingSettings patched, Supplier<T> body) {
        ScoresConfig live = ScoresRuntime.config();
        return withConfig(
                new ScoresConfig(
                        live.playerDefaults(),
                        live.derivation(),
                        live.profilesEnabled(),
                        live.skillsEnabled(),
                        live.proficiency(),
                        live.critfall(),
                        live.statScreenEnabled(),
                        live.creation(),
                        patched,
                        live.extensions(),
                        live.body(),
                        live.saves(),
                        live.traits(),
                        live.social(),
                        live.rollMessages(),
                        live.deathMessages(),
                        live.exploration()),
                body);
    }

    /** The default levelling settings, switched on or off. */
    static ScoresConfig.LevellingSettings levellingEnabled(boolean enabled) {
        ScoresConfig.LevellingSettings defaults = ScoresConfig.DEFAULTS.levelling();
        return new ScoresConfig.LevellingSettings(
                enabled,
                defaults.xpThresholds(),
                defaults.proficiencyBonuses(),
                defaults.xpSources(),
                defaults.improvements());
    }

    static <T> T withBackgrounds(Map<ResourceLocation, Background> patched, Supplier<T> body) {
        Map<ResourceLocation, Background> before = Presets.BACKGROUNDS.all();
        Presets.BACKGROUNDS.set(patched);
        try {
            return body.get();
        } finally {
            Presets.BACKGROUNDS.set(before);
        }
    }

    /**
     * Runs {@code body} with every server-to-client payload going to {@code patched}, which answers whether
     * the client took it. A test player's client has no Checks channel, so an untouched payload answers false.
     */
    static <T> T withSender(BiPredicate<ServerPlayer, CustomPacketPayload> patched, Supplier<T> body) {
        BiPredicate<ServerPlayer, CustomPacketPayload> before = ClientPayloads.sender();
        ClientPayloads.setSender(patched);
        try {
            return body.get();
        } finally {
            ClientPayloads.setSender(before);
        }
    }

    /** A sender that adds every payload of {@code type} to {@code sent}; no client takes any other. */
    static <P extends CustomPacketPayload> BiPredicate<ServerPlayer, CustomPacketPayload> collecting(
            Class<P> type, List<? super P> sent) {
        return (to, payload) -> type.isInstance(payload) && sent.add(type.cast(payload));
    }

    static <T> T withCritfallSettings(ScoresConfig.CritfallSettings patched, Supplier<T> body) {
        ScoresConfig live = ScoresRuntime.config();
        return withConfig(
                new ScoresConfig(
                        live.playerDefaults(),
                        live.derivation(),
                        live.profilesEnabled(),
                        live.skillsEnabled(),
                        live.proficiency(),
                        patched,
                        live.statScreenEnabled(),
                        live.creation(),
                        live.levelling(),
                        live.extensions(),
                        live.body(),
                        live.saves(),
                        live.traits(),
                        live.social(),
                        live.rollMessages(),
                        live.deathMessages(),
                        live.exploration()),
                body);
    }

    static <T> T withExtensions(ScoresConfig.ExtensionSettings patched, Supplier<T> body) {
        ScoresConfig live = ScoresRuntime.config();
        return withConfig(
                new ScoresConfig(
                        live.playerDefaults(),
                        live.derivation(),
                        live.profilesEnabled(),
                        live.skillsEnabled(),
                        live.proficiency(),
                        live.critfall(),
                        live.statScreenEnabled(),
                        live.creation(),
                        live.levelling(),
                        patched,
                        live.body(),
                        live.saves(),
                        live.traits(),
                        live.social(),
                        live.rollMessages(),
                        live.deathMessages(),
                        live.exploration()),
                body);
    }

    static <T> T withBody(ScoresConfig.BodySettings patched, Supplier<T> body) {
        ScoresConfig live = ScoresRuntime.config();
        return withConfig(
                new ScoresConfig(
                        live.playerDefaults(),
                        live.derivation(),
                        live.profilesEnabled(),
                        live.skillsEnabled(),
                        live.proficiency(),
                        live.critfall(),
                        live.statScreenEnabled(),
                        live.creation(),
                        live.levelling(),
                        live.extensions(),
                        patched,
                        live.saves(),
                        live.traits(),
                        live.social(),
                        live.rollMessages(),
                        live.deathMessages(),
                        live.exploration()),
                body);
    }

    static <T> T withTraits(ScoresConfig.TraitSettings patched, Supplier<T> body) {
        ScoresConfig live = ScoresRuntime.config();
        return withConfig(
                new ScoresConfig(
                        live.playerDefaults(),
                        live.derivation(),
                        live.profilesEnabled(),
                        live.skillsEnabled(),
                        live.proficiency(),
                        live.critfall(),
                        live.statScreenEnabled(),
                        live.creation(),
                        live.levelling(),
                        live.extensions(),
                        live.body(),
                        live.saves(),
                        patched,
                        live.social(),
                        live.rollMessages(),
                        live.deathMessages(),
                        live.exploration()),
                body);
    }

    /** Reads the trait use store back from disk. Call {@link #saveWorldData} first. */
    static TraitUses savedTraitUses(GameTestHelper helper) {
        return TraitUses.load(
                savedData(helper, TraitUses.DATA_NAME), helper.getLevel().registryAccess());
    }

    static <T> T withSaves(ScoresConfig.SaveSettings patched, Supplier<T> body) {
        ScoresConfig live = ScoresRuntime.config();
        return withConfig(
                new ScoresConfig(
                        live.playerDefaults(),
                        live.derivation(),
                        live.profilesEnabled(),
                        live.skillsEnabled(),
                        live.proficiency(),
                        live.critfall(),
                        live.statScreenEnabled(),
                        live.creation(),
                        live.levelling(),
                        live.extensions(),
                        live.body(),
                        patched,
                        live.traits(),
                        live.social(),
                        live.rollMessages(),
                        live.deathMessages(),
                        live.exploration()),
                body);
    }

    static <T> T withSocial(ScoresConfig.SocialSettings patched, Supplier<T> body) {
        ScoresConfig live = ScoresRuntime.config();
        return withConfig(
                new ScoresConfig(
                        live.playerDefaults(),
                        live.derivation(),
                        live.profilesEnabled(),
                        live.skillsEnabled(),
                        live.proficiency(),
                        live.critfall(),
                        live.statScreenEnabled(),
                        live.creation(),
                        live.levelling(),
                        live.extensions(),
                        live.body(),
                        live.saves(),
                        live.traits(),
                        patched,
                        live.rollMessages(),
                        live.deathMessages(),
                        live.exploration()),
                body);
    }

    static <T> T withRollMessages(ScoresConfig.RollMessageSettings patched, Supplier<T> body) {
        ScoresConfig live = ScoresRuntime.config();
        return withConfig(
                new ScoresConfig(
                        live.playerDefaults(),
                        live.derivation(),
                        live.profilesEnabled(),
                        live.skillsEnabled(),
                        live.proficiency(),
                        live.critfall(),
                        live.statScreenEnabled(),
                        live.creation(),
                        live.levelling(),
                        live.extensions(),
                        live.body(),
                        live.saves(),
                        live.traits(),
                        live.social(),
                        patched,
                        live.deathMessages(),
                        live.exploration()),
                body);
    }

    static <T> T withFeel(ScoresConfig.FeelSettings patched, Supplier<T> body) {
        ScoresConfig.SocialSettings live = ScoresRuntime.config().social();
        return withSocial(
                new ScoresConfig.SocialSettings(
                        live.enabled(),
                        live.nearbyRadius(),
                        live.gossipRadius(),
                        live.persuade(),
                        live.deceive(),
                        live.intimidate(),
                        live.pickpocket(),
                        live.passivePrices(),
                        live.reactions(),
                        live.witnesses(),
                        live.golemAlarm(),
                        live.onGuard(),
                        live.wary(),
                        patched,
                        live.intimidateMob(),
                        live.calm(),
                        live.insight(),
                        live.performance()),
                body);
    }

    /** Runs {@code body} with {@code patched} telling which players' clients have Checks. */
    static <T> T withChecksClients(Predicate<ServerPlayer> patched, Supplier<T> body) {
        Predicate<ServerPlayer> before = SocialFeel.checksClient();
        SocialFeel.setChecksClient(patched);
        try {
            return body.get();
        } finally {
            SocialFeel.setChecksClient(before);
        }
    }

    static <T> T withDeathMessages(ScoresConfig.DeathMessageSettings patched, Supplier<T> body) {
        ScoresConfig live = ScoresRuntime.config();
        return withConfig(
                new ScoresConfig(
                        live.playerDefaults(),
                        live.derivation(),
                        live.profilesEnabled(),
                        live.skillsEnabled(),
                        live.proficiency(),
                        live.critfall(),
                        live.statScreenEnabled(),
                        live.creation(),
                        live.levelling(),
                        live.extensions(),
                        live.body(),
                        live.saves(),
                        live.traits(),
                        live.social(),
                        live.rollMessages(),
                        patched,
                        live.exploration()),
                body);
    }

    static <T> T withExploration(ScoresConfig.ExplorationSettings patched, Supplier<T> body) {
        ScoresConfig live = ScoresRuntime.config();
        return withConfig(
                new ScoresConfig(
                        live.playerDefaults(),
                        live.derivation(),
                        live.profilesEnabled(),
                        live.skillsEnabled(),
                        live.proficiency(),
                        live.critfall(),
                        live.statScreenEnabled(),
                        live.creation(),
                        live.levelling(),
                        live.extensions(),
                        live.body(),
                        live.saves(),
                        live.traits(),
                        live.social(),
                        live.rollMessages(),
                        live.deathMessages(),
                        patched),
                body);
    }

    /** Runs {@code body} with only these check listeners registered; none are left behind. */
    static <T> T withListeners(Consumer<BeforeCheckEvent> before, Consumer<AfterCheckEvent> after, Supplier<T> body) {
        CheckEvents.clearListeners();
        ChecksApi.onBeforeCheck(before);
        ChecksApi.onAfterCheck(after);
        try {
            return body.get();
        } finally {
            CheckEvents.clearListeners();
        }
    }

    /** Parses {@code data/checks/checks/bonus/gametest_<i>.json}-style sources from inline JSON. */
    static Map<ResourceLocation, BonusSource> bonusSources(String... jsons) {
        Map<ResourceLocation, BonusSource> sources = new HashMap<>();
        for (int i = 0; i < jsons.length; i++) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("checks", "gametest_bonus_" + i);
            sources.put(
                    id,
                    BonusSource.parse(
                            id,
                            JsonParser.parseString(jsons[i]).getAsJsonObject(),
                            SkillStore::find,
                            BuiltInRegistries.ITEM::containsKey,
                            BuiltInRegistries.MOB_EFFECT::containsKey,
                            w -> {}));
        }
        return sources;
    }

    static <T> T withBonusSources(Map<ResourceLocation, BonusSource> patched, Supplier<T> body) {
        List<BonusSource> before = BonusSources.all();
        BonusSources.set(patched);
        try {
            return body.get();
        } finally {
            BonusSources.set(before.stream().collect(Collectors.toMap(BonusSource::id, Function.identity())));
        }
    }

    static CheckTrigger trigger(String path, String json) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("checks", path);
        return CheckTrigger.parse(
                id,
                JsonParser.parseString(json).getAsJsonObject(),
                SkillStore::find,
                on -> switch (on) {
                    case USE_BLOCK -> BuiltInRegistries.BLOCK::containsKey;
                    case USE_ENTITY -> BuiltInRegistries.ENTITY_TYPE::containsKey;
                    case USE_ITEM -> BuiltInRegistries.ITEM::containsKey;
                },
                w -> {});
    }

    /** Runs {@code body} with only these triggers loaded and no cooldowns, then restores what was loaded. */
    static <T> T withTriggers(List<CheckTrigger> patched, Supplier<T> body) {
        List<CheckTrigger> before = CheckTriggers.all();
        CheckTriggers.clear();
        CheckTriggers.set(patched.stream().collect(Collectors.toMap(CheckTrigger::id, Function.identity())));
        try {
            return body.get();
        } finally {
            CheckTriggers.clear();
            CheckTriggers.set(before.stream().collect(Collectors.toMap(CheckTrigger::id, Function.identity())));
        }
    }

    /** Runs a command the way a function or command block does, so {@code execute store} sees its result. */
    static void perform(GameTestHelper helper, String command) {
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands()
                .performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    /** A dummy objective, created on first use. */
    static Objective objective(GameTestHelper helper, String name) {
        Scoreboard scoreboard = helper.getLevel().getServer().getScoreboard();
        Objective existing = scoreboard.getObjective(name);
        if (existing != null) {
            return existing;
        }
        return scoreboard.addObjective(
                name,
                ObjectiveCriteria.DUMMY,
                Component.literal(name),
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                null);
    }

    /** The score of a fake holder like {@code #roll}. */
    static int score(GameTestHelper helper, Objective objective, String holder) {
        return helper.getLevel()
                .getServer()
                .getScoreboard()
                .getOrCreatePlayerScore(ScoreHolder.forNameOnly(holder), objective)
                .get();
    }

    /** Runs {@code body} as if Checks were absent: Critfall's modifier provider slot is empty. */
    static <T> T withoutModifierProvider(Supplier<T> body) {
        return withModifierProvider(Optional.empty(), body);
    }

    /** Runs {@code body} as if another mod held Critfall's modifier provider slot. */
    static <T> T withOtherModifierProvider(ModifierProvider other, Supplier<T> body) {
        return withModifierProvider(Optional.of(other), body);
    }

    private static <T> T withModifierProvider(Optional<ModifierProvider> patched, Supplier<T> body) {
        Optional<ModifierProvider> before = RollService.modifierProvider();
        fillProviderSlot(patched);
        try {
            return body.get();
        } finally {
            fillProviderSlot(before);
        }
    }

    private static void fillProviderSlot(Optional<ModifierProvider> provider) {
        provider.ifPresentOrElse(RollService::registerModifierProvider, RollService::clearModifierProvider);
    }

    /** Runs {@code body} with Critfall's roller forced to the given d20 faces, in roll order; other dice roll max. */
    static <T> T withD20s(Supplier<T> body, int... faces) {
        return withRoller(scripted(bound -> bound == D20_SIDES, faces), body);
    }

    /** Runs {@code body} with every die, of any size, showing the given faces in roll order; running out fails. */
    static <T> T withDice(Supplier<T> body, int... faces) {
        return withRoller(scripted(bound -> true, faces), body);
    }

    private static <T> T withRoller(RandomGenerator rng, Supplier<T> body) {
        RollService.setRoller(new DiceRoller(rng));
        try {
            return body.get();
        } finally {
            RollService.resetRoller();
        }
    }

    /** Dice whose size {@code scripts} accepts show {@code faces} in roll order; any other die rolls its max. */
    private static RandomGenerator scripted(IntPredicate scripts, int... faces) {
        Queue<Integer> queue = new ArrayDeque<>();
        for (int face : faces) {
            queue.add(face);
        }
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new IllegalStateException("forced dice must only draw bounded ints");
            }

            @Override
            public int nextInt(int bound) {
                if (!scripts.test(bound)) {
                    return bound - 1;
                }
                Integer face = queue.poll();
                if (face == null || face > bound) {
                    throw new IllegalStateException("no scripted face left for a d" + bound);
                }
                return face - 1;
            }
        };
    }

    static GameProfile newProfile(String name) {
        return new GameProfile(UUID.randomUUID(), name);
    }

    /** Mirrors vanilla's deprecated makeMockServerPlayerInLevel, but with a reusable profile for relogs. */
    static ServerPlayer login(GameTestHelper helper, GameProfile profile) {
        return login(helper, profile, new Connection(PacketFlow.SERVERBOUND));
    }

    /** As {@link #login}, with every packet the server sends the player also added to {@code sent}. */
    static ServerPlayer loginWatched(GameTestHelper helper, GameProfile profile, List<Packet<?>> sent) {
        return login(helper, profile, new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public void send(Packet<?> packet, @Nullable PacketSendListener listener, boolean flush) {
                sent.add(packet);
                super.send(packet, listener, flush);
            }
        });
    }

    private static ServerPlayer login(GameTestHelper helper, GameProfile profile, Connection connection) {
        MinecraftServer server = helper.getLevel().getServer();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, cookie.clientInformation());
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    static void logout(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    /** A player who just joined takes no damage for a while; a test player only ticks when told to. */
    static void waitOutSpawnProtection(ServerPlayer player) {
        for (int tick = 0; tick < SPAWN_PROTECTION_TICKS; tick++) {
            player.tick();
        }
    }

    /** Reads the player store back from disk. Call {@link #saveWorldData} first. */
    static PlayerScoreStore savedPlayerStore(GameTestHelper helper) {
        return PlayerScoreStore.load(
                savedData(helper, PlayerScoreStore.DATA_NAME), helper.getLevel().registryAccess());
    }

    /** Reads the character store back from disk. Call {@link #saveWorldData} first. */
    static CharacterStore savedCharacterStore(GameTestHelper helper) {
        return CharacterStore.load(
                savedData(helper, CharacterStore.DATA_NAME), helper.getLevel().registryAccess());
    }

    /** Reads the level store back from disk. Call {@link #saveWorldData} first. */
    static LevelStore savedLevelStore(GameTestHelper helper) {
        return LevelStore.load(
                savedData(helper, LevelStore.DATA_NAME), helper.getLevel().registryAccess());
    }

    private static CompoundTag savedData(GameTestHelper helper, String name) {
        try {
            return helper.getLevel()
                    .getServer()
                    .overworld()
                    .getDataStorage()
                    .readTagFromDisk(
                            name,
                            null,
                            SharedConstants.getCurrentVersion().getDataVersion().getVersion())
                    .getCompound("data");
        } catch (IOException e) {
            helper.fail("could not read " + name + " back from the world save: " + e);
            return new CompoundTag();
        }
    }

    static void expectClose(GameTestHelper helper, double expected, double actual, String what) {
        expect(helper, Math.abs(expected - actual) < CLOSE_ENOUGH, what + " must be " + expected + ", was " + actual);
    }

    static ModifierProvider provider() {
        return RollService.modifierProvider()
                .orElseThrow(() -> new IllegalStateException("Checks registered no Critfall modifier provider"));
    }

    static int reputation(Villager villager, ServerPlayer player, GossipType type) {
        return villager.getGossips().getReputation(player.getUUID(), gossip -> gossip == type);
    }

    /** Saves every dirty SavedData file and waits until the writes have reached the disk. */
    static void saveWorldData(GameTestHelper helper) {
        helper.getLevel().getServer().overworld().getDataStorage().save();
        SavedDataWrites.await();
    }

    static int run(GameTestHelper helper, String command) {
        try {
            return execute(helper, command, serverSource(helper).withSuppressedOutput());
        } catch (CommandSyntaxException e) {
            helper.fail("command \"" + command + "\" failed: " + e.getMessage());
            return 0;
        }
    }

    static void runExpectingFailure(GameTestHelper helper, String command) {
        try {
            execute(helper, command, serverSource(helper).withSuppressedOutput());
        } catch (CommandSyntaxException e) {
            return;
        }
        helper.fail("command \"" + command + "\" must fail");
    }

    /** Runs a command and returns the chat lines it sent back to its source. */
    static List<String> runCapturingOutput(GameTestHelper helper, String command) {
        List<String> lines = new ArrayList<>();
        CommandSource capture = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                lines.add(message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        try {
            execute(helper, command, serverSource(helper).withSource(capture));
        } catch (CommandSyntaxException e) {
            helper.fail("command \"" + command + "\" failed: " + e.getMessage());
        }
        return lines;
    }

    private static int execute(GameTestHelper helper, String command, CommandSourceStack source)
            throws CommandSyntaxException {
        return helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
    }

    private static CommandSourceStack serverSource(GameTestHelper helper) {
        return helper.getLevel().getServer().createCommandSourceStack();
    }

    /** Gives the player a confirmed character of {@code species}: every score 10, nothing else chosen. */
    static void confirm(ServerPlayer player, ResourceLocation species, Optional<Size> size) {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            scores.put(ability, 10);
        }
        CharacterBuild build = new CharacterBuild(
                CreationMethod.STANDARD_ARRAY,
                scores,
                Set.of(),
                new PresetChoices(Optional.of(species), Optional.empty(), Optional.empty(), size),
                PresetGrants.NONE);
        CharacterStore.get(player.server).update(player.getUUID(), character -> character.withBuild(build));
    }

    /** The first skills, by id, the species' trait choices may pick besides {@code picks}. */
    static List<ResourceLocation> speciesPicks(
            ServerPlayer player, PresetChoices choices, List<ResourceLocation> picks) {
        SkillPlan plan = SkillPlan.of(CharacterCreation.offerFor(player), choices);
        return plan.speciesOptions().stream()
                .filter(skill -> !picks.contains(skill))
                .sorted()
                .limit(plan.speciesRequired())
                .toList();
    }

    static Mob spawnCalm(GameTestHelper helper, EntityType<? extends Mob> type) {
        Mob mob = helper.spawn(type, new BlockPos(1, 2, 1));
        mob.setNoAi(true);
        return mob;
    }

    static Skill standardSkill(String path) {
        return ChecksApi.skill(ResourceLocation.withDefaultNamespace(path))
                .orElseThrow(() -> new IllegalStateException("standard skill '" + path + "' is not loaded"));
    }

    static void expect(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    static void expectEquals(GameTestHelper helper, int expected, int actual, String what) {
        expect(helper, actual == expected, what + " must be " + expected + ", was " + actual);
    }
}
