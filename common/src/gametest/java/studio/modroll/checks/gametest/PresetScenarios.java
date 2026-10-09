package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.saveWorldData;
import static studio.modroll.checks.gametest.ScenarioSupport.savedCharacterStore;
import static studio.modroll.checks.gametest.ScenarioSupport.speciesPicks;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withBackgrounds;
import static studio.modroll.checks.gametest.ScenarioSupport.withPresetSettings;
import static studio.modroll.checks.gametest.ScenarioSupport.withSender;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.creation.CharacterBuild;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.creation.CharacterStore;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.CreationOffer;
import studio.modroll.checks.creation.CreationSubmission;
import studio.modroll.checks.creation.PlayerCharacter;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.creation.Rejection;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.data.SkillStore;
import studio.modroll.checks.preset.Background;
import studio.modroll.checks.preset.ClassPreset;
import studio.modroll.checks.preset.Preset;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.checks.preset.Presets;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.sheet.StatSheet;
import studio.modroll.checks.sheet.StatSheets;

/** GameTest bodies for species, backgrounds and class presets, against the presets Checks ships. */
public final class PresetScenarios {

    private static final ResourceLocation ELF = Checks.id("elf");
    private static final ResourceLocation SOLDIER = Checks.id("soldier");
    private static final ResourceLocation CRIMINAL = Checks.id("criminal");
    private static final ResourceLocation FIGHTER = Checks.id("fighter");
    private static final ResourceLocation WIZARD = Checks.id("wizard");
    private static final ResourceLocation KNIGHT = ResourceLocation.parse("gametest:knight");
    private static final ResourceLocation ATHLETICS = Checks.id("athletics");
    private static final ResourceLocation INTIMIDATION = Checks.id("intimidation");
    private static final ResourceLocation STEALTH = Checks.id("stealth");
    private static final ResourceLocation PERCEPTION = Checks.id("perception");
    private static final ResourceLocation ARCANA = Checks.id("arcana");
    private static final ResourceLocation HISTORY = Checks.id("history");
    /** The standard array in the Fighter's suggested order: STR 15, DEX 13, CON 14, INT 10, WIS 12, CHA 8. */
    private static final List<Integer> FIGHTER_ARRAY = List.of(15, 13, 14, 10, 12, 8);

    private static final List<Integer> STR_TWO_CON_ONE = List.of(2, 0, 1, 0, 0, 0);
    private static final List<Integer> NO_BONUSES = List.of(0, 0, 0, 0, 0, 0);
    private static final PresetChoices ELF_SOLDIER_FIGHTER =
            new PresetChoices(Optional.of(ELF), Optional.of(SOLDIER), Optional.of(FIGHTER), Optional.empty());

    private PresetScenarios() {}

