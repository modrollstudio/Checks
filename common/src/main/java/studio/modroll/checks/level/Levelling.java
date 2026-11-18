package studio.modroll.checks.level;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import studio.modroll.checks.ClientPayloads;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.LevelUpEvent;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.data.SkillStore;
import studio.modroll.checks.preset.ClassPreset;
import studio.modroll.checks.preset.Presets;
import studio.modroll.checks.score.ScoreService;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.sheet.StatSheetPayload;
import studio.modroll.checks.sheet.StatSheets;
import studio.modroll.checks.text.FallbackText;

/** While levelling is off no XP is gained and no level changes, but stored levels are kept. */
public final class Levelling {

    private static final List<Consumer<LevelUpEvent>> LISTENERS = new CopyOnWriteArrayList<>();

    private Levelling() {}

    public static void addListener(Consumer<LevelUpEvent> listener) {
        LISTENERS.add(listener);
    }

    public static boolean enabled() {
        return settings().enabled();
    }

    public static PlayerLevel activeLevel(ServerPlayer player) {
        return enabled() ? LevelStore.get(player.server).level(player.getUUID()) : PlayerLevel.START;
    }

    /** Counts what Mending spends from an orb too; spending or losing XP later never takes character XP. */
    public static void onVanillaXp(ServerPlayer player, int points) {
        ScoresConfig.XpSources sources = settings().xpSources();
        if (sources.vanillaXpEnabled() && points > 0) {
            gainXp(player, (int) Math.min(Integer.MAX_VALUE, (long) points * sources.xpPerPoint()));
        }
    }

    /** Only advancements shown in the advancement screen count, so recipe unlocks give nothing. */
    public static void onAdvancement(ServerPlayer player, AdvancementHolder advancement) {
        ScoresConfig.XpSources sources = settings().xpSources();
        if (sources.advancementsEnabled() && advancement.value().display().isPresent() && !player.isDeadOrDying()) {
            gainXp(player, sources.xpPerAdvancement());
        }
    }

    public static void gainXp(ServerPlayer player, int amount) {
        if (enabled() && amount > 0) {
            change(player, level -> LevelRules.gainXp(level, amount, settings()));
        }
    }

    public static void setLevel(ServerPlayer player, int level) {
        if (enabled()) {
            change(player, current -> LevelRules.setLevel(current, level, settings()));
        }
    }

    public static Optional<ImprovementRejection> improve(ServerPlayer player, List<Integer> increases) {
        return choose(
                player,
                checkImprovement(player, increases),
                level -> level.withImprovement(LevelRules.improvement(increases)),
                "checks.level.improved");
    }

    public static Optional<ImprovementRejection> chooseExpertise(ServerPlayer player, List<ResourceLocation> skills) {
        return choose(
                player,
                checkExpertise(player, skills),
                level -> level.withExpertise(skills),
                "checks.level.expertise_chosen");
    }

    /** Either way the player's stat sheet is sent again, so the screen shows the outcome. */
    private static Optional<ImprovementRejection> choose(
            ServerPlayer player,
            Optional<ImprovementRejection> rejection,
            UnaryOperator<PlayerLevel> apply,
            String acceptedKey) {
        rejection.ifPresentOrElse(
                reason -> player.sendSystemMessage(
                        reason.message(settings().improvements()).copy().withStyle(ChatFormatting.RED)),
                () -> {
                    LevelStore store = LevelStore.get(player.server);
                    UUID uuid = player.getUUID();
                    store.set(uuid, apply.apply(store.level(uuid)));
                    player.sendSystemMessage(Component.translatable(acceptedKey));
                });
        StatSheets.forPlayer(player).ifPresent(sheet -> ClientPayloads.send(player, new StatSheetPayload(sheet)));
        return rejection;
    }

    public static void reset(MinecraftServer server, UUID player) {
        LevelStore.get(server).reset(player);
    }

    /** Expertise needs proficiency already, from the character or a command; an unknown skill has none. */
    private static Optional<ImprovementRejection> checkExpertise(ServerPlayer player, List<ResourceLocation> skills) {
        if (!enabled()) {
            return Optional.of(ImprovementRejection.LEVELLING_DISABLED);
        }
        int pending = LevelRules.pendingExpertise(
                LevelStore.get(player.server).level(player.getUUID()), classId(player), settings());
        return LevelRules.validateExpertise(
                skills,
                id -> SkillStore.find(id)
                        .map(skill -> ScoreService.skillProficiency(player, skill) == Proficiency.PROFICIENT)
                        .orElse(false),
                pending);
    }

    private static Optional<ImprovementRejection> checkImprovement(ServerPlayer player, List<Integer> increases) {
        if (!enabled()) {
            return Optional.of(ImprovementRejection.LEVELLING_DISABLED);
        }
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            scores.put(ability, ScoreService.abilityScore(player, ability));
        }
        int pending = LevelRules.pendingImprovements(
                LevelStore.get(player.server).level(player.getUUID()), classId(player), settings());
        return LevelRules.validateImprovement(
                increases, scores, pending, settings().improvements());
    }

    private static void change(ServerPlayer player, UnaryOperator<PlayerLevel> update) {
        LevelStore store = LevelStore.get(player.server);
        PlayerLevel before = store.level(player.getUUID());
        PlayerLevel after = update.apply(before);
        store.set(player.getUUID(), after);
        for (int level = before.level() + 1; level <= after.level(); level++) {
            LevelUpEvent event = new LevelUpEvent(player, level - 1, level);
            LISTENERS.forEach(listener -> listener.accept(event));
        }
        if (after.level() > before.level()) {
            announce(player, before, after);
        }
    }

    public static Optional<ResourceLocation> classId(ServerPlayer player) {
        return CharacterCreation.activePresets(player).flatMap(PresetChoices::classPreset);
    }

    public static Optional<ClassPreset> classPreset(ServerPlayer player) {
        return classId(player).flatMap(Presets.CLASSES::find);
    }

    private static void announce(ServerPlayer player, PlayerLevel before, PlayerLevel after) {
        player.server
                .getPlayerList()
                .broadcastSystemMessage(levelUpLine(player.getDisplayName(), after.level()), false);
        Optional<ResourceLocation> classId = classId(player);
        if (LevelRules.pendingImprovements(after, classId, settings())
                > LevelRules.pendingImprovements(before, classId, settings())) {
            player.sendSystemMessage(Component.translatable(
                    "checks.level.improvement_ready", Component.keybind("key.checks.stat_sheet")));
        }
        if (LevelRules.pendingExpertise(after, classId, settings())
                > LevelRules.pendingExpertise(before, classId, settings())) {
            player.sendSystemMessage(
                    Component.translatable("checks.level.expertise_ready", Component.keybind("key.checks.stat_sheet")));
        }
    }

    /** Broadcast to every player, so it carries its English fallback. */
    static Component levelUpLine(Component playerName, int level) {
        return FallbackText.of("chat.checks.level_up", playerName, level);
    }

    private static ScoresConfig.LevellingSettings settings() {
        return ScoresRuntime.config().levelling();
    }
}
