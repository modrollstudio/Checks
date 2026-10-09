package studio.modroll.checks.social;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import studio.modroll.critfall.api.dice.RollMode;

/** The social actions offered for one entity, built on the server; the client only draws it. */
public record SocialMenu(int entityId, List<Option> options) {

    private static final int MAX_OPTIONS = SocialAction.values().length;

    public static final StreamCodec<ByteBuf, SocialMenu> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SocialMenu::entityId,
            Option.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_OPTIONS)),
            SocialMenu::options,
            SocialMenu::new);

    public SocialMenu {
        options = List.copyOf(options);
    }

    /** Why an action cannot be tried right now. */
    public enum Availability {
        AVAILABLE,
        WRONG_TARGET,
        NO_TRADES,
        COOLDOWN,
        /** The villager is on guard: Pickpocket and Intimidate are out. */
        ON_GUARD,
        /** Plead or Lie, already tried during this alarm. */
        TRIED,
        /** A mob too strong to scare: too much health, a boss, or excluded. */
        FEARLESS,
        /** Calm on a mob that is not angry at the player. */
        NOT_ANGRY;

        public static final StreamCodec<ByteBuf, Availability> STREAM_CODEC =
                ByteBufCodecs.idMapper(index -> values()[index], Availability::ordinal);
    }

    /**
     * One action: its skill modifier with the player's bonus sources, whether it would roll with advantage
     * or disadvantage, and its availability, with the ticks left while it cools down.
     */
    public record Option(
            SocialAction action,
            int modifier,
            boolean advantage,
            boolean disadvantage,
            Availability availability,
            long cooldownTicks) {

        static final StreamCodec<ByteBuf, SocialAction> ACTION_CODEC =
                ByteBufCodecs.idMapper(index -> SocialAction.values()[index], SocialAction::ordinal);

        public static final StreamCodec<ByteBuf, Option> STREAM_CODEC = StreamCodec.composite(
                ACTION_CODEC,
                Option::action,
                ByteBufCodecs.VAR_INT,
                Option::modifier,
                ByteBufCodecs.BOOL,
                Option::advantage,
                ByteBufCodecs.BOOL,
                Option::disadvantage,
                Availability.STREAM_CODEC,
                Option::availability,
                ByteBufCodecs.VAR_LONG,
                Option::cooldownTicks,
                Option::new);

        public boolean available() {
            return availability == Availability.AVAILABLE;
        }

        /** Advantage and disadvantage cancel out, as in 5e. */
        public RollMode mode() {
            if (advantage == disadvantage) {
                return RollMode.NORMAL;
            }
            return advantage ? RollMode.ADVANTAGE : RollMode.DISADVANTAGE;
        }
    }

    public boolean worthOpening() {
        return options.stream().anyMatch(option -> option.availability() != Availability.WRONG_TARGET);
    }
}
