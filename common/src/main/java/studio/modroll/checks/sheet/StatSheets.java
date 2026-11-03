package studio.modroll.checks.sheet;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.CheckKind;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.body.BodyValues;
import studio.modroll.checks.body.Extra;
import studio.modroll.checks.bonus.BonusSources;
import studio.modroll.checks.bonus.SituationalBonus;
import studio.modroll.checks.creation.CharacterBuild;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.critfall.ChecksModifierProvider;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.data.SkillStore;
import studio.modroll.checks.level.LevelGain;
import studio.modroll.checks.level.LevelRules;
import studio.modroll.checks.level.Levelling;
import studio.modroll.checks.level.Levels;
import studio.modroll.checks.level.PlayerLevel;
import studio.modroll.checks.preset.Presets;
import studio.modroll.checks.preset.Species;
import studio.modroll.checks.rest.Rests;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.critfall.api.ArmorClassBreakdown;
import studio.modroll.critfall.api.RollService;

/**
 * Builds a player's {@link StatSheet} on the server, every value read through {@link ChecksApi}. Skill and
 * save modifiers and passive scores include the player's current bonus sources, as {@code /checks get}
 * shows them.
 */
public final class StatSheets {

    private static final List<ResourceLocation> PASSIVE_SKILLS = Stream.of("perception", "investigation", "insight")
            .map(path -> Checks.id(path))
            .toList();

    private StatSheets() {}

    /** Empty while the stat screen is disabled, so the request goes unanswered and nothing opens. */
    public static Optional<StatSheet> forPlayer(ServerPlayer player) {
        if (!ScoresRuntime.config().statScreenEnabled()) {
            return Optional.empty();
        }
        return Optional.of(build(player));
    }

    private static StatSheet build(ServerPlayer player) {
        return new StatSheet(
                ChecksApi.proficiencyBonus(player),
                Arrays.stream(Ability.values())
                        .map(ability -> abilityLine(player, ability))
                        .toList(),
                SkillStore.skills().values().stream()
                        .map(skill -> skillLine(player, skill))
                        .toList(),
                PASSIVE_SKILLS.stream()
                        .flatMap(id -> ChecksApi.skill(id).stream())
                        .map(skill -> new StatSheet.PassiveLine(skill.id(), ChecksApi.passiveScore(player, skill)))
                        .toList(),
                CharacterCreation.available(player),
                identity(player),
                level(player),
                ScoresRuntime.config().extensions().sheetSections(),
                body(player));
    }

    private static StatSheet.Body body(ServerPlayer player) {
        ScoresConfig.BodySettings settings = ScoresRuntime.config().body();
        return new StatSheet.Body(
                BodyValues.health(player)
                        .map(health -> new StatSheet.HealthLine(
                                player.getMaxHealth(),
                                health.conModifier(),
                                health.conHealth(),
                                health.hitDie(),
                                health.levelHealth(),
                                BodyValues.maxHealthBonus(player) - health.total() - BodyValues.traitHealth(player))),
                armor(player, settings.armorClass()),
                BodyValues.size(player),
                hitDice(player),
                Arrays.stream(Extra.values())
                        .filter(extra -> settings.extra(extra).enabled())
                        .map(extra -> new StatSheet.ExtraLine(extra, BodyValues.extra(player, extra)))
                        .toList());
    }

    /** Only while Critfall asks Checks for it, so the AC shown is the one attacks roll against. */
    private static Optional<StatSheet.ArmorLine> armor(ServerPlayer player, ScoresConfig.ArmorClassSettings settings) {
        if (!ChecksModifierProvider.active()) {
            return Optional.empty();
        }
        ArmorClassBreakdown breakdown = RollService.armorClassBreakdown(player);
        Optional<StatSheet.ArmorPoints> worn = worn(player, breakdown);
        return BodyValues.armorClass(player)
                .map(armor -> new StatSheet.ArmorLine(
                        breakdown.total(),
                        worn,
                        armor.dexModifier(),
                        armor.category(),
                        armor.dexBonus(),
                        armor.floorBonus(),
                        settings.mediumMaxDex()));
    }

    /** Critfall's parts, while its AC is the one it derives from the armor and toughness. */
    private static Optional<StatSheet.ArmorPoints> worn(ServerPlayer player, ArmorClassBreakdown breakdown) {
        if (!breakdown.derived()) {
            return Optional.empty();
        }
        return Optional.of(new StatSheet.ArmorPoints(
                breakdown.base(),
                player.getAttributeValue(Attributes.ARMOR),
                breakdown.armorPart(),
                player.getAttributeValue(Attributes.ARMOR_TOUGHNESS),
                breakdown.toughnessPart()));
    }

