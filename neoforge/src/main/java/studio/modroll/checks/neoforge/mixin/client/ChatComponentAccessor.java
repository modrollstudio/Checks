package studio.modroll.checks.neoforge.mixin.client;

import java.util.List;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Roll lines move up out of the chat's way, which needs the chat's visible lines; vanilla keeps them
 * private and no loader event reports them.
 */
@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {

    @Accessor("trimmedMessages")
    List<GuiMessage.Line> checks$trimmedMessages();
}
