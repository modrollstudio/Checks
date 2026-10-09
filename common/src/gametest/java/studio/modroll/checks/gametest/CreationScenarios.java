package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.expectEquals;
import static studio.modroll.checks.gametest.ScenarioSupport.login;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.profiles;
import static studio.modroll.checks.gametest.ScenarioSupport.run;
import static studio.modroll.checks.gametest.ScenarioSupport.saveWorldData;
import static studio.modroll.checks.gametest.ScenarioSupport.savedCharacterStore;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.withCreation;
import static studio.modroll.checks.gametest.ScenarioSupport.withDice;
import static studio.modroll.checks.gametest.ScenarioSupport.withProfiles;
import static studio.modroll.checks.gametest.ScenarioSupport.withSender;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.creation.CharacterBuild;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.creation.CharacterRolls;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.CreationOffer;
import studio.modroll.checks.creation.CreationOfferPayload;
import studio.modroll.checks.creation.CreationSubmission;
import studio.modroll.checks.creation.PlayerCharacter;
import studio.modroll.checks.creation.PresetChoices;
import studio.modroll.checks.creation.Rejection;
import studio.modroll.checks.data.EntityScoreProfile;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.sheet.StatSheets;
import studio.modroll.critfall.api.dice.DieRoll;

/** GameTest bodies for character creation: the server rolls, validates and stores every character. */
public final class CreationScenarios {

    private static final ResourceLocation ATHLETICS = ResourceLocation.parse("checks:athletics");
    private static final ResourceLocation STEALTH = ResourceLocation.parse("checks:stealth");
    private static final ResourceLocation PERCEPTION = ResourceLocation.parse("checks:perception");
    private static final List<ResourceLocation> TWO_SKILLS = List.of(ATHLETICS, STEALTH);
    private static final List<Integer> ARRAY_ASSIGNED = List.of(8, 10, 12, 13, 14, 15);
    private static final List<Integer> DEFAULT_SCORES = List.of(10, 10, 10, 10, 10, 10);
    /** Six 4d6 rolls, the lowest of each dropped; the second is 1, 1, 2 and a dropped 1. */
    private static final int[] ROLL_FACES = {5, 5, 5, 2, 1, 1, 2, 1, 6, 6, 6, 1, 3, 3, 3, 3, 4, 4, 4, 1, 2, 2, 3, 1};

    private static final List<Integer> ROLL_TOTALS = List.of(15, 4, 18, 9, 12, 7);
    /** Six 3d6 rolls in ability order. */
    private static final int[] HARDCORE_FACES = {6, 6, 6, 1, 1, 1, 2, 3, 4, 5, 5, 5, 1, 2, 3, 4, 4, 4};

    private static final List<Integer> HARDCORE_TOTALS = List.of(18, 3, 9, 15, 6, 12);

    private CreationScenarios() {}

    /** A valid submission for each method becomes the player's scores, and the picked skills proficient. */
    public static void eachMethodProducesValidScores(GameTestHelper helper) {
        for (CreationMethod method : CreationMethod.values()) {
            ServerPlayer player = login(helper, newProfile("checks-cr-m" + method.ordinal()));
            try {
                List<Integer> scores = validScores(player, method);
                expect(
                        helper,
                        submit(player, method, scores, TWO_SKILLS).isEmpty(),
                        method.id() + " submission " + scores + " must be accepted");
                expectScores(helper, player, scores, method.id());
                expect(
                        helper,
                        unprofiled(() -> ChecksApi.skillProficiency(
                                        player, ChecksApi.skill(STEALTH).orElseThrow()))
                                == Proficiency.PROFICIENT,
                        method.id() + ": a picked skill must be proficient");
            } finally {
                logout(helper, player);
            }
        }
        helper.succeed();
    }

