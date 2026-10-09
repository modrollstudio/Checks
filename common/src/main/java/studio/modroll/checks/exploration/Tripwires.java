package studio.modroll.checks.exploration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import studio.modroll.checks.Checks;
import studio.modroll.checks.check.RollMessages;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.score.ScoresRuntime;
import studio.modroll.checks.text.FallbackText;

/**
 * Armed tripwires: strung between hooks and not disarmed. Perception, passively: while a player's passive
 * Perception meets the DC, the armed tripwires within range glint for that player alone, and a newly spotted
 * one is pointed out above the hotbar. Sleight of Hand: sneaking and using an armed tripwire with an empty
 * hand rolls through the API; a made check disarms it as shears do, a failed one snaps it and springs the
 * trap. Either way the string drops, so a tripwire is rolled for once. A wire that is not armed rolls
 * nothing.
 */
public final class Tripwires {

    private static final ResourceLocation PERCEPTION = Checks.id("perception");
    private static final ResourceLocation SLEIGHT_OF_HAND = Checks.id("sleight_of_hand");
    private static final double CENTER = 0.5;
    private static final double STRING_HEIGHT = 0.1;
    private static final int GLINTS = 3;
    private static final double GLINT_SPREAD = 0.3;
    private static final double GLINT_RISE = 0.02;
    private static final double GLINT_SPEED = 0;

    /** Per player, the tripwires they spotted on their last look. */
    private static final Map<UUID, Set<BlockPos>> SPOTTED = new HashMap<>();

    private Tripwires() {}

    /** Called once per server tick; looks around for every player once per interval. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount()
                        % ScoresRuntime.config().exploration().spotTripwires().intervalTicks()
                != 0) {
            return;
        }
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        players.forEach(Tripwires::lookAround);
        SPOTTED.keySet().retainAll(players.stream().map(ServerPlayer::getUUID).collect(Collectors.toSet()));
    }

    /** The armed tripwires around {@code player} glint for them while their passive Perception meets the DC. */
    public static void lookAround(ServerPlayer player) {
        Set<BlockPos> spotted =
                spotted(player, ScoresRuntime.config().exploration().spotTripwires());
        Set<BlockPos> before = SPOTTED.getOrDefault(player.getUUID(), Set.of());
        SPOTTED.put(player.getUUID(), spotted);
        spotted.forEach(wire -> glint(player, wire));
        if (!before.containsAll(spotted)) {
            RollMessages.showResult(player, FallbackText.oneOf("checks.exploration.spot_tripwire", player.getRandom()));
        }
    }

    public static InteractionResult onUseBlock(ServerPlayer player, InteractionHand hand, BlockPos pos) {
        ScoresConfig.DisarmTripwireSettings settings =
                ScoresRuntime.config().exploration().disarmTripwires();
        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (!settings.enabled()
                || hand != InteractionHand.MAIN_HAND
                || !ExplorationChecks.rollsFor(player)
                || !player.isShiftKeyDown()
                || !player.getMainHandItem().isEmpty()
                || !armed(state)
                || !ExplorationChecks.mayBreak(player, pos)) {
            return InteractionResult.PASS;
        }
        Optional<Boolean> made = ExplorationChecks.made(player, SLEIGHT_OF_HAND, settings.dc(), "disarm");
        if (made.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (made.get()) {
            disarm(level, pos, state, player);
        } else {
            level.destroyBlock(pos, true, player);
        }
        return InteractionResult.SUCCESS;
    }

    public static void clear() {
        SPOTTED.clear();
    }

    /** As shears do: marked disarmed first, so its hooks let go without firing. */
    private static void disarm(Level level, BlockPos pos, BlockState state, ServerPlayer player) {
        level.setBlock(pos, state.setValue(TripWireBlock.DISARMED, true), Block.UPDATE_INVISIBLE);
        level.destroyBlock(pos, true, player);
        // MC-129055: as its hooks let go, vanilla puts the disarmed string straight back; NeoForge patches that out.
        if (level.getBlockState(pos).getBlock() instanceof TripWireBlock) {
            level.removeBlock(pos, false);
        }
    }

    private static Set<BlockPos> spotted(ServerPlayer player, ScoresConfig.SpotTripwireSettings settings) {
        if (!settings.enabled() || player.isSpectator()) {
            return Set.of();
        }
        Set<BlockPos> armed = armedAround(player, settings.range());
        boolean seen = !armed.isEmpty() && ExplorationChecks.passive(player, PERCEPTION) >= settings.dc();
        return seen ? armed : Set.of();
    }

    private static Set<BlockPos> armedAround(ServerPlayer player, int range) {
        BlockPos center = player.blockPosition();
        Set<BlockPos> armed = new HashSet<>();
        for (BlockPos pos :
                BlockPos.betweenClosed(center.offset(-range, -range, -range), center.offset(range, range, range))) {
            if (armed(player.level().getBlockState(pos))) {
                armed.add(pos.immutable());
            }
        }
        return armed;
    }

    private static boolean armed(BlockState state) {
        return state.getBlock() instanceof TripWireBlock
                && state.getValue(TripWireBlock.ATTACHED)
                && !state.getValue(TripWireBlock.DISARMED);
    }

    private static void glint(ServerPlayer player, BlockPos wire) {
        player.serverLevel()
                .sendParticles(
                        player,
                        ParticleTypes.ELECTRIC_SPARK,
                        false,
                        wire.getX() + CENTER,
                        wire.getY() + STRING_HEIGHT,
                        wire.getZ() + CENTER,
                        GLINTS,
                        GLINT_SPREAD,
                        GLINT_RISE,
                        GLINT_SPREAD,
                        GLINT_SPEED);
    }
}
