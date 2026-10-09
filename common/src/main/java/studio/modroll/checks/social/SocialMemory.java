package studio.modroll.checks.social;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import studio.modroll.checks.Checks;
import studio.modroll.checks.PlayerSavedData;

/**
 * What social actions left behind, per player and target, each until a game time: action cooldowns,
 * price changes, trade refusals, refusals by witnesses, piglin disguises, and the piglin charm a
 * Performance gives. In its own world-level {@link SavedData} so a long cooldown survives restarts;
 * anything past its time is dropped on the next change.
 */
public final class SocialMemory extends SavedData {

    public static final String DATA_NAME = "checks_social";

    private static final String COOLDOWNS = "cooldowns";
    private static final String PRICES = "prices";
    private static final String REFUSALS = "refusals";
    private static final String WITNESS_REFUSALS = "witness_refusals";
    private static final String DISGUISES = "disguises";
    private static final String CHARMS = "charms";
    private static final String ACTION = "action";
    private static final String PLAYER = "player";
    private static final String TARGET = "target";
    private static final String UNTIL = "until";
    private static final String PERCENT = "percent";

    private record ActionKey(SocialAction action, UUID player, UUID target) {}

    private record PriceChange(double percent, long until) {}

    private final Map<ActionKey, Long> cooldowns = new HashMap<>();
    private final Map<ActionKey, PriceChange> prices = new HashMap<>();
    private final Map<ActionKey, Long> refusals = new HashMap<>();
    private final Map<ActionKey, Long> witnessRefusals = new HashMap<>();
    private final Map<UUID, Long> disguises = new HashMap<>();
    /** Kept apart from disguises so that switching Performance or Deceive off lifts only its own. */
    private final Map<UUID, Long> charms = new HashMap<>();

    public static SocialMemory get(MinecraftServer server) {
        return PlayerSavedData.get(server, DATA_NAME, SocialMemory::new, SocialMemory::load);
    }

    /** The ticks until {@code action} is ready again on this target; empty when it is ready now. */
    public OptionalLong cooldownLeft(SocialAction action, UUID player, UUID target, long now) {
        Long readyAt = cooldowns.get(new ActionKey(action, player, target));
        return readyAt == null || readyAt <= now ? OptionalLong.empty() : OptionalLong.of(readyAt - now);
    }

    public void startCooldown(SocialAction action, UUID player, UUID target, long now, int ticks) {
        prune(now);
        if (ticks > 0) {
            cooldowns.put(new ActionKey(action, player, target), now + ticks);
            setDirty();
        }
    }

    /** Replaces any earlier price change this action made for the player at this target. */
    public void changePrices(SocialAction action, UUID player, UUID target, double percent, long now, int ticks) {
        prune(now);
        if (percent != 0 && ticks > 0) {
            prices.put(new ActionKey(action, player, target), new PriceChange(percent, now + ticks));
            setDirty();
        }
    }

    /** The player's current price changes at this target, per action that made one. */
    public Map<SocialAction, Double> priceChanges(UUID player, UUID target, long now) {
        Map<SocialAction, Double> current = new EnumMap<>(SocialAction.class);
        prices.forEach((key, change) -> {
            if (key.player().equals(player) && key.target().equals(target) && now < change.until()) {
                current.put(key.action(), change.percent());
            }
        });
        return current;
    }

    public void refuse(SocialAction action, UUID player, UUID target, long now, int ticks) {
        refuse(refusals, action, player, target, now, ticks);
    }

    /** A refusal by a villager that saw the action, kept apart so that switching witnesses off lifts it. */
    public void refuseAsWitness(SocialAction action, UUID player, UUID witness, long now, int ticks) {
        refuse(witnessRefusals, action, player, witness, now, ticks);
    }

    /** Whether the target refuses to trade with the player over something one of {@code actions} did. */
    public boolean refuses(UUID player, UUID target, long now, Predicate<SocialAction> actions) {
        return refuses(refusals, player, target, now, actions);
    }

    /** Whether the target refuses the player over one of {@code actions} it saw them do to another villager. */
    public boolean refusesAsWitness(UUID player, UUID target, long now, Predicate<SocialAction> actions) {
        return refuses(witnessRefusals, player, target, now, actions);
    }

    /** Halves the time left on the player's refusals, the witnesses' too, over one of {@code actions}. */
    public void halveRefusals(UUID player, long now, Predicate<SocialAction> actions) {
        prune(now);
        for (Map<ActionKey, Long> map : List.of(refusals, witnessRefusals)) {
            map.replaceAll((key, until) ->
                    key.player().equals(player) && actions.test(key.action()) ? now + (until - now) / 2 : until);
        }
        setDirty();
    }

    private void refuse(Map<ActionKey, Long> into, SocialAction action, UUID player, UUID target, long now, int ticks) {
        prune(now);
        if (ticks > 0) {
            into.merge(new ActionKey(action, player, target), now + ticks, Math::max);
            setDirty();
        }
    }

