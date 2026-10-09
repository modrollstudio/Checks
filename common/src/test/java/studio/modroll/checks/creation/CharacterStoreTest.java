package studio.modroll.checks.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.body.Size;
import studio.modroll.critfall.api.dice.DieRoll;

class CharacterStoreTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void unknownPlayersHaveNoCharacter() {
        assertEquals(PlayerCharacter.NONE, new CharacterStore().character(PLAYER));
    }

    @Test
    void promptRollsAndBuildRoundTrip() {
        CharacterStore store = new CharacterStore();
        CharacterRolls rolls = new CharacterRolls(
                CreationMethod.HARDCORE,
                List.of(
                        rolled(3, 6, 6, 5),
                        rolled(1, 2, 3, 4),
                        rolled(4, 4, 4, 4),
                        rolled(2, 5, 5, 2),
                        rolled(1, 6, 6, 2),
                        rolled(1, 1, 1, 4)));
        CharacterBuild build = new CharacterBuild(
                CreationMethod.HARDCORE,
                scores(17, 9, 12, 12, 14, 6),
                Set.of(Offers.STEALTH),
                PresetChoices.CUSTOM,
                PresetGrants.NONE);
        store.update(
                PLAYER, character -> character.withPrompted().withRolls(rolls).withBuild(build));
        assertTrue(store.isDirty());

        PlayerCharacter reloaded = roundTrip(store).character(PLAYER);

        assertEquals(new PlayerCharacter(true, Optional.of(rolls), Optional.of(build)), reloaded);
    }

    @Test
    void presetChoicesAndGrantsRoundTrip() {
        CharacterStore store = new CharacterStore();
        CharacterBuild build = new CharacterBuild(
                CreationMethod.STANDARD_ARRAY,
                scores(15, 14, 13, 12, 10, 8),
                Set.of(Offers.PERCEPTION),
                new PresetChoices(
                        Optional.of(Offers.ELF),
                        Optional.of(Offers.SOLDIER),
                        Optional.of(Offers.FIGHTER),
                        Optional.empty()),
                new PresetGrants(
                        Map.of(Ability.STRENGTH, 2, Ability.CONSTITUTION, 1),
                        Set.of(Offers.ATHLETICS, Offers.INTIMIDATION),
                        Set.of(Ability.STRENGTH, Ability.CONSTITUTION),
                        Set.of(Offers.STEALTH)));
        store.update(PLAYER, character -> character.withBuild(build));

        assertEquals(Optional.of(build), roundTrip(store).character(PLAYER).build());
    }

    @Test
    void aChosenSizeRoundTrips() {
        CharacterStore store = new CharacterStore();
        CharacterBuild build = new CharacterBuild(
                CreationMethod.STANDARD_ARRAY,
                scores(15, 14, 13, 12, 10, 8),
                Set.of(),
                new PresetChoices(
                        Optional.of(Offers.HUMAN), Optional.empty(), Optional.empty(), Optional.of(Size.SMALL)),
                PresetGrants.NONE);
        store.update(PLAYER, character -> character.withBuild(build));

        assertEquals(Optional.of(build), roundTrip(store).character(PLAYER).build());
    }

    @Test
    void aBuildMissingAPartIsSkipped() {
        CharacterStore store = new CharacterStore();
        CharacterBuild build = new CharacterBuild(
                CreationMethod.STANDARD_ARRAY,
                scores(15, 14, 13, 12, 10, 8),
                Set.of(),
                PresetChoices.CUSTOM,
                PresetGrants.NONE);
        store.update(PLAYER, character -> character.withPrompted().withBuild(build));
        CompoundTag tag = store.save(new CompoundTag(), null);
        tag.getCompound("players")
                .getCompound(PLAYER.toString())
                .getCompound("build")
                .remove("grants");

        assertEquals(PlayerCharacter.NONE, CharacterStore.load(tag, null).character(PLAYER));
    }

    @Test
    void resetForgetsTheWholeCharacter() {
        CharacterStore store = new CharacterStore();
        store.update(PLAYER, PlayerCharacter::withPrompted);
        store.update(OTHER, PlayerCharacter::withPrompted);

        store.reset(PLAYER);

        assertEquals(PlayerCharacter.NONE, roundTrip(store).character(PLAYER));
        assertTrue(roundTrip(store).character(OTHER).prompted());
    }

    @Test
    void anEmptySaveHasNoCharacters() {
        assertEquals(
                PlayerCharacter.NONE,
                CharacterStore.load(new CompoundTag(), null).character(PLAYER));
    }

    @Test
    void malformedEntriesAreSkippedAndTheRestKept() {
        CompoundTag badRolls = new CompoundTag();
        badRolls.putString("method", "roll");
        badRolls.put("rolled", new ListTag());
        CompoundTag broken = new CompoundTag();
        broken.put("rolls", badRolls);
        CompoundTag fine = new CompoundTag();
        fine.putBoolean("prompted", true);
        CompoundTag players = new CompoundTag();
        players.put(PLAYER.toString(), broken);
        players.put(OTHER.toString(), fine);
        players.put("not-a-uuid", fine.copy());
        CompoundTag tag = new CompoundTag();
        tag.put("players", players);

        CharacterStore loaded = CharacterStore.load(tag, null);

        assertEquals(PlayerCharacter.NONE, loaded.character(PLAYER));
        assertTrue(loaded.character(OTHER).prompted());
    }

    private static Map<Ability, Integer> scores(int... values) {
        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) {
            scores.put(ability, values[ability.ordinal()]);
        }
        return scores;
    }

    /** A 4d6-drop-lowest style roll: the first face is dropped, the rest are kept. */
    private static RolledScore rolled(int dropped, int... kept) {
        List<DieRoll> dice = new ArrayList<>();
        dice.add(new DieRoll(6, dropped, false));
        for (int face : kept) {
            dice.add(new DieRoll(6, face, true));
        }
        return new RolledScore(IntStream.of(kept).sum(), dice);
    }

    private static CharacterStore roundTrip(CharacterStore store) {
        return CharacterStore.load(store.save(new CompoundTag(), null), null);
    }
}
