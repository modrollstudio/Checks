package studio.modroll.checks.sheet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.api.Stat;
import studio.modroll.checks.body.Size;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.checks.text.FallbackText;

/** Lang-file names for the stat and creation screens. */
public final class SheetText {

    private static final String SKILL = "skill";
    private static final String TRAIT = "trait";
    private static final String BONUS_SOURCE = "bonus";
    private static final String CUSTOM = "checks.preset.custom";
    private static final String BONUS = "checks.creation.bonus.";
    private static final String LIST_COMMA = "checks.list.comma";
    private static final String LIST_OR = "checks.list.or";
    private static final String LIST_AND = "checks.list.and";
    private static final int DECIMAL_PLACES = 2;

    private SheetText() {}

    public static Component abilityName(Ability ability) {
        return Component.translatable("checks.ability." + ability.id());
    }

    /** With its English fallback: hardcore creation rolls broadcast it to every player. */
    public static Component abilityAbbreviation(Ability ability) {
        return FallbackText.of("checks.ability." + ability.id() + ".short");
    }

    public static Component abilityDescription(Ability ability) {
        return Component.translatable("checks.ability." + ability.id() + ".description");
    }

    /** With an English fallback, for chat a client without Checks may see. */
    public static Component statName(Stat stat) {
        return switch (stat) {
            case Ability ability -> FallbackText.of("checks.ability." + ability.id());
            case Skill skill -> skillName(skill.id());
        };
    }

    /** The tooltip line naming a skill's governing ability. */
    public static Component skillUses(Ability ability) {
        return Component.translatable("screen.checks.tooltip.skill", abilityName(ability));
    }

    /** {@code checks.skill.<namespace>.<path>}, falling back to the id's last path segment in title case. */
    public static Component skillName(ResourceLocation id) {
        return name(SKILL, id);
    }

    /** The skill name key plus {@code .desc}; empty when no lang file provides one. */
    public static Optional<Component> skillDescription(ResourceLocation id) {
        return description(SKILL, id);
    }

    /** The skill name key plus {@code .use}: the "In this mod:" line; empty for a skill Checks has no use for. */
    public static Optional<Component> skillUse(ResourceLocation id) {
        return ifTranslated(key(SKILL, id) + ".use");
    }

    /** {@code checks.<kind>.<namespace>.<path>} like skills; an empty choice is Custom. */
    public static Component presetName(PresetKind kind, Optional<ResourceLocation> choice) {
        return choice.map(id -> name(kind.id(), id)).orElse(Component.translatable(CUSTOM));
    }

    /** The preset name key plus {@code .desc}, or what Custom means for this kind. */
    public static Optional<Component> presetDescription(PresetKind kind, Optional<ResourceLocation> choice) {
        return choice.map(id -> description(kind.id(), id))
                .orElse(Optional.of(Component.translatable(CUSTOM + "." + kind.id())));
    }

    /** {@code checks.bonus.<namespace>.<path>}, falling back to the source id's last path segment in title case. */
    public static Component bonusName(ResourceLocation id) {
        return name(BONUS_SOURCE, id);
    }

    public static Component traitName(ResourceLocation id) {
        return name(TRAIT, id);
    }

    public static Optional<Component> traitDescription(ResourceLocation id) {
        return description(TRAIT, id);
    }

    /**
     * The background bonus splits in plain words, e.g. {@code Raise one of these by 2 and another by 1,
     * or all three by 1.} An option that raises every one of {@code abilityCount} abilities says "all".
     */
    public static Component bonusSplits(List<List<Integer>> options, int abilityCount) {
        List<Component> splits =
                options.stream().map(option -> split(option, abilityCount)).toList();
        return Component.translatable(BONUS + "sentence", joined(splits, BONUS + "or", BONUS + "or"));
    }

    /** {@code Bonuses left: +1}, or {@code All bonuses used} once a remainder is empty. */
    public static Component bonusesLeft(List<List<Integer>> remainders) {
        if (remainders.contains(List.of())) {
            return Component.translatable(BONUS + "left.none");
        }
        List<Component> alternatives = remainders.stream()
                .map(remainder -> joined(
                        remainder.stream()
                                .<Component>map(bonus -> Component.literal(signed(bonus)))
                                .toList(),
                        LIST_COMMA,
                        BONUS + "and"))
                .toList();
        return Component.translatable(BONUS + "left", joined(alternatives, LIST_OR, LIST_OR));
    }

