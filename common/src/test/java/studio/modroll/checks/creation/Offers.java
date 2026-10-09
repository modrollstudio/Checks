package studio.modroll.checks.creation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.body.Size;
import studio.modroll.checks.body.SizeOption;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.preset.Background;
import studio.modroll.checks.preset.ClassPreset;
import studio.modroll.checks.preset.KitItem;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.checks.preset.Species;
import studio.modroll.critfall.api.dice.DieRoll;

/**
 * Offers built from the default creation settings: three skills with presets off, or six skills with
 * a small set of test presets.
 */
final class Offers {

    static final ResourceLocation ATHLETICS = ResourceLocation.parse("checks:athletics");
    static final ResourceLocation STEALTH = ResourceLocation.parse("checks:stealth");
    static final ResourceLocation PERCEPTION = ResourceLocation.parse("checks:perception");
    static final ResourceLocation INTIMIDATION = ResourceLocation.parse("checks:intimidation");
    static final ResourceLocation ARCANA = ResourceLocation.parse("checks:arcana");
    static final ResourceLocation HISTORY = ResourceLocation.parse("checks:history");
    static final List<ResourceLocation> TWO_SKILLS = List.of(ATHLETICS, STEALTH);
    static final List<Integer> NO_BONUSES = List.of(0, 0, 0, 0, 0, 0);

    static final ResourceLocation ELF = ResourceLocation.parse("test:elf");
    static final ResourceLocation HUMAN = ResourceLocation.parse("test:human");
    static final ResourceLocation HOMUNCULUS = ResourceLocation.parse("test:homunculus");
    static final ResourceLocation SOLDIER = ResourceLocation.parse("test:soldier");
    static final ResourceLocation SAGE = ResourceLocation.parse("test:sage");
    static final ResourceLocation FIGHTER = ResourceLocation.parse("test:fighter");
    static final ResourceLocation WIZARD = ResourceLocation.parse("test:wizard");

    private static final ScoresConfig.PresetSettings PRESET_DEFAULTS =
            ScoresConfig.DEFAULTS.creation().presets();
    static final PresetOffer NO_PRESETS = new PresetOffer(
            false, List.of(), List.of(), List.of(), PRESET_DEFAULTS.bonusOptions(), PRESET_DEFAULTS.bonusMaxScore());

    /** Soldier and Fighter overlap on Athletics and Intimidation; Sage and Fighter on History. */
    static final PresetOffer PRESETS = new PresetOffer(
            true,
            List.of(
                    new Species(
                            ELF,
                            List.of(ResourceLocation.parse("test:darkvision")),
                            List.of(new SizeOption(Size.MEDIUM, 0.95)),
                            PresetKind.SPECIES.defaultIcon()),
                    new Species(
                            HUMAN,
                            List.of(),
                            List.of(new SizeOption(Size.MEDIUM, 1.0), new SizeOption(Size.SMALL, 0.6)),
                            PresetKind.SPECIES.defaultIcon()),
                    new Species(HOMUNCULUS, List.of(), List.of(), PresetKind.SPECIES.defaultIcon())),
            List.of(
                    new Background(
                            SOLDIER,
                            List.of(Ability.STRENGTH, Ability.DEXTERITY, Ability.CONSTITUTION),
                            List.of(ATHLETICS, INTIMIDATION),
                            List.of(new KitItem(ResourceLocation.parse("minecraft:bread"), 3)),
                            PresetKind.BACKGROUND.defaultIcon()),
                    new Background(
                            SAGE,
                            List.of(Ability.CONSTITUTION, Ability.INTELLIGENCE, Ability.WISDOM),
                            List.of(ARCANA, HISTORY),
                            List.of(),
                            PresetKind.BACKGROUND.defaultIcon())),
            List.of(
                    new ClassPreset(
                            FIGHTER,
                            List.of(Ability.STRENGTH, Ability.CONSTITUTION),
                            List.of(ATHLETICS, INTIMIDATION, PERCEPTION, HISTORY),
                            2,
                            List.of(
                                    Ability.STRENGTH,
                                    Ability.CONSTITUTION,
                                    Ability.DEXTERITY,
                                    Ability.WISDOM,
                                    Ability.INTELLIGENCE,
                                    Ability.CHARISMA),
                            10,
                            PresetKind.CLASS.defaultIcon()),
                    new ClassPreset(
                            WIZARD,
                            List.of(Ability.INTELLIGENCE, Ability.WISDOM),
                            List.of(ARCANA, HISTORY, PERCEPTION),
                            2,
                            List.of(
                                    Ability.INTELLIGENCE,
                                    Ability.CONSTITUTION,
                                    Ability.DEXTERITY,
                                    Ability.WISDOM,
                                    Ability.CHARISMA,
                                    Ability.STRENGTH),
                            6,
                            PresetKind.CLASS.defaultIcon())),
            PRESET_DEFAULTS.bonusOptions(),
            PRESET_DEFAULTS.bonusMaxScore());

