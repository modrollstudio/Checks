package studio.modroll.checks.neoforge.gametest;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.IOUtilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import studio.modroll.checks.Checks;
import studio.modroll.checks.gametest.SavedDataWrites;

/** NeoForge writes SavedData on its IO worker; the shared scenarios wait for it before reading a save back. */
@EventBusSubscriber(modid = Checks.MOD_ID)
public final class GameTestSetup {

    private GameTestSetup() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        SavedDataWrites.awaitWith(IOUtilities::waitUntilIOWorkerComplete);
    }
}
