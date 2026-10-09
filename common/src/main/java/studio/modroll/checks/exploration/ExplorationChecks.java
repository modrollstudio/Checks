package studio.modroll.checks.exploration;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import studio.modroll.checks.api.CheckRoll;
import studio.modroll.checks.api.ChecksApi;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.check.RollMessages;
import studio.modroll.checks.check.RollText;
import studio.modroll.checks.text.FallbackText;
import studio.modroll.critfall.api.combat.SaveResult;

/** A skill check for an exploration use, rolled through {@link ChecksApi} and shown above the hotbar with a flavor line. */
final class ExplorationChecks {

    private ExplorationChecks() {}

    /** Players in survival or adventure; creative and spectator players never roll. */
    static boolean rollsFor(ServerPlayer player) {
        return !player.isCreative() && !player.isSpectator();
    }

    /** Whether the player may break the block there: not in adventure mode, nor under spawn protection. */
    static boolean mayBreak(ServerPlayer player, BlockPos pos) {
        return !player.blockActionRestricted(player.level(), pos, player.gameMode.getGameModeForPlayer())
                && player.level().mayInteract(player, pos);
    }

    /**
     * Whether the check against {@code dc} was made; empty when the skill is not loaded or a listener
     * canceled the roll. The flavor line comes from {@code checks.exploration.<use>.success} or {@code .failure}.
     */
    static Optional<Boolean> made(ServerPlayer player, ResourceLocation skillId, int dc, String use) {
        return made(player, skillId, dc, made -> flavor(player, use, made));
    }

    /** As {@link #made(ServerPlayer, ResourceLocation, int, String)}, with the line below the roll from {@code detail}. */
    static Optional<Boolean> made(
            ServerPlayer player, ResourceLocation skillId, int dc, Function<Boolean, Component> detail) {
        Optional<Skill> skill = ChecksApi.skill(skillId);
        if (skill.isEmpty()) {
            return Optional.empty();
        }
        CheckRoll roll = ChecksApi.check(player, skill.get(), dc);
        if (roll.canceled()) {
            return Optional.empty();
        }
        SaveResult result = roll.result();
        RollMessages.showResult(player, RollText.check(skill.get(), result), Optional.of(detail.apply(result.saved())));
        return Optional.of(result.saved());
    }

    /** A line from {@code checks.exploration.<use>.success} or {@code .failure}. */
    static Component flavor(ServerPlayer player, String use, boolean made) {
        return FallbackText.oneOf(
                "checks.exploration." + use + "." + (made ? "success" : "failure"), player.getRandom());
    }

    /** The skill modifier (ability, proficiency, flat skill bonus); 0 when the skill is not loaded. */
    static int modifier(LivingEntity entity, ResourceLocation skillId) {
        return ChecksApi.skill(skillId)
                .map(skill -> ChecksApi.skillModifier(entity, skill))
                .orElse(0);
    }

    /** {@code perPoint} per point of a positive modifier, at most {@code max}; 0 unless the modifier is positive. */
    static double share(int modifier, double perPoint, double max) {
        return Math.min(max, Math.max(0, modifier) * perPoint);
    }

    /** {@code 10 +} the skill modifier and bonus sources; 0 when the skill is not loaded. */
    static int passive(LivingEntity entity, ResourceLocation skillId) {
        return ChecksApi.skill(skillId)
                .map(skill -> ChecksApi.passiveScore(entity, skill))
                .orElse(0);
    }
}
