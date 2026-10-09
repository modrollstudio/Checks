package studio.modroll.checks.creation;

import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.text.FallbackText;
import studio.modroll.critfall.api.dice.DieRoll;
import studio.modroll.critfall.api.dice.RollResult;

/**
 * The chat lines for creation rolls, e.g. {@code Steve rolled STR: 1, 1, 2, ~~1~~ = 4} with the dropped
 * die struck through. They are broadcast to every player, so every part carries its English fallback.
 */
public final class RollAnnouncement {

    private RollAnnouncement() {}

    public static List<Component> lines(Component playerName, CreationMethod method, List<RollResult> results) {
        return IntStream.range(0, results.size())
                .mapToObj(index -> line(playerName, label(method, index), results.get(index)))
                .toList();
    }

    public static MutableComponent dice(List<DieRoll> dice) {
        MutableComponent text = Component.empty();
        for (int i = 0; i < dice.size(); i++) {
            if (i > 0) {
                text.append(FallbackText.of("chat.checks.creation.separator"));
            }
            text.append(die(dice.get(i)));
        }
        return text;
    }

    private static Component line(Component playerName, Component label, RollResult result) {
        return FallbackText.of("chat.checks.creation.rolled", playerName, label, dice(result.dice()), result.total());
    }

    /** Hardcore rolls belong to an ability; free rolls are only numbered until the player assigns them. */
    private static Component label(CreationMethod method, int index) {
        if (method == CreationMethod.HARDCORE) {
            return SheetText.abilityAbbreviation(Ability.values()[index]);
        }
        return FallbackText.of("chat.checks.creation.roll_number", index + 1);
    }

    private static Component die(DieRoll die) {
        MutableComponent face = Component.literal(Integer.toString(die.value()));
        return die.kept() ? face : face.withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.GRAY);
    }
}
