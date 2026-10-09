package studio.modroll.checks.trigger;

import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.api.Stat;
import studio.modroll.checks.check.RollModes;
import studio.modroll.checks.check.StatIds;
import studio.modroll.checks.data.LenientJson;
import studio.modroll.checks.data.MatchEntry;
import studio.modroll.critfall.api.dice.RollMode;

/**
 * One {@code data/<ns>/checks/trigger/*.json} file: when a player uses a matching block, entity or item,
 * they roll {@code stat} against {@code dc} and the outcome's function and loot table run.
 */
public record CheckTrigger(
        ResourceLocation id,
        Interaction on,
        MatchEntry target,
        Stat stat,
        int dc,
        RollMode mode,
        Cooldown cooldown,
        Map<TriggerOutcome, Action> actions) {

    public static final int FORMAT_VERSION = 1;

    public CheckTrigger {
        actions = Map.copyOf(actions);
    }

    /** What the player does; {@code targetKey} is the JSON key naming what it is done to. */
    public enum Interaction {
        USE_BLOCK("block"),
        USE_ENTITY("entity"),
        USE_ITEM("item");

        private final String targetKey;

        Interaction(String targetKey) {
            this.targetKey = targetKey;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String targetKey() {
            return targetKey;
        }

        /** Item use has no target in the world, so a target cooldown cannot apply to it. */
        public boolean hasTarget() {
            return this != USE_ITEM;
        }

        static Optional<Interaction> byId(String id) {
            return Arrays.stream(values()).filter(on -> on.id().equals(id)).findFirst();
        }
    }

    /** Ticks before the same player, or anyone on the same target, can fire this trigger again; 0 for none. */
    public record Cooldown(int playerTicks, int targetTicks) {}

    public record Action(Optional<ResourceLocation> function, Optional<ResourceLocation> lootTable) {}

    public Optional<Action> action(TriggerOutcome outcome) {
        return Optional.ofNullable(actions.get(outcome));
    }

    public TriggerOutcome outcome(int natural, boolean metDc) {
        return TriggerOutcome.of(natural, metDc, actions.keySet());
    }

    /** {@code targetExists} tells whether an exact id names a block, entity type or item, per {@code on}. */
    public static CheckTrigger parse(
            ResourceLocation id,
            JsonObject json,
            Function<ResourceLocation, Optional<Skill>> skills,
            Function<Interaction, Predicate<ResourceLocation>> targetExists,
            Consumer<String> warn) {
        LenientJson j = new LenientJson(json, "trigger " + id, warn);
        j.checkFormatVersion(FORMAT_VERSION);
        String onText = requireString(j, "on");
        Interaction on = Interaction.byId(onText)
                .orElseThrow(() -> new IllegalArgumentException(
                        "'on' must be use_block, use_entity or use_item, was '" + onText + "'"));
        MatchEntry target = parseTarget(j, on, targetExists.apply(on));
        Stat stat = parseStat(j, skills);
        int dc = j.optionalInt("dc").orElseThrow(() -> new IllegalArgumentException("'dc' must be an integer"));
        RollMode mode = j.optionalString("mode")
                .map(text -> RollModes.byId(text)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "'mode' must be normal, advantage or disadvantage, was '" + text + "'")))
                .orElse(RollMode.NORMAL);
        Cooldown cooldown = parseCooldown(j.object("cooldown"), on);
        Map<TriggerOutcome, Action> actions = parseActions(j.object("outcomes"));
        j.finish();
        return new CheckTrigger(id, on, target, stat, dc, mode, cooldown, actions);
    }

    private static MatchEntry parseTarget(LenientJson json, Interaction on, Predicate<ResourceLocation> exists) {
        MatchEntry target = MatchEntry.parse(requireString(json, on.targetKey()));
        if (target instanceof MatchEntry.Exact exact && !exists.test(exact.id())) {
            throw new IllegalArgumentException("unknown " + on.targetKey() + " '" + exact.id() + "'");
        }
        return target;
    }

    private static Stat parseStat(LenientJson json, Function<ResourceLocation, Optional<Skill>> skills) {
        String text = requireString(json, "stat");
        return Optional.ofNullable(ResourceLocation.tryParse(text))
                .flatMap(statId -> StatIds.find(statId, skills))
                .orElseThrow(() -> new IllegalArgumentException("unknown ability or skill '" + text + "'"));
    }

    private static Cooldown parseCooldown(LenientJson json, Interaction on) {
        int playerTicks = nonNegative(json, "player");
        int targetTicks = nonNegative(json, "target");
        if (targetTicks > 0 && !on.hasTarget()) {
            json.warn("'target' has no effect on use_item, which has no target (ignored)");
            targetTicks = 0;
        }
        return new Cooldown(playerTicks, targetTicks);
    }

    private static int nonNegative(LenientJson json, String key) {
        int ticks = json.getInt(key, 0);
        if (ticks < 0) {
            throw new IllegalArgumentException("cooldown '" + key + "' must not be negative");
        }
        return ticks;
    }

    private static Map<TriggerOutcome, Action> parseActions(LenientJson json) {
        Map<TriggerOutcome, Action> actions = new EnumMap<>(TriggerOutcome.class);
        for (TriggerOutcome outcome : TriggerOutcome.values()) {
            if (json.has(outcome.id())) {
                actions.put(outcome, parseAction(json.object(outcome.id()), outcome));
            }
        }
        if (actions.isEmpty()) {
            throw new IllegalArgumentException(
                    "'outcomes' needs at least one of success, failure, natural_1, natural_20");
        }
        return actions;
    }

    private static Action parseAction(LenientJson json, TriggerOutcome outcome) {
        Action action = new Action(optionalId(json, "function"), optionalId(json, "loot_table"));
        if (action.function().isEmpty() && action.lootTable().isEmpty()) {
            throw new IllegalArgumentException("outcome '" + outcome.id() + "' needs a 'function' or a 'loot_table'");
        }
        return action;
    }

    private static Optional<ResourceLocation> optionalId(LenientJson json, String key) {
        return json.optionalString(key).map(text -> Optional.ofNullable(ResourceLocation.tryParse(text))
                .orElseThrow(() -> new IllegalArgumentException("'" + key + "' is not a valid id: '" + text + "'")));
    }

    private static String requireString(LenientJson json, String key) {
        return json.optionalString(key).orElseThrow(() -> new IllegalArgumentException("'" + key + "' is required"));
    }
}
