package studio.modroll.checks.sheet;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.CheckKind;
import studio.modroll.checks.api.ChecksApi;
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
import studio.modroll.checks.level.LevelRules;
import studio.modroll.checks.level.Levelling;
import studio.modroll.checks.level.PlayerLevel;
import studio.modroll.checks.preset.Presets;
import studio.modroll.checks.preset.Species;
import studio.modroll.checks.score.ScoresRuntime;
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
                                player.getMaxHealth(), health.conModifier(), health.conHealth(), health.levelHealth())),
                armor(player, settings.armorClass()),
                BodyValues.size(player),
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
        int base = RollService.effectiveEntity(player).armorClass();
        return BodyValues.armorClass(player)
                .map(armor -> new StatSheet.ArmorLine(
                        base + armor.bonus(),
                        base,
                        armor.dexModifier(),
                        armor.category(),
                        armor.bonus(),
                        settings.mediumMaxDex()));
    }

    private static Optional<StatSheet.LevelLine> level(ServerPlayer player) {
        ScoresConfig.LevellingSettings settings = ScoresRuntime.config().levelling();
        if (!settings.enabled()) {
            return Optional.empty();
        }
        PlayerLevel level = Levelling.activeLevel(player);
        return Optional.of(new StatSheet.LevelLine(
                level.level(),
                level.xp(),
                LevelRules.threshold(level.level(), settings),
                LevelRules.nextThreshold(level.level(), settings),
                new StatSheet.Improvements(
                        LevelRules.pendingImprovements(level, settings),
                        settings.improvements().options(),
                        settings.improvements().maxScore())));
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
