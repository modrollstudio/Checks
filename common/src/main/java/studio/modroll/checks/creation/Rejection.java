package studio.modroll.checks.creation;

import net.minecraft.network.chat.Component;
import studio.modroll.checks.sheet.SheetText;

/** Why the server refused a character submission; each has a lang line under {@code checks.creation.rejected}. */
public enum Rejection {
    CREATION_DISABLED("creation_disabled"),
    ALREADY_CREATED("already_created"),
    UNKNOWN_SPECIES("unknown_species"),
    SIZE_NOT_OFFERED("size_not_offered"),
    UNKNOWN_BACKGROUND("unknown_background"),
    UNKNOWN_CLASS("unknown_class"),
    METHOD_NOT_ALLOWED("method_not_allowed"),
    METHOD_LOCKED("method_locked"),
    SCORE_COUNT("score_count"),
    WRONG_ARRAY("wrong_array"),
    POINT_BUY_RANGE("point_buy_range"),
    POINTS_OVERSPENT("points_overspent"),
    NOT_ROLLED("not_rolled"),
    ROLLS_CHANGED("rolls_changed"),
    HARDCORE_ORDER("hardcore_order"),
    BONUS_NOT_LISTED("bonus_not_listed"),
    BONUS_PATTERN("bonus_pattern"),
    BONUS_OVER_MAX("bonus_over_max"),
    SKILL_COUNT("skill_count"),
    UNKNOWN_SKILL("unknown_skill"),
    DUPLICATE_SKILL("duplicate_skill"),
    SKILL_ALREADY_PROFICIENT("skill_already_proficient"),
    SKILL_NOT_ON_CLASS_LIST("skill_not_on_class_list"),
    SPECIES_SKILL_COUNT("species_skill_count"),
    SKILL_NOT_FROM_SPECIES("skill_not_from_species");

    private final String id;

    Rejection(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Some messages name a number or the bonus splits, which depend on the chosen presets. */
    public Component message(CreationOffer offer, PresetChoices choices) {
        String key = "checks.creation.rejected." + id;
        return switch (this) {
            case SKILL_COUNT ->
                Component.translatable(key, SkillPlan.of(offer, choices).required());
            case SPECIES_SKILL_COUNT ->
                Component.translatable(key, SkillPlan.of(offer, choices).speciesRequired());
            case BONUS_OVER_MAX -> Component.translatable(key, offer.presets().bonusMaxScore());
            case BONUS_PATTERN -> Component.translatable(key, bonusSplits(offer.presets(), choices));
            default -> Component.translatable(key);
        };
    }

    private static Component bonusSplits(PresetOffer presets, PresetChoices choices) {
        return presets.background(choices.background())
                .map(background -> SheetText.bonusSplits(
                        presets.bonusOptionsFor(background),
                        background.abilities().size()))
                .orElse(Component.empty());
    }
}
