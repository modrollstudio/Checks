package studio.modroll.checks.social;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import studio.modroll.checks.text.FallbackText;

/** Lang-file text for the social menu and its action bar messages. */
public final class SocialText {

    private static final int TICKS_PER_SECOND = 20;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final String WANDERING_TRADER = SocialTarget.WANDERING_TRADER.id();

    private SocialText() {}

    /**
     * The flavor pool for an outcome ({@code result}, such as {@code success} or {@code piglin.failure}):
     * a wandering trader has pools of his own, {@code checks.social.<action>.wandering_trader.<result>},
     * and falls back to the generic one where the lang file has none.
     */
    static String flavorPool(SocialAction action, Optional<SocialTarget> target, String result) {
        String generic = "checks.social." + action.id() + "." + result;
        String own = "checks.social." + action.id() + "." + WANDERING_TRADER + "." + result;
        boolean hasOwn =
                target.filter(kind -> kind == SocialTarget.WANDERING_TRADER).isPresent()
                        && FallbackText.poolSize(own) > 0;
        return hasOwn ? own : generic;
    }

    /**
     * The menu's title around the target's name: "Face" while Intimidate fits the mob, else "Calm" while
     * Calm does, else "Talk to".
     */
    public static Component title(SocialMenu menu, Component name) {
        return FallbackText.of(titleKey(menu), name);
    }

    static String titleKey(SocialMenu menu) {
        if (fits(menu, SocialAction.INTIMIDATE_MOB)) {
            return "screen.checks.social.title.face";
        }
        if (fits(menu, SocialAction.CALM)) {
            return "screen.checks.social.title.calm";
        }
        return "screen.checks.social.title.talk";
    }

    private static boolean fits(SocialMenu menu, SocialAction action) {
        return menu.options().stream()
                .anyMatch(option ->
                        option.action() == action && option.availability() != SocialMenu.Availability.WRONG_TARGET);
    }

    public static Component actionName(SocialAction action) {
        return FallbackText.of("checks.social.action." + action.id());
    }

    /** Why an option is greyed out; empty text for an available one. */
    public static Component unavailable(SocialMenu.Option option) {
        return switch (option.availability()) {
            case AVAILABLE -> Component.empty();
            case WRONG_TARGET ->
                FallbackText.of("checks.social.unavailable.wrong_target."
                        + option.action().id());
            case NO_TRADES -> FallbackText.of("checks.social.unavailable.no_trades");
            case COOLDOWN -> FallbackText.of("checks.social.unavailable.cooldown", duration(option.cooldownTicks()));
            case ON_GUARD -> FallbackText.of("checks.social.unavailable.on_guard");
            case TRIED ->
                FallbackText.of(
                        "checks.social.unavailable.tried." + option.action().id());
            case FEARLESS -> FallbackText.of("checks.social.unavailable.fearless");
            case NOT_ANGRY -> FallbackText.of("checks.social.unavailable.not_angry");
        };
    }

    /** Whole seconds under a minute, else whole minutes; both rounded up so a cooldown never reads 0. */
    static Component duration(long ticks) {
        long seconds = Math.ceilDiv(ticks, TICKS_PER_SECOND);
        if (seconds < SECONDS_PER_MINUTE) {
            return FallbackText.of("checks.social.duration.seconds", seconds);
        }
        return FallbackText.of("checks.social.duration.minutes", Math.ceilDiv(seconds, SECONDS_PER_MINUTE));
    }
}
