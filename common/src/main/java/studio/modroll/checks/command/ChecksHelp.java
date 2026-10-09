package studio.modroll.checks.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import studio.modroll.checks.text.FallbackText;

/**
 * {@code /checks help [command]}. The commands listed are read from the registered command tree and
 * filtered by each one's own permission check, so a source only ever sees what it may run. Every text
 * comes from the lang file: {@code commands.checks.help.<command>.<part>} for each of {@link #PARTS}.
 */
public final class ChecksHelp {

    public static final String NAME = "help";
    public static final List<String> PARTS = List.of("syntax", "description", "details", "example");
    private static final String KEY = "commands.checks.help.";
    private static final String COMMAND_ARGUMENT = "command";

    private static final DynamicCommandExceptionType UNKNOWN_COMMAND =
            new DynamicCommandExceptionType(name -> FallbackText.of(KEY + "unknown", name));

    private ChecksHelp() {}

    static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal(NAME)
                .executes(ChecksHelp::list)
                .then(Commands.argument(COMMAND_ARGUMENT, StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(usable(ctx), builder))
                        .executes(ChecksHelp::describe));
    }

    public static String key(String command, String part) {
        return KEY + command + "." + part;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        List<String> commands = usable(ctx);
        reply(ctx, FallbackText.of(KEY + "header"));
        for (String command : commands) {
            reply(ctx, FallbackText.of(KEY + "entry", syntax(command), text(command, "description")));
        }
        return commands.size();
    }

    /** A command the source may not run is unknown to it, as in the list. */
    private static int describe(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String command = StringArgumentType.getString(ctx, COMMAND_ARGUMENT);
        if (!usable(ctx).contains(command)) {
            throw UNKNOWN_COMMAND.create(command);
        }
        reply(ctx, syntax(command));
        reply(ctx, text(command, "details"));
        reply(ctx, FallbackText.of(KEY + "example", text(command, "example")));
        return Command.SINGLE_SUCCESS;
    }

    /** The {@code /checks} subcommands the source may run, in registration order. */
    private static List<String> usable(CommandContext<CommandSourceStack> ctx) {
        return ctx.getRootNode().getChild(ChecksCommands.ROOT).getChildren().stream()
                .filter(node -> node.canUse(ctx.getSource()))
                .map(CommandNode::getName)
                .toList();
    }

    private static Component syntax(String command) {
        return text(command, "syntax").withStyle(ChatFormatting.GOLD);
    }

    private static MutableComponent text(String command, String part) {
        return FallbackText.of(key(command, part));
    }

    private static void reply(CommandContext<CommandSourceStack> ctx, Component line) {
        ctx.getSource().sendSuccess(() -> line, false);
    }
}
