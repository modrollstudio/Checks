package studio.modroll.checks.neoforge.uishots;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import studio.modroll.checks.Checks;
import studio.modroll.checks.uishots.UiShots;

@Mod(value = Checks.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeUiShots {

    public NeoForgeUiShots() {
        if (UiShots.directory().isPresent()) {
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> UiShots.tick());
        }
    }
}
