package studio.modroll.checks.sheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.api.SheetRow;
import studio.modroll.checks.api.SheetSection;
import studio.modroll.checks.body.Extra;
import studio.modroll.checks.body.SizeOption;
import studio.modroll.checks.bonus.BonusPart;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.critfall.api.dice.RollMode;

/**
 * Hover text for the stat screen. Breakdowns only format the snapshot's values; zero parts are left out.
 * Bonus sources add their flat bonus to the breakdown and a line per advantage or disadvantage.
 */
public final class SheetTooltips {

    private static final String KEY = "screen.checks.tooltip.";
    private static final int PERCENT = 100;

    private SheetTooltips() {}

    /** Then each attribute extra the ability's modifier changes, e.g. {@code Mining speed +10%}. */
    public static List<Component> ability(StatSheet.AbilityLine line, List<StatSheet.ExtraLine> extras) {
        List<Component> lines = new ArrayList<>();
        lines.add(SheetText.abilityDescription(line.ability()));
        lines.add(Component.translatable(KEY + "ability"));
        List<BonusPart> checks = line.bonuses().checks();
        List<Component> flat = sourceParts(checks);
        if (!flat.isEmpty()) {
            lines.add(Component.translatable(KEY + "ability_checks", joined(flat)));
        }
        lines.addAll(modeLines(checks));
        extras.stream()
                .filter(extra -> extra.extra().ability() == line.ability() && extra.amount() != 0)
                .map(SheetTooltips::extra)
                .forEach(lines::add);
        return lines;
    }

