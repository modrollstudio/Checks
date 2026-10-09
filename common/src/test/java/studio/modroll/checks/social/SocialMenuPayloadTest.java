package studio.modroll.checks.social;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.jupiter.api.Test;
import studio.modroll.critfall.api.dice.RollMode;

class SocialMenuPayloadTest {

    @Test
    void menuSurvivesTheWire() {
        SocialMenu menu = new SocialMenu(
                42,
                List.of(
                        option(SocialAction.PERSUADE, false, false, SocialMenu.Availability.AVAILABLE, 0),
                        option(SocialAction.PICKPOCKET, true, false, SocialMenu.Availability.COOLDOWN, 6000),
                        option(SocialAction.INTIMIDATE, false, false, SocialMenu.Availability.WRONG_TARGET, 0),
                        option(SocialAction.PICKPOCKET, false, true, SocialMenu.Availability.ON_GUARD, 0),
                        option(SocialAction.LIE, false, false, SocialMenu.Availability.TRIED, 0)));
        ByteBuf buffer = Unpooled.buffer();
        SocialMenuPayload.STREAM_CODEC.encode(buffer, new SocialMenuPayload(menu));

        assertEquals(menu, SocialMenuPayload.STREAM_CODEC.decode(buffer).menu());
    }

    @Test
    void actionSurvivesTheWire() {
        ByteBuf buffer = Unpooled.buffer();
        SocialActionPayload sent = new SocialActionPayload(7, SocialAction.DECEIVE);
        SocialActionPayload.STREAM_CODEC.encode(buffer, sent);

        assertEquals(sent, SocialActionPayload.STREAM_CODEC.decode(buffer));
    }

    @Test
    void advantageAndDisadvantageCancelOut() {
        SocialMenu.Availability available = SocialMenu.Availability.AVAILABLE;
        assertEquals(
                RollMode.ADVANTAGE,
                option(SocialAction.PICKPOCKET, true, false, available, 0).mode());
        assertEquals(
                RollMode.DISADVANTAGE,
                option(SocialAction.PICKPOCKET, false, true, available, 0).mode());
        assertEquals(
                RollMode.NORMAL,
                option(SocialAction.PICKPOCKET, true, true, available, 0).mode());
        assertEquals(
                RollMode.NORMAL,
                option(SocialAction.PICKPOCKET, false, false, available, 0).mode());
    }

    @Test
    void aMenuOfOnlyWrongTargetsIsNotWorthOpening() {
        SocialMenu menu = new SocialMenu(
                1, List.of(option(SocialAction.PICKPOCKET, false, false, SocialMenu.Availability.WRONG_TARGET, 0)));
        assertFalse(menu.worthOpening());
    }

    private static SocialMenu.Option option(
            SocialAction action,
            boolean advantage,
            boolean disadvantage,
            SocialMenu.Availability availability,
            long cooldownTicks) {
        return new SocialMenu.Option(action, 0, advantage, disadvantage, availability, cooldownTicks);
    }
}
