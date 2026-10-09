package studio.modroll.checks.check.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClientRollLinesTest {

    private static final int LEFT = 50;
    private static final int TOP = 180;
    private static final int HEIGHT = 24;

    @Test
    void withNoChatShowingTheLinesStayPut() {
        assertEquals(TOP, ClientRollLines.clearOf(Optional.empty(), LEFT, TOP, HEIGHT));
    }

    @Test
    void chatBelowTheLinesLeavesThemPut() {
        assertEquals(TOP, ClientRollLines.clearOf(Optional.of(new ChatBox(330, TOP + HEIGHT)), LEFT, TOP, HEIGHT));
    }

    @Test
    void chatBesideTheLinesLeavesThemPut() {
        assertEquals(TOP, ClientRollLines.clearOf(Optional.of(new ChatBox(LEFT, 150)), LEFT, TOP, HEIGHT));
    }

    @Test
    void chatReachingUnderTheLinesLiftsThemClearAboveIt() {
        int chatTop = 190;
        int lifted = ClientRollLines.clearOf(Optional.of(new ChatBox(330, chatTop)), LEFT, TOP, HEIGHT);
        assertEquals(chatTop - 2, lifted + HEIGHT);
    }
}
