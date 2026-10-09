package studio.modroll.checks.social.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.HitResult;
import studio.modroll.checks.creation.client.CharacterCreationScreen;
import studio.modroll.checks.sheet.SheetText;
import studio.modroll.checks.social.SocialActionPayload;
import studio.modroll.checks.social.SocialMenu;
import studio.modroll.checks.social.SocialText;
import studio.modroll.checks.text.Names;
import studio.modroll.checks.ui.Area;
import studio.modroll.checks.ui.Frames;

/**
 * The social menu for the entity under the crosshair, on a panel under a banner with the entity's spawn
 * egg: one button per action with its skill and modifier. An action that can't be tried right now is
 * greyed out with the reason below it. Choosing one sends it to the server, which checks it again, rolls
 * and reports the result in the action bar.
 */
public final class SocialScreen extends Screen {

    // G is free in vanilla; players can rebind it.
    public static final KeyMapping KEY = new KeyMapping(
            "key.checks.social", InputConstants.Type.KEYSYM, InputConstants.KEY_G, "key.categories.checks");

    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;
    private static final int LINE = 10;
    /** The panel's frame and padding, plus room for the buttons' bracket caps. */
    private static final int INSET = Frames.FRAME + Frames.PADDING + Frames.CAP_REACH;

    private static final ItemStack NO_EGG = new ItemStack(Items.NAME_TAG);

    private final SocialMenu menu;
    private final List<Reason> reasons = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();
    private Area panel;
    private Area banner;
    private ItemStack icon = NO_EGG;

    private record Reason(Component text, int top) {}

    private SocialScreen(SocialMenu menu, Component title) {
        super(title);
        this.menu = menu;
    }

    /** The entity under the crosshair, which vanilla only picks within interaction reach. */
    public static OptionalInt target() {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.crosshairPickEntity;
        if (entity == null || minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.ENTITY) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(entity.getId());
    }

    public static void open(SocialMenu menu) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(menu.entityId());
        if (entity != null) {
            minecraft.setScreen(new SocialScreen(menu, SocialText.title(menu, Names.of(entity))));
        }
    }

    @Override
    protected void init() {
        reasons.clear();
        buttons.clear();
        icon = egg();
        int contentHeight = Frames.BANNER_HEIGHT
                + GAP
                + menu.options().stream()
                        .mapToInt(option -> BUTTON_HEIGHT + GAP + (option.available() ? 0 : LINE))
                        .sum()
                - GAP;
        int panelWidth = BUTTON_WIDTH + 2 * INSET;
        int panelHeight = contentHeight + 2 * INSET;
        panel = new Area((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
        Area content = panel.inset(INSET);
        banner = new Area(content.left(), content.top(), content.width(), Frames.BANNER_HEIGHT);
        int left = content.left();
        int top = banner.bottom() + GAP;
        for (SocialMenu.Option option : menu.options()) {
            Button button = addRenderableWidget(Button.builder(label(option), pressed -> choose(option))
                    .bounds(left, top, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
            button.active = option.available();
            buttons.add(button);
            top += BUTTON_HEIGHT;
            if (!option.available()) {
                reasons.add(new Reason(SocialText.unavailable(option), top + 1));
                top += LINE;
            }
            top += GAP;
        }
    }

    /**
     * {@code Persuade: Persuasion +3}, plus {@code with advantage} or {@code with disadvantage} when it
     * would roll that way.
     */
    private static Component label(SocialMenu.Option option) {
        Component text = Component.translatable(
                "screen.checks.social.option",
                SocialText.actionName(option.action()),
                SheetText.skillName(option.action().skill()),
                SheetText.signed(option.modifier()));
        return switch (option.mode()) {
            case ADVANTAGE -> Component.translatable("screen.checks.social.advantage", text);
            case DISADVANTAGE -> Component.translatable("screen.checks.social.disadvantage", text);
            case NORMAL -> text;
        };
    }

    private void choose(SocialMenu.Option option) {
        CharacterCreationScreen.sendToServer(new SocialActionPayload(menu.entityId(), option.action()));
        onClose();
    }

    /** The entity's spawn egg, or a name tag for one without an egg. */
    private ItemStack egg() {
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(menu.entityId());
        SpawnEggItem egg = entity == null ? null : SpawnEggItem.byId(entity.getType());
        return egg == null ? NO_EGG : new ItemStack(egg);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        Frames.panel(graphics, panel);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Frames.banner(graphics, font, banner, icon, title);
        buttons.forEach(button -> Frames.caps(graphics, button));
        for (Reason reason : reasons) {
            graphics.drawCenteredString(font, reason.text(), width / 2, reason.top(), Frames.MUTED);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KEY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
