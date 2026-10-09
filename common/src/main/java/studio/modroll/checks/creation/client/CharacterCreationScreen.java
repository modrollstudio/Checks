package studio.modroll.checks.creation.client;

import static studio.modroll.checks.creation.client.CreationUi.BUTTON_HEIGHT;
import static studio.modroll.checks.creation.client.CreationUi.GAP;
import static studio.modroll.checks.creation.client.CreationUi.KEY;
import static studio.modroll.checks.creation.client.CreationUi.VALUE;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import studio.modroll.checks.creation.CreationDraft;
import studio.modroll.checks.creation.CreationOffer;
import studio.modroll.checks.creation.CreationOpenPayload;
import studio.modroll.checks.creation.CreationStep;
import studio.modroll.checks.creation.CreationSubmitPayload;
import studio.modroll.checks.preset.PresetKind;
import studio.modroll.checks.sheet.client.StatSheetScreen;
import studio.modroll.checks.sheet.client.WrappedTooltip;
import studio.modroll.checks.ui.Area;
import studio.modroll.checks.ui.Frames;

/**
 * Character creation, one page per step with Back and Next: species, background and class (while
 * presets are on), then scores, skills and a summary to confirm. The screen only edits a
 * {@link CreationDraft}; the server rolls the dice and has the final say on the submission.
 */
public final class CharacterCreationScreen extends Screen {

    private static final Component TITLE = Component.translatable(KEY + "title");
    private static final Component SKIP = Component.translatable(KEY + "skip");
    private static final Component START_OVER = Component.translatable(KEY + "start_over");
    private static final Component BACK = Component.translatable(KEY + "back");
    private static final Component NEXT = Component.translatable(KEY + "next");
    private static final Component CONFIRM = Component.translatable(KEY + "confirm");
    private static final Component SKIP_TITLE = Component.translatable(KEY + "skip.confirm.title");

    private static final int MAX_SHEET_WIDTH = 500;
    private static final int MAX_BODY_HEIGHT = 260;
    private static final int MARGIN = 12;
    /** The strip with the title and step names, then the body, then the footer buttons. */
    private static final int CHROME_HEIGHT = Frames.STRIP_HEIGHT + GAP + GAP + BUTTON_HEIGHT;

    private static final int TITLE_SPACING = 16;

    private static final int FOOTER_BUTTON_WIDTH = 100;
    private static final int FOOTER_GAP = 2 * (Frames.CAP_REACH + GAP);
    private static final int MAX_STEP_GAP = 16;
    private static final int TOOLTIP_WIDTH = 200;

    private static CreationOffer pending;

    private final CreationDraft draft;
    private final Map<CreationStep, StepView> views = new EnumMap<>(CreationStep.class);
    private final List<Button> footer = new ArrayList<>();
    private int panelLeft;
    private int panelWidth;
    private Area header;
    private int bodyBottom;
    private int mouseX;
    private int mouseY;
    private List<Component> tooltip = List.of();

    private CharacterCreationScreen(CreationOffer offer) {
        super(TITLE);
        this.draft = new CreationDraft(offer);
        views.put(CreationStep.SPECIES, new PresetStepView(this, PresetKind.SPECIES));
        views.put(CreationStep.BACKGROUND, new PresetStepView(this, PresetKind.BACKGROUND));
        views.put(CreationStep.CLASS, new PresetStepView(this, PresetKind.CLASS));
        views.put(CreationStep.SCORES, new ScoresStepView(this));
        views.put(CreationStep.SKILLS, new SkillsStepView(this));
        views.put(CreationStep.CONFIRM, new SummaryStepView(this));
    }