    /**
     * Both loaders load every shipped species, background and class, each with a registered icon of its
     * own, and offer them at creation.
     */
    public static void shippedPresetsLoad(GameTestHelper helper) {
        expectEquals(helper, 9, Presets.SPECIES.all().size(), "shipped species");
        expectEquals(helper, 8, Presets.BACKGROUNDS.all().size(), "shipped backgrounds");
        expectEquals(helper, 12, Presets.CLASSES.all().size(), "shipped classes");
        expectOwnIcons(helper, PresetKind.SPECIES, Presets.SPECIES.all().values());
        expectOwnIcons(helper, PresetKind.BACKGROUND, Presets.BACKGROUNDS.all().values());
        expectOwnIcons(helper, PresetKind.CLASS, Presets.CLASSES.all().values());
        ClassPreset bard = Presets.CLASSES.find(Checks.id("bard")).orElseThrow();
        expectEquals(helper, SkillStore.skills().size(), bard.skillOptions().size(), "bard picks from every skill");
        ServerPlayer player = login(helper, newProfile("checks-pr-load"));
        try {
            CreationOffer offer = CharacterCreation.offerFor(player);
            expect(helper, offer.presets().enabled(), "presets must be offered");
            expectEquals(helper, 8, offer.presets().backgrounds().size(), "offered backgrounds");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    private static void expectOwnIcons(GameTestHelper helper, PresetKind kind, Collection<? extends Preset> presets) {
        for (Preset preset : presets) {
            expect(helper, BuiltInRegistries.ITEM.containsKey(preset.icon()), preset.id() + " icon is registered");
            expect(helper, !preset.icon().equals(kind.defaultIcon()), preset.id() + " has an icon of its own");
        }
    }

    /** A pack file adds a background and replaces a shipped one; a bad file is refused; hiding drops the shipped. */
    public static void packAddsAndReplacesEntries(GameTestHelper helper) {
        Map<ResourceLocation, Background> patched = new HashMap<>(Presets.BACKGROUNDS.all());
        patched.put(
                KNIGHT,
                background(KNIGHT, "{\"abilities\": [\"str\", \"cha\", \"wis\"], \"skills\": [\"persuasion\"]}"));
        patched.put(
                SOLDIER,
                background(SOLDIER, "{\"abilities\": [\"wis\", \"int\", \"cha\"], \"skills\": [\"history\"]}"));
        boolean badRefused;
        try {
            background(KNIGHT, "{\"abilities\": [\"luck\"]}");
            badRefused = false;
        } catch (IllegalArgumentException e) {
            badRefused = true;
        }
        expect(helper, badRefused, "a background naming an unknown ability must be refused");
        ServerPlayer player = login(helper, newProfile("checks-pr-pack"));
        try {
            withBackgrounds(patched, () -> {
                CreationOffer offer = CharacterCreation.offerFor(player);
                expect(
                        helper,
                        offer.presets().background(Optional.of(KNIGHT)).isPresent(),
                        "the added background must be offered");
                expect(
                        helper,
                        offer.presets()
                                .background(Optional.of(SOLDIER))
                                .orElseThrow()
                                .abilities()
                                .equals(List.of(Ability.WISDOM, Ability.INTELLIGENCE, Ability.CHARISMA)),
                        "the replaced soldier must use the pack's abilities");
                List<ResourceLocation> shown = withPresetSettings(
                        hideShipped(), () -> CharacterCreation.offerFor(player).presets().backgrounds().stream()
                                .map(Preset::id)
                                .toList());
                expect(
                        helper,
                        shown.equals(List.of(KNIGHT)),
                        "hiding shipped presets must leave only the pack's, left " + shown);
                expect(
                        helper,
                        submit(player, List.of(0, 0, 0, 1, 2, 0), List.of(STEALTH, PERCEPTION), ELF_SOLDIER_FIGHTER)
                                .isEmpty(),
                        "bonuses on the replaced soldier's abilities must be accepted");
                return null;
            });
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Bonuses off the background's abilities, skills off the class list and wrong counts are refused; nothing is saved or given. */
    public static void serverRejectsInvalidPresetSubmissions(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-pr-bad"));
        try {
            withSender((target, payload) -> false, () -> {
                expectRejected(
                        helper,
                        player,
                        Rejection.BONUS_NOT_LISTED,
                        List.of(2, 0, 0, 0, 1, 0),
                        List.of(STEALTH, PERCEPTION),
                        ELF_SOLDIER_FIGHTER);
                expectRejected(
                        helper,
                        player,
                        Rejection.SKILL_NOT_ON_CLASS_LIST,
                        STR_TWO_CON_ONE,
                        List.of(STEALTH, PERCEPTION),
                        new PresetChoices(
                                Optional.of(ELF), Optional.of(SOLDIER), Optional.of(WIZARD), Optional.empty()));
                expectRejected(
                        helper, player, Rejection.SKILL_COUNT, STR_TWO_CON_ONE, List.of(STEALTH), ELF_SOLDIER_FIGHTER);
                expectRejected(
                        helper,
                        player,
                        Rejection.UNKNOWN_SPECIES,
                        STR_TWO_CON_ONE,
                        List.of(STEALTH, PERCEPTION),
                        new PresetChoices(
                                Optional.of(Checks.id("merfolk")),
                                Optional.of(SOLDIER),
                                Optional.of(FIGHTER),
                                Optional.empty()));
                return null;
            });
            expect(helper, CharacterCreation.available(player), "a rejected character must stay unconfirmed");
            expect(helper, player.getInventory().isEmpty(), "a rejected character must get no kit");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Soldier and Fighter share Athletics and Intimidation, so both Fighter picks may be any other skill. */
    public static void overlapLetsThePlayerPickAnySkill(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-pr-overlap"));
        try {
            expect(
                    helper,
                    submit(player, STR_TWO_CON_ONE, List.of(STEALTH, ARCANA), ELF_SOLDIER_FIGHTER)
                            .isEmpty(),
                    "two off-list picks must be accepted for the two overlapping skills");
            for (ResourceLocation skill : List.of(ATHLETICS, INTIMIDATION, STEALTH, ARCANA)) {
                expect(
                        helper,
                        skillProficiency(player, skill) == Proficiency.PROFICIENT,
                        skill + " must be proficient");
            }
            expect(helper, skillProficiency(player, HISTORY) == Proficiency.NONE, "history must not be proficient");
        } finally {
            logout(helper, player);
        }
        ServerPlayer criminal = login(helper, newProfile("checks-pr-overlap2"));
        try {
            expect(
                    helper,
                    submit(
                                    criminal,
                                    List.of(0, 2, 1, 0, 0, 0),
                                    List.of(PERCEPTION, ARCANA),
                                    new PresetChoices(
                                            Optional.empty(),
                                            Optional.of(CRIMINAL),
                                            Optional.of(FIGHTER),
                                            Optional.empty()))
                            .equals(Optional.of(Rejection.SKILL_NOT_ON_CLASS_LIST)),
                    "a criminal fighter shares no skill, so arcana is off the list");
        } finally {
            logout(helper, criminal);
        }
        helper.succeed();
    }

    /** Background bonuses land on top of the placed scores, class saves become proficient, and the sheet names all three. */
    public static void backgroundBonusesAndClassSavesApply(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-pr-bonus"));
        try {
            submit(player, STR_TWO_CON_ONE, List.of(STEALTH, PERCEPTION), ELF_SOLDIER_FIGHTER);
            expectEquals(helper, 17, unprofiled(() -> ChecksApi.abilityScore(player, Ability.STRENGTH)), "STR 15 + 2");
            expectEquals(
                    helper, 15, unprofiled(() -> ChecksApi.abilityScore(player, Ability.CONSTITUTION)), "CON 14 + 1");
            expectEquals(
                    helper,
                    13,
                    unprofiled(() -> ChecksApi.abilityScore(player, Ability.DEXTERITY)),
                    "DEX without bonus");
            expect(
                    helper,
                    unprofiled(() -> ChecksApi.saveProficiency(player, Ability.CONSTITUTION)) == Proficiency.PROFICIENT,
                    "fighter CON save");
            expect(
                    helper,
                    unprofiled(() -> ChecksApi.saveProficiency(player, Ability.WISDOM)) == Proficiency.NONE,
                    "no WIS save");
            StatSheet.Identity identity =
                    StatSheets.forPlayer(player).orElseThrow().identity().orElseThrow();
            expect(
                    helper,
                    identity.choices().equals(ELF_SOLDIER_FIGHTER),
                    "the sheet must name elf, soldier and fighter");
            expect(helper, identity.traits().contains(Checks.id("darkvision")), "the sheet must list the elf's traits");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** The kit arrives on confirm and never again: not on relog, not on a second submission. */
    public static void startingKitIsGivenOnce(GameTestHelper helper) {
        GameProfile profile = newProfile("checks-pr-kit");
        ServerPlayer first = login(helper, profile);
        submit(first, STR_TWO_CON_ONE, List.of(STEALTH, PERCEPTION), ELF_SOLDIER_FIGHTER);
        expectKit(helper, first, "after confirming");
        logout(helper, first);

        ServerPlayer second = login(helper, profile);
        try {
            CharacterCreation.onJoin(second);
            expectKit(helper, second, "after relogging");
            Optional<Rejection> again = withSender(
                    (target, payload) -> false,
                    () -> submit(second, STR_TWO_CON_ONE, List.of(STEALTH, PERCEPTION), ELF_SOLDIER_FIGHTER));
            expect(helper, again.equals(Optional.of(Rejection.ALREADY_CREATED)), "a second submission must be refused");
            expectKit(helper, second, "after submitting again");
        } finally {
            logout(helper, second);
        }
        helper.succeed();
    }

    /** The chosen presets and what they grant reach the world save. */
    public static void presetsPersistInTheWorldSave(GameTestHelper helper) {
        GameProfile profile = newProfile("checks-pr-save");
        ServerPlayer player = login(helper, profile);
        submit(player, STR_TWO_CON_ONE, List.of(STEALTH, PERCEPTION), ELF_SOLDIER_FIGHTER);
        logout(helper, player);

        saveWorldData(helper);
        Optional<CharacterBuild> build =
                savedCharacterStore(helper).character(profile.getId()).build();
        expect(helper, build.isPresent(), "the save must hold the build");
        CharacterBuild saved = build.get();
        expect(
                helper,
                saved.choices().equals(ELF_SOLDIER_FIGHTER),
                "the save must hold the presets, held " + saved.choices());
        expect(
                helper,
                saved.grants().saves().equals(Set.of(Ability.STRENGTH, Ability.CONSTITUTION))
                        && saved.grants().skills().equals(Set.of(ATHLETICS, INTIMIDATION))
                        && saved.grants().bonus(Ability.STRENGTH) == 2,
                "the save must hold what the presets grant, held " + saved.grants());

        helper.succeed();
    }

    /** {@code /checks reset} clears the presets with the rest of the character. */
    public static void resetClearsPresets(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-pr-reset"));
        try {
            submit(player, STR_TWO_CON_ONE, List.of(STEALTH, PERCEPTION), ELF_SOLDIER_FIGHTER);
            run(helper, "checks reset " + player.getScoreboardName());
            expect(
                    helper,
                    CharacterStore.get(player.server)
                            .character(player.getUUID())
                            .equals(PlayerCharacter.NONE),
                    "reset must clear the presets");
            expectEquals(
                    helper, 10, unprofiled(() -> ChecksApi.abilityScore(player, Ability.STRENGTH)), "STR after reset");
            expect(
                    helper,
                    unprofiled(() -> ChecksApi.saveProficiency(player, Ability.STRENGTH)) == Proficiency.NONE,
                    "no class save after reset");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** With presets off, creation and every character work as without presets. */
    public static void presetsToggleOffRestoresTheOldBehaviour(GameTestHelper helper) {
        ServerPlayer created = login(helper, newProfile("checks-pr-off1"));
        ServerPlayer newcomer = login(helper, newProfile("checks-pr-off2"));
        try {
            submit(created, STR_TWO_CON_ONE, List.of(STEALTH, PERCEPTION), ELF_SOLDIER_FIGHTER);
            ScoresConfig.PresetSettings live = ScoresRuntime.config().creation().presets();
            withPresetSettings(withEnabled(live, false), () -> {
                expectEquals(
                        helper,
                        15,
                        unprofiled(() -> ChecksApi.abilityScore(created, Ability.STRENGTH)),
                        "STR without the bonus");
                expect(
                        helper,
                        skillProficiency(created, ATHLETICS) == Proficiency.NONE,
                        "no background skill while off");
                expect(helper, skillProficiency(created, STEALTH) == Proficiency.PROFICIENT, "picked skills stay");
                expect(
                        helper,
                        unprofiled(() -> ChecksApi.saveProficiency(created, Ability.STRENGTH)) == Proficiency.NONE,
                        "no class save while off");
                expect(
                        helper,
                        StatSheets.forPlayer(created).orElseThrow().identity().isEmpty(),
                        "the sheet hides presets while off");
                CreationOffer offer = CharacterCreation.offerFor(newcomer);
                expect(
                        helper,
                        !offer.presets().enabled()
                                && offer.presets().backgrounds().isEmpty(),
                        "no presets offered while off");
                Optional<Rejection> withPreset = withSender(
                        (target, payload) -> false,
                        () -> submit(newcomer, STR_TWO_CON_ONE, List.of(STEALTH, PERCEPTION), ELF_SOLDIER_FIGHTER));
                expect(
                        helper,
                        withPreset.equals(Optional.of(Rejection.UNKNOWN_SPECIES)),
                        "presets must be refused while off, was " + withPreset);
                expect(
                        helper,
                        submit(newcomer, NO_BONUSES, List.of(STEALTH, PERCEPTION), PresetChoices.CUSTOM)
                                .isEmpty(),
                        "a pre-preset character must be accepted");
                return null;
            });
            expectEquals(
                    helper,
                    17,
                    unprofiled(() -> ChecksApi.abilityScore(created, Ability.STRENGTH)),
                    "STR with presets back on");
        } finally {
            logout(helper, created);
            logout(helper, newcomer);
        }
        helper.succeed();
    }

    private static Optional<Rejection> submit(
            ServerPlayer player, List<Integer> bonuses, List<ResourceLocation> skills, PresetChoices choices) {
        return CharacterCreation.submit(
                player,
                new CreationSubmission(
                        CreationMethod.STANDARD_ARRAY,
                        FIGHTER_ARRAY,
                        bonuses,
                        skills,
                        choices,
                        speciesPicks(player, choices, skills)));
    }

    private static void expectRejected(
            GameTestHelper helper,
            ServerPlayer player,
            Rejection expected,
            List<Integer> bonuses,
            List<ResourceLocation> skills,
            PresetChoices choices) {
        Optional<Rejection> actual = submit(player, bonuses, skills, choices);
        expect(helper, actual.equals(Optional.of(expected)), "must be refused as " + expected + ", was " + actual);
    }

    /** The shipped Soldier kit: a stone sword, a shield and three bread. */
    private static void expectKit(GameTestHelper helper, ServerPlayer player, String when) {
        for (Map.Entry<Item, Integer> stack :
                Map.of(Items.STONE_SWORD, 1, Items.SHIELD, 1, Items.BREAD, 3).entrySet()) {
            expectEquals(
                    helper,
                    stack.getValue(),
                    player.getInventory().countItem(stack.getKey()),
                    BuiltInRegistries.ITEM.getKey(stack.getKey()) + " " + when);
        }
    }

    private static Proficiency skillProficiency(ServerPlayer player, ResourceLocation skill) {
        return unprofiled(
                () -> ChecksApi.skillProficiency(player, ChecksApi.skill(skill).orElseThrow()));
    }

    private static Background background(ResourceLocation id, String json) {
        return Background.parse(
                id,
                JsonParser.parseString(json).getAsJsonObject(),
                SkillStore::find,
                BuiltInRegistries.ITEM::containsKey,
                warning -> {});
    }

    private static ScoresConfig.PresetSettings hideShipped() {
        ScoresConfig.PresetSettings live = ScoresRuntime.config().creation().presets();
        return new ScoresConfig.PresetSettings(live.enabled(), false, live.bonusOptions(), live.bonusMaxScore());
    }

    private static ScoresConfig.PresetSettings withEnabled(ScoresConfig.PresetSettings live, boolean enabled) {
        return new ScoresConfig.PresetSettings(
                enabled, live.includeShipped(), live.bonusOptions(), live.bonusMaxScore());
    }
}
