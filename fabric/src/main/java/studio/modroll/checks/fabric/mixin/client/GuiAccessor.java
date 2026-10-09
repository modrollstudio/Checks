package studio.modroll.checks.fabric.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Vanilla keeps every action bar message up for a fixed 60 ticks. Setting the timer right after showing a
 * roll line keeps it up longer, and any newer message resets the timer and replaces it, as in vanilla.
 * Roll lines are drawn while their marker is still the message, faded by its timer.
 */
@Mixin(Gui.class)
public interface GuiAccessor {

    @Accessor("overlayMessageTime")
    void checks$setOverlayMessageTime(int ticks);

    @Accessor("overlayMessageTime")
    int checks$overlayMessageTime();

    @Accessor("overlayMessageString")
    Component checks$overlayMessage();
}
