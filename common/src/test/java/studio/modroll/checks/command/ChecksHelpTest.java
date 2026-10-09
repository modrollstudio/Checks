package studio.modroll.checks.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.ModLang;

/** Runs {@code /checks help} on the real command tree, as a player (permission 0) and as an op (2). */
class ChecksHelpTest {

    private static final int PLAYER = 0;
    private static final int OP = 2;

    private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
    private final List<Component> messages = new ArrayList<>();
    private final List<String> output = new ArrayList<>();
    private final Language before = Language.getInstance();

    @BeforeEach
    void setUp() throws IOException {
        ModLang.inject();
        ChecksCommands.register(dispatcher);
    }

    @AfterEach
    void restoreLang() {
        Language.inject(before);
    }

    @Test
    void everyRegisteredSubcommandHasEveryHelpEntry() {
        CommandNode<CommandSourceStack> checks = dispatcher.getRoot().getChild(ChecksCommands.ROOT);
        for (CommandNode<CommandSourceStack> subcommand : checks.getChildren()) {
            for (String part : ChecksHelp.PARTS) {
                String key = ChecksHelp.key(subcommand.getName(), part);
                assertTrue(Language.getInstance().has(key), "/checks " + subcommand.getName() + " has no " + key);
            }
        }
    }

    /** Minecraft's font draws "|" much like "l", so options are split with "/" instead. */
    @Test
    void noUsageSplitsItsOptionsWithAPipe() {
        CommandNode<CommandSourceStack> checks = dispatcher.getRoot().getChild(ChecksCommands.ROOT);
        for (CommandNode<CommandSourceStack> subcommand : checks.getChildren()) {
            for (String part : ChecksHelp.PARTS) {
                String text = Language.getInstance().getOrDefault(ChecksHelp.key(subcommand.getName(), part));
                assertFalse(text.contains("|"), ChecksHelp.key(subcommand.getName(), part) + ": " + text);
            }
        }
    }

    @Test
    void playersSeeOnlyTheCommandsTheyMayRun() throws CommandSyntaxException {
        assertEquals(2, run("checks help", PLAYER));
        assertEquals(
                List.of(
                        "Checks commands (/checks help <command> for details):",
                        "/checks get <target> [ability/skill]",
                        "  Shows an entity's scores, proficiency, saves and skills.",
                        "/checks help [command]",
                        "  Lists the /checks commands you can use."),
                output);
    }

    @Test
    void opsSeeEveryCommandInRegistrationOrder() throws CommandSyntaxException {
        assertEquals(9, run("checks help", OP));
        List<String> commands = new ArrayList<>();
        for (int line = 1; line < output.size(); line += 2) {
            commands.add(output.get(line).split(" ")[1]);
        }
        assertEquals(List.of("get", "set", "prof", "roll", "check", "reset", "level", "xp", "help"), commands);
    }

    /** The usage in gold, its description indented in gray; clicking either suggests the command. */
    @Test
    void eachEntryIsAGoldUsageOverAGrayDescriptionThatSuggestsTheCommand() throws CommandSyntaxException {
        run("checks help", OP);
        Style usage = messages.get(1).getStyle();
        Style description = messages.get(2).getStyle();
        ClickEvent suggest = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/checks get ");

        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD), usage.getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GRAY), description.getColor());
        assertEquals(suggest, usage.getClickEvent());
        assertEquals(suggest, description.getClickEvent());
    }

    @Test
    void helpForOneCommandShowsItsDetailsAndAnExample() throws CommandSyntaxException {
        run("checks help roll", OP);
        assertEquals(
                List.of(
                        "/checks roll <target> <ability/skill> [dc]",
                        "Rolls d20 plus the target's modifier for that ability or skill and prints the roll. With a"
                                + " DC it also says whether the check succeeds; meeting the DC succeeds.",
                        "Example: /checks roll @s stealth 15"),
                output);
    }

    @Test
    void helpRefusesCommandsTheSourceMayNotRunAndUnknownOnes() {
        assertThrows(CommandSyntaxException.class, () -> run("checks help set", PLAYER));
        assertThrows(CommandSyntaxException.class, () -> run("checks help teleport", OP));
        assertEquals(List.of(), output);
    }

    private int run(String command, int permission) throws CommandSyntaxException {
        return dispatcher.execute(command, source(permission));
    }

    /** No level, server or entity: help only reads the command tree and replies to its source. */
    private CommandSourceStack source(int permission) {
        CommandSource capture = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                messages.add(message);
                output.add(message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        return new CommandSourceStack(
                capture, Vec3.ZERO, Vec2.ZERO, null, permission, "test", Component.literal("test"), null, null);
    }
}
