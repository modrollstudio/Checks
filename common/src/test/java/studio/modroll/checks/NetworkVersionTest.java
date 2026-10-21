package studio.modroll.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NetworkVersionTest {

    private static final ResourceLocation STAT_SHEET = Checks.id("stat_sheet");
    private static final ResourceLocation OTHER_MOD = ResourceLocation.parse("othermod:sync");

    private final Language before = Language.getInstance();

    @BeforeEach
    void useALanguageWithoutChecks() {
        ModLang.injectWithoutChecks();
    }

    @AfterEach
    void restoreLang() {
        Language.inject(before);
    }

    @Test
    void theSameVersionJoins() {
        assertEquals(
                Optional.empty(),
                NetworkVersion.refusal(
                        new NetworkVersionPayload(2, "0.1.3"), new NetworkVersionPayload(2, "0.1.3-dev")));
    }

    @Test
    void anotherVersionIsTurnedAwayNamingBothChecksVersions() {
        Optional<Component> refusal =
                NetworkVersion.refusal(new NetworkVersionPayload(2, "0.1.3"), new NetworkVersionPayload(3, "0.1.4"));

        assertEquals(
                "This server runs Critfall: Checks 0.1.3, and your Checks 0.1.4 can't talk to it."
                        + " Install Checks 0.1.3 to join.",
                refusal.orElseThrow().getString());
    }

    @Test
    void aChecksWithoutTheVersionChannelIsFromBeforeTheCheck() {
        assertTrue(NetworkVersion.fromBeforeTheCheck(List.of(OTHER_MOD, STAT_SHEET)));
    }

    @Test
    void aChecksWithTheVersionChannelIsNot() {
        assertFalse(NetworkVersion.fromBeforeTheCheck(List.of(STAT_SHEET, NetworkVersionPayload.TYPE.id())));
    }

    @Test
    void aSideWithoutChecksIsLeftAlone() {
        assertFalse(NetworkVersion.fromBeforeTheCheck(List.of(OTHER_MOD)));
        assertFalse(NetworkVersion.fromBeforeTheCheck(List.of()));
    }

    @Test
    void theOlderSideMessagesNameAVersionToInstall() {
        assertEquals(
                "This server runs Critfall: Checks 0.1.3, and your Checks is older. Install Checks 0.1.3 to join.",
                NetworkVersion.olderClient("0.1.3").getString());
        assertEquals(
                "This server runs an older Critfall: Checks than your 0.1.3. Install the server's Checks version to"
                        + " join.",
                NetworkVersion.olderServer("0.1.3").getString());
    }

    @Test
    void versionSurvivesTheWire() {
        ByteBuf buffer = Unpooled.buffer();
        NetworkVersionPayload payload = new NetworkVersionPayload(NetworkVersion.CURRENT, "0.1.3");

        NetworkVersionPayload.STREAM_CODEC.encode(buffer, payload);

        assertEquals(payload, NetworkVersionPayload.STREAM_CODEC.decode(buffer));
    }
}