    private Offers() {}

    static CreationOffer unrolled() {
        return offer(Optional.empty());
    }

    /** Each total is backed by a single die showing it, which is all the rules look at. */
    static CreationOffer rolled(CreationMethod method, List<Integer> totals) {
        List<RolledScore> scores = totals.stream()
                .map(total -> new RolledScore(total, List.of(new DieRoll(20, total, true))))
                .toList();
        return offer(Optional.of(new CharacterRolls(method, scores)));
    }

    static CreationOffer withPresets() {
        ScoresConfig.CreationSettings defaults = ScoresConfig.DEFAULTS.creation();
        return new CreationOffer(
                defaults.methods(),
                defaults.standardArray(),
                defaults.pointBuy(),
                defaults.skillChoices(),
                List.of(
                        new Skill(ATHLETICS, Ability.STRENGTH),
                        new Skill(STEALTH, Ability.DEXTERITY),
                        new Skill(PERCEPTION, Ability.WISDOM),
                        new Skill(INTIMIDATION, Ability.CHARISMA),
                        new Skill(ARCANA, Ability.INTELLIGENCE),
                        new Skill(HISTORY, Ability.INTELLIGENCE)),
                Optional.empty(),
                PRESETS,
                Map.of());
    }

    /** The presets offer, with Elf choosing one of Perception and Stealth and Human two of any skill. */
    static CreationOffer withSpeciesSkillChoices() {
        CreationOffer presets = withPresets();
        return new CreationOffer(
                presets.methods(),
                presets.standardArray(),
                presets.pointBuy(),
                presets.skillChoices(),
                presets.skills(),
                Optional.empty(),
                PRESETS,
                Map.of(ELF, new SkillChoice(1, Set.of(PERCEPTION, STEALTH)), HUMAN, new SkillChoice(2, Set.of())));
    }

    /** A submission with every preset Custom and no bonuses. */
    static CreationSubmission custom(CreationMethod method, List<Integer> scores, List<ResourceLocation> skills) {
        return new CreationSubmission(method, scores, NO_BONUSES, skills, PresetChoices.CUSTOM, List.of());
    }

    static CreationOffer withMethodsAndChoices(List<CreationMethod> methods, int skillChoices) {
        CreationOffer base = unrolled();
        return new CreationOffer(
                methods,
                base.standardArray(),
                base.pointBuy(),
                skillChoices,
                base.skills(),
                Optional.empty(),
                NO_PRESETS,
                Map.of());
    }

    private static CreationOffer offer(Optional<CharacterRolls> rolls) {
        ScoresConfig.CreationSettings defaults = ScoresConfig.DEFAULTS.creation();
        return new CreationOffer(
                defaults.methods(),
                defaults.standardArray(),
                defaults.pointBuy(),
                defaults.skillChoices(),
                List.of(
                        new Skill(ATHLETICS, Ability.STRENGTH),
                        new Skill(STEALTH, Ability.DEXTERITY),
                        new Skill(PERCEPTION, Ability.WISDOM)),
                rolls,
                NO_PRESETS,
                Map.of());
    }
}
