package studio.modroll.checks.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.proficiency.Proficiencies;
import studio.modroll.checks.skill.Skills;

class PlayerScoreStoreTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final ResourceLocation STEALTH = ResourceLocation.parse("checks:stealth");
    private static final ResourceLocation SAILING = ResourceLocation.parse("pack:sailing");

    @Test
    void storesAndReadsBackAScore() {
        PlayerScoreStore store = new PlayerScoreStore();
        store.setScore(PLAYER, Ability.STRENGTH, 17);
        assertEquals(OptionalInt.of(17), store.getScore(PLAYER, Ability.STRENGTH));
        assertEquals(OptionalInt.empty(), store.getScore(PLAYER, Ability.DEXTERITY));
    }

    @Test
    void setClampsToRangeAndMarksDirty() {
        PlayerScoreStore store = new PlayerScoreStore();
        store.setScore(PLAYER, Ability.STRENGTH, 999);
        assertEquals(OptionalInt.of(30), store.getScore(PLAYER, Ability.STRENGTH));
        assertTrue(store.isDirty());
    }

    @Test
    void loadSkipsCorruptEntriesAndKeepsTheRest() {
        CompoundTag good = new CompoundTag();
        good.putInt("str", 99);
        CompoundTag players = new CompoundTag();
        players.put(PLAYER.toString(), good);
        players.put("not-a-uuid", good.copy());
        CompoundTag tag = new CompoundTag();
        tag.put("players", players);

        PlayerScoreStore loaded = PlayerScoreStore.load(tag, null);

        assertEquals(OptionalInt.of(30), loaded.getScore(PLAYER, Ability.STRENGTH));
    }

    @Test
    void storesClampsAndReadsBackASkillBonus() {
        PlayerScoreStore store = new PlayerScoreStore();
        store.setSkillBonus(PLAYER, STEALTH, 4);
        store.setSkillBonus(PLAYER, SAILING, 999);
        assertEquals(OptionalInt.of(4), store.getSkillBonus(PLAYER, STEALTH));
        assertEquals(OptionalInt.of(Skills.MAX_BONUS), store.getSkillBonus(PLAYER, SAILING));
        assertEquals(OptionalInt.empty(), store.getSkillBonus(PLAYER, ResourceLocation.parse("checks:arcana")));
        assertTrue(store.isDirty());
    }

    @Test
    void playerWithOnlySkillBonusesRoundTrips() {
        PlayerScoreStore store = new PlayerScoreStore();
        store.setSkillBonus(PLAYER, STEALTH, 3);

        PlayerScoreStore reloaded = roundTrip(store);

        assertEquals(OptionalInt.of(3), reloaded.getSkillBonus(PLAYER, STEALTH));
        assertTrue(reloaded.getScore(PLAYER, Ability.DEXTERITY).isEmpty());
    }

    @Test
    void loadsSaveWithoutSkills() {
        CompoundTag entry = new CompoundTag();
        entry.putInt("str", 12);
        PlayerScoreStore loaded = loadEntry(entry);

        assertEquals(OptionalInt.of(12), loaded.getScore(PLAYER, Ability.STRENGTH));
        assertTrue(loaded.getSkillBonus(PLAYER, STEALTH).isEmpty());
    }

    @Test
    void storesClampsAndReadsBackProficiency() {
        PlayerScoreStore store = new PlayerScoreStore();
        store.setProficiencyBonus(PLAYER, 99);
        store.setSkillProficiency(PLAYER, STEALTH, Proficiency.EXPERTISE);
        store.setSaveProficiency(PLAYER, Ability.DEXTERITY, Proficiency.PROFICIENT);
        assertEquals(OptionalInt.of(Proficiencies.MAX_BONUS), store.getProficiencyBonus(PLAYER));
        assertEquals(Optional.of(Proficiency.EXPERTISE), store.getSkillProficiency(PLAYER, STEALTH));
        assertEquals(Optional.empty(), store.getSkillProficiency(PLAYER, SAILING));
        assertEquals(Optional.of(Proficiency.PROFICIENT), store.getSaveProficiency(PLAYER, Ability.DEXTERITY));
        assertEquals(Optional.empty(), store.getSaveProficiency(PLAYER, Ability.WISDOM));
        assertTrue(store.isDirty());
    }

    @Test
    void everyValueSurvivesNbtRoundTrip() {
        PlayerScoreStore store = new PlayerScoreStore();
        store.setScore(PLAYER, Ability.STRENGTH, 17);
        store.setScore(PLAYER, Ability.WISDOM, 8);
        store.setSkillBonus(PLAYER, STEALTH, 1);
        store.setSkillBonus(PLAYER, SAILING, -2);
        store.setProficiencyBonus(PLAYER, 4);
        store.setSkillProficiency(PLAYER, STEALTH, Proficiency.EXPERTISE);
        store.setSkillProficiency(PLAYER, SAILING, Proficiency.NONE);
        store.setSaveProficiency(PLAYER, Ability.CONSTITUTION, Proficiency.PROFICIENT);

        PlayerScoreStore reloaded = roundTrip(store);

        assertEquals(OptionalInt.of(17), reloaded.getScore(PLAYER, Ability.STRENGTH));
        assertEquals(OptionalInt.of(8), reloaded.getScore(PLAYER, Ability.WISDOM));
        assertTrue(reloaded.getScore(PLAYER, Ability.CHARISMA).isEmpty());
        assertEquals(OptionalInt.of(1), reloaded.getSkillBonus(PLAYER, STEALTH));
        assertEquals(OptionalInt.of(-2), reloaded.getSkillBonus(PLAYER, SAILING));
        assertEquals(OptionalInt.of(4), reloaded.getProficiencyBonus(PLAYER));
        assertEquals(Optional.of(Proficiency.EXPERTISE), reloaded.getSkillProficiency(PLAYER, STEALTH));
        assertEquals(Optional.of(Proficiency.NONE), reloaded.getSkillProficiency(PLAYER, SAILING));
        assertEquals(Optional.of(Proficiency.PROFICIENT), reloaded.getSaveProficiency(PLAYER, Ability.CONSTITUTION));
    }

    @Test
    void playerWithOnlyProficiencyRoundTrips() {
        PlayerScoreStore store = new PlayerScoreStore();
        store.setSaveProficiency(PLAYER, Ability.WISDOM, Proficiency.PROFICIENT);

        PlayerScoreStore reloaded = roundTrip(store);

        assertEquals(Optional.of(Proficiency.PROFICIENT), reloaded.getSaveProficiency(PLAYER, Ability.WISDOM));
        assertTrue(reloaded.getProficiencyBonus(PLAYER).isEmpty());
    }

    @Test
    void loadsSaveWithoutProficiency() {
        CompoundTag skills = new CompoundTag();
        skills.putInt(STEALTH.toString(), 3);
        CompoundTag entry = new CompoundTag();
        entry.putInt("dex", 14);
        entry.put("skills", skills);
        PlayerScoreStore loaded = loadEntry(entry);

        assertEquals(OptionalInt.of(14), loaded.getScore(PLAYER, Ability.DEXTERITY));
        assertEquals(OptionalInt.of(3), loaded.getSkillBonus(PLAYER, STEALTH));
        assertTrue(loaded.getProficiencyBonus(PLAYER).isEmpty());
        assertTrue(loaded.getSkillProficiency(PLAYER, STEALTH).isEmpty());
        assertTrue(loaded.getSaveProficiency(PLAYER, Ability.DEXTERITY).isEmpty());
    }

    @Test
    void loadSkipsUnknownLevelsAndExpertiseOnSaves() {
        CompoundTag skillLevels = new CompoundTag();
        skillLevels.putString(STEALTH.toString(), "master");
        skillLevels.putString(SAILING.toString(), "expertise");
        CompoundTag saveLevels = new CompoundTag();
        saveLevels.putString("dex", "expertise");
        saveLevels.putString("con", "proficient");
        CompoundTag entry = new CompoundTag();
        entry.putInt("proficiency", 42);
        entry.put("skill_proficiencies", skillLevels);
        entry.put("save_proficiencies", saveLevels);
        PlayerScoreStore loaded = loadEntry(entry);

        assertEquals(OptionalInt.of(Proficiencies.MAX_BONUS), loaded.getProficiencyBonus(PLAYER));
        assertTrue(loaded.getSkillProficiency(PLAYER, STEALTH).isEmpty());
        assertEquals(Optional.of(Proficiency.EXPERTISE), loaded.getSkillProficiency(PLAYER, SAILING));
        assertTrue(loaded.getSaveProficiency(PLAYER, Ability.DEXTERITY).isEmpty());
        assertEquals(Optional.of(Proficiency.PROFICIENT), loaded.getSaveProficiency(PLAYER, Ability.CONSTITUTION));
    }

    private static PlayerScoreStore roundTrip(PlayerScoreStore store) {
        return PlayerScoreStore.load(store.save(new CompoundTag(), null), null);
    }

    private static PlayerScoreStore loadEntry(CompoundTag entry) {
        CompoundTag players = new CompoundTag();
        players.put(PLAYER.toString(), entry);
        CompoundTag tag = new CompoundTag();
        tag.put("players", players);
        return PlayerScoreStore.load(tag, null);
    }
}
