package studio.modroll.checks.creation;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import studio.modroll.checks.ClientPayloads;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.data.SkillStore;
import studio.modroll.checks.preset.Presets;
import studio.modroll.checks.preset.Species;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.trait.Traits;
import studio.modroll.critfall.api.RollService;
import studio.modroll.critfall.api.dice.RollResult;

/**
 * The server side of character creation. The client only proposes; every roll happens and every
 * choice is validated here, against the same {@link CreationOffer} the client was sent.
 */
public final class CharacterCreation {

    private CharacterCreation() {}

    public static boolean available(ServerPlayer player) {
        return settings().enabled() && !character(player).created();
    }

    /** Offers creation on the first join after the character was cleared; skipping it is remembered. */
    public static void onJoin(ServerPlayer player) {
        if (!available(player) || character(player).prompted()) {
            return;
        }
        store(player).update(player.getUUID(), PlayerCharacter::withPrompted);
        sendOffer(player);
    }

    public static void requestOpen(ServerPlayer player) {
        if (available(player)) {
            sendOffer(player);
        }
    }

    /**
     * Rolls one total per ability through Critfall and broadcasts each roll. Rolls are kept for good:
     * asking again, or for another method, only resends the offer. Returns the broadcast lines.
     */
    public static List<Component> roll(ServerPlayer player, CreationMethod method) {
        if (!available(player) || !method.rollsDice() || !settings().methods().contains(method)) {
            return List.of();
        }
        if (character(player).rolls().isPresent()) {
            sendOffer(player);
            return List.of();
        }
        List<RollResult> results = Stream.generate(
                        () -> RollService.roll(settings().dice(method)))
                .limit(CreationSubmission.ABILITY_COUNT)
                .toList();
        CharacterRolls rolls =
                new CharacterRolls(method, results.stream().map(RolledScore::of).toList());
        store(player).update(player.getUUID(), character -> character.withRolls(rolls));
        List<Component> lines = RollAnnouncement.lines(player.getDisplayName(), method, results);
        lines.forEach(line -> player.server.getPlayerList().broadcastSystemMessage(line, false));
        sendOffer(player);
        return lines;
    }

    /** An accepted submission becomes the player's character, and its background's kit is handed out once. */
    public static Optional<Rejection> submit(ServerPlayer player, CreationSubmission submission) {
        CreationOffer offer = offerFor(player);
        Optional<Rejection> rejection = check(player, submission, offer);
        rejection.ifPresentOrElse(
                reason -> reject(player, reason, offer, submission.choices()),
                () -> confirm(player, submission, offer));
        return rejection;
    }

    /** Clears the character, rolls included, so creation opens again on the player's next join. */
    public static void reset(MinecraftServer server, UUID player) {
        CharacterStore.get(server).reset(player);
    }

    public static CreationOffer offerFor(ServerPlayer player) {
        ScoresConfig.CreationSettings settings = settings();
        PresetOffer presets = presetOffer(settings.presets());
        return new CreationOffer(
                settings.methods(),
                settings.standardArray(),
                settings.pointBuy(),
                settings.skillChoices(),
                SkillStore.skills().values().stream()
                        .sorted(Comparator.comparing(Skill::id))
                        .toList(),
                character(player).rolls(),
                presets,
                presets.species().stream().collect(Collectors.toMap(Species::id, Traits::skillChoice)));
    }

    /** The player's confirmed character while creation is on. */
    public static Optional<CharacterBuild> activeBuild(ServerPlayer player) {
        return settings().enabled() ? character(player).build() : Optional.empty();
    }

    private static PresetOffer presetOffer(ScoresConfig.PresetSettings settings) {
        if (!settings.enabled()) {
            return new PresetOffer(
                    false, List.of(), List.of(), List.of(), settings.bonusOptions(), settings.bonusMaxScore());
        }
        boolean shipped = settings.includeShipped();
        return new PresetOffer(
                true,
                Presets.SPECIES.offered(shipped),
                Presets.BACKGROUNDS.offered(shipped),
                Presets.CLASSES.offered(shipped),
                settings.bonusOptions(),
                settings.bonusMaxScore());
    }

    private static Optional<Rejection> check(ServerPlayer player, CreationSubmission submission, CreationOffer offer) {
        if (!settings().enabled()) {
            return Optional.of(Rejection.CREATION_DISABLED);
        }
        if (character(player).created()) {
            return Optional.of(Rejection.ALREADY_CREATED);
        }
        return CreationRules.validate(submission, offer);
    }

    private static void confirm(ServerPlayer player, CreationSubmission submission, CreationOffer offer) {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            scores.put(ability, Abilities.clamp(submission.scores().get(ability.ordinal())));
        }
        CharacterBuild build = new CharacterBuild(
                submission.method(),
                scores,
                Set.copyOf(submission.skills()),
                submission.choices(),
                PresetGrants.of(submission, offer.presets()));
        store(player).update(player.getUUID(), character -> character.withBuild(build));
        offer.presets()
                .background(submission.choices().background())
                .ifPresent(background -> StartingKit.give(player, background.kit()));
        player.sendSystemMessage(Component.translatable("checks.creation.confirmed"));
    }

    /** Explains the refusal and reopens the screen so the player can fix their choices. */
    private static void reject(ServerPlayer player, Rejection reason, CreationOffer offer, PresetChoices choices) {
        player.sendSystemMessage(reason.message(offer, choices).copy().withStyle(ChatFormatting.RED));
        requestOpen(player);
    }

    private static void sendOffer(ServerPlayer player) {
        ClientPayloads.send(player, new CreationOfferPayload(offerFor(player)));
    }

    private static PlayerCharacter character(ServerPlayer player) {
        return store(player).character(player.getUUID());
    }

    private static CharacterStore store(ServerPlayer player) {
        return CharacterStore.get(player.server);
    }

    private static ScoresConfig.CreationSettings settings() {
        return ScoresRuntime.config().creation();
    }
}