    /** Wrong arrays, overspent points, edited rolls and too many skills are refused, and nothing is saved. */
    public static void serverRejectsInvalidSubmissions(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-cr-bad"));
        try {
            List<CreationOffer> reopened = new ArrayList<>();
            withSender(capturingOffers(reopened), () -> {
                expectRejected(
                        helper,
                        player,
                        Rejection.WRONG_ARRAY,
                        CreationMethod.STANDARD_ARRAY,
                        List.of(15, 15, 13, 12, 10, 8),
                        TWO_SKILLS);
                expectRejected(
                        helper,
                        player,
                        Rejection.POINTS_OVERSPENT,
                        CreationMethod.POINT_BUY,
                        List.of(15, 15, 15, 9, 8, 8),
                        TWO_SKILLS);
                expectRejected(
                        helper,
                        player,
                        Rejection.SKILL_COUNT,
                        CreationMethod.STANDARD_ARRAY,
                        ARRAY_ASSIGNED,
                        List.of(ATHLETICS, STEALTH, PERCEPTION));
                withDice(() -> CharacterCreation.roll(player, CreationMethod.ROLL), ROLL_FACES);
                List<Integer> edited = new ArrayList<>(ROLL_TOTALS);
                edited.set(1, 18);
                expectRejected(helper, player, Rejection.ROLLS_CHANGED, CreationMethod.ROLL, edited, TWO_SKILLS);
                return null;
            });
            expect(helper, reopened.size() >= 4, "every rejection must reopen the screen, got " + reopened.size());
            expect(helper, CharacterCreation.available(player), "a rejected character must stay unconfirmed");
            expectScores(helper, player, DEFAULT_SCORES, "after rejections");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Rolls are stored on the server: reopening, relogging or asking again never rerolls. */
    public static void rollsDoNotChangeOnReopenOrRelog(GameTestHelper helper) {
        GameProfile profile = newProfile("checks-cr-fixed");
        ServerPlayer first = login(helper, profile);
        List<Component> lines = withDice(() -> CharacterCreation.roll(first, CreationMethod.ROLL), ROLL_FACES);
        expectEquals(helper, 6, lines.size(), "broadcast roll lines");
        expectRolls(helper, first, CreationMethod.ROLL, ROLL_TOTALS, "after rolling");
        logout(helper, first);

        ServerPlayer second = login(helper, profile);
        try {
            List<CreationOffer> offers = new ArrayList<>();
            withSender(capturingOffers(offers), () -> {
                CharacterCreation.requestOpen(second);
                List<Component> again = withDice(() -> CharacterCreation.roll(second, CreationMethod.ROLL));
                List<Component> other = withDice(() -> CharacterCreation.roll(second, CreationMethod.HARDCORE));
                expect(helper, again.isEmpty() && other.isEmpty(), "rolling again must not roll or broadcast");
                return null;
            });
            expectRolls(helper, second, CreationMethod.ROLL, ROLL_TOTALS, "after relog and rolling again");
            expect(
                    helper,
                    !offers.isEmpty()
                            && offers.stream().allMatch(offer -> offer.rolledTotals(CreationMethod.ROLL)
                                    .equals(Optional.of(ROLL_TOTALS))),
                    "every reopened offer must carry the original rolls, got " + offers);
        } finally {
            logout(helper, second);
        }
        helper.succeed();
    }

    /** Hardcore rolls land on STR to CHA in order; any other order is refused. */
    public static void hardcoreAssignsInOrder(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-cr-hard"));
        try {
            withDice(() -> CharacterCreation.roll(player, CreationMethod.HARDCORE), HARDCORE_FACES);
            expectRolls(helper, player, CreationMethod.HARDCORE, HARDCORE_TOTALS, "hardcore");
            List<Integer> sorted =
                    HARDCORE_TOTALS.stream().sorted(Comparator.reverseOrder()).toList();
            expectRejected(helper, player, Rejection.HARDCORE_ORDER, CreationMethod.HARDCORE, sorted, TWO_SKILLS);
            expect(
                    helper,
                    submit(player, CreationMethod.HARDCORE, HARDCORE_TOTALS, TWO_SKILLS)
                            .isEmpty(),
                    "hardcore rolls in order must be accepted");
            expectScores(helper, player, HARDCORE_TOTALS, "hardcore");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Each broadcast line lists every die, the dropped one struck through; the offer carries the same dice. */
    public static void rollChatListsEveryDie(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-cr-chat"));
        try {
            List<Component> lines = withDice(() -> CharacterCreation.roll(player, CreationMethod.ROLL), ROLL_FACES);
            expectEquals(helper, 6, lines.size(), "one line per roll");
            Object[] args = ((TranslatableContents) lines.get(1).getContents()).getArgs();
            List<String> dice = new ArrayList<>();
            List<Boolean> struck = new ArrayList<>();
            ((Component) args[2])
                    .visit(
                            (style, text) -> {
                                if (Character.isDigit(text.charAt(0))) {
                                    dice.add(text);
                                    struck.add(style.isStrikethrough());
                                }
                                return Optional.empty();
                            },
                            Style.EMPTY);
            expect(helper, dice.equals(List.of("1", "1", "2", "1")), "dice must be 1, 1, 2, 1, were " + dice);
            expect(helper, struck.equals(List.of(false, false, false, true)), "only the last 1 is dropped: " + struck);
            expect(helper, Integer.valueOf(4).equals(args[3]), "the total must be 4, was " + args[3]);
            List<DieRoll> offered = CharacterCreation.offerFor(player)
                    .rolledScores(CreationMethod.ROLL)
                    .orElseThrow()
                    .get(1)
                    .dice();
            expect(
                    helper,
                    offered.equals(List.of(
                            new DieRoll(6, 1, true),
                            new DieRoll(6, 1, true),
                            new DieRoll(6, 2, true),
                            new DieRoll(6, 1, false))),
                    "the offer must carry the same dice for the screen, carried " + offered);
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** Creation opens on the first join only; {@code /checks reset} clears the character and opens it again. */
    public static void resetReopensCreation(GameTestHelper helper) {
        GameProfile profile = newProfile("checks-cr-reset");
        expectEquals(
                helper,
                1,
                offersOnJoin(
                        helper,
                        profile,
                        player -> submit(player, CreationMethod.STANDARD_ARRAY, ARRAY_ASSIGNED, TWO_SKILLS)),
                "offers on first join");
        expectEquals(
                helper,
                0,
                offersOnJoin(helper, profile, player -> run(helper, "checks reset " + player.getScoreboardName())),
                "offers once created");
        expectEquals(
                helper,
                1,
                offersOnJoin(helper, profile, player -> {
                    expectScores(helper, player, DEFAULT_SCORES, "after reset");
                    return null;
                }),
                "offers after reset");
        helper.succeed();
    }

    /** With creation off nothing opens or saves, and a confirmed character is ignored until it is back on. */
    public static void creationToggleOffSkipsIt(GameTestHelper helper) {
        ServerPlayer created = login(helper, newProfile("checks-cr-off1"));
        GameProfile newcomer = newProfile("checks-cr-off2");
        try {
            submit(created, CreationMethod.STANDARD_ARRAY, ARRAY_ASSIGNED, TWO_SKILLS);
            ScoresConfig.CreationSettings live = ScoresRuntime.config().creation();
            withCreation(withEnabled(live, false), () -> {
                expectEquals(
                        helper,
                        0,
                        offersOnJoin(helper, newcomer, player -> {
                            List<CreationOffer> reopened = new ArrayList<>();
                            withSender(capturingOffers(reopened), () -> {
                                CharacterCreation.requestOpen(player);
                                return null;
                            });
                            expect(helper, reopened.isEmpty(), "reopening must do nothing while creation is off");
                            expect(
                                    helper,
                                    submit(player, CreationMethod.STANDARD_ARRAY, ARRAY_ASSIGNED, TWO_SKILLS)
                                            .equals(Optional.of(Rejection.CREATION_DISABLED)),
                                    "submitting must be refused while creation is off");
                            expect(
                                    helper,
                                    !StatSheets.forPlayer(player).orElseThrow().creationAvailable(),
                                    "the stat screen must hide the creation button while creation is off");
                            return null;
                        }),
                        "offers while creation is off");
                expectScores(helper, created, DEFAULT_SCORES, "while creation is off");
                return null;
            });
            expectScores(helper, created, ARRAY_ASSIGNED, "with creation back on");
            expectEquals(helper, 1, offersOnJoin(helper, newcomer, player -> null), "offers once creation is on");
        } finally {
            logout(helper, created);
        }
        helper.succeed();
    }

    /** Character scores beat a {@code minecraft:player} profile; {@code /checks set} beats both. */
    public static void characterRanksBetweenCommandAndProfile(GameTestHelper helper) {
        ServerPlayer player = login(helper, newProfile("checks-cr-rank"));
        try {
            submit(player, CreationMethod.STANDARD_ARRAY, ARRAY_ASSIGNED, TWO_SKILLS);
            Map<ResourceLocation, EntityScoreProfile> playerProfile =
                    profiles("{\"matches\": [\"minecraft:player\"], \"abilities\": {\"cha\": 13}}");
            int character = withProfiles(playerProfile, () -> ChecksApi.abilityScore(player, Ability.CHARISMA));
            expectEquals(helper, 15, character, "character CHA over the player profile");
            run(helper, "checks set " + player.getScoreboardName() + " cha 17");
            int commanded = withProfiles(playerProfile, () -> ChecksApi.abilityScore(player, Ability.CHARISMA));
            expectEquals(helper, 17, commanded, "/checks set CHA over the character");
        } finally {
            logout(helper, player);
        }
        helper.succeed();
    }

    /** A confirmed character, its rolls included, reaches the world save. */
    public static void characterPersistsInTheWorldSave(GameTestHelper helper) {
        GameProfile profile = newProfile("checks-cr-save");
        ServerPlayer player = login(helper, profile);
        withDice(() -> CharacterCreation.roll(player, CreationMethod.HARDCORE), HARDCORE_FACES);
        submit(player, CreationMethod.HARDCORE, HARDCORE_TOTALS, TWO_SKILLS);
        logout(helper, player);

        saveWorldData(helper);
        PlayerCharacter saved = savedCharacterStore(helper).character(profile.getId());
        expect(
                helper,
                saved.rolls().map(CharacterRolls::totals).equals(Optional.of(HARDCORE_TOTALS)),
                "the save must hold the hardcore rolls, held " + saved.rolls());
        expect(
                helper,
                saved.rolls().orElseThrow().scores().stream()
                        .allMatch(score -> score.dice().size() == 3),
                "the save must keep the three dice behind each hardcore roll");
        expect(
                helper,
                saved.build().map(CharacterBuild::skills).equals(Optional.of(Set.copyOf(TWO_SKILLS))),
                "the save must hold the confirmed build, held " + saved.build());

        helper.succeed();
    }

    private static List<Integer> validScores(ServerPlayer player, CreationMethod method) {
        return switch (method) {
            case STANDARD_ARRAY -> ARRAY_ASSIGNED;
            case POINT_BUY -> List.of(15, 15, 15, 8, 8, 8);
            case ROLL -> {
                withDice(() -> CharacterCreation.roll(player, method), ROLL_FACES);
                yield ROLL_TOTALS.reversed();
            }
            case HARDCORE -> {
                withDice(() -> CharacterCreation.roll(player, method), HARDCORE_FACES);
                yield HARDCORE_TOTALS;
            }
        };
    }

    private static Optional<Rejection> submit(
            ServerPlayer player, CreationMethod method, List<Integer> scores, List<ResourceLocation> skills) {
        return CharacterCreation.submit(
                player,
                new CreationSubmission(
                        method, scores, List.of(0, 0, 0, 0, 0, 0), skills, PresetChoices.CUSTOM, List.of()));
    }

    private static void expectRejected(
            GameTestHelper helper,
            ServerPlayer player,
            Rejection expected,
            CreationMethod method,
            List<Integer> scores,
            List<ResourceLocation> skills) {
        Optional<Rejection> actual = submit(player, method, scores, skills);
        expect(
                helper,
                actual.equals(Optional.of(expected)),
                method.id() + " " + scores + " must be refused as " + expected + ", was " + actual);
    }

    private static void expectScores(GameTestHelper helper, ServerPlayer player, List<Integer> scores, String when) {
        for (Ability ability : Ability.values()) {
            expectEquals(
                    helper,
                    scores.get(ability.ordinal()),
                    unprofiled(() -> ChecksApi.abilityScore(player, ability)),
                    ability.id() + " " + when);
        }
    }

    private static void expectRolls(
            GameTestHelper helper, ServerPlayer player, CreationMethod method, List<Integer> totals, String when) {
        Optional<List<Integer>> rolled = CharacterCreation.offerFor(player).rolledTotals(method);
        expect(
                helper,
                rolled.equals(Optional.of(totals)),
                method.id() + " rolls " + when + " must be " + totals + ", were " + rolled);
    }

    /**
     * Logs in, runs the join hook (the loader's own join event may already have), runs {@code then}, logs
     * out, and counts the creation offers sent meanwhile.
     */
    private static int offersOnJoin(GameTestHelper helper, GameProfile profile, Function<ServerPlayer, ?> then) {
        List<CreationOffer> offers = new ArrayList<>();
        withSender(capturingOffers(offers), () -> {
            ServerPlayer player = login(helper, profile);
            try {
                CharacterCreation.onJoin(player);
                int beforeThen = offers.size();
                then.apply(player);
                offers.subList(beforeThen, offers.size()).clear();
            } finally {
                logout(helper, player);
            }
            return null;
        });
        return offers.size();
    }

    private static BiPredicate<ServerPlayer, CustomPacketPayload> capturingOffers(List<CreationOffer> offers) {
        return (player, payload) -> payload instanceof CreationOfferPayload offer && offers.add(offer.offer());
    }

    private static ScoresConfig.CreationSettings withEnabled(ScoresConfig.CreationSettings live, boolean enabled) {
        return new ScoresConfig.CreationSettings(
                enabled,
                live.methods(),
                live.skillChoices(),
                live.standardArray(),
                live.pointBuy(),
                live.rollDice(),
                live.hardcoreDice(),
                live.presets());
    }
}