    /** Hit points, armor class and size, each row explaining where its value comes from; empty with none on. */
    public static Optional<SheetSection> body(StatSheet.Body body) {
        List<SheetRow> rows = new ArrayList<>();
        body.health()
                .ifPresent(health -> rows.add(new SheetRow(
                        Component.translatable(KEY + "health.label"),
                        Component.literal(SheetText.decimal(health.maxHealth())),
                        health(health))));
        body.armor()
                .ifPresent(armor -> rows.add(new SheetRow(
                        Component.translatable(KEY + "armor_class.label"),
                        Component.literal(Integer.toString(armor.armorClass())),
                        armorClass(armor))));
        body.size()
                .ifPresent(size -> rows.add(new SheetRow(
                        Component.translatable(KEY + "size.label"), SheetText.sizeName(size.size()), size(size))));
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SheetSection(Component.translatable("screen.checks.body"), rows));
    }

    static List<Component> health(StatSheet.HealthLine health) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(KEY + "health"));
        lines.add(Component.translatable(
                KEY + "health.con",
                SheetText.abilityAbbreviation(Ability.CONSTITUTION),
                SheetText.signed(health.conModifier()),
                SheetText.signed(health.conHealth())));
        if (health.levelHealth() != 0) {
            lines.add(Component.translatable(KEY + "health.levels", SheetText.signed(health.levelHealth())));
        }
        return lines;
    }

    /** E.g. {@code DEX +2 (medium armor, max +2)}. */
    static List<Component> armorClass(StatSheet.ArmorLine armor) {
        return List.of(
                Component.translatable(KEY + "armor_class"),
                Component.translatable(KEY + "armor_class.armor", armor.baseArmorClass()),
                Component.translatable(
                        KEY + "armor_class.dex." + armor.category().id(),
                        SheetText.abilityAbbreviation(Ability.DEXTERITY),
                        SheetText.signed(armor.dexBonus()),
                        SheetText.signed(armor.mediumMaxDex())));
    }

    static List<Component> size(SizeOption size) {
        return List.of(Component.translatable(KEY + "size"), SheetText.scale(size.scale()));
    }

    /** A percentage for fractions, and a minus for an amount that makes something smaller. */
    static Component extra(StatSheet.ExtraLine line) {
        Extra extra = line.extra();
        double shown = extra.reduces() ? -line.amount() : line.amount();
        String value = extra.percent() ? SheetText.signed(shown * PERCENT) + "%" : SheetText.signed(shown);
        return Component.translatable(KEY + "extra." + extra.id(), value);
    }

    public static List<Component> proficiencyBonus() {
        return List.of(Component.translatable(KEY + "proficiency_bonus"));
    }

    public static List<Component> save(StatSheet.AbilityLine line, int proficiencyBonus) {
        List<Component> parts = new ArrayList<>();
        parts.add(abilityPart(line));
        addPart(parts, line.saveProficiency().id(), line.saveProficiency().bonus(proficiencyBonus));
        List<BonusPart> saves = line.bonuses().saves();
        parts.addAll(sourceParts(saves));
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(KEY + "save"));
        lines.add(breakdown(parts, line.saveModifier()));
        lines.addAll(modeLines(saves));
        return lines;
    }

    public static List<Component> skill(StatSheet.SkillLine line, StatSheet sheet) {
        List<Component> parts = new ArrayList<>();
        parts.add(abilityPart(sheet.ability(line.ability())));
        addPart(parts, line.proficiency().id(), line.proficiency().bonus(sheet.proficiencyBonus()));
        addPart(parts, "bonus", line.bonus());
        parts.addAll(sourceParts(line.situational()));
        List<Component> lines = new ArrayList<>(about(line.id(), line.ability()));
        lines.add(breakdown(parts, line.modifier()));
        lines.addAll(modeLines(line.situational()));
        return lines;
    }

    /** The governing ability, then the skill's description and what Checks uses it for, when it has them. */
    public static List<Component> about(ResourceLocation skill, Ability ability) {
        List<Component> lines = new ArrayList<>();
        lines.add(SheetText.skillUses(ability));
        SheetText.skillDescription(skill).ifPresent(lines::add);
        SheetText.skillUse(skill).ifPresent(lines::add);
        return lines;
    }

    /** A preset's name and description, then its traits each with their description. */
    public static List<Component> preset(
            PresetKind kind, Optional<ResourceLocation> choice, List<ResourceLocation> traits) {
        List<Component> lines = new ArrayList<>();
        lines.add(SheetText.presetName(kind, choice));
        SheetText.presetDescription(kind, choice).ifPresent(lines::add);
        if (!traits.isEmpty()) {
            lines.add(Component.translatable(KEY + "traits"));
        }
        for (ResourceLocation trait : traits) {
            Component name = SheetText.traitName(trait);
            lines.add(SheetText.traitDescription(trait)
                    .<Component>map(description -> Component.translatable(KEY + "trait", name, description))
                    .orElse(name));
        }
        return lines;
    }

    /** The XP towards the next level, and any ability score improvements waiting to be chosen. */
    public static List<Component> level(StatSheet.LevelLine line) {
        List<Component> lines = new ArrayList<>();
        lines.add(line.nextLevelXp()
                .map(next -> Component.translatable(KEY + "xp", line.xp(), next))
                .orElseGet(() -> Component.translatable(KEY + "xp.max", line.xp())));
        int pending = line.improvements().pending();
        if (pending > 0) {
            lines.add(Component.translatable(KEY + "improvements", pending));
        }
        return lines;
    }

    /** What the dot before a save or skill means. */
    public static List<Component> legend() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(KEY + "legend"));
        for (Proficiency level : Proficiency.values()) {
            lines.add(SheetText.legend(level));
        }
        return lines;
    }

    public static List<Component> passive() {
        return List.of(Component.translatable(KEY + "passive"), Component.translatable(KEY + "passive.use"));
    }

    private static Component abilityPart(StatSheet.AbilityLine line) {
        return Component.translatable(
                KEY + "part.ability", SheetText.abilityAbbreviation(line.ability()), SheetText.signed(line.modifier()));
    }

    private static void addPart(List<Component> parts, String kind, int value) {
        if (value != 0) {
            parts.add(Component.translatable(KEY + "part." + kind, SheetText.signed(value)));
        }
    }

    private static List<Component> sourceParts(List<BonusPart> sources) {
        return sources.stream()
                .filter(source -> source.bonus() != 0)
                .<Component>map(source -> Component.translatable(
                        KEY + "part.source", SheetText.bonusName(source.source()), SheetText.signed(source.bonus())))
                .toList();
    }

    /** Which sources grant advantage or impose disadvantage, and that having both rolls normally. */
    private static List<Component> modeLines(List<BonusPart> sources) {
        List<Component> lines = new ArrayList<>();
        for (BonusPart source : sources) {
            if (source.mode() != RollMode.NORMAL) {
                String key = source.mode() == RollMode.ADVANTAGE ? "advantage" : "disadvantage";
                lines.add(Component.translatable(KEY + key, SheetText.bonusName(source.source())));
            }
        }
        boolean advantage = sources.stream().anyMatch(source -> source.mode() == RollMode.ADVANTAGE);
        boolean disadvantage = sources.stream().anyMatch(source -> source.mode() == RollMode.DISADVANTAGE);
        if (advantage && disadvantage) {
            lines.add(Component.translatable(KEY + "cancel_out"));
        }
        return lines;
    }

    private static Component breakdown(List<Component> parts, int total) {
        return Component.translatable(KEY + "breakdown", joined(parts), SheetText.signed(total));
    }

    private static Component joined(List<Component> parts) {
        return ComponentUtils.formatList(parts, Component.translatable(KEY + "separator"));
    }
}
