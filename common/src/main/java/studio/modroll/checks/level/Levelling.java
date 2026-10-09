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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import studio.modroll.checks.ClientPayloads;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.LevelUpEvent;
import studio.modroll.checks.creation.CharacterBuild;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.preset.ClassPreset;
import studio.modroll.checks.preset.Presets;
import studio.modroll.checks.score.ScoreService;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.sheet.StatSheetPayload;
import studio.modroll.checks.sheet.StatSheets;
import studio.modroll.checks.text.FallbackText;

/**
 * The server side of levelling: XP sources, level changes, level-up events and ability score
 * improvements. While levelling is disabled no XP is gained and no level changes; stored levels are kept.
 */
public final class Levelling {

    private static final List<Consumer<LevelUpEvent>> LISTENERS = new CopyOnWriteArrayList<>();

    private Levelling() {}

    public static void addListener(Consumer<LevelUpEvent> listener) {
        LISTENERS.add(listener);
    }

    public static boolean enabled() {
        return settings().enabled();
    }

    /** The player's stored level, or the starting level while levelling is disabled. */
    public static PlayerLevel activeLevel(ServerPlayer player) {
        return enabled() ? LevelStore.get(player.server).level(player.getUUID()) : PlayerLevel.START;
    }

    /**
     * Vanilla XP points as they are gained: those reaching the player's XP bar, plus what Mending spends
     * from a collected orb. Spending or losing them later never takes character XP.
     */
    public static void onVanillaXp(ServerPlayer player, int points) {
        ScoresConfig.XpSources sources = settings().xpSources();
        if (sources.vanillaXpEnabled() && points > 0) {
            gainXp(player, (int) Math.min(Integer.MAX_VALUE, (long) points * sources.xpPerPoint()));
        }
    }

    /**
     * Only advancements shown in the advancement screen count, so recipe unlocks give nothing. Dying gives
     * nothing either, though a first death to a mob earns the shown Adventure advancement.
     */
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

    /**
     * Applies one ability score improvement, {@code increases} in ability order, if the server accepts
     * it. Either way the player's stat sheet is sent again, so the screen shows the outcome.
     */
    public static Optional<ImprovementRejection> improve(ServerPlayer player, List<Integer> increases) {
        Optional<ImprovementRejection> rejection = checkImprovement(player, increases);
        rejection.ifPresentOrElse(
                reason -> player.sendSystemMessage(
                        reason.message(settings().improvements()).copy().withStyle(ChatFormatting.RED)),
                () -> {
                    LevelStore store = LevelStore.get(player.server);
                    UUID uuid = player.getUUID();
                    store.set(uuid, store.level(uuid).withImprovement(LevelRules.improvement(increases)));
                    player.sendSystemMessage(Component.translatable("checks.level.improved"));
                });
        StatSheets.forPlayer(player).ifPresent(sheet -> ClientPayloads.send(player, new StatSheetPayload(sheet)));
        return rejection;
    }

    public static void reset(MinecraftServer server, UUID player) {
        LevelStore.get(server).reset(player);
    }

    private static Optional<ImprovementRejection> checkImprovement(ServerPlayer player, List<Integer> increases) {
        if (!enabled()) {
            return Optional.of(ImprovementRejection.LEVELLING_DISABLED);
        }
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            scores.put(ability, ScoreService.abilityScore(player, ability));
        }
        int pending =
                LevelRules.pendingImprovements(LevelStore.get(player.server).level(player.getUUID()), settings());
        return LevelRules.validateImprovement(
                increases, scores, pending, settings().improvements());
    }

    /** Stores the change, then fires one event per level gained and announces the new level. */
    private static void change(ServerPlayer player, UnaryOperator<PlayerLevel> update) {
        LevelStore store = LevelStore.get(player.server);
        PlayerLevel before = store.level(player.getUUID());
        PlayerLevel after = update.apply(before);
        for (int level = before.level() + 1; level <= after.level(); level++) {
            after = withClassHitDie(player, after, level);
        }
        store.set(player.getUUID(), after);
        for (int level = before.level() + 1; level <= after.level(); level++) {
            LevelUpEvent event = new LevelUpEvent(player, level - 1, level);
            LISTENERS.forEach(listener -> listener.accept(event));
        }
        if (after.level() > before.level()) {
            announce(player, before, after);
        }
    }

    private static PlayerLevel withClassHitDie(ServerPlayer player, PlayerLevel level, int atLevel) {
        return classPreset(player)
                .map(preset -> level.withHitDie(atLevel, preset.hitDie()))
                .orElse(level);
    }

    private static Optional<ClassPreset> classPreset(ServerPlayer player) {
        if (!ScoresRuntime.config().creation().presets().enabled()) {
            return Optional.empty();
        }
        return CharacterCreation.activeBuild(player)
                .map(CharacterBuild::choices)
                .flatMap(PresetChoices::classPreset)
                .flatMap(Presets.CLASSES::find);
    }

    private static void announce(ServerPlayer player, PlayerLevel before, PlayerLevel after) {
        player.server
                .getPlayerList()
                .broadcastSystemMessage(levelUpLine(player.getDisplayName(), after.level()), false);
        if (LevelRules.pendingImprovements(after, settings()) > LevelRules.pendingImprovements(before, settings())) {
            player.sendSystemMessage(Component.translatable(
                    "checks.level.improvement_ready", Component.keybind("key.checks.stat_sheet")));
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