    /** {@code Highest score in CHA, then CON, DEX, WIS, INT, lowest in STR.} */
    public static Component suggestedOrder(List<Ability> order) {
        List<Component> middle = order.subList(1, order.size() - 1).stream()
                .map(SheetText::abilityAbbreviation)
                .toList();
        return Component.translatable(
                "checks.creation.suggested_order",
                abilityAbbreviation(order.getFirst()),
                joined(middle, LIST_COMMA, LIST_COMMA),
                abilityAbbreviation(order.getLast()));
    }

    /** Equal bonuses are grouped, largest first: "one of these by 2", "another by 1", "two others by 1 each". */
    private static Component split(List<Integer> option, int abilityCount) {
        Map<Integer, Long> counts = option.stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.groupingBy(bonus -> bonus, LinkedHashMap::new, Collectors.counting()));
        if (counts.size() == 1 && option.size() == abilityCount && abilityCount > 1) {
            return Component.translatable(BONUS + "all", number(abilityCount), option.getFirst());
        }
        List<Component> parts = new ArrayList<>();
        counts.forEach((bonus, count) -> parts.add(part(parts.isEmpty(), count.intValue(), bonus)));
        return joined(parts, BONUS + "and", BONUS + "and");
    }

    private static Component part(boolean first, int count, int bonus) {
        if (count == 1) {
            return Component.translatable(BONUS + (first ? "one" : "another"), bonus);
        }
        return Component.translatable(BONUS + (first ? "some" : "others"), number(count), bonus);
    }

    /** Small counts as words ("three"), from the lang file; larger ones as digits. */
    private static Component number(int count) {
        return Component.translatableWithFallback("checks.number." + count, Integer.toString(count));
    }

    /** {@code a, b and c}. */
    public static Component andList(List<Component> items) {
        return joined(items, LIST_COMMA, LIST_AND);
    }

    /** {@code a, b and c}: {@code separatorKey} between items, {@code lastKey} before the last one. */
    private static Component joined(List<Component> items, String separatorKey, String lastKey) {
        MutableComponent text = Component.empty();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                text.append(Component.translatable(i == items.size() - 1 ? lastKey : separatorKey));
            }
            text.append(items.get(i));
        }
        return text;
    }

    private static Component name(String kind, ResourceLocation id) {
        return Component.translatableWithFallback(key(kind, id), readableName(id));
    }

    private static Optional<Component> description(String kind, ResourceLocation id) {
        return ifTranslated(key(kind, id) + ".desc");
    }

    private static Optional<Component> ifTranslated(String key) {
        return Language.getInstance().has(key) ? Optional.of(Component.translatable(key)) : Optional.empty();
    }

    private static String key(String kind, ResourceLocation id) {
        return "checks." + kind + "." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    static String readableName(ResourceLocation id) {
        String path = id.getPath();
        return Arrays.stream(path.substring(path.lastIndexOf('/') + 1).split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    public static Component legend(Proficiency proficiency) {
        return Component.translatable("screen.checks.legend." + proficiency.id());
    }

    public static String signed(int value) {
        return value < 0 ? Integer.toString(value) : "+" + value;
    }

    /** Rounded to two places without trailing zeros: {@code 26}, {@code 0.6}, {@code 0.55}. */
    public static String decimal(double value) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(DECIMAL_PLACES, RoundingMode.HALF_UP);
        return rounded.signum() == 0 ? "0" : rounded.stripTrailingZeros().toPlainString();
    }

    public static String signed(double value) {
        String text = decimal(value);
        return text.startsWith("-") ? text : "+" + text;
    }

    public static Component sizeName(Size size) {
        return Component.translatable("checks.size." + size.id());
    }

    /** A size's scale as a multiple of normal player size, e.g. {@code ×0.6}. */
    public static Component scale(double scale) {
        return Component.translatable("checks.size.scale", decimal(scale));
    }
}
