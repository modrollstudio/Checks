package studio.modroll.checks.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.Checks;
import studio.modroll.checks.ability.Abilities;
import studio.modroll.checks.api.Ability;
import studio.modroll.checks.api.CheckRoll;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.OpenRoll;
import studio.modroll.checks.api.Proficiency;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.api.Stat;
import studio.modroll.checks.bonus.BonusSources;
import studio.modroll.checks.check.RollText;
import studio.modroll.checks.check.StatIds;
import studio.modroll.checks.creation.CharacterCreation;
import studio.modroll.checks.data.SkillStore;
import studio.modroll.checks.level.Levelling;
import studio.modroll.checks.level.Levels;
import studio.modroll.checks.proficiency.Proficiencies;
import studio.modroll.checks.score.PlayerScoreStore;
import studio.modroll.checks.score.ScoreService;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.skill.Skills;
import studio.modroll.checks.text.FallbackText;
import studio.modroll.critfall.api.combat.SaveResult;
import studio.modroll.critfall.api.dice.RollDetail;
import studio.modroll.critfall.api.dice.RollResult;

/** {@code /checks get|set|prof|roll|check|reset|level|xp|help}; see {@code docs/scores-config.md#commands}. */
public final class ChecksCommands {

    public static final String ROOT = "checks";
    private static final int OP_PERMISSION_LEVEL = 2;