    private static Optional<StatSheet.HitDiceLine> hitDice(ServerPlayer player) {
        ScoresConfig.RestSettings rest = ScoresRuntime.config().rest();
        if (!rest.hitDice().enabled()) {
            return Optional.empty();
        }
        boolean longRests = rest.longRest().enabled();
        return Optional.of(new StatSheet.HitDiceLine(
                BodyValues.hitDie(player),
                Rests.hitDiceLeft(player),
                Levelling.activeLevel(player).level(),
                longRests ? rest.longRest().regainFraction() : 0.0,
                longRests && rest.longRest().healToFull()));
    }

    private static Optional<StatSheet.LevelLine> level(ServerPlayer player) {
        ScoresConfig.LevellingSettings settings = ScoresRuntime.config().levelling();
        if (!settings.enabled()) {
            return Optional.empty();
        }
        PlayerLevel level = Levelling.activeLevel(player);
        Optional<ResourceLocation> classId = Levelling.classId(player);
        return Optional.of(new StatSheet.LevelLine(
                level.level(),
                level.xp(),
                LevelRules.threshold(level.level(), settings),
                LevelRules.nextThreshold(level.level(), settings),
                new StatSheet.Improvements(
                        LevelRules.pendingImprovements(level, classId, settings),
                        settings.improvements().options(),
                        settings.improvements().maxScore()),
                expertise(player, level, classId, settings),
                gains(player, level.level(), classId, settings)));
    }

    /** The skills the player is proficient in, without Expertise, while they have Expertise to choose. */
    private static StatSheet.ExpertiseChoice expertise(
            ServerPlayer player,
            PlayerLevel level,
            Optional<ResourceLocation> classId,
            ScoresConfig.LevellingSettings settings) {
        int pending = LevelRules.pendingExpertise(level, classId, settings);
        if (pending <= 0) {
            return StatSheet.ExpertiseChoice.NONE;
        }
        return new StatSheet.ExpertiseChoice(
                pending,
                SkillStore.skills().values().stream()
                        .filter(skill -> ChecksApi.skillProficiency(player, skill) == Proficiency.PROFICIENT)
                        .map(Skill::id)
                        .sorted(Comparator.comparing(ResourceLocation::toString))
                        .toList());
    }

    /** Level 1 is given the max health the later levels don't add, so the list adds up to the max health. */
    private static List<LevelGain> gains(
            ServerPlayer player,
            int level,
            Optional<ResourceLocation> classId,
            ScoresConfig.LevellingSettings settings) {
        double levelOne = player.getMaxHealth()
                - (BodyValues.maxHealthBonus(player, level) - BodyValues.maxHealthBonus(player, Levels.MIN_LEVEL));
        return LevelRules.gains(level, classId, settings, at -> BodyValues.maxHealthBonus(player, at), levelOne);
    }

    private static Optional<StatSheet.Identity> identity(ServerPlayer player) {
        if (!ScoresRuntime.config().creation().presets().enabled()) {
            return Optional.empty();
        }
        return CharacterCreation.activeBuild(player)
                .map(CharacterBuild::choices)
                .map(choices -> new StatSheet.Identity(
                        choices,
                        choices.species()
                                .flatMap(Presets.SPECIES::find)
                                .map(Species::traits)
                                .orElse(List.of())));
    }

    private static StatSheet.AbilityLine abilityLine(ServerPlayer player, Ability ability) {
        SituationalBonus checks = BonusSources.forCheck(player, CheckKind.CHECK, ability);
        SituationalBonus saves = BonusSources.forCheck(player, CheckKind.SAVE, ability);
        return new StatSheet.AbilityLine(
                ability,
                ChecksApi.abilityScore(player, ability),
                ChecksApi.abilityModifier(player, ability),
                BonusSources.saveModifier(player, ability),
                ChecksApi.saveProficiency(player, ability),
                new StatSheet.AbilityBonuses(checks.parts(), saves.parts()));
    }

    private static StatSheet.SkillLine skillLine(ServerPlayer player, Skill skill) {
        SituationalBonus situational = BonusSources.forCheck(player, CheckKind.CHECK, skill);
        return new StatSheet.SkillLine(
                skill.id(),
                skill.ability(),
                BonusSources.skillModifier(player, skill),
                ChecksApi.skillProficiency(player, skill),
                ChecksApi.skillBonus(player, skill),
                situational.parts());
    }
}
