package studio.modroll.checks.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.api.Ability;

class PresetGrantsTest {

    @Test
    void anAcceptedSubmissionGrantsItsBonusesTheBackgroundSkillsAndTheClassSaves() {
        CreationSubmission submission = new CreationSubmission(
                CreationMethod.STANDARD_ARRAY,
                List.of(15, 14, 13, 12, 10, 8),
                List.of(2, 0, 1, 0, 0, 0),
                List.of(Offers.STEALTH, Offers.PERCEPTION),
                new PresetChoices(
                        Optional.of(Offers.ELF),
                        Optional.of(Offers.SOLDIER),
                        Optional.of(Offers.FIGHTER),
                        Optional.empty()),
                List.of());

        PresetGrants grants = PresetGrants.of(submission, Offers.PRESETS);

        assertEquals(Map.of(Ability.STRENGTH, 2, Ability.CONSTITUTION, 1), grants.bonuses());
        assertEquals(Set.of(Offers.ATHLETICS, Offers.INTIMIDATION), grants.skills());
        assertEquals(Set.of(Ability.STRENGTH, Ability.CONSTITUTION), grants.saves());
    }

    @Test
    void customPresetsGrantNothing() {
        assertEquals(
                PresetGrants.NONE,
                PresetGrants.of(
                        Offers.custom(CreationMethod.POINT_BUY, List.of(8, 8, 8, 8, 8, 8), Offers.TWO_SKILLS),
                        Offers.PRESETS));
    }
}