    private static final SuggestionProvider<CommandSourceStack> STAT_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(statIds(), builder);
    private static final DynamicCommandExceptionType UNKNOWN_STAT =
            new DynamicCommandExceptionType(id -> FallbackText.of("commands.checks.error.unknown_stat", id));
    private static final Dynamic3CommandExceptionType OUT_OF_RANGE = new Dynamic3CommandExceptionType(
            (what, min, max) -> FallbackText.of("commands.checks.error.out_of_range", what, min, max));
    private static final SimpleCommandExceptionType NO_SCORES =
            new SimpleCommandExceptionType(FallbackText.of("commands.checks.error.no_scores"));
    private static final SuggestionProvider<CommandSourceStack> PROFICIENCY_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Proficiency.values()).map(Proficiency::id), builder);
    private static final DynamicCommandExceptionType UNKNOWN_PROFICIENCY =
            new DynamicCommandExceptionType(id -> FallbackText.of("commands.checks.error.unknown_proficiency", id));
    private static final SimpleCommandExceptionType EXPERTISE_ON_SAVE =
            new SimpleCommandExceptionType(FallbackText.of("commands.checks.error.expertise_on_save"));
    private static final SimpleCommandExceptionType LEVELLING_DISABLED =
            new SimpleCommandExceptionType(FallbackText.of("commands.checks.error.levelling_disabled"));

    private ChecksCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ROOT)
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ChecksCommands::getAll)
                                .then(statArgument().executes(ChecksCommands::getOne))))
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(OP_PERMISSION_LEVEL))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("proficiency")
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .executes(ChecksCommands::setProficiencyBonus)))
                                .then(statArgument()
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .executes(ChecksCommands::set)))))
                .then(Commands.literal("prof")
                        .requires(source -> source.hasPermission(OP_PERMISSION_LEVEL))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(statArgument()
                                        .then(Commands.argument("level", StringArgumentType.word())
                                                .suggests(PROFICIENCY_SUGGESTIONS)
                                                .executes(ChecksCommands::setProficiency)))))
                .then(Commands.literal("roll")
                        .requires(source -> source.hasPermission(OP_PERMISSION_LEVEL))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(statArgument()
                                        .executes(ChecksCommands::roll)
                                        .then(Commands.argument("dc", IntegerArgumentType.integer())
                                                .executes(ChecksCommands::rollAgainstDc)))))
                .then(Commands.literal("check")
                        .requires(source -> ScoresRuntime.config().extensions().checkCommand()
                                && source.hasPermission(OP_PERMISSION_LEVEL))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(statArgument()
                                        .then(Commands.argument("dc", IntegerArgumentType.integer())
                                                .executes(ChecksCommands::check)))))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(OP_PERMISSION_LEVEL))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ChecksCommands::resetCharacter)))
                .then(Commands.literal("level")
                        .requires(source -> source.hasPermission(OP_PERMISSION_LEVEL))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("set")
                                        .then(Commands.argument("level", IntegerArgumentType.integer())
                                                .executes(ChecksCommands::setLevel)))))
                .then(Commands.literal("xp")
                        .requires(source -> source.hasPermission(OP_PERMISSION_LEVEL))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("add")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ChecksCommands::addXp)))))
                .then(ChecksHelp.command()));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> statArgument() {
        return Commands.argument("stat", ResourceLocationArgument.id()).suggests(STAT_SUGGESTIONS);
    }

    private static int getAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        LivingEntity target = livingTarget(ctx);
        List<Component> abilities = Arrays.stream(Ability.values())
                .map(ability -> describe(ability, ScoreService.abilityScore(target, ability)))
                .toList();
        reply(ctx, FallbackText.of("commands.checks.get.entity", target.getName(), joined(abilities)));

        List<Component> saves = Arrays.stream(Ability.values())
                .<Component>map(ability ->
                        FallbackText.of("commands.checks.get.save", ability.id(), describeSave(target, ability)))
                .toList();
        reply(
                ctx,
                FallbackText.of(
                        "commands.checks.get.saves",
                        SheetText.signed(ScoreService.proficiencyBonus(target)),
                        joined(saves)));

        List<Skill> skills = sortedSkills();
        if (!skills.isEmpty()) {
            List<Component> skillParts =
                    skills.stream().map(skill -> describe(target, skill)).toList();
            reply(ctx, FallbackText.of("commands.checks.get.skills", joined(skillParts)));
        }
        return Ability.values().length + skills.size();
    }

    private static int getOne(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        LivingEntity target = livingTarget(ctx);
        return switch (stat(ctx)) {
            case Ability ability -> {
                int score = ScoreService.abilityScore(target, ability);
                reply(
                        ctx,
                        FallbackText.of(
                                "commands.checks.get.ability_with_save",
                                target.getName(),
                                describe(ability, score),
                                describeSave(target, ability)));
                yield score;
            }
            case Skill skill -> {
                reply(ctx, FallbackText.of("commands.checks.get.entity", target.getName(), describe(target, skill)));
                yield BonusSources.skillModifier(target, skill);
            }
        };
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int value = IntegerArgumentType.getInteger(ctx, "value");
        PlayerScoreStore store = PlayerScoreStore.get(ctx.getSource().getServer());
        Component what =
                switch (stat(ctx)) {
                    case Ability ability -> {
                        requireInRange(
                                value,
                                Abilities.MIN_SCORE,
                                Abilities.MAX_SCORE,
                                "commands.checks.range.ability_scores");
                        store.setScore(player.getUUID(), ability, value);
                        yield Component.literal(ability.id());
                    }
                    case Skill skill -> {
                        requireInRange(
                                value, Skills.MIN_BONUS, Skills.MAX_BONUS, "commands.checks.range.skill_bonuses");
                        store.setSkillBonus(player.getUUID(), skill.id(), value);
                        yield FallbackText.of("commands.checks.set.skill_bonus", displayId(skill));
                    }
                };
        announceSet(ctx, player, what, Integer.toString(value));
        return value;
    }

    private static int setProficiencyBonus(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int value = IntegerArgumentType.getInteger(ctx, "value");
        requireInRange(
                value, Proficiencies.MIN_BONUS, Proficiencies.MAX_BONUS, "commands.checks.range.proficiency_bonuses");
        PlayerScoreStore.get(ctx.getSource().getServer()).setProficiencyBonus(player.getUUID(), value);
        announceSet(ctx, player, FallbackText.of("commands.checks.set.proficiency_bonus"), Integer.toString(value));
        return value;
    }

    private static int setProficiency(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Stat stat = stat(ctx);
        Proficiency level = proficiency(StringArgumentType.getString(ctx, "level"));
        PlayerScoreStore store = PlayerScoreStore.get(ctx.getSource().getServer());
        Component what =
                switch (stat) {
                    case Ability ability -> {
                        if (!Proficiencies.allowedOnSave(level)) {
                            throw EXPERTISE_ON_SAVE.create();
                        }
                        store.setSaveProficiency(player.getUUID(), ability, level);
                        yield FallbackText.of("commands.checks.set.save", ability.id());
                    }
                    case Skill skill -> {
                        store.setSkillProficiency(player.getUUID(), skill.id(), level);
                        yield Component.literal(displayId(skill));
                    }
                };
        announceSet(ctx, player, what, level.id());
        return Command.SINGLE_SUCCESS;
    }

    private static int roll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        LivingEntity target = livingTarget(ctx);
        Stat stat = stat(ctx);
        OpenRoll roll = ChecksApi.roll(target, stat);
        RollResult result = roll.result();
        if (roll.canceled()) {
            reply(ctx, canceled(target, stat));
            return result.total();
        }
        reply(
                ctx,
                FallbackText.of(
                        "commands.checks.roll.result",
                        target.getName(),
                        statName(stat),
                        RollText.d20(RollDetail.of(roll.mode(), result)),
                        SheetText.signed(result.modifier()),
                        result.total()));
        return result.total();
    }

    private static int rollAgainstDc(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        LivingEntity target = livingTarget(ctx);
        Stat stat = stat(ctx);
        int dc = IntegerArgumentType.getInteger(ctx, "dc");
        CheckRoll roll = ChecksApi.check(target, stat, dc);
        reply(ctx, describeCheck(target, stat, roll));
        return roll.result().saveTotal();
    }

    /**
     * Returns 1 on success. A failed check fails the command, since {@code execute store success} counts
     * any command that does not fail as a success, whatever it returns.
     */
    private static int check(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        LivingEntity target = livingTarget(ctx);
        Stat stat = stat(ctx);
        CheckRoll roll = ChecksApi.check(target, stat, IntegerArgumentType.getInteger(ctx, "dc"));
        Component line = describeCheck(target, stat, roll);
        if (!roll.result().saved()) {
            throw new SimpleCommandExceptionType(line).create();
        }
        reply(ctx, line);
        return Command.SINGLE_SUCCESS;
    }

    private static Component describeCheck(LivingEntity target, Stat stat, CheckRoll roll) {
        if (roll.canceled()) {
            return canceled(target, stat);
        }
        SaveResult result = roll.result();
        return FallbackText.of(
                result.saved() ? "commands.checks.roll.success" : "commands.checks.roll.failure",
                target.getName(),
                statName(stat),
                result.dc(),
                RollText.d20(result.roll()),
                SheetText.signed(result.saveBonus()),
                result.saveTotal());
    }

    private static Component canceled(LivingEntity target, Stat stat) {
        return FallbackText.of("commands.checks.roll.canceled", target.getName(), statName(stat));
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int level = IntegerArgumentType.getInteger(ctx, "level");
        requireLevelling();
        requireInRange(level, Levels.MIN_LEVEL, Levels.MAX_LEVEL, "commands.checks.range.levels");
        Levelling.setLevel(player, level);
        announceSet(ctx, player, FallbackText.of("commands.checks.set.level"), Integer.toString(level));
        return level;
    }

    private static int addXp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        requireLevelling();
        Levelling.gainXp(player, amount);
        int xp = ChecksApi.experience(player);
        int level = ChecksApi.level(player);
        ctx.getSource()
                .sendSuccess(
                        () -> FallbackText.of("commands.checks.xp.success", player.getName(), amount, xp, level), true);
        return xp;
    }

    private static void requireLevelling() throws CommandSyntaxException {
        if (!Levelling.enabled()) {
            throw LEVELLING_DISABLED.create();
        }
    }

    private static int resetCharacter(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        CharacterCreation.reset(ctx.getSource().getServer(), player.getUUID());
        Levelling.reset(ctx.getSource().getServer(), player.getUUID());
        ctx.getSource()
                .sendSuccess(() -> FallbackText.of("commands.checks.reset.success", player.getDisplayName()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static void reply(CommandContext<CommandSourceStack> ctx, Component line) {
        ctx.getSource().sendSuccess(() -> line, false);
    }

    private static void announceSet(
            CommandContext<CommandSourceStack> ctx, ServerPlayer player, Component what, String value) {
        ctx.getSource()
                .sendSuccess(() -> FallbackText.of("commands.checks.set.success", player.getName(), what, value), true);
    }

    private static String statName(Stat stat) {
        return switch (stat) {
            case Ability ability -> ability.id();
            case Skill skill -> displayId(skill);
        };
    }

    private static LivingEntity livingTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(ctx, "target");
        if (target instanceof LivingEntity living) {
            return living;
        }
        throw NO_SCORES.create();
    }

    private static Stat stat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "stat");
        return StatIds.find(id, SkillStore::find).orElseThrow(() -> UNKNOWN_STAT.create(id));
    }

    private static Proficiency proficiency(String id) throws CommandSyntaxException {
        return Proficiency.byId(id).orElseThrow(() -> UNKNOWN_PROFICIENCY.create(id));
    }

    private static void requireInRange(int value, int min, int max, String whatKey) throws CommandSyntaxException {
        if (value < min || value > max) {
            throw OUT_OF_RANGE.create(FallbackText.of(whatKey), min, max);
        }
    }

    private static Stream<String> statIds() {
        return Stream.concat(
                Arrays.stream(Ability.values()).map(Ability::id),
                sortedSkills().stream().map(ChecksCommands::displayId));
    }

    private static List<Skill> sortedSkills() {
        return SkillStore.skills().values().stream()
                .sorted(Comparator.comparing(Skill::id))
                .toList();
    }

    private static String displayId(Skill skill) {
        ResourceLocation id = skill.id();
        return id.getNamespace().equals(Checks.MOD_ID) ? id.getPath() : id.toString();
    }

    private static Component describe(Ability ability, int score) {
        return FallbackText.of(
                "commands.checks.get.ability", ability.id(), score, SheetText.signed(Abilities.modifier(score)));
    }

    /**
     * Proficiency levels are shown by their command ids ({@code proficient}, {@code expertise}). Like the
     * stat screen, the modifier and passive score include bonus sources.
     */
    private static Component describe(LivingEntity target, Skill skill) {
        Proficiency proficiency = ScoreService.skillProficiency(target, skill);
        String modifier = SheetText.signed(BonusSources.skillModifier(target, skill));
        int passive = BonusSources.passiveScore(target, skill);
        if (proficiency == Proficiency.NONE) {
            return FallbackText.of(
                    "commands.checks.get.skill",
                    displayId(skill),
                    modifier,
                    skill.ability().id(),
                    passive);
        }
        return FallbackText.of(
                "commands.checks.get.skill.proficient",
                displayId(skill),
                modifier,
                skill.ability().id(),
                proficiency.id(),
                passive);
    }

    private static Component describeSave(LivingEntity target, Ability ability) {
        Proficiency proficiency = ScoreService.saveProficiency(target, ability);
        String modifier = SheetText.signed(BonusSources.saveModifier(target, ability));
        if (proficiency == Proficiency.NONE) {
            return Component.literal(modifier);
        }
        return FallbackText.of("commands.checks.get.save_modifier.proficient", modifier, proficiency.id());
    }

    private static Component joined(List<Component> parts) {
        return ComponentUtils.formatList(parts, FallbackText.of("commands.checks.list.separator"));
    }
}
