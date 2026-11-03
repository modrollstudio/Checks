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
import studio.modroll.checks.level.LevelGain;
import studio.modroll.checks.level.Levels;
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

    /**
     * Hit points, Hit Dice, armor class and size, each row explaining where its value comes from; empty with none
     * on.
     */
    public static Optional<SheetSection> body(StatSheet.Body body) {
        List<SheetRow> rows = new ArrayList<>();
        body.health()
                .ifPresent(health -> rows.add(new SheetRow(
                        Component.translatable(KEY + "health.label"),
                        Component.literal(SheetText.decimal(health.maxHealth())),
                        health(health))));
        body.hitDice()
                .ifPresent(dice -> rows.add(new SheetRow(
                        Component.translatable(KEY + "hit_dice.label"),
                        Component.translatable(KEY + "hit_dice.value", dice.left(), dice.total(), dice.sides()),
                        hitDice(dice))));
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
            lines.add(Component.translatable(
                    KEY + "health.levels", health.hitDie(), SheetText.signed(health.levelHealth())));
        }
        if (health.rounding() != 0) {
            lines.add(Component.translatable(KEY + "health.whole_hearts", SheetText.signed(health.rounding())));
        }
        return lines;
    }

    /** The pool, and in a line each how a short and a long rest use it. */
    static List<Component> hitDice(StatSheet.HitDiceLine dice) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(KEY + "hit_dice", dice.left(), dice.total(), dice.sides()));
        lines.add(Component.translatable(KEY + "hit_dice.short_rest"));
        longRest(dice).ifPresent(lines::add);
        return lines;
    }

    /**
     * What sleeping does, by the real settings: {@code Sleep to heal fully and get half your Hit Dice back.} at
     * 0.5; empty when it does nothing.
     */
    private static Optional<Component> longRest(StatSheet.HitDiceLine dice) {
        String heal = dice.healsFully() ? "heal_" : "";
        double fraction = dice.regainFraction();
        String key = KEY + "hit_dice.long_rest.";
        if (fraction >= 1.0) {
            return Optional.of(Component.translatable(key + heal + "all"));
        }
        if (fraction == 0.5) {
            return Optional.of(Component.translatable(key + heal + "half"));
        }
        if (fraction > 0.0) {
            return Optional.of(Component.translatable(key + heal + "part", Math.round(fraction * 100)));
        }
        return dice.healsFully() ? Optional.of(Component.translatable(key + "heal")) : Optional.empty();
    }

    /**
     * Critfall's base and what the armor and toughness points add, e.g. {@code Armor 13: +3}, as Critfall broke
     * them down, then the DEX part and the body armor rule that limits it, e.g. {@code DEX +2 (medium body armor,
     * max +2)}, and what keeps that cap from leaving AC below the AC without the body armor, e.g. {@code Never below
     * your AC without body armor: +2}. An AC Critfall did not derive from the armor, such as an entity profile's,
     * shows as one line.
     */
    static List<Component> armorClass(StatSheet.ArmorLine armor) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(KEY + "armor_class"));
        armor.worn()
                .ifPresentOrElse(
                        worn -> {
                            lines.add(Component.translatable(KEY + "armor_class.base", worn.base()));
                            if (worn.armor() != 0) {
                                lines.add(Component.translatable(
                                        KEY + "armor_class.armor",
                                        SheetText.decimal(worn.armor()),
                                        SheetText.signed(worn.armorPart())));
                            }
                            if (worn.toughness() != 0) {
                                lines.add(Component.translatable(
                                        KEY + "armor_class.toughness",
                                        SheetText.decimal(worn.toughness()),
                                        SheetText.signed(worn.toughnessPart())));
                            }
                        },
                        () -> lines.add(Component.translatable(KEY + "armor_class.set", armor.baseArmorClass())));
        lines.add(Component.translatable(
                KEY + "armor_class.dex." + armor.category().id(),
                SheetText.abilityAbbreviation(Ability.DEXTERITY),
                SheetText.signed(armor.dexBonus()),
                SheetText.signed(armor.mediumMaxDex())));
        if (armor.floorBonus() != 0) {
            lines.add(Component.translatable(KEY + "armor_class.floor", SheetText.signed(armor.floorBonus())));
        }
        return lines;
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
        if (line.expertise().pending() > 0) {
            lines.add(Component.translatable(KEY + "expertise", line.expertise().pending()));
        }
        return lines;
    }

    /**
     * What one level gave, e.g. {@code +2 max health}, {@code Hit Die (d10)}, {@code Proficiency bonus +3},
     * {@code Ability score improvement} and {@code Expertise in 2 skills}; {@code hitDie} is empty while Hit Dice are
     * off.
     */
    public static List<Component> levelGain(LevelGain gain, Optional<Integer> hitDie) {
        List<Component> parts = new ArrayList<>();
        if (gain.health() != 0) {
            String health = gain.level() == Levels.MIN_LEVEL
                    ? SheetText.decimal(gain.health())
                    : SheetText.signed(gain.health());
            parts.add(Component.translatable(KEY + "gain.health", health));
        }
        hitDie.ifPresent(sides -> parts.add(Component.translatable(KEY + "gain.hit_die", sides)));
        gain.proficiencyBonus()
                .ifPresent(bonus ->
                        parts.add(Component.translatable(KEY + "gain.proficiency_bonus", SheetText.signed(bonus))));
        if (gain.improvements() == 1) {
            parts.add(Component.translatable(KEY + "gain.improvement"));
        } else if (gain.improvements() > 1) {
            parts.add(Component.translatable(KEY + "gain.improvements", gain.improvements()));
        }
        if (gain.expertise() > 0) {
            parts.add(Component.translatable(KEY + "gain.expertise", gain.expertise()));
        }
        return parts;
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
