package studio.modroll.checks.creation;

import java.util.Optional;

/**
 * One player's creation progress: whether the screen was already offered on join, the dice they
 * rolled, and the confirmed build, which locks creation.
 */
public record PlayerCharacter(boolean prompted, Optional<CharacterRolls> rolls, Optional<CharacterBuild> build) {

    public static final PlayerCharacter NONE = new PlayerCharacter(false, Optional.empty(), Optional.empty());

    public boolean created() {
        return build.isPresent();
    }

    public PlayerCharacter withPrompted() {
        return new PlayerCharacter(true, rolls, build);
    }

    public PlayerCharacter withRolls(CharacterRolls newRolls) {
        return new PlayerCharacter(prompted, Optional.of(newRolls), build);
    }

    public PlayerCharacter withBuild(CharacterBuild newBuild) {
        return new PlayerCharacter(prompted, rolls, Optional.of(newBuild));
    }
}
