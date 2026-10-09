package studio.modroll.checks.bonus;

import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import studio.modroll.checks.api.CheckKind;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.api.Stat;
import studio.modroll.checks.check.RollModes;
import studio.modroll.checks.data.LenientJson;
import studio.modroll.checks.data.MatchEntry;
import studio.modroll.critfall.api.dice.RollMode;

/**
 * One {@code data/<ns>/checks/bonus/*.json} file: while its condition holds (an item in a slot, or a
 * mob effect), checks and saves it targets get a flat bonus and/or a roll mode.
 */
public record BonusSource(ResourceLocation id, Condition condition, BonusTargets targets, int bonus, RollMode mode) {

    public static final int FORMAT_VERSION = 1;

    /** What a condition can ask of an entity; a seam so conditions are testable without a world. */
    public interface Bearer {

        boolean wears(List<EquipmentSlot> slots, MatchEntry item);

        boolean hasEffect(ResourceLocation effect);
    }

    public sealed interface Condition {

        boolean isMet(Bearer bearer);
    }

    public record Equipped(SlotGroup slots, MatchEntry item) implements Condition {
        @Override
        public boolean isMet(Bearer bearer) {
            return bearer.wears(slots.slots(), item);
        }
    }

    public record Effect(ResourceLocation effect) implements Condition {
        @Override
        public boolean isMet(Bearer bearer) {
            return bearer.hasEffect(effect);
        }
    }

    public enum SlotGroup {
        MAINHAND(List.of(EquipmentSlot.MAINHAND)),
        OFFHAND(List.of(EquipmentSlot.OFFHAND)),
        ARMOR(List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET));

        private final List<EquipmentSlot> slots;

        SlotGroup(List<EquipmentSlot> slots) {
            this.slots = slots;
        }

        public List<EquipmentSlot> slots() {
            return slots;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Optional<SlotGroup> byId(String id) {
            return Arrays.stream(values())
                    .filter(group -> group.id().equals(id))
                    .findFirst();
        }
    }

    public boolean appliesTo(Bearer bearer, CheckKind kind, Stat stat) {
        return targets.appliesTo(kind, stat) && condition.isMet(bearer);
    }

    public BonusPart part() {
        return new BonusPart(id, bonus, mode);
    }

    /**
     * An unknown item, effect, slot or mode, a missing source, or a file that changes nothing rejects
     * the file; an unknown skill target is skipped with a warning.
     */
    public static BonusSource parse(
            ResourceLocation id,
            JsonObject json,
            Function<ResourceLocation, Optional<Skill>> skills,
            Predicate<ResourceLocation> itemExists,
            Predicate<ResourceLocation> effectExists,
            Consumer<String> warn) {
        LenientJson j = new LenientJson(json, "bonus " + id, warn);
        j.checkFormatVersion(FORMAT_VERSION);
        Condition condition = parseCondition(j.object("source"), itemExists, effectExists);
        BonusTargets targets = BonusTargets.parse(j.object("applies_to"), skills);
        int bonus = j.getInt("bonus", 0);
        RollMode mode = j.optionalString("mode")
                .map(text -> RollModes.byId(text)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "'mode' must be normal, advantage or disadvantage, was '" + text + "'")))
                .orElse(RollMode.NORMAL);
        if (bonus == 0 && mode == RollMode.NORMAL) {
            throw new IllegalArgumentException("a bonus source needs a non-zero 'bonus' or a 'mode'");
        }
        j.finish();
        return new BonusSource(id, condition, targets, bonus, mode);
    }

    private static Condition parseCondition(
            LenientJson json, Predicate<ResourceLocation> itemExists, Predicate<ResourceLocation> effectExists) {
        Optional<String> item = json.optionalString("item");
        Optional<String> effect = json.optionalString("effect");
        if (item.isPresent() == effect.isPresent()) {
            throw new IllegalArgumentException("'source' needs exactly one of 'item' or 'effect'");
        }
        if (effect.isPresent()) {
            return new Effect(requireKnown(effect.get(), effectExists, "effect"));
        }
        MatchEntry match = MatchEntry.parse(item.get());
        if (match instanceof MatchEntry.Exact exact) {
            requireKnown(exact.id().toString(), itemExists, "item");
        }
        String slot = json.optionalString("slot")
                .orElseThrow(() -> new IllegalArgumentException("an item source needs a 'slot'"));
        SlotGroup slots = SlotGroup.byId(slot)
                .orElseThrow(() ->
                        new IllegalArgumentException("'slot' must be mainhand, offhand or armor, was '" + slot + "'"));
        return new Equipped(slots, match);
    }

    private static ResourceLocation requireKnown(String text, Predicate<ResourceLocation> exists, String what) {
        ResourceLocation id = ResourceLocation.tryParse(text);
        if (id == null || !exists.test(id)) {
            throw new IllegalArgumentException("unknown " + what + " '" + text + "'");
        }
        return id;
    }
}