    /** Refreshes an open creation screen, replaces the stat screen, or waits until no other screen is open. */
    public static void open(CreationOffer offer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CharacterCreationScreen screen) {
            screen.draft.update(offer);
            screen.refresh();
        } else if (minecraft.screen == null || minecraft.screen instanceof StatSheetScreen) {
            minecraft.setScreen(new CharacterCreationScreen(offer));
        } else {
            pending = offer;
        }
    }

    /** Called every client tick so an offer that arrived during world loading opens once loading ends. */
    public static void openPending() {
        Minecraft minecraft = Minecraft.getInstance();
        if (pending != null && minecraft.screen == null && minecraft.player != null) {
            minecraft.setScreen(new CharacterCreationScreen(pending));
            pending = null;
        }
    }

    public static void requestOpen() {
        sendToServer(CreationOpenPayload.INSTANCE);
    }

    // The vanilla packet works on both loaders, keeping this screen loader-agnostic.
    public static void sendToServer(CustomPacketPayload payload) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(payload));
        }
    }

    /**
     * One size for every page, so nothing moves on Back or Next: as large as the window allows up to a
     * maximum, centered, with a slim margin above and below on short windows.
     */
    @Override
    protected void init() {
        panelWidth = Math.min(MAX_SHEET_WIDTH, width - 2 * MARGIN);
        panelLeft = (width - panelWidth) / 2;
        int bodyHeight = Math.min(MAX_BODY_HEIGHT, height - 2 * GAP - CHROME_HEIGHT);
        int headerTop = (height - CHROME_HEIGHT - bodyHeight) / 2;
        header = new Area(panelLeft, headerTop, panelWidth, Frames.STRIP_HEIGHT);
        int bodyTop = header.bottom() + GAP;
        bodyBottom = bodyTop + bodyHeight;
        view().init(new Area(panelLeft, bodyTop, panelWidth, bodyHeight));
        addFooterButtons();
    }

    private StepView view() {
        return views.get(draft.step());
    }

    private void addFooterButtons() {
        footer.clear();
        int top = bodyBottom + GAP;
        int left = (width - 3 * FOOTER_BUTTON_WIDTH - 2 * FOOTER_GAP) / 2;
        footer.add(addRenderableWidget(firstFooterButton()
                .bounds(left, top, FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()));
        Button back = addRenderableWidget(Button.builder(BACK, button -> {
                    draft.back();
                    refresh();
                })
                .bounds(left + FOOTER_BUTTON_WIDTH + FOOTER_GAP, top, FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        back.active = !draft.onFirstStep();
        footer.add(back);
        Button forward = addRenderableWidget(Button.builder(draft.onLastStep() ? CONFIRM : NEXT, button -> forward())
                .bounds(left + 2 * (FOOTER_BUTTON_WIDTH + FOOTER_GAP), top, FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        footer.add(forward);
        draft.stepProblem().ifPresent(problem -> {
            forward.active = false;
            forward.setTooltip(Tooltip.create(problem));
        });
    }

    /** Skip, or on the last page Start Over, which reopens the screen with a fresh draft of the same offer. */
    private Button.Builder firstFooterButton() {
        if (draft.onLastStep()) {
            return Button.builder(
                    START_OVER, button -> minecraft.setScreen(new CharacterCreationScreen(draft.offer())));
        }
        return Button.builder(SKIP, button -> confirmSkip());
    }

    private void forward() {
        if (draft.onLastStep()) {
            sendToServer(new CreationSubmitPayload(draft.submission()));
            minecraft.setScreen(null);
            return;
        }
        draft.next();
        refresh();
    }

    /** Skip and Escape both ask once before leaving; declining returns to the unchanged draft. */
    private void confirmSkip() {
        Component message =
                Component.translatable(KEY + "skip.confirm.message", StatSheetScreen.KEY.getTranslatedKeyMessage());
        minecraft.setScreen(new ConfirmScreen(skip -> minecraft.setScreen(skip ? null : this), SKIP_TITLE, message));
    }

    @Override
    public void onClose() {
        confirmSkip();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        Frames.strip(graphics, header);
        view().renderBackground(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        tooltip = List.of();
        renderHeader(graphics);
        footer.forEach(button -> Frames.caps(graphics, button));
        view().render(graphics);
        WrappedTooltip.render(graphics, font, tooltip, TOOLTIP_WIDTH, mouseX, mouseY);
    }

    /**
     * On the header strip, the title at the left and the step names at the right, an even gap apart, the
     * current one highlighted; the title gives way when a window is too narrow for both.
     */
    private void renderHeader(GuiGraphics graphics) {
        List<Component> names = draft.steps().stream()
                .map(step -> (Component) Component.translatable(KEY + "step." + step.id()))
                .toList();
        int textWidth = names.stream().mapToInt(font::width).sum();
        int gaps = Math.max(1, names.size() - 1);
        int inner = header.width() - 2 * Frames.STRIP_INSET;
        int titleSpace = font.width(title) + TITLE_SPACING;
        boolean showTitle = textWidth + gaps * GAP + titleSpace <= inner;
        int stepsSpace = showTitle ? inner - titleSpace : inner;
        int gap = Math.min(MAX_STEP_GAP, (stepsSpace - textWidth) / gaps);
        int top = Frames.stripText(header.top());
        if (showTitle) {
            graphics.drawString(font, title, header.left() + Frames.STRIP_INSET, top, VALUE);
        }
        int x = header.right() - Frames.STRIP_INSET - textWidth - gap * (names.size() - 1);
        int current = draft.steps().indexOf(draft.step());
        for (int i = 0; i < names.size(); i++) {
            graphics.drawString(font, names.get(i), x, top, i == current ? Frames.GOLD_TEXT : Frames.MUTED);
            x += font.width(names.get(i)) + gap;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (view().mouseClicked(mouseX, mouseY)) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            refresh();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        view().mouseScrolled(mouseX, mouseY, scrollY);
        return true;
    }

    Font font() {
        return font;
    }

    public CreationDraft draft() {
        return draft;
    }

    <T extends AbstractWidget> T addControl(T widget) {
        return addRenderableWidget(widget);
    }

    /** Rebuilds the page after the draft changed. */
    public void refresh() {
        rebuildWidgets();
    }

    /** Shows {@code lines} as a tooltip this frame when the mouse is over the area. */
    void hover(int left, int top, int right, int bottom, Supplier<List<Component>> lines) {
        if (over(left, top, right, bottom)) {
            tooltip = lines.get();
        }
    }

    boolean over(int left, int top, int right, int bottom) {
        return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