    private static boolean refuses(
            Map<ActionKey, Long> from, UUID player, UUID target, long now, Predicate<SocialAction> actions) {
        return from.entrySet().stream()
                .anyMatch(refusal -> refusal.getKey().player().equals(player)
                        && refusal.getKey().target().equals(target)
                        && actions.test(refusal.getKey().action())
                        && now < refusal.getValue());
    }

    public void disguise(UUID player, long now, int ticks) {
        extend(disguises, player, now, ticks);
    }

    private void extend(Map<UUID, Long> into, UUID player, long now, int ticks) {
        prune(now);
        if (ticks > 0) {
            into.merge(player, now + ticks, Math::max);
            setDirty();
        }
    }

    public boolean disguised(UUID player, long now) {
        return now < disguises.getOrDefault(player, now);
    }

    /** Piglins take the player for a gold wearer after a Performance, as after a lie. */
    public void charm(UUID player, long now, int ticks) {
        extend(charms, player, now, ticks);
    }

    public boolean charmed(UUID player, long now) {
        return now < charms.getOrDefault(player, now);
    }

    private void prune(long now) {
        boolean pruned = cooldowns.values().removeIf(until -> until <= now)
                | prices.values().removeIf(change -> change.until() <= now)
                | refusals.values().removeIf(until -> until <= now)
                | witnessRefusals.values().removeIf(until -> until <= now)
                | disguises.values().removeIf(until -> until <= now)
                | charms.values().removeIf(until -> until <= now);
        if (pruned) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(COOLDOWNS, list(cooldowns, SocialMemory::until));
        tag.put(PRICES, list(prices, (key, change) -> {
            CompoundTag entry = until(key, change.until());
            entry.putDouble(PERCENT, change.percent());
            return entry;
        }));
        tag.put(REFUSALS, list(refusals, SocialMemory::until));
        tag.put(WITNESS_REFUSALS, list(witnessRefusals, SocialMemory::until));
        tag.put(DISGUISES, list(disguises, SocialMemory::playerUntil));
        tag.put(CHARMS, list(charms, SocialMemory::playerUntil));
        return tag;
    }

    public static SocialMemory load(CompoundTag tag, HolderLookup.Provider registries) {
        SocialMemory memory = new SocialMemory();
        read(tag, COOLDOWNS, entry -> actionKey(entry)
                .ifPresent(key -> memory.cooldowns.put(key, entry.getLong(UNTIL))));
        read(tag, PRICES, entry -> actionKey(entry)
                .ifPresent(key ->
                        memory.prices.put(key, new PriceChange(entry.getDouble(PERCENT), entry.getLong(UNTIL)))));
        read(tag, REFUSALS, entry -> actionKey(entry).ifPresent(key -> memory.refusals.put(key, entry.getLong(UNTIL))));
        read(tag, WITNESS_REFUSALS, entry -> actionKey(entry)
                .ifPresent(key -> memory.witnessRefusals.put(key, entry.getLong(UNTIL))));
        read(tag, DISGUISES, entry -> memory.disguises.put(entry.getUUID(PLAYER), entry.getLong(UNTIL)));
        read(tag, CHARMS, entry -> memory.charms.put(entry.getUUID(PLAYER), entry.getLong(UNTIL)));
        return memory;
    }

    private static <K, V> ListTag list(Map<K, V> entries, BiFunction<K, V, CompoundTag> writer) {
        ListTag list = new ListTag();
        entries.forEach((key, value) -> list.add(writer.apply(key, value)));
        return list;
    }

    private static CompoundTag playerUntil(UUID player, long until) {
        CompoundTag entry = new CompoundTag();
        entry.putUUID(PLAYER, player);
        entry.putLong(UNTIL, until);
        return entry;
    }

    private static CompoundTag until(ActionKey key, long until) {
        CompoundTag entry = actionKey(key);
        entry.putLong(UNTIL, until);
        return entry;
    }

    private static CompoundTag actionKey(ActionKey key) {
        CompoundTag entry = new CompoundTag();
        entry.putString(ACTION, key.action().id());
        entry.putUUID(PLAYER, key.player());
        entry.putUUID(TARGET, key.target());
        return entry;
    }

    /** Empty for an action this version does not know. */
    private static Optional<ActionKey> actionKey(CompoundTag entry) {
        return SocialAction.byId(entry.getString(ACTION))
                .map(action -> new ActionKey(action, entry.getUUID(PLAYER), entry.getUUID(TARGET)));
    }

    /** An entry missing a UUID is skipped with a warning. */
    private static void read(CompoundTag tag, String key, Consumer<CompoundTag> reader) {
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                reader.accept(list.getCompound(i));
            } catch (IllegalArgumentException | NullPointerException e) {
                Checks.LOG.warn("Skipping a social {} entry: {}", key, e.getMessage());
            }
        }
    }
}
